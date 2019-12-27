package info.nightscout.androidaps.plugins.pump.omnipod.history;

public enum OmnicoreCommandHistoryStatus {
    NEW("New"),
    PENDING("Pending"),
    SUCCESS("Success"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    UNKNOWN("Unknown");



    private String description;

    OmnicoreCommandHistoryStatus(String description) {

        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
