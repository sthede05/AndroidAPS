package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import java.math.BigDecimal;

public class OmniCoreTempBasalRequest extends OmniCoreRequest {

    public OmniCoreTempBasalRequest(BigDecimal temporaryRate, BigDecimal durationInHours)
    {
        super();
        joRequest.addProperty("type", "SetTempBasal");
        joRequest.addProperty("temporaryRate", temporaryRate);
        joRequest.addProperty("durationHours", durationInHours);
    }

}
