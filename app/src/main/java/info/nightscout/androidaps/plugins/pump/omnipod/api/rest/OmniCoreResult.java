package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;

public class OmniCoreResult {

    public transient OmniCoreRequest originalRequest;

    public boolean canceled;
    public boolean success;
    public JsonObject response;
    public OmnipodStatus status;

    public double datetime;
    public Exception exception;
    public int battery_level = -1;

    public static OmniCoreResult fromJson(String jsonResponse, OmniCoreRequest request) {
        try {
            Gson gson = new Gson();
            OmniCoreResult result = gson.fromJson(jsonResponse, OmniCoreResult.class);
            result.originalRequest = request;
            return result;
        } catch (Exception e)
        {
            LoggerFactory.getLogger(L.PUMP).debug(jsonResponse);
            throw e;
        }
    }
}