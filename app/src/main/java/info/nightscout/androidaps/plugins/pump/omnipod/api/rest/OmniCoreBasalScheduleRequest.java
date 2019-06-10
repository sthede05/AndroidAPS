package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import java.math.BigDecimal;

public class OmniCoreBasalScheduleRequest extends OmniCoreRequest {
    public OmniCoreBasalScheduleRequest(BigDecimal[] schedule, int utcOffsetMinutes){
        super();
        joRequest.addProperty("type", "SetBasalSchedule");
        joRequest.addProperty("utcOffset", utcOffsetMinutes);

        for(int i=0; i<48; i++)
            joRequest.addProperty("rate" + i, schedule[i]);
    }

}
