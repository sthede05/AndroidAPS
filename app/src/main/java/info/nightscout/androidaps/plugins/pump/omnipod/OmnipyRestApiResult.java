package info.nightscout.androidaps.plugins.pump.omnipod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;

public class OmnipyRestApiResult {
    public JsonObject api;
    public JsonObject response;
    public OmnipodStatus status;
    public boolean success;
    public Double datetime;

    public static OmnipyRestApiResult fromJson(String jsonResponse) {
        Gson gson = new Gson();
        return gson.fromJson(jsonResponse, OmnipyRestApiResult.class);
    }
}
