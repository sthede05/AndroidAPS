package info.nightscout.androidaps.plugins.pump.omnipod

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.db.CareportalEvent
import info.nightscout.androidaps.events.EventTempBasalChange
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.T
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.omnicore_fragment.*
import org.slf4j.LoggerFactory

class OmniCorePumpFragment : Fragment() {
    private val disposable = CompositeDisposable()
    private val log = LoggerFactory.getLogger(L.PUMP)


    private val loopHandler = Handler()
    private lateinit var refreshLoop: Runnable

    init {
        refreshLoop = Runnable {
            activity?.runOnUiThread { updateGui() }
            loopHandler.postDelayed(refreshLoop, T.mins(1).msecs())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.omnicore_fragment, container, false)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        omnicore_updatestatusbutton.setOnClickListener {
            if (L.isEnabled(L.PUMP))
                log.debug("Omnicore Status Button clicked")
            OmnipodPlugin.getPlugin().pdm.getPodStatus()
        }

    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        disposable.add(RxBus
                .toObservable(EventOmnipodUpdateGui::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateGui() }, { FabricPrivacy.logException(it) })
        )
        disposable.add(RxBus
                .toObservable(EventTempBasalChange::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateGui() }, { FabricPrivacy.logException(it) })
        )

        loopHandler.postDelayed(refreshLoop, T.mins(1).msecs())
        updateGui()
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
        loopHandler.removeCallbacks(refreshLoop)
    }

    @Synchronized
    private fun updateGui() {
        if (L.isEnabled(L.PUMP))
            log.debug("Omnicore Fragment GUI Update")
        val omnicorePump = OmnipodPlugin.getPlugin()
        //val lastResult = OmniCoreResult.fromJson(SP.getString(R.string.key_omnicore_last_result, null));
        //val lastSuccessfulResult = OmniCoreResult.fromJson(SP.getString(R.string.key_omnicore_last_successful_result, null));

        val lastResult = omnicorePump.pdm.commandHistory.lastCommand
        val lastSuccessfulResult = omnicorePump.pdm.commandHistory.lastSuccess

        omnicorestatus_podid?.text= omnicorePump.serialNumber()
        omnicorestatus_connectionstatus?.text = omnicorePump.pdm.podStatusText
        omnicorestatus_reservoir?.text = omnicorePump.pdm.GetReservoirLevel().toString()
        val podChangeEvent = MainApp.getDbHelper().getLastCareportalEvent(CareportalEvent.SITECHANGE)
        if (podChangeEvent != null) {
            omnicorestatus_podage?.text = podChangeEvent.age()
        } else {
            omnicorestatus_podage?.text = "Not Available"
        }

        if (omnicorePump.pdm.lastStatusResponse > 0) {
            omnicorestatus_laststatustime?.text = DateUtil.minAgo(omnicorePump.pdm.lastStatusResponse)
        }
        else {
            omnicorestatus_laststatustime?.text = "Never"
        }

        if (lastResult != null) {
            omnicorestatus_lastcommand?.text = lastResult.request.requestType
            omnicorestatus_lastresult?.text = lastResult.status
            omnicorestatus_lastresulttime?.text = DateUtil.minAgo(lastResult.request.requested)

        }

        if (lastSuccessfulResult != null) {
            omnicorestatus_lastsuccess_command?.text = lastSuccessfulResult.request.requestType
            if (lastSuccessfulResult.result != null) {
                omnicorestatus_lastsuccess_time?.text = DateUtil.minAgo(lastSuccessfulResult.result.ResultDate)
            }
        }

        var historyList = ""
        val commandHistory = omnicorePump.pdm.commandHistory.allHistory
        var i = commandHistory.size
        while (i-- > 0) {
            historyList += ("Command: " + commandHistory.get(i).request.getRequestType()
                    + "\nStatus: " + commandHistory.get(i).status
                    + "\nTime: " + DateUtil.dateAndTimeString(commandHistory.get(i).request.requested)
                    + "\nProcessing: " + commandHistory.get(i).runTime + "ms\n\n")
            //     if ( _commandHistory.get(i).result != null) {
            //         historyList += "\nFullResponse: " + _commandHistory.get(i).result.asJson();
            //     }
        }

        omnicorestatus_commandhistory?.text = historyList
/*
        omnicore_connectionstatus?.text = omnicorePump.pdm.podStatusText
        omnicore_podid?.text = omnicorePump.serialNumber()

        if (lastResult != null) {
            omnicore_lastresulttime?.text = DateUtil.minAgo(lastResult.ResultDate)
            var lastResultMessage = "Success: " + lastResult.Success.toString() + "\n"
            lastResultMessage += "FullText: " + lastResult.asJson()
            omnicorepump_status?.text = lastResultMessage

        }
        else {
            omnicore_lastresulttime?.text = "No response yet"
            omnicorepump_status?.text = ""
        }

        if (lastSuccessfulResult != null) {
            omnicore_lastsuccessfulresulttime?.text = DateUtil.minAgo(lastSuccessfulResult.ResultDate)
            var lastSuccessfulResultMessage = "Success: " + lastSuccessfulResult.Success.toString() + "\n"
            lastSuccessfulResultMessage += "FullText: " + lastSuccessfulResult.asJson()
            omnicorepump_status?.text = lastSuccessfulResultMessage
        }
        else {
            omnicore_lastresulttime?.text = "No response yet"
            omnicorepump_status?.text = ""
        }
*/

   //     omnicorepump_type?.text = pumpType.description
   //     omnicorepump_type_def?.text = pumpType.getFullDescription(MainApp.gs(R.string.virtualpump_pump_def), pumpType.hasExtendedBasals())
    }
}
