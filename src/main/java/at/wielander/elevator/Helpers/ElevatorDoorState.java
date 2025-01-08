package at.wielander.elevator.Helpers;

/**
 * Enums for Elevator Door State
 */
public enum ElevatorDoorState {
    OPEN(1),
    CLOSED(2),
    OPENING(3),
    CLOSING(4);

    /* Index value for elevator door state */
    private final int value;

    /**
     * Set the elevator Door state
     * @param value Index value
     */
    ElevatorDoorState(int value) {
        this.value = value;
    }

    /**
     * Gets the index value of the door state
     * @return value
     */
    public static ElevatorDoorState getValue(int value) {
        for (ElevatorDoorState doorState : values()) {
            if (doorState.value == value) {
                return doorState;
            }
        }
        throw new IllegalArgumentException("Invalid door state value: " + value);
    }
}

