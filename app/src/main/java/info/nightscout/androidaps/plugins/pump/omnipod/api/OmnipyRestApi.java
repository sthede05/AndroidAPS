package info.nightscout.androidaps.plugins.pump.omnipod.api;

import android.content.Context;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyCallback;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyConstants;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyRequestType;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyResult;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipyApiResult;

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
    private String REST_URL_SET_BASAL_SCHEDULE = "/pdm/setbasalschedule";

    private String REST_URL_ARCHIVE_POD = "/pdm/archive";
    private String REST_URL_PAIR_POD = "/pdm/pair";
    private String REST_URL_ACTIVATE_POD = "/pdm/activate";
    private String REST_URL_START_POD = "/pdm/start";

    private String REST_URL_RL_INFO = "/rl/info";

    private String _baseUrl;
    private OmnipyApiSecret _apiSecret;
    private final Context _context;
    private boolean _connectable;
    private boolean _authenticated;
    private long _lastSuccessfulConnection = 0;

    private String _host;
    private final Logger _log;

    private final LinkedBlockingDeque<OmnipyRequest> _requestQueue;


    public OmnipyRestApi(Context context) {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
        _requestQueue = new LinkedBlockingDeque<OmnipyRequest>(10);
    }

    public boolean isConnectable() {
        return _connectable;
    }
    public boolean isAuthenticated() {
        return _authenticated;
    }
    public String getHost() { return _host; }

    public long getLastSuccessfulConnection()
    {
        return _lastSuccessfulConnection;
    }



    public OmnipyRequest UpdateStatus(OmnipyCallback callback)
    {
        return UpdateStatus(0, callback);
    }

    public OmnipyRequest UpdateStatus(int requestType, OmnipyCallback callback)
    {
        return queue(new OmnipyRequest(OmnipyRequestType.Status, _baseUrl)
                .withAuthentication(_apiSecret)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_STATUS_TYPE,
                        Integer.toString(requestType))
                .withCallback(callback));
    }

    public OmnipyRequest Ping(OmnipyCallback callback) {
        return new OmnipyRequest(OmnipyRequestType.Ping, _baseUrl)
                .withCallback(callback)
                .executeAsync();
    }

    public OmnipyRequest CheckAuthentication(OmnipyCallback callback) {
        return new OmnipyRequest(OmnipyRequestType.CheckPassword, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .executeAsync();
    }

    public OmnipyRequest GetAddressFromPdm(int timeout, OmnipyCallback callback) {
        return new OmnipyRequest(OmnipyRequestType.ReadPdmAddress, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_PDM_ADDRESS_TIMEOUT,
                        Integer.toString(timeout))
                .executeAsync();
    }

    public OmnipyRequest CreateNewPod(int lot, int tid, int address, OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.NewPod, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_NEW_POD_LOT,
                        Integer.toString(lot))
                .withParameter(OmnipyConstants.OMNIPY_PARAM_NEW_POD_TID,
                        Integer.toString(tid))
                .withParameter(OmnipyConstants.OMNIPY_PARAM_NEW_POD_ADDRESS,
                        Integer.toString(address)));
    }

    public OmnipyRequest UpdatePodParameter(String parameter, String value, OmnipyCallback callback)
    {
        return queue(new OmnipyRequest(OmnipyRequestType.SetPodParameters, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .withParameter(parameter, value));
    }

    public OmnipyRequest GetRLInfo(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.RLInfo, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest AcknowledgeAlerts(int alertMask, OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.AckAlerts, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_ACK_ALERT_MASK,
                        Integer.toString(alertMask)));
    }

    public OmnipyRequest DeactivatePod(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.DeactivatePod, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }


    public OmnipyRequest ArchivePod(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.ArchivePod, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest PairPod(int utc_offset, OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.PairPod, _baseUrl)
                .withAuthentication(_apiSecret)
                .withParameter("utc", Integer.toString(utc_offset))
                .withCallback(callback));
    }

    public OmnipyRequest ActivatePod(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.ActivatePod, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest StartPod(BigDecimal[] basalSchedule, OmnipyCallback callback) {
        OmnipyRequest request = new OmnipyRequest(OmnipyRequestType.StartPod, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback);

        for(int i=0; i<48; i++)
            request.withParameter("h" + Integer.toString(i), basalSchedule[i].toString());

        return queue(request);
    }


    public OmnipyRequest Bolus(BigDecimal bolusAmount, OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.Bolus, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_BOLUS_AMOUNT,
                        bolusAmount.toString()));
    }

    public OmnipyRequest CancelBolus(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.CancelBolus, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest Shutdown(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.Shutdown, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest Restart(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.Restart, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest SetTempBasal(BigDecimal basalRate, BigDecimal durationInHours,
                             OmnipyCallback callback)
    {
        return queue(new OmnipyRequest(OmnipyRequestType.TempBasal, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_TEMPBASAL_RATE,
                        basalRate.toString())
                .withParameter(OmnipyConstants.OMNIPY_PARAM_TEMPBASAL_HOURS,
                        durationInHours.toString()));
    }

    public OmnipyRequest CancelTempBasal(OmnipyCallback callback) {
        return queue(new OmnipyRequest(OmnipyRequestType.CancelTempBasal, _baseUrl)
                .withAuthentication(_apiSecret)
                .withCallback(callback));
    }

    public OmnipyRequest setBasalSchedule(BigDecimal[] basalSchedule, int utc_offset,
                                          OmnipyCallback callback) {
        OmnipyRequest request = new OmnipyRequest(OmnipyRequestType.SetBasalSchedule, _baseUrl)
                .withAuthentication(_apiSecret)
                .withParameter("utc", Integer.toString(utc_offset))
                .withCallback(callback);

        for(int i=0; i<48; i++)
            request.withParameter("h" + Integer.toString(i), basalSchedule[i].toString());

        return queue(request);
    }


    public OmnipyResult IsBusy() {
        return new OmnipyRequest(OmnipyRequestType.IsBusy, _baseUrl)
                .execute(10000);
    }

    public OmnipyRequest IsBusy(OmnipyCallback callback) {
        return new OmnipyRequest(OmnipyRequestType.IsBusy, _baseUrl)
                .withCallback(callback);
    }

    @Subscribe
    public void onRequestComplete(final EventOmnipyApiResult result) {
        if (result.getResult().success)
            _lastSuccessfulConnection = System.currentTimeMillis();
        runNext();
    }

    private synchronized OmnipyRequest queue(OmnipyRequest request)
    {
        Iterator<OmnipyRequest> iterator = _requestQueue.iterator();
        ArrayList<OmnipyRequest> requeuedList = new ArrayList<>();
        while (iterator.hasNext())
        {
            OmnipyRequest queuedRequest = iterator.next();
            OmnipyRequestType queuedType = queuedRequest.getRequestType();

            switch (request.getRequestType())
            {
                case Ping:
                    if (queuedRequest.getRequestType() != OmnipyRequestType.Ping) {
                        iterator.remove();
                        queuedRequest.cancel();
                    }
                    break;
                case Status:
                    if (queuedRequest.getRequestType() != OmnipyRequestType.Status) {
                        iterator.remove();
                        queuedRequest.cancel();
                    }
                case CancelBolus:
                    iterator.remove();
                    if (queuedRequest.getRequestType() != OmnipyRequestType.Bolus)
                        requeuedList.add(queuedRequest);
                    else
                        queuedRequest.cancel();
                    break;
                case TempBasal:
                    if (queuedRequest.getRequestType() == OmnipyRequestType.TempBasal)
                        queuedRequest.cancel();
                    break;
                case CancelTempBasal:
                    if (queuedRequest.getRequestType() == OmnipyRequestType.TempBasal)
                        queuedRequest.cancel();
                default:
                    break;
            }
        }

        _requestQueue.addAll(requeuedList);
        _requestQueue.add(request);

        runNext();
        return request;
    }

    private synchronized void runNext()
    {
        if (_requestQueue.size() > 0) {
            OmnipyRequest request = _requestQueue.remove();
            request.executeAsync();
        }
    }

    public void onAvailable(Network network) {

    }

    public void onLosing(Network network, int maxMsToLive) {
    }

    public void onLost(Network network) {

    }

    public void onUnavailable() {

    }

    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {

    }

    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {

    }
}
