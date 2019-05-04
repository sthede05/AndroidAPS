package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyApiVersion;

public class OmnipyResult {

    public transient OmnipyRequest originalRequest;

    public boolean canceled;
    public boolean success;
    public OmnipyApiVersion api;
    public JsonObject response;
    public OmnipodStatus status;

    public double datetime;
    public Exception exception;
    public int battery_level = -1;

    public static OmnipyResult fromJson(String jsonResponse, OmnipyRequest request) {
        try {
            Gson gson = new Gson();
            OmnipyResult result = gson.fromJson(jsonResponse, OmnipyResult.class);
            result.originalRequest = request;
            return result;
        } catch (Exception e)
        {
            LoggerFactory.getLogger(L.PUMP).debug(jsonResponse);
            throw e;
        }
    }
}