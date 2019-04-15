package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;

import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.events.EventPreferenceChange;
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyCallback;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyRequestType;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipyApiResult;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyRestApi;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipyConfigurationComplete;
import info.nightscout.androidaps.utils.SP;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyResult;

public class OmnipodPdm {

    private final Context _context;

    //private Profile _profile;

    private OmnipyRestApi _restApi;
    private OmnipyResult _lastResult;
    private OmnipodStatus _lastStatus;

    private Profile _pendingProfile;

    private Timer _pingTimer;

    private final Logger _log;

    public OmnipodPdm(Context context)
    {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
    }

    public void OnStart() {
        _restApi = new OmnipyRestApi(_context);
        MainApp.bus().register(this);
        MainApp.bus().register(_restApi);
        _pendingProfile = ProfileFunctions.getInstance().getProfile();
        _lastStatus = OmnipodStatus.fromJson(SP.getString(R.string.key_omnipod_status, null));
        _restApi.StartConfiguring();
    }

    public void OnStop() {
        MainApp.bus().unregister(_restApi);
        MainApp.bus().unregister(this);
        _restApi.StopConfiguring();
        _restApi = null;
    }

    private long _lastPing = 0;
    private void pingOmnipy()
    {
        long t0 = System.currentTimeMillis();
        if (t0 - _lastPing > 5000) {
            _lastPing = t0;
            _restApi.Ping(result -> {
                if (!result.canceled && !result.success) {
                    handleDisconnect();
                }
            });
        }
    }

    private void handleDisconnect()
    {
        if (_pingTimer != null) {
            _pingTimer.cancel();
            _pingTimer = null;
        }
        MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
        MainApp.bus().post(new Notification(Notification.OMNIPY_CONNECTION_STATUS, "Disconnected from omnipy", Notification.NORMAL));
        _restApi.StartConfiguring();
    }

    public boolean IsInitialized() {
        return (_lastStatus != null &&
                (_lastStatus.state_progress == 8 || _lastStatus.state_progress == 9));
    }

    public boolean IsSuspended() {
        return false;
    }

    public boolean IsBusy() {
        _lastResult = _restApi.IsBusy();

        if (_lastResult.success)
        {
            return _lastResult.response.get("busy").getAsBoolean();
        }
        else
            return true;
    }

    public boolean IsConnected() {
        return _restApi.isConfigured() && _restApi.isConnectable()
                && _restApi.isAuthenticated() && IsInitialized();
    }

    public void Connect() {
    }

    public OmnipyRestApi GetRestApi()
    {
        return _restApi;
    }

    public boolean IsConnecting() {
        if (!_restApi.isConfigured())
            return true;
        else
        {
            if (!_restApi.isConnectable())
                return true;
            if (!_restApi.isAuthenticated())
                return true;
            return false;
        }
    }

    public void StopConnecting() {
    }

    public void FinishHandshaking() { }

    public boolean IsHandshakeInProgress() {
        return false;
    }

    public void Disconnect() {}


    @Subscribe
    public synchronized void onResultReceived(final EventOmnipyApiResult or) {
        OmnipyResult result = or.getResult();
        onResultReceived(result);
    }

    @Subscribe
    public void onStatusEvent(final EventPreferenceChange s) {
        if (s.isChanged(R.string.key_omnipy_autodetect_host)
                || s.isChanged(R.string.key_omnipy_host)
                ||s.isChanged(R.string.key_omnipy_password))
        {
            _restApi.StartConfiguring();
        }

        if (s.isChanged(R.string.key_omnipod_limit_max_temp)) {
        }
        if (s.isChanged(R.string.key_omnipod_limit_max_bolus)) {
        }

        if (s.isChanged(R.string.key_omnipod_remind_low_reservoir_units)) {
        }
        if (s.isChanged(R.string.key_omnipod_remind_pod_expiry_minutes)) {
        }

        if (s.isChanged(R.string.key_omnipod_remind_basal_change)) {
        }
        if (s.isChanged(R.string.key_omnipod_remind_bolus_start)) {
        }
        if (s.isChanged(R.string.key_omnipod_remind_bolus_cancel)) {
        }
        if (s.isChanged(R.string.key_omnipod_remind_temp_start)) {
        }
        if (s.isChanged(R.string.key_omnipod_remind_temp_running)) {
        }
        if (s.isChanged(R.string.key_omnipod_remind_temp_cancel)) {
        }
    }

    public synchronized void onResultReceived(OmnipyResult result) {
        if (result != null && !result.canceled && result.status != null) {
            _lastResult = result;

            if (_lastStatus != null && (_lastStatus.radio_address != result.status.radio_address ||
                    _lastStatus.id_lot != result.status.id_lot || _lastStatus.id_t != result.status.id_t))
            {
                _lastStatus = null;
            }

            if (result.status.state_progress == 0 && (_lastStatus == null || _lastStatus.state_progress != 0))
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "No pod registered", Notification.NORMAL);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
            else if (result.status.state_faulted && (_lastStatus == null || !_lastStatus.state_faulted))
            {
                // TODO: log fault event
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS,
                        String.format("Pod faulted with error: %d", result.status.fault_event), Notification.URGENT);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
            else if (result.status.state_progress < 8 && result.status.state_progress > 0 &&
                    (_lastStatus == null || _lastStatus.state_progress == 0))
            {
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "Pod in activation progress", Notification.NORMAL);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
            else if (result.status.state_progress == 8 && (_lastStatus == null || _lastStatus.state_progress < 8))
            {
                // TODO: log pod activated
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "Pod is activated and running", Notification.NORMAL);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
            else if (result.status.state_progress == 9 && (_lastStatus == null || _lastStatus.state_progress == 8))
            {
                // TODO: log reservoir event
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "Pod running with low reservoir (less than 50U)", Notification.NORMAL);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
            else if (result.status.state_progress > 9 && (_lastStatus == null || _lastStatus.state_progress <= 9))
            {
                // TODO: log pod stopped
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "Pod stopped", Notification.INFO);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }

            if ((result.status.state_progress == 8 || result.status.state_progress == 9) && result.status.state_alert != 0
                && (_lastStatus == null || _lastStatus.state_alert != result.status.state_alert))
            {
                String alertText = "";
                for (String alert : getAlerts(result.status.state_alert)) {
                    if (alertText.length() == 0)
                        alertText += alert;
                    else
                        alertText += ", " + alert;
                }
                MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "Pod alert: " + alertText, Notification.INFO);
                MainApp.bus().post(new EventNewNotification(notification));
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }

            _lastStatus = result.status;
            SP.putString(R.string.key_omnipod_status, _lastStatus.asJson());
            OmnipodStatus.fromJson(SP.getString(R.string.key_omnipod_status, null));
        }
    }

    @Subscribe
    public void onConfigurationComplete(final EventOmnipyConfigurationComplete confResult) {
        if (_pingTimer != null) {
            _pingTimer.cancel();
            _pingTimer = null;
        }
        MainApp.bus().post(new EventOmnipodUpdateGui());
        String errorMessage;
        if (!confResult.isConnectable)
        {
            if (confResult.isDiscovered)
            {
                errorMessage = "Omnipy located at network address " + confResult.hostName +
                        " but connection cannot be established";
            }
            else
            {
                errorMessage = "Omnipy connection cannot be established at the configured address: " + confResult.hostName
                        + " Please verify the address in configuration.";
            }
            MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
            Notification notification = new Notification(Notification.OMNIPY_CONNECTION_STATUS, errorMessage, Notification.NORMAL);
            MainApp.bus().post(new EventNewNotification(notification));

            _pingTimer = new Timer();
            _pingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    _restApi.StartConfiguring();
                }
            }, 30000);

        }
        else if (!confResult.isAuthenticated)
        {
            errorMessage = "Omnipy connection established at address " + confResult.hostName +
                    " but authentication failed. Please verify your password in settings.";
            MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
            Notification notification = new Notification(Notification.OMNIPY_CONNECTION_STATUS, errorMessage, Notification.NORMAL);
            MainApp.bus().post(new EventNewNotification(notification));

            _pingTimer = new Timer();
            _pingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    _restApi.StartConfiguring();
                }
            }, 30000);
        }
        else
        {
            MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
            Notification notification = new Notification(Notification.OMNIPY_CONNECTION_STATUS,
                    "Connected to omnipy running at " + confResult.hostName, Notification.INFO, 1);
            MainApp.bus().post(new EventNewNotification(notification));

            UpdateStatus();

            _pingTimer = new Timer();
            _pingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    pingOmnipy();
                }
            }, 30000, 30000);
        }

    }

    private long _lastStatusRequest = 0;
    public void UpdateStatus() {
        if (IsConnected() && IsInitialized()) {
            long t0 = System.currentTimeMillis();
            if (t0 - _lastStatusRequest > 10000) {
                _lastStatusRequest = t0;
                _restApi.UpdateStatus(null);
            }
        }
    }

    public long GetLastUpdated() {
        if (_lastStatus != null)
            return  (long)_lastStatus.state_last_updated * 1000;
        else
            return 0;
    }

    public long GetLastResultDate() {
        if (_lastResult != null)
            return  (long)_lastResult.datetime * 1000;
        else
            return System.currentTimeMillis();
    }

    public ArrayList<String> getAlerts(int alertMask)
    {
        ArrayList<String> alerts = new ArrayList<>();
        if ((alertMask & 0x01) > 0)MainApp.bus().post(new EventDismissNotification(Notification.OMNIPY_CONNECTION_STATUS));
            alerts.add("Auto-off");
        if ((alertMask & 0x02) > 0)
            alerts.add("Unknown");
        if ((alertMask & 0x04) > 0)
            alerts.add("Pod expiring soon");
        if ((alertMask & 0x08) > 0)
            alerts.add("Replace pod soon");
        if ((alertMask & 0x10) > 0)
            alerts.add("Low reservoir");
        if ((alertMask & 0x20) > 0)
            alerts.add("Insulin Suspended");
        if ((alertMask & 0x40) > 0)
            alerts.add("End of insulin suspend");
        if ((alertMask & 0x80) > 0)
            alerts.add("Pod expired");
        return alerts;
    }

    public String getPodStatusText()
    {
        if (_lastStatus == null)
            return "Status unknown";

        StringBuilder sb = new StringBuilder();
        sb.append("Lot: ").append(_lastStatus.id_lot).append(" TID: ").append(_lastStatus.id_t)
                .append(" Radio address: ")
                .append(String.format("%08X", _lastStatus.radio_address));

        sb.append("\nStatus: ");
        if (_lastStatus.state_progress < 8)
        {
            sb.append("Not yet running");
        }
        else if (_lastStatus.state_progress == 8)
        {
            sb.append("Running");
        }
        else if (_lastStatus.state_progress == 9)
        {
            sb.append("Running with low insulin (<50U)");
        }
        else
        {
            sb.append("Inactive");
        }

        if (_lastStatus.state_faulted)
        {
            sb.append("\nPOD FAULTED");
        }

        int minutes = _lastStatus.state_active_minutes;
        int days = minutes / 60 / 24;
        minutes -= days * 60 * 24;
        int hours = minutes / 60;
        minutes -= hours * 60;

        sb.append("\nTotal insulin delivered: ").append(String.format("%3.2f",
                _lastStatus.insulin_delivered)).append("U");
        sb.append("\nReservoir: ");
        if (_lastStatus.insulin_reservoir > 50)
            sb.append("more than 50U");
        else
            sb.append(String.format("%2.2f", _lastStatus.insulin_reservoir)).append("U");

        sb.append("\nPod age: ").append(String.format("%dd%dh%dm", days, hours, minutes));

        Date date = new Date((long)_lastStatus.state_last_updated * 1000);
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(_context);
        sb.append("\nStatus updated: ").append(dateFormat.format(date));

        return sb.toString();
    }

    public String getConnectionStatusText()
    {
        StringBuilder sb = new StringBuilder();
        if (!_restApi.isConfigured())
        {
            sb.append("Address configuration in progress..");
        }
        else
        {
            if (_restApi.isDiscovered())
                sb.append("Discovered Address: ").append(_restApi.getHost());
            else
                sb.append("Configured Address: ").append(_restApi.getHost());

            if (!_restApi.isConnectable())
                sb.append("\nConnectable: No");
            else
            {
                sb.append("\nConnectable: Yes");
                if (_restApi.isAuthenticated())
                    sb.append("\nAuthentication: Verified");
                else
                    sb.append("\nAuthentication: Failed");
                if (_lastResult != null && _lastResult.api != null)
                {
                    sb.append("\nOmnipy API Version: v" + _lastResult.api.version_major
                        + "." + _lastResult.api.version_minor);
                }
            }
        }

        sb.append("\nLast successful connection: ");
        long tx = _restApi.getLastSuccessfulConnection();
        if (tx == 0)
        {
            sb.append("N/A");
        }
        else
        {
            Date date = new Date(tx);
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(_context);
            sb.append(dateFormat.format(date));
        }

        return sb.toString();
    }

    public boolean IsProfileSet(Profile profile) {
        if (IsInitialized()) {
            if (_lastStatus.var_basal_schedule == null || _lastStatus.var_basal_schedule.length == 0)
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
        int offset_minutes = profile.getTimeZone().getRawOffset() / (60 * 1000);
        if (_lastStatus.var_utc_offset != offset_minutes)
            return false;

        BigDecimal[] scheduleToVerify = getBasalScheduleFromProfile(profile);
        for(int i=0; i<48; i++)
            if (_lastStatus.var_basal_schedule[i].compareTo(scheduleToVerify[i]) != 0)
                return false;
        return true;
    }

    public double GetBaseBasalRate() {
        if (IsInitialized() && _lastStatus.var_basal_schedule != null
                && _lastStatus.var_basal_schedule.length > 0) {
            long t = System.currentTimeMillis();
            t += _lastStatus.var_utc_offset;
            Date dt = new Date(t);
            int h = dt.getHours();
            int m = dt.getMinutes();

            int index = h * 2;
            if (m >= 30)
                index++;

            return _lastStatus.var_basal_schedule[index].doubleValue();
        }
        else
            return -1d;
    }

    public OmnipyResult SetNewBasalProfile(Profile profile) {
        OmnipyResult result = null;
        if (IsConnected() && IsInitialized() && _lastStatus != null) {
            int offset_minutes = profile.getTimeZone().getRawOffset() / (60 * 1000);
            BigDecimal[] basalSchedule = getBasalScheduleFromProfile(profile);
            result = _restApi.setBasalSchedule(basalSchedule, offset_minutes, null).waitForResult();
            this.onResultReceived(result);
        }
        return result;
    }

    public OmnipyResult Bolus(BigDecimal bolusUnits) {
        OmnipyResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = _restApi.Bolus(bolusUnits, null).waitForResult();
            this.onResultReceived(r);
        }
        return r;
    }

    public void CancelBolus(OmnipyCallback cb) {
        if (IsConnected() && IsInitialized()) {
            _restApi.CancelBolus(cb);
        }
    }

    public OmnipyResult SetTempBasal(BigDecimal iuRate, BigDecimal durationHours) {
        OmnipyResult r = null;
        if (IsConnected() && IsInitialized()) {
            r = _restApi.SetTempBasal(iuRate, durationHours, null).waitForResult();
            this.onResultReceived(r);
        }
        return r;
    }

    public OmnipyResult CancelTempBasal() {
        OmnipyResult result = null;
        if (IsConnected() && IsInitialized()) {
            result = _restApi.CancelTempBasal(null).waitForResult();
            this.onResultReceived(result);
        }
        return result;
    }

    public String GetPodId() {
        return String.format("L%dT%d", _lastStatus.id_lot, _lastStatus.id_t);
    }

    public String GetStatusShort() {
        if (_lastStatus != null) {
            if (_lastStatus.state_faulted)
                return "FAULT";
            if (_lastStatus.state_progress == 9)
                return "IU<50";
            if (_lastStatus.state_progress == 8)
                return "OK";
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

    private BigDecimal[] getBasalScheduleFromProfile(Profile profile)
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
            return _lastStatus.insulin_reservoir;
        else
            return -1;
    }
}
