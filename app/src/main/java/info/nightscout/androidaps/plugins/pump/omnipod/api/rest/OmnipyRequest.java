package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.plugins.pump.omnipod.exceptions.OmnipyConnectionException;

public class OmnipyRequest {

    private final OmnipyRequestType _requestType;
    private final JSONObject joRequest;
    public long created;
    public long requested;
    public long responseReceived;

    public OmnipyRequest(OmnipyRequestType requestType)
    {
        _requestType = requestType;
        created = System.currentTimeMillis();
        joRequest = new JSONObject();
    }

    public OmnipyRequest withParameter(String key, String value) {
        try
        {
            joRequest.put(key, value);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
        return this;
    }

    public OmnipyResult getResult() {
        OmnipyResult result = new OmnipyResult();
        result.originalRequest = this;

        this.requested = System.currentTimeMillis();
        this.withParameter("req_t", Long.toString(this.requested));

        Intent intent = new Intent();
        intent.setAction("net.balya.OmniCore.Mobile.Android");
        intent.putExtra("request", getRequestJson());
        MainApp.instance().sendBroadcast(intent);

        result.exception = new OmnipyConnectionException();
        result.success = false;
        return result;
    }

    public OmnipyRequestType getRequestType() { return _requestType; }

    public String getRequestJson()
    {
        return joRequest.toString();
    }

}

