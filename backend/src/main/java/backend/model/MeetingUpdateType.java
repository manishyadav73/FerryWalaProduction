package backend.model;

/** Arrival / coordination updates one party can send the other for a request. */
public enum MeetingUpdateType {
    RUNNING_LATE(true),
    PROPOSE_TIME(true),
    ARRIVED(false),
    CANT_FIND(false),
    NEED_HELP(false);

    private final boolean needsEta;

    MeetingUpdateType(boolean needsEta) {
        this.needsEta = needsEta;
    }

    public boolean needsEta() {
        return needsEta;
    }
}
