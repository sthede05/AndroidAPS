package info.nightscout.androidaps.plugins.pump.omnipod.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.bus.RxBus;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.SP;

public class OmniCoreAlerts {


    private final Logger _log;


    private Boolean[] _changeBlackout = new Boolean[24];

    private long _lastExpirationAlert = 0;
    protected long _expirationAlertSnooze = 10 * 60 * 1000;


    public OmniCoreAlerts() {
        _log =  LoggerFactory.getLogger(L.PUMP);

        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreAlerts: Creating Change Window");
        }

    }

    public void setupWindows() {
        //This is really ugly.
        _changeBlackout[0] = SP.getBoolean(R.string.key_omnicore_podchange_0,false);
        _changeBlackout[1] = SP.getBoolean(R.string.key_omnicore_podchange_1,false);
        _changeBlackout[2] = SP.getBoolean(R.string.key_omnicore_podchange_2,false);
        _changeBlackout[3] = SP.getBoolean(R.string.key_omnicore_podchange_3,false);
        _changeBlackout[4] = SP.getBoolean(R.string.key_omnicore_podchange_4,false);
        _changeBlackout[5] = SP.getBoolean(R.string.key_omnicore_podchange_5,false);
        _changeBlackout[6] = SP.getBoolean(R.string.key_omnicore_podchange_6,false);
        _changeBlackout[7] = SP.getBoolean(R.string.key_omnicore_podchange_7,false);
        _changeBlackout[8] = SP.getBoolean(R.string.key_omnicore_podchange_8,false);
        _changeBlackout[9] = SP.getBoolean(R.string.key_omnicore_podchange_9,false);
        _changeBlackout[10] = SP.getBoolean(R.string.key_omnicore_podchange_10,false);
        _changeBlackout[11] = SP.getBoolean(R.string.key_omnicore_podchange_11,false);
        _changeBlackout[12] = SP.getBoolean(R.string.key_omnicore_podchange_12,false);
        _changeBlackout[13] = SP.getBoolean(R.string.key_omnicore_podchange_13,false);
        _changeBlackout[14] = SP.getBoolean(R.string.key_omnicore_podchange_14,false);
        _changeBlackout[15] = SP.getBoolean(R.string.key_omnicore_podchange_15,false);
        _changeBlackout[16] = SP.getBoolean(R.string.key_omnicore_podchange_16,false);
        _changeBlackout[17] = SP.getBoolean(R.string.key_omnicore_podchange_17,false);
        _changeBlackout[18] = SP.getBoolean(R.string.key_omnicore_podchange_18,false);
        _changeBlackout[19] = SP.getBoolean(R.string.key_omnicore_podchange_19,false);
        _changeBlackout[20] = SP.getBoolean(R.string.key_omnicore_podchange_20,false);
        _changeBlackout[21] = SP.getBoolean(R.string.key_omnicore_podchange_21,false);
        _changeBlackout[22] = SP.getBoolean(R.string.key_omnicore_podchange_22,false);
        _changeBlackout[23] = SP.getBoolean(R.string.key_omnicore_podchange_23,false);

    }

    public long getAdjustedExpirationTime(long expirationTime) {
        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreAlerts: Checking Expiration Time: " + DateUtil.dateAndTimeString(expirationTime));
        }

        setupWindows();

        if (!isBlackoutWindowValid()) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreAlerts: Blackout Window is invalid. Returing entered value");
            }
            return expirationTime;
        }


        Date dt = new Date(expirationTime);
        int h = dt.getHours();

        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreAlerts: Blackout Window. Window at hour " + h + " is blackout");
        }

        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreAlerts: Expiration Hour is: " + h);
        }

        if (!_changeBlackout[h]) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreAlerts: Change is not in blackout window");
            }
            return expirationTime;
        }

        int hoursBack = 0;
        while (_changeBlackout[h]) {
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreAlerts: Blackout Window. Window at hour " + h + " is blackout");
            }
            h = (h -1 + 24) % 24;
            hoursBack++;
        }

        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreAlerts: Blackout Window. Change window open at hour: " + h);
        }

        if (hoursBack >= 24) {
            //that shouldn't happen.
            if (L.isEnabled(L.PUMP)) {
                _log.debug("OmniCoreAlerts: Blackout Window. Something borked. We had to look back more than a day to get a pod change time");
            }
            return expirationTime;
        }

        //round expiration time down to the hour, then subtract hoursBack hours to get the new expiration time
        expirationTime = expirationTime - expirationTime % (1000 * 60 * 60);
        if (L.isEnabled(L.PUMP)) {
            _log.debug("OmniCoreAlerts: Blackout Window. Expiration Time Rounded to Hour:" + DateUtil.dateAndTimeString(expirationTime));
        }
        expirationTime = expirationTime - (hoursBack -1) * (1000 * 60 * 60);
        return  expirationTime;
    }

    public Boolean isBlackoutWindowValid() {
        //We have to have at least one hour of the day to change pods
        Boolean isValid = false;

        for (int i = 0; i < 23; i++) {
            isValid = isValid || !_changeBlackout[i];
        }
        return isValid;
    }

    public void processExpirationAlerts(long reservoirExpiration, long podExpiration) {
        long soonestExpire = Math.min(reservoirExpiration, podExpiration);
        long currentTime = System.currentTimeMillis();

        //Check Urgent Alarm
        //TODO: Add to Strings
        if ((soonestExpire - currentTime) < (SP.getInt(R.string.key_omnicore_alert_urgent_expire,30) * 60 * 1000)) {
            RxBus.INSTANCE.send(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
            Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, "Pod will expire very soon: " + DateUtil.timeString(soonestExpire), Notification.URGENT);
            notification.soundId = R.raw.alarm;
            RxBus.INSTANCE.send(new EventNewNotification(notification));
        }
        else {

            //Don't bother if we're snoozing
            if (currentTime > _lastExpirationAlert + _expirationAlertSnooze) {
                long soonestExpireWithBlackout = getAdjustedExpirationTime(soonestExpire);
                Boolean blackoutInEffect = ((soonestExpireWithBlackout < soonestExpire) && (soonestExpireWithBlackout > currentTime));
                long expirationPriorMS = SP.getInt(R.string.key_omnicore_alert_prior_expire, 8) * 60 * 60 * 1000;
                String message = reservoirExpiration < podExpiration ? "Reservoir will be empty by " : "Pod will expire by ";
                message += DateUtil.timeString(soonestExpire);

                if (blackoutInEffect && (soonestExpireWithBlackout - currentTime < expirationPriorMS)) {
                    message += "\nPod change window ends " + DateUtil.timeString(soonestExpireWithBlackout);
                    RxBus.INSTANCE.send(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                    Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, message, Notification.URGENT);
                    RxBus.INSTANCE.send(new EventNewNotification(notification));
                    _lastExpirationAlert = currentTime;
                } else if (soonestExpire - currentTime < expirationPriorMS) {
                    RxBus.INSTANCE.send(new EventDismissNotification(Notification.OMNIPY_POD_STATUS));
                    Notification notification = new Notification(Notification.OMNIPY_POD_STATUS, message, Notification.URGENT);
                    RxBus.INSTANCE.send(new EventNewNotification(notification));
                    _lastExpirationAlert = currentTime;
                }
            }
        }
    }
}
