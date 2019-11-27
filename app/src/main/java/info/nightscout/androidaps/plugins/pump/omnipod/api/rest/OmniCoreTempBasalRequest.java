package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import java.math.BigDecimal;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;

public class OmniCoreTempBasalRequest extends OmniCoreRequest {

    public OmniCoreTempBasalRequest(BigDecimal temporaryRate, BigDecimal durationInHours)
    {
        super();
        joRequest.addProperty("Type", "SetTempBasal");
        joRequest.addProperty("TemporaryRate", temporaryRate);
        joRequest.addProperty("DurationHours", durationInHours);
    }

    @Override
    public String getRequestType()  {
        BigDecimal basalRate = joRequest.get("TemporaryRate").getAsBigDecimal();
        BigDecimal duration = joRequest.get("DurationHours").getAsBigDecimal();
        return String.format(MainApp.gs(R.string.omnipod_command_tbr),basalRate,duration);
    }

}
