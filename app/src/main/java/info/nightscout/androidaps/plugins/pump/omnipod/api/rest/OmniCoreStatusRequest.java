package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.JsonObject;

public class OmniCoreStatusRequest extends OmniCoreRequest {

    public OmniCoreStatusRequest(int statusRequestType)
    {
        super();
        joRequest.addProperty("Type", "UpdateStatus");
        joRequest.addProperty("StatusRequestType", statusRequestType);
    }
}
