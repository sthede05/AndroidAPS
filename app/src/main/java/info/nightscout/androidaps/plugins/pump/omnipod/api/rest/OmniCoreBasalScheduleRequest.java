package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import java.math.BigDecimal;

public class OmniCoreBasalScheduleRequest extends OmniCoreRequest {
    public OmniCoreBasalScheduleRequest(BigDecimal[] schedule, int utcOffsetMinutes){}

    @Override
    protected String getRequestJson() {
        return null;
    }
}
