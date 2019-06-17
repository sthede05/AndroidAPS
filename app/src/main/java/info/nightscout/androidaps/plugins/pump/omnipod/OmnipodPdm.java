package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreHistoricalResult;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreSetProfileRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreBolusRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreCancelBolusRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreCancelTempBasalRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreStatusRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreTempBasalRequest;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin;
import info.nightscout.androidaps.utils.SP;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreResult;

public class OmnipodPdm {

    private final Context _context;

    private OmniCoreResult _lastResult;
    private Timer _omniCoreTimer;
    private boolean _connected;
    private boolean _connectionStatusKnown;

    private final Logger _log;

    public OmnipodPdm(Context context)
    {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
    }

    public void OnStart() {
        _lastResult = OmniCoreResult.fromJson(SP.getString(R.string.key_omnicore_last_result, null));
        if (_lastResult == null)
        {
            _lastResult = new OmniCoreResult();
            _lastResult.Success = true;
            _lastResult.BasalSchedule = new BigDecimal[48];
            for (int i = 0; i < 48; i++) {
                _lastResult.BasalSchedule[i] = new BigDecimal(0);
            }
            SP.putString(R.string.key_omnicore_last_result, _lastResult.asJson());
        }
        _connectionStatusKnown = false;
        _omniCoreTimer = new Timer();
        _omniCoreTimer.schedule(new TimerTask() {
            @Override
            public void run() {
            onResultReceived(new OmniCoreStatusRequest().getResult(_lastResult.LastResultId));
            }
        }, 0, 30000);
    }

    public void OnStop() {
        _omniCoreTimer.cancel();
    }

    public boolean IsInitialized() {
        return _lastResult.PodRunning;
    }

    public boolean IsSuspended() {
        return !_lastResult.PodRunning;
    }

    public boolean IsBusy() {
        return false;
    }

    public boolean IsConnected() {
        return _connectionStatusKnown && _connected;
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
        if (result != null) {

            if (!_connected || !_connectionStatusKnown)
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_CONNECTION_STATUS,
                        MainApp.gs(R.string.omnicore_connected), Notification.INFO, 1);
                MainApp.bus().post(new EventNewNotification(notification));
            }
            _connectionStatusKnown = true;
            _connected = true;

            processHistory(result);

            if (_lastResult.PodRunning && !result.PodRunning)
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_CHANGE));
                Notification notification = new Notification(Notification.OMNIPY_POD_CHANGE,
                        String.format(MainApp.gs(R.string.omnipod_pod_state_POD_IDs_has_been_removed), _lastResult.PodId), Notification.NORMAL);       //"Pod with Lot %d and Serial %d has been removed."
                MainApp.bus().post(new EventNewNotification(notification));
            }
            else if (!_lastResult.PodRunning && result.PodRunning)
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, MainApp.gs(R.string.omnipod_pod_state_Pod_is_activated_and_running), Notification.INFO);      //"Pod is activated and running"
                MainApp.bus().post(new EventNewNotification(notification));
            }
            SP.putString(R.string.key_omnicore_last_result, _lastResult.asJson());
            _lastResult = result;
        }
        else
        {
            if (_connected || !_connectionStatusKnown)
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_CONNECTION_STATUS,
                        MainApp.gs(R.string.omnicore_not_connected), Notification.NORMAL, 60);
                MainApp.bus().post(new EventNewNotification(notification));
            }
            _connectionStatusKnown = true;
            _connected = false;
        }
        MainApp.bus().post(new EventOmnipodUpdateGui());
    }

    private void processHistory(OmniCoreResult result) {
        if (_lastResult.ResultId == 0 && !_lastResult.PodRunning)
        {
            TemporaryBasal tempBasalCurrent = TreatmentsPlugin.getPlugin()
                    .getTempBasalFromHistory(System.currentTimeMillis());
            if (_lastResult.PodRunning && tempBasalCurrent != null) {
                TemporaryBasal tempStop = new TemporaryBasal()
                        .date(result.ResultDate)
                        .source(Source.USER);
                TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempStop);
            }
            else if (!_lastResult.PodRunning && tempBasalCurrent == null)
            {
                TemporaryBasal tempBasal = new TemporaryBasal()
                        .date(result.ResultDate)
                        .absolute(0)
                        .duration(24*60*14)
                        .source(Source.USER);
                TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempBasal);
            }
        }

        if (result.ResultsToDate == null)
            return;

        boolean podWasRunning = _lastResult.PodRunning;

        for (JsonElement historicalResultJson : result.ResultsToDate) {

            OmniCoreHistoricalResult historicalResult = new Gson()
                    .fromJson(historicalResultJson.toString(), OmniCoreHistoricalResult.class);

            if (podWasRunning && !historicalResult.PodRunning)
            {
                TemporaryBasal tempBasal = new TemporaryBasal()
                        .date(result.ResultDate)
                        .absolute(0)
                        .duration(24*60*14)
                        .pumpId(historicalResult.ResultId)
                        .source(Source.PUMP);
                TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempBasal);
            }

            if (!podWasRunning && historicalResult.PodRunning) {
                if (TreatmentsPlugin.getPlugin().isTempBasalInProgress()) {
                    TemporaryBasal tempStop = new TemporaryBasal()
                            .date(result.ResultDate)
                            .pumpId(historicalResult.ResultId)
                            .source(Source.PUMP);
                    TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempStop);
                }
            }

            switch(historicalResult.Type)
            {
                case SetBasalSchedule:
                    break;
                case Bolus:
                    break;
                case CancelBolus:
                    break;
                case SetTempBasal:
                    BasalParameters p = new Gson().fromJson(historicalResult.Parameters, BasalParameters.class);

                    double basalRate = p.BasalRate.doubleValue();
                    int minutes = p.Duration.multiply(new BigDecimal(60)).intValue();
                    TemporaryBasal tempBasal = new TemporaryBasal()
                            .date(historicalResult.ResultDate)
                            .absolute(basalRate)
                            .duration(minutes)
                            .pumpId(historicalResult.ResultId)
                            .source(Source.PUMP);
                    TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempBasal);
                    break;
                case CancelTempBasal:
                    TemporaryBasal tempStop = new TemporaryBasal()
                            .date(historicalResult.ResultDate)
                            .pumpId(historicalResult.ResultId)
                            .source(Source.PUMP);
                    TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempStop);
                    break;
                case StartExtendedBolus:
                    break;
                case StopExtendedBolus:
                    break;
                case Status:
                    break;
            }
        }
    }

    public class BasalParameters {
        public BigDecimal BasalRate;
        public BigDecimal Duration;
    }


    private long _lastStatusRequest = 0;
    public void UpdateStatus() {
        if (IsConnected() && IsInitialized() && !IsBusy()) {
            long t0 = System.currentTimeMillis();
            if (t0 - _lastStatusRequest > 60000) {
                _lastStatusRequest = t0;
                onResultReceived(new OmniCoreStatusRequest().getResult(_lastResult.LastResultId));
            }
        }
    }

    public long GetLastUpdated() {
        return _lastResult.ResultDate;
    }

    public String getPodStatusText()
    {
        if (!_lastResult.PodRunning)
            return MainApp.gs(R.string.omnipod_pod_status_Not_yet_running);
        else
            return MainApp.gs(R.string.omnipod_pod_status_Running);
    }

    public boolean IsProfileSet(Profile profile) {
        TimeZone tz = profile.getTimeZone();
        int offset_minutes = (tz.getRawOffset() + tz.getDSTSavings()) / (60 * 1000);
        if (_lastResult.UtcOffset != offset_minutes)
            return false;

        BigDecimal[] scheduleToVerify = getBasalScheduleFromProfile(profile);
        for(int i=0; i<48; i++)
            if (_lastResult.BasalSchedule[i].compareTo(scheduleToVerify[i]) != 0)
                return false;
        return true;
    }


    public double GetBaseBasalRate() {
        long t = System.currentTimeMillis();
        t += _lastResult.UtcOffset;
        Date dt = new Date(t);
        int h = dt.getHours();
        int m = dt.getMinutes();

        int index = h * 2;
        if (m >= 30)
            index++;

        return _lastResult.BasalSchedule[index].doubleValue();
}

    public OmniCoreResult SetNewBasalProfile(Profile profile) {
        OmniCoreResult result = null;
        if (IsConnected()) {
            TimeZone tz = profile.getTimeZone();
            int offset_minutes = (tz.getRawOffset() + tz.getDSTSavings()) / (60 * 1000);
            BigDecimal[] basalSchedule = getBasalScheduleFromProfile(profile);
            result = new OmniCoreSetProfileRequest(basalSchedule, offset_minutes).getResult(_lastResult.LastResultId);
            onResultReceived(result);
        }
        return result;
    }

    public OmniCoreResult Bolus(BigDecimal bolusUnits) {
        OmniCoreResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = new OmniCoreBolusRequest(bolusUnits).getResult(_lastResult.LastResultId);
            this.onResultReceived(r);
        }
        return r;
    }

    public OmniCoreResult CancelBolus() {
        OmniCoreResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = new OmniCoreCancelBolusRequest().getResult(_lastResult.LastResultId);
            this.onResultReceived(r);
        }
        return r;
    }

    public OmniCoreResult SetTempBasal(BigDecimal iuRate, BigDecimal durationHours) {
        OmniCoreResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = new OmniCoreTempBasalRequest(iuRate, durationHours).getResult(_lastResult.LastResultId);
            this.onResultReceived(r);
        }
        return r;
    }

    public OmniCoreResult CancelTempBasal() {
        OmniCoreResult result = null;
        if (IsConnected() && IsInitialized()) {
            result = new OmniCoreCancelTempBasalRequest().getResult(_lastResult.LastResultId);
            this.onResultReceived(result);
        }
        return result;
    }

    public String GetPodId() {
        if (!_lastResult.PodRunning)
            return "NO POD";
        else
            return _lastResult.PodId;
    }

    public String GetStatusShort() {
        if (_lastResult.PodRunning) {
                return "OK";
        }
        return "NO POD";
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
        return _lastResult.ReservoirLevel;
    }

    public int getBatteryLevel() {
        return _lastResult.BatteryLevel;
    }
}
