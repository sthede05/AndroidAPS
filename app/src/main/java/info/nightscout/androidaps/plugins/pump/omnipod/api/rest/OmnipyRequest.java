package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipodStatus;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public OmnipyResult getResult() {
        OmnipyResult result = new OmnipyResult();
        result.originalRequest = this;

        this.requested = System.currentTimeMillis();
        this.withParameter("req_t", Long.toString(this.requested));

        Intent startIntent = new Intent("OmniCoreIntentService.START_SERVICE");
        startIntent.setClassName("net.balya.OmniCore.Mobile.Android", "OmniCore.Mobile.Droid.OmniCoreIntentService");
        MainApp.instance().startService(startIntent);

        Handler myHandler = new Handler() { }
        MyReceiver myReceiver = new MyReceiver(myHandler);

        Intent intent = new Intent();
        intent.setClassName("net.balya.OmniCore.Mobile.Android","OmniCore.Mobile.Droid.OmniCoreIntentService");
        intent.putExtra("request", getRequestJson());
        intent.putExtra("response", myReceiver);
        MainApp.instance().startService(intent);

        //result.exception = new OmnipyConnectionException();
        result.success = true;
        result.status = new OmnipodStatus();
        return result;
    }

    @SuppressLint("RestrictedApi")
    class MyReceiver extends ResultReceiver {

        public MyReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

        }
    }

    public OmnipyRequestType getRequestType() { return _requestType; }

    public String getRequestJson()
    {
        return joRequest.toString();
    }

}

