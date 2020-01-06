package info.nightscout.androidaps.plugins.pump.omnipod.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.utils.SP;

public class OmniCoreStats {
    private Map<OmnicoreStatType, Long> _omnicoreStats;
    private final Logger _log;

    private final long _saveTimer = 30 * 1000;
    private long _lastSave = 0;

    public enum OmnicoreStatType {
        TBRTOTAL,
        TBRHIGH,
        TBRLOW,
        TBRZERO,
        TBRCANCEL,
        TBRTIME,
        BOLUS,
        BOLUSSMB,
        BOLUSTIME,
        BOLUSCANCEL,
        POD,
        PROFILESET,
        PROFILESETTIME,
        STARTDATE,
        ENDDATE,
        COMMANDFAIL,
        TOTALTIME,
        TOTALCOMMANDS
    }


    public OmniCoreStats() {
        _log =  LoggerFactory.getLogger(L.PUMP);

        _omnicoreStats = new EnumMap<>(OmnicoreStatType.class);

        loadStats();
        if (getStat(OmnicoreStatType.STARTDATE) == 0) {
            putStat(OmnicoreStatType.STARTDATE,System.currentTimeMillis());
        }

        if (getStat(OmnicoreStatType.TOTALCOMMANDS) == 0) {
            putStat(OmnicoreStatType.TOTALCOMMANDS,getStat(OmnicoreStatType.BOLUS) + getStat(OmnicoreStatType.TBRTOTAL) + getStat(OmnicoreStatType.PROFILESET));
        }


    }

    public long getStat(OmnicoreStatType stat) {
        long retVal = 0;
        try {
            Long val = _omnicoreStats.get(stat);
            retVal = (val != null) ? val : 0;
        }
        catch (Exception e) {
            _log.debug("OmniCoreStats: Error getting value: " + stat);
            _log.debug(e.getMessage());
        }
        return retVal;
    }



    public long putStat(OmnicoreStatType key, long value) {
        try {
            long currentTime = System.currentTimeMillis();
            _omnicoreStats.put(key,value);
            _omnicoreStats.put(OmnicoreStatType.ENDDATE,currentTime);
            if (currentTime > _lastSave + _saveTimer) {
                saveStats();
            }
        }
        catch (Exception e) {
            _log.debug("OmniCoreStats: Error putting value: " + value);
            _log.debug(e.getMessage());
        }
        return getStat(key);
    }

    public long incrementStat(OmnicoreStatType key) {
        return addToStat(key,1);
    }

    public long addToStat(OmnicoreStatType key, long delta) {
        long val = getStat(key);
        val += delta;
        return putStat(key,val);
    }

    private void loadStats() {
        String savedStats = SP.getString(R.string.key_omnicore_stats,"");
        String[] nameValuePairs = savedStats.split("&");
        for (String nameValuePair : nameValuePairs) {
            String[] nameValue = nameValuePair.split("=");
            try {
                _omnicoreStats.put(OmnicoreStatType.valueOf(nameValue[0]), nameValue.length > 1 ? Long.parseLong(nameValue[1]) : 0);
            } catch (Exception e) {
                _log.debug("OmniCoreStats: Error loading stats: " + e.getMessage() );
            }
        }

    }

    private void saveStats() {
        StringBuilder stringBuilder = new StringBuilder();

        for (OmnicoreStatType key : _omnicoreStats.keySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&");
            }

            try {
                if (key != null) {
                    stringBuilder.append(key.toString());
                    stringBuilder.append("=");
                    stringBuilder.append(getStat(key));
                }
            } catch (Exception e) {
                _log.debug("OmniCoreStats: Error saving stats: " + e.getMessage() );
            }
        }

        SP.putString(R.string.key_omnicore_stats,stringBuilder.toString());
        _lastSave = System.currentTimeMillis();

    }

    public String getDurationAsString(OmnicoreStatType key) {
        long millis = getStat(key);
        if(millis <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long milliseconds = millis % 1000;

        return String.format("%dd %dh %dm %ds %dms",
                days, hours, minutes, seconds, milliseconds);
    }

    public Set<OmnicoreStatType> getKeys() {
        return _omnicoreStats.keySet();
    }
}
