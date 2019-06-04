package info.nightscout.androidaps.plugins.pump.omnipod.api;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyConstants;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyRequestType;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyRequest;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyResult;

public class OmniCoreApi {
    private final Context _context;
    private final Logger _log;

    public OmniCoreApi(Context context) {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
    }

    public OmnipyResult UpdateStatus(int requestType)
    {
        return new OmnipyRequest(OmnipyRequestType.Status)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_STATUS_TYPE,
                        Integer.toString(requestType))
                .getResult();
    }

    public OmnipyResult Bolus(BigDecimal bolusAmount) {
        return new OmnipyRequest(OmnipyRequestType.Bolus)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_BOLUS_AMOUNT,
                        bolusAmount.toString())
                .getResult();

    }

    public OmnipyResult CancelBolus() {
        return new OmnipyRequest(OmnipyRequestType.CancelBolus)
                .getResult();
    }

    public OmnipyResult SetTempBasal(BigDecimal basalRate, BigDecimal durationInHours)
    {
        return new OmnipyRequest(OmnipyRequestType.TempBasal)
                .withParameter(OmnipyConstants.OMNIPY_PARAM_TEMPBASAL_RATE,
                        basalRate.toString())
                .withParameter(OmnipyConstants.OMNIPY_PARAM_TEMPBASAL_HOURS,
                        durationInHours.toString()).getResult();
    }

    public OmnipyResult CancelTempBasal() {
        return new OmnipyRequest(OmnipyRequestType.CancelTempBasal).getResult();
    }

    public OmnipyResult setBasalSchedule(BigDecimal[] basalSchedule, int utc_offset) {
        OmnipyRequest request = new OmnipyRequest(OmnipyRequestType.SetBasalSchedule)
                .withParameter("utc", Integer.toString(utc_offset));

        for(int i=0; i<48; i++)
            request.withParameter("h" + Integer.toString(i), basalSchedule[i].toString());

        return request.getResult();
    }


    public OmnipyResult IsBusy() {
        return new OmnipyRequest(OmnipyRequestType.IsBusy)
                .getResult();
    }
}
