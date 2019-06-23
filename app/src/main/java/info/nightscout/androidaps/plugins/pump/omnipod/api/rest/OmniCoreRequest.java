package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.tools.jsc.Main;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import info.nightscout.androidaps.MainApp;

public abstract class OmniCoreRequest {

    public long created;
    public long requested;

    protected JsonObject joRequest;
    private static HandlerThread mHandlerThread;

    public OmniCoreRequest()
    {
        created = System.currentTimeMillis();
        joRequest = new JsonObject();
    }

    public OmniCoreResult getRemoteResult(long lastResultDateTime) {
        this.requested = System.currentTimeMillis();

        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("OmniCoreHandler");
            mHandlerThread.start();
        }

        Semaphore semaphore = new Semaphore(0);
        OmniCoreHandler handler = new OmniCoreHandler(mHandlerThread.getLooper(), semaphore);

        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("OMNICORE_AAPS", "getRemoteResult: Start Service intent ");
                Intent startIntent = new Intent("OmniCoreIntentService.START_SERVICE");
                startIntent.setClassName("net.balya.OmniCore.Mobile.Android", "somethingsomething.OmniCoreIntentService");
                try {
                    ComponentName componentName = MainApp.instance().startService(startIntent);
                    if (componentName == null)
                    {
                        Log.d("OMNICORE_AAPS", "getRemoteResult: Start Service failed");
                        semaphore.release();
                        return;
                    }
                }
                catch(IllegalStateException e)
                {
                    Log.d("OMNICORE_AAPS", "getRemoteResult: Start Service failed due to it being in background");
                    semaphore.release();
                    return;
                }

                Log.d("OMNICORE_AAPS", "getRemoteResult: Start Service success");

                Messenger messenger = new Messenger(handler);
                Intent intent = new Intent("OmniCoreIntentService.REQUEST_COMMAND");
                intent.setClassName("net.balya.OmniCore.Mobile.Android","somethingsomething.OmniCoreIntentService");
                joRequest.addProperty("LastResultDateTime", lastResultDateTime);
                String jsonRequest = joRequest.toString();
                intent.putExtra("request", jsonRequest);
                intent.putExtra("messenger", messenger);
                Log.d("OMNICORE_AAPS", "getRemoteResult sending request: " + jsonRequest);
                try {
                    ComponentName componentName = MainApp.instance().startService(intent);
                    if (componentName == null) {
                        Log.d("OMNICORE_AAPS", "getRemoteResult: send request failed");
                        semaphore.release();
                        return;
                    }
                }
                catch(IllegalStateException e)
                {
                    Log.d("OMNICORE_AAPS", "getRemoteResult: Request Command failed due to service being in background");
                    semaphore.release();
                    return;
                }
                Log.d("OMNICORE_AAPS", "getRemoteResult: request sent");
            }
        });

        while(true)
        {
            try
            {
                if (semaphore.tryAcquire(45000, TimeUnit.MILLISECONDS))
                    break;

                if (System.currentTimeMillis() - handler.LastBusy > 15000)
                {
                    Log.e("OMNICORE_AAPS", "geRemoteResult: timed out");
                    return null;
                }
            }
            catch (InterruptedException e)
            {
                    return null;
            }
        }

        Log.d("OMNICORE_AAPS", "getRemoteResult: response received");
        if (handler.Response == null)
            return null;
        Log.d("OMNICORE_AAPS", "getRemoteResult: " + handler.Response);
        return OmniCoreResult.fromJson(handler.Response);
    }

    class OmniCoreHandler extends Handler {

        public OmniCoreHandler(Looper looper, Semaphore semaphore)
        {
            super(looper);
            Semaphore = semaphore;
        }

        private Semaphore Semaphore;
        public String Response;
        public long LastBusy = 0;

        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            boolean busy = b.getBoolean("busy", false);
            if (busy)
            {
                Log.d("OMNICORE_AAPS", "handleMessage: keep-alive received");
                LastBusy = System.currentTimeMillis();
            }
            boolean finished = b.getBoolean("finished", false);
            if (finished)
            {
                Log.d("OMNICORE_AAPS", "handleMessage: finished received");
                Response = b.getString("response", null);
                Semaphore.release();
            }
        }
    }
}

