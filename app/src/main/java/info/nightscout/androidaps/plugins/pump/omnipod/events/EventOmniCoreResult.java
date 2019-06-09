package info.nightscout.androidaps.plugins.pump.omnipod.events;

import info.nightscout.androidaps.events.Event;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmniCoreResult;

public class EventOmniCoreResult extends Event {
    private final OmniCoreResult _result;

    public EventOmniCoreResult(OmniCoreResult result)
    {
        _result = result;
    }

    public OmniCoreResult getResult()
    {
        return _result;
    }
}
