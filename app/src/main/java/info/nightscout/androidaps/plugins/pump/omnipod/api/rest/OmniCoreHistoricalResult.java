package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import com.google.gson.JsonObject;

public class OmniCoreHistoricalResult {
        public long ResultId;
        public long ResultDate;
        public HistoricalResultType Type;
        public Boolean PodRunning;
        public String Parameters;
}
