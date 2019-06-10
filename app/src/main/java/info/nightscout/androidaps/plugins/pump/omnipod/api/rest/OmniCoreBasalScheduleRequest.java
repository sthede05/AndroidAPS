package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.JsonArray;

import java.math.BigDecimal;

public class OmniCoreBasalScheduleRequest extends OmniCoreRequest {
    public OmniCoreBasalScheduleRequest(BigDecimal[] schedule, int utcOffsetMinutes){
        super();
        joRequest.addProperty("Type", "SetBasalSchedule");
        joRequest.addProperty("UtcOffsetMinutes", utcOffsetMinutes);

        JsonArray scheduleArray = new JsonArray();
        for(int i=0; i<48; i++)
            scheduleArray.add(schedule[i]);

        joRequest.add("BasalSchedule", scheduleArray);
    }

}
