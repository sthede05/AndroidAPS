package info.nightscout.androidaps.plugins.PumpOmnipod;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.BuildConfig;
import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.db.ExtendedBolus;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventPumpStatusChanged;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.interfaces.PumpDescription;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.Overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.Overview.events.EventOverviewBolusProgress;
import info.nightscout.androidaps.plugins.Overview.notifications.Notification;
import info.nightscout.androidaps.plugins.PumpCommon.defs.PumpType;
import info.nightscout.androidaps.plugins.PumpVirtual.events.EventVirtualPumpUpdateGui;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;
import info.nightscout.utils.DateUtil;
import info.nightscout.utils.SP;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmnipodPlugin extends PluginBase implements PumpInterface {


    private Logger log = LoggerFactory.getLogger(L.PUMP);

    private static OmnipodPlugin instance = null;
    private PumpDescription pumpDescription = new PumpDescription();
    private final OmnipodPdm _pdm;
    private DetailedBolusInfo _runningBolusInfo;

    public static OmnipodPlugin getPlugin() {
        if (instance == null)
            instance = new OmnipodPlugin();
        return instance;
    }
    public OmnipodPlugin() {
        super(new PluginDescription()
                .mainType(PluginType.PUMP)
                .fragmentClass(OmnipodFragment.class.getName())
                .pluginName(R.string.omnipod)
                .shortName(R.string.omnipod_shortname)
                .preferencesId(R.xml.pref_omnipod)
                .neverVisible(Config.NSCLIENT)
                .description(R.string.omnipod_description)
        );

        pumpDescription.setPumpDescription(PumpType.Insulet_Omnipod);
        log.debug("omnipod plug initialized");
        _pdm = new OmnipodPdm(MainApp.instance().getApplicationContext());
    }

    @Override
    protected void onStart() {
        super.onStart();
        MainApp.bus().register(this);
    }

    @Override
    protected void onStop() {
        MainApp.bus().unregister(this);
    }

    @Override
    public boolean isInitialized() {
        //log.debug("isInitialized()");
        //return _pdm.IsInitialized();
        return true;
    }

    @Override
    public boolean isSuspended() {
        //log.debug("isSuspended()");
        //return _pdm.IsSuspended();
        return false;
    }

    @Override
    public boolean isBusy() {
        log.debug("isBusy()");
        return _pdm.IsBusy();
    }

    @Override
    public boolean isConnected() {
        //log.debug("isConnected()");
        //return _pdm.IsConnected();
        return true;
    }

    @Override
    public boolean isConnecting() {
        //log.debug("isConnecting()");
        //return _pdm.IsConnecting();
        return false;
    }

    @Override
    public boolean isHandshakeInProgress() {
        //log.debug("isHandshakeInProgress()");
        //return _pdm.IsHandshakeInProgress();
        return false;
    }

    @Override
    public void finishHandshaking() {
        //log.debug("finishHandshaking()");
        //_pdm.FinishHandshaking();
    }

    @Override
    public void connect(String reason) {
        log.debug("omnipod plugin connect()");
        //_pdm.Connect();
    }

    @Override
    public void disconnect(String reason) {
        log.debug("omnipod plugin disconnect() reason: " + reason);
        //_pdm.Disconnect();
    }

    @Override
    public void stopConnecting() {
        log.debug("omnipod plugin stopConnecting()");
        //_pdm.StopConnecting();
    }

    @Override
    public void getPumpStatus() {
        log.debug("omnipod plugin getPumpStatus()");
        _pdm.UpdateStatus();
    }

    @Override
    public PumpEnactResult setNewBasalProfile(Profile profile) {
        log.debug("omnipod plugin setNewBasalProfile()");

        // TODO: This needs to be fixed together with pod initialization
        PumpEnactResult ret = _pdm.SetProfile(profile);
        ret.success = false;
        // Notification notification = new Notification(Notification.PROFILE_SET_OK, MainApp.gs(R.string.profile_set_ok), Notification.INFO, 60);
        // MainApp.bus().post(new EventNewNotification(notification));
        return ret;
    }

    @Override
    public boolean isThisProfileSet(Profile profile) {
        log.debug("omnipod plugin isThisProfileSet()");
        return _pdm.VerifyProfile(profile);
    }

    @Override
    public long lastDataTime()
    {
        return _pdm.GetLastUpdated();
    }

    @Override
    public double getBaseBasalRate() {
        log.debug("omnipod plugin GetBaseBasalRate()");
        return _pdm.GetBasalRate();
    }

    @Override
    public PumpEnactResult deliverTreatment(DetailedBolusInfo detailedBolusInfo) {

        log.debug("omnipod plugin DeliverTreatment()");
        PumpEnactResult result = _pdm.Bolus(detailedBolusInfo);

        if (!result.enacted) {
            result.bolusDelivered = 0;
            return result;
        }

        result.carbsDelivered = detailedBolusInfo.carbs;
        result.comment = "";

        _runningBolusInfo = detailedBolusInfo;
        Double delivering = 0d;

        while (delivering < detailedBolusInfo.insulin) {
            EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();
            bolusingEvent.status = String.format(MainApp.gs(R.string.bolusdelivering), delivering);
            bolusingEvent.percent = Math.min((int) (delivering / detailedBolusInfo.insulin * 100), 100);
            MainApp.bus().post(bolusingEvent);
            delivering += 0.05d;
            SystemClock.sleep(2000);
        }
        SystemClock.sleep(200);
        EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();
        bolusingEvent.status = String.format(MainApp.gs(R.string.bolusdelivered), detailedBolusInfo.insulin);
        bolusingEvent.percent = 100;
        MainApp.bus().post(bolusingEvent);
        SystemClock.sleep(1000);
        if (L.isEnabled(L.PUMPCOMM))
            log.debug("Delivering treatment insulin: " + detailedBolusInfo.insulin + "U carbs: " + detailedBolusInfo.carbs + "g " + result);
        MainApp.bus().post(new EventVirtualPumpUpdateGui());
        detailedBolusInfo.deliverAt = _pdm.GetLastUpdated();
        TreatmentsPlugin.getPlugin().addToHistoryTreatment(detailedBolusInfo, false);
        return result;
    }

    @Override
    public void stopBolusDelivering() {
        log.debug("omnipod plugin StopBolusDelivering()");
        if (_runningBolusInfo == null)
            return;

        Double delivered = -1d;
        EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();
        bolusingEvent.status = String.format(MainApp.gs(R.string.bolusstopping));
        MainApp.bus().post(bolusingEvent);
        while (delivered < 0d || !_pdm.IsBusy())
        {
            delivered = _pdm.CancelBolus();
        }

        Double supposedToDeliver = _runningBolusInfo.insulin;
        if (delivered < 0d)
        {
            bolusingEvent.status = String.format("Couldn't stop bolus in time, delivered: %f.2u", delivered);
        }
        else
        {
            bolusingEvent.status = String.format(MainApp.gs(R.string.bolusstopped));
            _runningBolusInfo.insulin = delivered;
        }
        MainApp.bus().post(bolusingEvent);
        SystemClock.sleep(1000);
        _runningBolusInfo.notes = String.format("Delivery stopped at %f.2u. Original bolus request was: %f.2u", delivered, supposedToDeliver);
        TreatmentsPlugin.getPlugin().addToHistoryTreatment(_runningBolusInfo, false);
    }

    @Override
    public PumpEnactResult setTempBasalAbsolute(Double absoluteRate, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        log.debug("omnipod plugin SetTempBasalAbsolute()");
        PumpEnactResult result = _pdm.SetTempBasal(absoluteRate, durationInMinutes, profile, enforceNew);
        if (result.enacted) {
            result.absolute = absoluteRate;
            result.duration = durationInMinutes;
            result.comment = "";

            TemporaryBasal tempBasal = new TemporaryBasal()
                    .date(_pdm.GetLastUpdated())
                    .absolute(result.absolute)
                    .duration(result.duration)
                    .source(Source.USER);
            TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempBasal);
            if (L.isEnabled(L.PUMPCOMM))
                log.debug("Setting temp basal absolute: " + result);
        }
        MainApp.bus().post(new EventVirtualPumpUpdateGui());
        return result;
    }

    @Override
    public PumpEnactResult setTempBasalPercent(Integer percent, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        log.debug("omnipod plugin SetTempBasalPercent()");
        PumpEnactResult per = new PumpEnactResult();
        per.success = false;
        return per;
    }

    @Override
    public PumpEnactResult setExtendedBolus(Double insulin, Integer durationInMinutes) {
        log.debug("omnipod plugin SetExtendedBolus()");
        PumpEnactResult per = new PumpEnactResult();
        per.success = false;
        return per;
    }

    @Override
    public PumpEnactResult cancelTempBasal(boolean enforceNew) {
        log.debug("CancelTempBasal()");
        PumpEnactResult result = _pdm.CancelTempBasal(enforceNew);
        if (result.enacted) {
            if (TreatmentsPlugin.getPlugin().isTempBasalInProgress()) {
                TemporaryBasal tempStop = new TemporaryBasal().date(_pdm.GetLastUpdated()).source(Source.USER);
                TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempStop);
                MainApp.bus().post(new EventVirtualPumpUpdateGui());
            }
        }
        return result;
    }

    @Override
    public PumpEnactResult cancelExtendedBolus() {
        log.debug("CancelExtendedBolus()");
        PumpEnactResult per = new PumpEnactResult();
        per.success = true;
        return per;
    }

    @Override
    public JSONObject getJSONStatus(Profile profile, String profileName) {
        log.debug("GetJSONStatus()");
        long now = System.currentTimeMillis();
        if (!SP.getBoolean("virtualpump_uploadstatus", false)) {
            return null;
        }
        JSONObject pump = new JSONObject();
        JSONObject battery = new JSONObject();
        JSONObject status = new JSONObject();
        JSONObject extended = new JSONObject();
        try {
            battery.put("percent", 50);
            status.put("status", "normal");
            extended.put("Version", BuildConfig.VERSION_NAME + "-" + BuildConfig.BUILDVERSION);
            try {
                extended.put("ActiveProfile", profileName);
            } catch (Exception ignored) {
            }
            TemporaryBasal tb = TreatmentsPlugin.getPlugin().getTempBasalFromHistory(now);
            if (tb != null) {
                extended.put("TempBasalAbsoluteRate", tb.tempBasalConvertedToAbsolute(now, profile));
                extended.put("TempBasalStart", DateUtil.dateAndTimeString(tb.date));
                extended.put("TempBasalRemaining", tb.getPlannedRemainingMinutes());
            }
            ExtendedBolus eb = TreatmentsPlugin.getPlugin().getExtendedBolusFromHistory(now);
            if (eb != null) {
                extended.put("ExtendedBolusAbsoluteRate", eb.absoluteRate());
                extended.put("ExtendedBolusStart", DateUtil.dateAndTimeString(eb.date));
                extended.put("ExtendedBolusRemaining", eb.getPlannedRemainingMinutes());
            }
            status.put("timestamp", DateUtil.toISOString(now));

            pump.put("battery", battery);
            pump.put("status", status);
            pump.put("extended", extended);
            pump.put("reservoir", 50);
            pump.put("clock", DateUtil.toISOString(now));
        } catch (JSONException e) {
            log.error("Unhandled exception", e);
        }
        return pump;
    }

    @Override
    public String deviceID() {
        log.debug("DeviceID()");
        return _pdm.GetPodId();
    }

    @Override
    public PumpDescription getPumpDescription() {
        //log.debug("getPumpDescription()");
        return pumpDescription;
    }

    @Override
    public String shortStatus(boolean veryShort) {
        log.debug("shortStatus()");
        return _pdm.GetStatusShort();
    }

    @Override
    public boolean isFakingTempsByExtendedBoluses() {
        //log.debug("isFaking()");
        return false;
    }

    @Override
    public PumpEnactResult loadTDDs() {
        log.debug("loadTDDs()");
        PumpEnactResult per = new PumpEnactResult();
        per.success = false;
        return per;
    }
}
