package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.TimeZone;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreBasalScheduleRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreBolusRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreCancelBolusRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreCancelTempBasalRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreStatusRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreTempBasalRequest;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;
import info.nightscout.androidaps.utils.SP;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreResult;

public class OmnipodPdm {

    private final Context _context;
    private OmnipodStatus _lastStatus;

    private final Logger _log;

    public OmnipodPdm(Context context)
    {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
    }

    public void OnStart() {
        _lastStatus = OmnipodStatus.fromJson(SP.getString(R.string.key_omnipod_status, null));
    }

    public void OnStop() {
    }

    public boolean IsInitialized() {
        return true;
//        return (_lastStatus != null &&
//                (_lastStatus.state_progress == 8 || _lastStatus.state_progress == 9));
    }

    public boolean IsSuspended() {
        return false;
    }

    public boolean IsBusy() {
        return false;
    }

    public boolean IsConnected() {
        return true;
    }

    public void Connect() {
    }

    public boolean IsConnecting() {
        return false;
    }

    public void StopConnecting() {
    }

    public void FinishHandshaking() { }

    public boolean IsHandshakeInProgress() {
        return false;
    }

    public void Disconnect() {}

    public synchronized void onResultReceived(OmniCoreResult result) {
        if (result != null && !result.canceled && result.Status != null) {

            if (_lastStatus != null && _lastStatus.PodRunning && !result.Status.PodRunning)
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_CHANGE));
                Notification notification = new Notification(Notification.OMNIPY_POD_CHANGE,
                        String.format(MainApp.gs(R.string.omnipod_pod_state_POD_IDs_has_been_removed), _lastStatus.PodId), Notification.NORMAL);       //"Pod with Lot %d and Serial %d has been removed."
                MainApp.bus().post(new EventNewNotification(notification));
            }

            if (_lastStatus != null && !_lastStatus.PodRunning && result.Status.PodRunning)
            {
                // TODO: log pod activated
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, MainApp.gs(R.string.omnipod_pod_state_Pod_is_activated_and_running), Notification.INFO);      //"Pod is activated and running"
                MainApp.bus().post(new EventNewNotification(notification));
            }

            _lastStatus = result.Status;

            SP.putString(R.string.key_omnipod_status, _lastStatus.asJson());
        }
        MainApp.bus().post(new EventOmnipodUpdateGui());
    }

    private long _lastStatusRequest = 0;
    public void UpdateStatus() {
        if (IsConnected() && IsInitialized() && !IsBusy()) {
            long t0 = System.currentTimeMillis();
            if (t0 - _lastStatusRequest > 60000) {
                _lastStatusRequest = t0;
                onResultReceived(new OmniCoreStatusRequest(0).getResult(getLastId()));
            }
        }
    }

    public long GetLastUpdated() {
        if (_lastStatus != null)
            return  (long)_lastStatus.LastUpdated * 1000;
        else
            return 0;
    }

    public String getPodStatusText()
    {
        if (_lastStatus == null)
            return MainApp.gs(R.string.omnipod_pod_status_Status_unknown);        //"Status unknown"

        return _lastStatus.StatusText;
    }

    public boolean IsProfileSet(Profile profile) {
        if (IsInitialized() && _lastStatus != null) {
            if (_lastStatus.BasalSchedule == null || _lastStatus.BasalSchedule.length == 0)
            {
                return false;
            }
            else
                return verifySchedule(profile);
        }
        return false;
    }

    private boolean verifySchedule(Profile profile)
    {
        TimeZone tz = profile.getTimeZone();
        int offset_minutes = (tz.getRawOffset() + tz.getDSTSavings()) / (60 * 1000);
        if (_lastStatus.UtcOffset != offset_minutes)
            return false;

        BigDecimal[] scheduleToVerify = getBasalScheduleFromProfile(profile);
        for(int i=0; i<48; i++)
            if (_lastStatus.BasalSchedule[i].compareTo(scheduleToVerify[i]) != 0)
                return false;
        return true;
    }

    public double GetBaseBasalRate() {
        if (IsInitialized() && _lastStatus != null && _lastStatus.BasalSchedule != null
                && _lastStatus.BasalSchedule.length > 0) {
            long t = System.currentTimeMillis();
            t += _lastStatus.UtcOffset;
            Date dt = new Date(t);
            int h = dt.getHours();
            int m = dt.getMinutes();

            int index = h * 2;
            if (m >= 30)
                index++;

            return _lastStatus.BasalSchedule[index].doubleValue();
        }
        else
            return 0d;
    }

    public OmniCoreResult SetNewBasalProfile(Profile profile) {
        OmniCoreResult result = null;
        if (IsConnected() && IsInitialized() && _lastStatus != null) {
            TimeZone tz = profile.getTimeZone();
            int offset_minutes = (tz.getRawOffset() + tz.getDSTSavings()) / (60 * 1000);
            BigDecimal[] basalSchedule = getBasalScheduleFromProfile(profile);
            result = new OmniCoreBasalScheduleRequest(basalSchedule, offset_minutes).getResult(getLastId());
            onResultReceived(result);
        }
        return result;
    }

    public OmniCoreResult Bolus(BigDecimal bolusUnits) {
        OmniCoreResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = new OmniCoreBolusRequest(bolusUnits).getResult(getLastId());
            this.onResultReceived(r);
        }
        return r;
    }

    public OmniCoreResult CancelBolus() {
        return new OmniCoreCancelBolusRequest().getResult(getLastId());
    }

    public OmniCoreResult SetTempBasal(BigDecimal iuRate, BigDecimal durationHours) {
        OmniCoreResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = new OmniCoreTempBasalRequest(iuRate, durationHours).getResult(getLastId());
            this.onResultReceived(r);
        }
        return r;
    }

    public OmniCoreResult CancelTempBasal() {
        OmniCoreResult result = null;
        if (IsConnected() && IsInitialized()) {
            result = new OmniCoreCancelTempBasalRequest().getResult(getLastId());
            this.onResultReceived(result);
        }
        return result;
    }

    public String GetPodId() {
        if (_lastStatus == null)
            return "UNKNOWN";
        else
            return _lastStatus.PodId;
    }

    public String GetStatusShort() {
        if (_lastStatus != null) {
            if (_lastStatus.PodRunning)
                return "OK";
            return "NO POD";
        }
        return "UNKNOWN";
    }

    public BigDecimal GetExactInsulinUnits(double iu)
    {
        BigDecimal big20 = new BigDecimal("20");
        // round to 0.05's complements
        return new BigDecimal(iu).multiply(big20).setScale(0, RoundingMode.HALF_UP).setScale(2).divide(big20);
    }

    public BigDecimal GetExactHourUnits(int minutes)
    {
        BigDecimal big30 = new BigDecimal("30");
        return new BigDecimal(minutes).divide(big30).setScale(0, RoundingMode.HALF_UP).setScale(1).divide(new BigDecimal(2));
    }

    public BigDecimal[] getBasalScheduleFromProfile(Profile profile)
    {
        BigDecimal[] basalSchedule = new BigDecimal[48];
        int secondsSinceMidnight = 0;
        for(int i=0; i<48; i++)
        {
            basalSchedule[i] = GetExactInsulinUnits(profile.getBasalTimeFromMidnight(secondsSinceMidnight));
            secondsSinceMidnight += 60*30;
        }
        return basalSchedule;
    }

    public double GetReservoirLevel() {
        if (_lastStatus != null)
            return _lastStatus.ReservoirLevel;
        else
            return -1;
    }

    public OmnipodStatus getStatus() {
        return _lastStatus;
    }

    public int getBatteryLevel() {
        return -1;
    }

    private long getLastId()
    {
        if (_lastStatus == null)
            return 0;
        else
            return _lastStatus.ResultId;
    }
}
