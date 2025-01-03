package at.wielander.elevator.Helpers.Constants;

public enum ElevatorRequest {
    ELEVATOR_BUTTON(0),
    FLOOR_BUTTON(1);

    /* Index field value for elevator request */
    private final int value;

    /**
     * Sets the type of elevator request
     * @param value index value of elevator request
     */
    ElevatorRequest(int value) {
        this.value = value;
    }

    /**
     * Returns the index value of the type of request
     * @return value
     */
    public static ElevatorRequest getValue(int value) {
        for (ElevatorRequest requestType : values()) {
            if (requestType.value == value) {
                return requestType;
            }
        }
        throw new IllegalArgumentException("Invalid door state value: " + value);
    }
}
