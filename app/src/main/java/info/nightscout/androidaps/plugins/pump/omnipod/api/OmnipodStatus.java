package info.nightscout.androidaps.plugins.pump.omnipod.api;

import com.google.gson.Gson;

import java.math.BigDecimal;

public class OmnipodStatus {

    public String StatusText;
    public String PodId;
    public Boolean PodRunning;
    public double ReservoirLevel;
    public double InsulinCanceled;

    public BigDecimal[] BasalSchedule;
    public int UtcOffset;

    public int LastUpdated;

    public long ResultId;

    public static OmnipodStatus fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, OmnipodStatus.class);
    }

    public String asJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
