package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import info.nightscout.androidaps.MainApp;

public abstract class OmniCoreRequest {

    public long created;
    public long requested;

    protected JsonObject joRequest;

    public OmniCoreRequest()
    {
        created = System.currentTimeMillis();
        joRequest = new JsonObject();
    }

    public OmniCoreResult getResult(long lastResultId) {
        this.requested = System.currentTimeMillis();

        Intent startIntent = new Intent("OmniCoreIntentService.START_SERVICE");
        startIntent.setClassName("net.balya.OmniCore.Mobile.Android", "somethingsomething.OmniCoreIntentService");
        final ComponentName componentName = MainApp.instance().startService(startIntent);

        if (componentName == null)
        {
            return null;
        }

        @SuppressLint("HandlerLeak")
        OmniCoreHandler omniCoreHandler = new OmniCoreHandler();

        Messenger messenger = new Messenger(omniCoreHandler);

        Intent intent = new Intent("OmniCoreIntentService.REQUEST_COMMAND");
        intent.setClassName("net.balya.OmniCore.Mobile.Android","somethingsomething.OmniCoreIntentService");
        joRequest.addProperty("LastResultId", lastResultId);
        String jsonRequest = joRequest.toString();
        intent.putExtra("request", jsonRequest);
        intent.putExtra("messenger", messenger);

        synchronized (omniCoreHandler)
        {
            MainApp.instance().startService(intent);
            try {
                omniCoreHandler.wait();
                return OmniCoreResult.fromJson(omniCoreHandler.response);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    class OmniCoreHandler extends Handler {

        public OmniCoreHandler()
        {
            super(Looper.getMainLooper());
        }

        public String response;

        @Override
        public synchronized void handleMessage(Message msg) {
            response = msg.getData().getString("response");
            this.notify();
        }
    }

}

