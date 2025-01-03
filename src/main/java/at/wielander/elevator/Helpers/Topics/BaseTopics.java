package at.wielander.elevator.Helpers.Topics;

public enum BaseTopics {
    ELEVATOR("elevator/"),
    BUILDING("building/"),
    INFO("info/"),
    STATUS("status/"),
    CONTROL("control/");

    public final String topic;

    BaseTopics(String topic) {
        this.topic = topic;
    }
    public String getTopic() {
        return topic;
    }
}
