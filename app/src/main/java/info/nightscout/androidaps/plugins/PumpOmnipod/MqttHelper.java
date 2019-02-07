package info.nightscout.androidaps.plugins.PumpOmnipod;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.events.EventPumpStatusChanged;


public class MqttHelper {

    private Context _context;
    private MqttAndroidClient _mqttClient;
    private MqttConnectOptions _mqttConnectOptions;

    private static final String BROKER_URL = "ssl://host:port";
    private static final String SUBSCRIBE_TOPIC = "omnipod/response";
    private static final String PUBLISH_TOPIC = "omnipod/command";
    private static final String CLIENT_ID = "omnipod-aaps-client";

    private String _message = null;
    private String _topic = null;
    private String _sequence = null;

    private final Lock _messageLock = new ReentrantLock();
    private final Lock _connectionLock = new ReentrantLock();

    private final Condition _messageReceived = _messageLock .newCondition();

    public MqttHelper(Context context)
    {
        _context = context;
    }

    public boolean IsConnected()
    {
        return _mqttClient != null && _mqttClient.isConnected();
    }

    public boolean IsConnecting()
    {
        return _mqttClient != null && !_mqttClient.isConnected();
    }

    public String[] SendAndGet(String msg, int timeout) {
        _messageLock.lock();
        String[] response = null;
        try
        {
            if (_mqttClient == null || !_mqttClient.isConnected())
            {
                if (!Connect())
                {
                    throw new InterruptedException();
                }
            }

            _sequence = Long.toString(System.currentTimeMillis());
            msg = _sequence + "|" + msg;
            IMqttDeliveryToken token = _mqttClient.publish(PUBLISH_TOPIC, msg.getBytes(), 2, false);

            if (!_messageReceived.await(90, TimeUnit.SECONDS))
                throw new InterruptedException();
            String[] messageParts = _message.split("\\|");
            if (messageParts.length < 2)
                throw new InterruptedException();
            if (!messageParts[0].equals(_sequence))
                throw new InterruptedException();

            response = new String[messageParts.length - 1];
            for (int i=0; i<response.length; i++)
                response[i] = messageParts[i+1];

        } catch (MqttException e) {

        } catch (InterruptedException e) {
        } finally {
            _messageLock.unlock();
        }
        return response;
    }

    public boolean Connect() {

        _connectionLock.lock();
        try {
            if (_mqttClient != null && _mqttClient.isConnected()) {
                MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.CONNECTED));
                return true;
            }
            return ConnectInternal();
        }
        finally
        {
            _connectionLock.unlock();
        }
    }

    private boolean ConnectInternal() {

        _mqttClient = new MqttAndroidClient(_context, BROKER_URL, CLIENT_ID);
        _mqttConnectOptions = new MqttConnectOptions();
        _mqttConnectOptions.setKeepAliveInterval(60);
        _mqttConnectOptions.setCleanSession(false);
        _mqttConnectOptions.setAutomaticReconnect(true);

        MqttSocketFactory.SocketFactoryOptions socketFactoryOptions = new MqttSocketFactory.SocketFactoryOptions();
        try {
            socketFactoryOptions.withCaInputStream(_context.getResources().openRawResource(R.raw.le_interim));
            _mqttConnectOptions.setSocketFactory(new MqttSocketFactory(socketFactoryOptions));
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        try {
            IMqttToken token = _mqttClient.connect(_mqttConnectOptions);
            token.waitForCompletion();
            if (token.getException() == null) {
                token = _mqttClient.subscribe(SUBSCRIBE_TOPIC, 2);
                token.waitForCompletion();
                if (token.getException() == null) {

                    _mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTED));
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            _messageLock.lock();
                            try {
                                String msg = message.toString();
                                String[] r = msg.split("\\|");
                                if (r[0].equals(_sequence))
                                {
                                    _topic = topic;
                                    _message = message.toString();
                                    _messageReceived.signal();
                                }
                                else
                                {
                                    // oob message - process?
                                }
                            } finally {
                                _messageLock.unlock();
                            }
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {
                        }
                    });

                    MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.CONNECTED));

                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public void Disconnect() {
        _connectionLock.lock();
        try {
            if (!_mqttClient.isConnected())
                return;
            _mqttClient.disconnect();
            _mqttClient = null;
        } catch (MqttException e) {
            e.printStackTrace();
        }
        finally
        {
            _connectionLock.unlock();
        }
    }

}
