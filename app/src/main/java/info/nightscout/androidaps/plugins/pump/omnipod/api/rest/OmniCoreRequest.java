package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

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
        OmniCoreResult result = new OmniCoreResult();
        result.originalRequest = this;
        this.requested = System.currentTimeMillis();

        Intent startIntent = new Intent("OmniCoreIntentService.START_SERVICE");
        startIntent.setClassName("net.balya.OmniCore.Mobile.Android", "OmniCore.Mobile.Droid.OmniCoreIntentService");
        MainApp.instance().startService(startIntent);

        @SuppressLint("HandlerLeak")
        OmniCoreHandler omniCoreHandler = new OmniCoreHandler();

        Messenger messenger = new Messenger(omniCoreHandler);

        Intent intent = new Intent();
        intent.setClassName("net.balya.OmniCore.Mobile.Android","OmniCore.Mobile.Droid.OmniCoreIntentService");
        joRequest.addProperty("lastResult", lastResultId);
        String jsonRequest = joRequest.toString();
        intent.putExtra("request", jsonRequest);
        intent.putExtra("messenger", messenger);

        synchronized (omniCoreHandler)
        {
            MainApp.instance().startService(intent);
            try {
                omniCoreHandler.wait();
                //result.response = omniCoreHandler.response;
                return result;
            } catch (InterruptedException e) {
                e.printStackTrace();
                result.success = false;
                return result;
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
        public void handleMessage(Message msg) {
            response = msg.getData().getString("response");
            this.notify();
        }
    }

}

