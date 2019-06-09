package info.nightscout.androidaps.plugins.pump.omnipod.exceptions;

public class OmniCoreException extends Exception {
    public Exception innerException;

    public OmniCoreException() {}
    public OmniCoreException(Exception e)
    {
        innerException = e;
    }
}
