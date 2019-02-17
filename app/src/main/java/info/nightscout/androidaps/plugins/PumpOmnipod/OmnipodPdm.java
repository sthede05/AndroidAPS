package info.nightscout.androidaps.plugins.PumpOmnipod;

import android.content.Context;
import android.os.SystemClock;

import java.math.BigDecimal;
import java.math.RoundingMode;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.R2;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventPumpStatusChanged;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.PumpVirtual.events.EventVirtualPumpUpdateGui;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;

public class OmnipodPdm {

    private Context _context;

    private Profile _profile;

    private boolean _lastCommandSucceeded = false;
    private long _statusDate;
    private int _activeMinutes;
    private double _insulinDelivered;
    private double _insulinCanceled;
    private int _podProgress;
    private int _bolusState;
    private int _basalState;
    private int _lot;
    private int _tid;
    private boolean _faulted;
    private Double _reservoir;
    private int[] _alarms;

    private long _busyUntil = 0;

    public OmnipodPdm(Context context)
    {
        _context = context;
    }

    public void UpdateStatus() {
    }

    private boolean ParseStatus(String[] r)
    {
        // OK|1543287020|2005|66.950000|0.400000|8|0|2|51|[]|False|lot|tid
        if (r == null)
            return false;

        _statusDate = Long.parseLong(r[1]);
        _activeMinutes = Integer.parseInt(r[2]);
        _insulinDelivered = Double.parseDouble(r[3]);
        _insulinCanceled = Double.parseDouble(r[4]);
        _podProgress = Integer.parseInt(r[5]);
        _bolusState = Integer.parseInt(r[6]);
        _basalState = Integer.parseInt(r[7]);
        _reservoir = Double.parseDouble(r[8]);
        String alarms = r[9];
        if (alarms.equals("[]"))
        {
            _alarms = null;
        }
        else
        {
            String[] alarmSplit = alarms.substring(1, alarms.length() -2).split(",");
            _alarms = new int[alarmSplit.length];
            for (int i=0; i<alarmSplit.length; i++)
            {
                _alarms[i] = Integer.parseInt(alarmSplit[i]);
            }
        }
        _faulted = r[10].equals("True");
        _lot = Integer.parseInt(r[11]);
        _tid = Integer.parseInt(r[12]);
        return r[0].equals("OK");
    }

    public PumpEnactResult SetProfile(Profile profile) {

        _profile = profile;
        return CreateResult(true);
    }

    public boolean VerifyProfile(Profile profile) {
        _profile = profile;
        return true;
    }

    public PumpEnactResult Bolus(DetailedBolusInfo detailedBolusInfo) {
        BigDecimal iuBolus = GetExactInsulinUnits(detailedBolusInfo.insulin);

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = _lastCommandSucceeded;
        r.success = _lastCommandSucceeded;
        if (r.enacted)
        {
            r.success = true;
            r.bolusDelivered = Double.parseDouble(iuBolus.toString());
            _busyUntil = SystemClock.elapsedRealtime() + (long)(r.bolusDelivered * 40000d);
        }
        return r;
    }

    public double CancelBolus() {

        double deliveredBefore = _insulinDelivered;

        if (_lastCommandSucceeded) {
            _busyUntil = 0;
            return _insulinDelivered - deliveredBefore;
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
        return _statusDate * 1000;
    }

    public PumpEnactResult SetTempBasal(Double absoluteRate, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        BigDecimal iuRate = GetExactInsulinUnits(absoluteRate);
        BigDecimal durationHours = GetExactHourUnits(durationInMinutes);

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = _lastCommandSucceeded;
        r.success = _lastCommandSucceeded;
        if (r.enacted)
        {
            r.duration = durationInMinutes;
            r.absolute = Double.parseDouble(iuRate.toString());
        }
        return r;
    }

    public PumpEnactResult CancelTempBasal(boolean enforceNew) {

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = _lastCommandSucceeded;
        r.success = _lastCommandSucceeded;
        if (r.enacted)
        {
            r.isTempCancel = true;
        }
        return r;
    }

    public String GetPodId() {
        return String.format("L%dT%d", _lot, _tid);
    }

    public String GetStatusShort() {
        if (_faulted)
            return "FAULT";
        if (_alarms != null)
            return "ALARM";
        if (_podProgress == 9)
            return "INSULIN <50";
        if (_podProgress == 8)
            return "OK";
        return "UNKNOWN";
    }

    private PumpEnactResult CreateResult(boolean enacted)
    {
        PumpEnactResult r = new PumpEnactResult();
        r.enacted = enacted;
        return r;
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

    public boolean IsInitialized() {
        return true;
    }

    public boolean IsSuspended() {
        return false;
    }

    public boolean IsBusy() {
        return _busyUntil == 0 || SystemClock.elapsedRealtime() >= _busyUntil;
    }

    public boolean IsConnected() {
        return true;
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

    }

    public void StopConnecting()
    {
    }
    public void Disconnect() {
    }
}
