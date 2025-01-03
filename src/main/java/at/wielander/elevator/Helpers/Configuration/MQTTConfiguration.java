package at.wielander.elevator.Helpers.Configuration;

public enum MQTTConfiguration {

    BROKER_URL("tcp://localhost:1883"),
    CLIENT_ID_ADAPTER("101"),
    CLIENT_ID_ALGORITHM("126");

    private final String value;

    MQTTConfiguration(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
