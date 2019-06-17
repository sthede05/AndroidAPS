package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import info.nightscout.androidaps.logging.L;

public class OmniCoreResult {

    public boolean Success;
    public Boolean PodRunning;
    public String PodId;
    public long ResultId;
    public long ResultDate;

    public BigDecimal[] BasalSchedule;
    public int UtcOffset;

    public double InsulinCanceled;
    public double ReservoirLevel;
    public int BatteryLevel;

    public long LastResultId;
    public JsonArray ResultsToDate;

    public static OmniCoreResult fromJson(String jsonResponse) {
        try {
            return new Gson().fromJson(jsonResponse, OmniCoreResult.class);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String asJson()
    {
        try {
            return new Gson().toJson(this);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}