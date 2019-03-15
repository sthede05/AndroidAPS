package info.nightscout.androidaps.plugins.pump.omnipod.events;

import info.nightscout.androidaps.events.Event;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyApiSecret;

public class EventOmnipyConfigurationComplete extends Event {

    public String hostName;
    public OmnipyApiSecret apiSecret;
    public boolean isDiscovered;
    public boolean isConnectable;
    public boolean isAuthenticated;

    public EventOmnipyConfigurationComplete(String host,
                                            OmnipyApiSecret secret,
                                            boolean discovered,
                                            boolean connectable, boolean authenticated)
    {
        hostName = host;
        apiSecret = secret;
        isDiscovered = discovered;
        isConnectable = connectable;
        isAuthenticated = authenticated;
    }
}