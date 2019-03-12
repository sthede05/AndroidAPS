package info.nightscout.androidaps.plugins.pump.omnipod;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OmnipyRestApi {

    private String REST_URL_PING = "/omnipy/ping";
    private String REST_URL_TOKEN = "/omnipy/token";
    private String REST_URL_NEW_POD = "/omnipy/newpod";
    private String REST_URL_GET_PDM_ADDRESS = "/omnipy/pdmspy";
    private String REST_URL_CHECK_PASSWORD = "/omnipy/pwcheck";
    private String REST_URL_SET_POD_PARAMETERS = "/omnipy/parameters";

    private String REST_URL_STATUS = "/pdm/status";
    private String REST_URL_PDM_BUSY = "/pdm/isbusy";
    private String REST_URL_ACK_ALERTS = "/pdm/ack";
    private String REST_URL_DEACTIVATE_POD = "/pdm/deactivate";
    private String REST_URL_BOLUS = "/pdm/bolus";
    private String REST_URL_CANCEL_BOLUS = "/pdm/cancelbolus";
    private String REST_URL_SET_TEMP_BASAL = "/pdm/settempbasal";
    private String REST_URL_CANCEL_TEMP_BASAL = "/pdm/canceltempbasal";

    private String REST_URL_RL_INFO = "/rl/info";

    private String _baseUrl;
    private OmnipyApiSecret _apiSecret;
    private int _connectionTimedOutCount = 0;
    private long _lastSuccessfulConnection = 0;


    public OmnipyRestApi(String baseUrl, OmnipyApiSecret apiSecret) {
        _baseUrl = baseUrl;
        _apiSecret = apiSecret;
    }

    public int getConnectionTimeOutCount() {
        return _connectionTimedOutCount;
    }

    public long getLastSuccessfulConnection()
    {
        return _lastSuccessfulConnection;
    }

    public OmnipyRestApiResult Status(){
        return Status(0);
    }

    public OmnipyRestApiResult Status(int requestType)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("type", Integer.toString(requestType)));
        return getApiResult(REST_URL_STATUS, parameters);
    }

    public OmnipyRestApiResult Ping()
    {
        return getApiResult(REST_URL_PING, null,
                5000);
    }

    public OmnipyRestApiResult CheckAuthentication()
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        return getApiResult(REST_URL_CHECK_PASSWORD, parameters,
                10000);
    }

    public OmnipyRestApiResult GetAddressFromPdm(int timeout)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("timeout", Integer.toString(timeout)));
        return getApiResult(REST_URL_GET_PDM_ADDRESS, parameters);
    }

    public OmnipyRestApiResult CreateNewPod(int lot, int tid, int address)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("lot", Integer.toString(lot)));
        parameters.add(new Pair<>("tid", Integer.toString(tid)));
        parameters.add(new Pair<>("address", Integer.toString(address)));
        return getApiResult(REST_URL_NEW_POD, parameters);
    }

    public OmnipyRestApiResult UpdatePodParameters(int lot, int tid, int address)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("lot", Integer.toString(lot)));
        parameters.add(new Pair<>("tid", Integer.toString(tid)));
        parameters.add(new Pair<>("address", Integer.toString(address)));
        return getApiResult(REST_URL_SET_POD_PARAMETERS, parameters);
    }

    public OmnipyRestApiResult GetRLInfo()
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        return getApiResult(REST_URL_RL_INFO, parameters);
    }

    public OmnipyRestApiResult AcknowledgeAlerts(int alertMask)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("alertmask", Integer.toString(alertMask) ));
        return getApiResult(REST_URL_ACK_ALERTS, parameters);
    }

    public OmnipyRestApiResult DeactivatePod()
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        return getApiResult(REST_URL_DEACTIVATE_POD, parameters);
    }

    public OmnipyRestApiResult Bolus(BigDecimal bolusAmount)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("amount", bolusAmount.toString() ));
        return getApiResult(REST_URL_BOLUS, parameters);
    }

    public OmnipyRestApiResult CancelBolus()
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        return getApiResult(REST_URL_CANCEL_BOLUS, parameters);
    }

    public OmnipyRestApiResult SetTempBasal(BigDecimal basalRate, BigDecimal durationInHours)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("amount", basalRate.toString() ));
        parameters.add(new Pair<>("hours", durationInHours.toString() ));
        return getApiResult(REST_URL_SET_TEMP_BASAL, parameters);
    }

    public OmnipyRestApiResult CancelTempBasal()
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        return getApiResult(REST_URL_CANCEL_TEMP_BASAL, parameters);
    }

    public OmnipyRestApiResult IsBusy()
    {
        return getApiResult(REST_URL_PDM_BUSY, null);
    }

    private ArrayList<Pair<String, String>> getAuthenticationParameters() {
        OmnipyApiToken token = getToken();
        String authToken = Base64.encode(token.getAuthenticationToken());
        String iv = Base64.encode(token.getIV());
        ArrayList<Pair<String,String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>("auth", authToken));
        parameters.add(new Pair<>("i", iv));
        return parameters;
    }

    private OmnipyApiToken getToken() {
        OmnipyApiToken apiToken = null;
        OmnipyRestApiResult result = getApiResult(REST_URL_TOKEN, null);

        String tokenStr = result.response.get("token").getAsString();
        apiToken = new OmnipyApiToken(Base64.decode(tokenStr), _apiSecret);
        return apiToken;
    }

    private OmnipyRestApiResult getApiResult(String path, ArrayList<Pair<String, String>> parameters)
    {
        return getApiResult(path, parameters, 0);
    }

    private OmnipyRestApiResult getApiResult(String path, ArrayList<Pair<String, String>> parameters,
                                int timeout)
    {
        OmnipyRestApiResult result = null;
        try
        {
            int parameterCount = 1;
            if (parameters != null)
                parameterCount += parameters.size() * 2;
            String[] stringParams = new String[parameterCount];
            stringParams[0] = _baseUrl + path;

            if (parameters != null)
            {
                int index = 1;
                for (Pair<String,String> param : parameters) {
                    stringParams[index++] = param.first;
                    stringParams[index++] = param.second;
                }
            }
            RestApiTask task = (RestApiTask) new RestApiTask().execute(stringParams);
            if (timeout == 0) {
                result = task.get();
                _connectionTimedOutCount = 0;
                _lastSuccessfulConnection = SystemClock.elapsedRealtime();
            } else {
                result = task.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            _connectionTimedOutCount += 1;
            e.printStackTrace();
        }

        if (result == null) {
            result = new OmnipyRestApiResult();
            result.success = false;
        }
        return result;
    }

}

class RestApiTask extends AsyncTask<String, Void,OmnipyRestApiResult> {
    @Override
    protected OmnipyRestApiResult doInBackground(String... strings) {
        OmnipyRestApiResult result = null;
        try {
            Uri.Builder ub = Uri.parse(strings[0]).buildUpon();
            if (strings.length > 1)
            {
                int index = 1;
                while(index < strings.length)
                {
                    ub.appendQueryParameter(strings[index], strings[index+1]);
                    index += 2;
                }
            }

            URL url = new URL(ub.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readStream(urlConnection.getInputStream());
                result = OmnipyRestApiResult.fromJson(response);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}