package at.wielander.elevator.Helpers.Configuration;

public enum MQTTConfiguration {

    RMI_CONTROLLER("rmi://localhost/ElevatorSim"),
    CLIENT_ID("mqttAdapter"),
    BROKER_URL("tcp://localhost:1883");

    private final String value;

    MQTTConfiguration(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
