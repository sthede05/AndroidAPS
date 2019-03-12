package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;
import android.os.SystemClock;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.Date;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.configBuilder.ProfileFunctions;
import info.nightscout.androidaps.utils.SP;

import static info.nightscout.androidaps.R2.string.result;

public class OmnipodPdm {

    private Context _context;

    private Profile _profile;

    private OmnipyApiSecret _omnipyApiSecretCached;
    private OmnipyNetworkDiscovery _omnipyNetworkDiscovery;

    private OmnipyRestApi _omnipyRestApiCached;
    private OmnipyRestApiResult _lastResult;
    private OmnipodStatus _lastStatus;

    private boolean _initialized;
    private boolean _suspended;

    private Logger _log;

    public OmnipodPdm(Context context)
    {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
        _omnipyNetworkDiscovery = new OmnipyNetworkDiscovery(_context);
        //_lastStatus = OmnipodStatus.fromJson(SP.getString(R.string.key_omnipod_status, null));
    }

    private boolean processResult(OmnipyRestApiResult result)
    {
        _lastResult = result;
        if (result.status != null) {
            _lastStatus = result.status;
            //SP.putString(R.string.key_omnipod_status, result.status.asJson());
            MainApp.bus().post(new EventOmnipodUpdateGui());
        }
        return result.success;
    }

    public void UpdateStatus() {
        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            processResult(rest.Status());
        }
    }

    public void InvalidateApiSecret()
    {
        _omnipyApiSecretCached = null;
        _omnipyRestApiCached = null;
    }

    public void InvalidateOmnipyHost()
    {
        _omnipyNetworkDiscovery.ClearKnownAddress();
        _omnipyRestApiCached = null;
    }

    private OmnipyRestApi getRestApi()
    {
        if (_omnipyRestApiCached == null) {
            String hostName = getOmnipyHost();
            OmnipyApiSecret apiSecret = getApiSecret();
            if (hostName != null && apiSecret != null) {
                _omnipyRestApiCached = new OmnipyRestApi("http://" + hostName + ":4444",
                        apiSecret);
            }
        } else
        {
            int count = _omnipyRestApiCached.getConnectionTimeOutCount();
            if (count > 0)
            {
                long timeDelta = _omnipyRestApiCached.getLastSuccessfulConnection() -
                        SystemClock.elapsedRealtime();

                if (timeDelta > 10*60*1000 || count > 3) {
                    InvalidateApiSecret();
                    InvalidateOmnipyHost();
                }
            }
        }

        return _omnipyRestApiCached;
    }

    public String getPodStatusText()
    {
        if (_lastStatus == null)
            return "Status unknown";

        StringBuilder sb = new StringBuilder();
        sb.append("Lot: ").append(_lastStatus.id_lot).append(" TID: ").append(_lastStatus.id_t)
                .append(" Radio address: ")
                .append(String.format("%08X", _lastStatus.radio_address));

        sb.append("\n\nStatus: ");
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
        else if (_lastStatus.state_progress > 9)
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

        sb.append("\n\nTotal insulin delivered: ").append(String.format("%3.2f",
                _lastStatus.insulin_delivered)).append("U");
        sb.append("\nReservoir: ");
        if (_lastStatus.insulin_reservoir > 50)
            sb.append("more than 50U");
        else
            sb.append(String.format("%2.2f", _lastStatus.insulin_reservoir)).append("U");

        sb.append("\n\nPod age: ").append(String.format("%dd%dh%dm", days, hours, minutes));

        Date date = new Date((long)_lastStatus.state_last_updated * 1000);
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(_context);
        sb.append("\n\nStatus updated: ").append(dateFormat.format(date));

        return sb.toString();
    }

    public String getConnectionStatusText()
    {
        StringBuilder sb = new StringBuilder();

        if (SP.getBoolean(R.string.key_omnipy_autodetect_host, true)) {
            String result = _omnipyNetworkDiscovery.GetLastKnownAddress();
            if (result != null)
                sb.append("Discovered Host Address: ").append(result);
            else
                sb.append("Host address autodiscovery in progress..");
        }
        else
        {
            String result = SP.getString(R.string.key_omnipy_host, null);
            if (result != null && result.length() == 0)
                sb.append("Host address not configured in pump preferences");
            else
                sb.append("Configured address: ").append(result);
        }

        if (_omnipyRestApiCached != null)
        {
            sb.append("\nLast successful connection: ");
            long tx = _omnipyRestApiCached.getLastSuccessfulConnection();
            if (tx == 0)
            {
                sb.append("Never");
            }
            else
            {
                long delta = SystemClock.elapsedRealtime() - tx;
                Date date = new Date(System.currentTimeMillis() - delta);
                DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(_context);
                sb.append(dateFormat.format(date));
            }
        }

        return sb.toString();
    }

    private String getOmnipyHost()
    {
        String omnipyHost;
        if (SP.getBoolean(R.string.key_omnipy_autodetect_host, true)) {
            omnipyHost = _omnipyNetworkDiscovery.GetLastKnownAddress();
            if (omnipyHost == null)
                _omnipyNetworkDiscovery.RunDiscovery();
        }
        else
        {
            omnipyHost = SP.getString(R.string.key_omnipy_host, null);
            if (omnipyHost != null && omnipyHost.length() == 0)
                return null;
        }

        return omnipyHost;
    }

    private OmnipyApiSecret getApiSecret()
    {
        if (_omnipyApiSecretCached == null)
        {
            String secret = SP.getString(R.string.key_omnipy_password, "");
            if (secret == null || secret.length() == 0)
                return null;

            _omnipyApiSecretCached = OmnipyApiSecret.fromPassphrase(
                    SP.getString(R.string.key_omnipy_password, ""));
        }
        return _omnipyApiSecretCached;
    }

    public PumpEnactResult SetProfile(Profile profile) {
        // fake set profile
        _profile = profile;
        PumpEnactResult r = new PumpEnactResult();
        r.enacted = true;
        r.success = true;
        return r;
    }

    public boolean VerifyProfile(Profile profile) {
        // fake verify
        _profile = profile;
        return true;
    }

    public PumpEnactResult Bolus(DetailedBolusInfo detailedBolusInfo) {
        BigDecimal iuBolus = GetExactInsulinUnits(detailedBolusInfo.insulin);

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = false;
        r.success = false;

        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            r.enacted = true;
            r.success = processResult(rest.Bolus(iuBolus));
            if (r.success)
               r.bolusDelivered = Double.parseDouble(iuBolus.toString());
        }
        return r;
    }

    public double CancelBolus() {
        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastResult = rest.CancelBolus();
            return _lastResult.status.insulin_canceled;
        }
        else
        {
            return -1d;
        }
    }

    public double GetBasalRate() {
        if (_profile == null)
        {
            _profile = ProfileFunctions.getInstance().getProfile();
        }

        if (_profile == null)
            return 0d;
        else
            return _profile.getBasal();
    }

    public long GetLastUpdated() {
        if (_lastStatus != null)
            return  (long)_lastStatus.state_last_updated * 1000;
        else
            return 0;
    }

    public PumpEnactResult SetTempBasal(Double absoluteRate, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        BigDecimal iuRate = GetExactInsulinUnits(absoluteRate);
        BigDecimal durationHours = GetExactHourUnits(durationInMinutes);

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = false;
        r.success = false;

        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastResult = rest.SetTempBasal(iuRate, durationHours);
            r.success = _lastResult.success;
            r.enacted = true;
            r.duration = durationInMinutes;
            r.absolute = Double.parseDouble(iuRate.toString());
        }

        return r;
    }

    public PumpEnactResult CancelTempBasal(boolean enforceNew) {

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = false;
        r.success = false;

        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastResult = rest.CancelTempBasal();
            r.success = _lastResult.success;
            r.enacted = true;
            r.isTempCancel = true;
        }
        return r;
    }

    public String GetPodId() {
        return String.format("L%dT%d", _lastStatus.id_lot, _lastStatus.id_t);
    }

    public String GetStatusShort() {
        if (_lastStatus.state_faulted)
            return "FAULT";
        if (_lastStatus.state_progress == 9)
            return "IU<50";
        if (_lastStatus.state_progress == 8)
            return "OK";
        return "UNKNOWN";
    }

    private BigDecimal GetExactInsulinUnits(double iu)
    {
        BigDecimal big20 = new BigDecimal("20");
        // round to 0.05's complements
        return new BigDecimal(iu).multiply(big20).setScale(0, RoundingMode.HALF_UP).setScale(2).divide(big20);
    }

    private BigDecimal GetExactHourUnits(int minutes)
    {
        BigDecimal big30 = new BigDecimal("30");
        return new BigDecimal(minutes).divide(big30).setScale(0, RoundingMode.HALF_UP).setScale(1).divide(new BigDecimal(2));
    }

    public int[] GetOmnipyVersion() {
        int[] version = null;
        OmnipyRestApi rest = getRestApi();

        if (rest != null) {
            _lastResult = rest.Ping();
            version = new int[2];
            version[0] = _lastResult.api.get("version_major").getAsInt();
            version[1] = _lastResult.api.get("version_minor").getAsInt();
        }
        return version;
    }

    public void OnStart() {

    }

    public void OnStop() {
    }

    public boolean IsInitialized() {
        OmnipyRestApi rest = getRestApi();
        return rest != null;
    }

    public boolean IsSuspended() {
        return false;
    }

    public boolean AcceptsCommands() {
        OmnipyRestApi rest = getRestApi();
        if (rest == null)
            return false;
        _lastResult = rest.IsBusy();
        if (!_lastResult.success)
            return false;
        else
            return _lastResult.response.get("busy").getAsBoolean();
    }

    public boolean IsConnected() {
        OmnipyRestApi rest = getRestApi();
        if (rest == null)
            return false;

        if ( SystemClock.elapsedRealtime() - rest.getLastSuccessfulConnection() < 30000)
            return true;

        _lastResult = rest.Ping();
        return _lastResult.success;
    }

    public boolean IsConnecting() {
        return false;
    }

    public boolean IsHandshakeInProgress() {
        return false;
    }

    public void FinishHandshaking() {
    }

    public void Connect() {
        IsConnected();
    }

    public void StopConnecting()
    {
    }

    public void Disconnect() {
        _omnipyRestApiCached = null;
    }
}
