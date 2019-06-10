package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;

public class OmniCoreResult {

    public boolean Success;
    public OmnipodStatus Status;
    public JsonArray RequestsToDate;

    public static OmniCoreResult fromJson(String jsonResponse) {
        try {
            return new Gson().fromJson(jsonResponse, OmniCoreResult.class);
        } catch (Exception e)
        {
            LoggerFactory.getLogger(L.PUMP).debug(jsonResponse);
            throw e;
        }
    }
}