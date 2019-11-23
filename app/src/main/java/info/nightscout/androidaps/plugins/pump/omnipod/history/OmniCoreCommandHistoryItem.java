package info.nightscout.androidaps.plugins.pump.omnipod.history;

import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreResult;
import info.nightscout.androidaps.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import info.nightscout.androidaps.logging.L;


public class OmniCoreCommandHistoryItem {

    private final Logger _log;

    private OmniCoreResult _result;
    private OmniCoreRequest _request;
    private String _status;
    private Long _lastUpdate;



    public OmniCoreCommandHistoryItem(OmniCoreRequest request, OmniCoreResult result) {
        _log =  LoggerFactory.getLogger(L.PUMP);
        this._request = request;
        this._result = result;

        processHistoryItem();

    }

    public String getStatus() {
        return this._status;
    }

    public OmniCoreRequest getRequest() {
        return this._request;
    }

    public OmniCoreResult getResult() {
        return this._result;
    }

    public void setResult(OmniCoreResult result) {

        this._result = result;
        processHistoryItem();
    }

    public void setFailed() {
        this._status = "Failure";
        this._lastUpdate = DateUtil.now();
        processHistoryItem();
    }

    private void processHistoryItem() {
        if (this._result == null) {
            if (this._status != "Failure") {
                this._status = "Pending";
            }
        }
        else {
            if (this._result.Success) {
                this._status = "Success";
            }
            else {
                this._status = "Failure";
            }
        }

        this._lastUpdate = DateUtil.now();
    }

    public long getStartTime() {
        return this._request.created;
    }


    public long getEndTime() {
       return this._lastUpdate;
    }

    public Boolean isSameRequest(OmniCoreRequest request) {
        return this.getStartTime() == request.created;
    }

    public long getRunTime() {
        return getEndTime() - getStartTime();
    }
}
