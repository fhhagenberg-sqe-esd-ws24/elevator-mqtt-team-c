package at.wielander.elevator.Helpers;

/**
 * Enum class for Elevator Direction
 */
public enum ElevatorCommittedDirection {
    UP(0),
    DOWN(1),
    UNCOMMITTED(2);

    /* Index field for enums */
    private final int value;

    /**
     * Sets the committed direction of the elevators
     *
     * @param value index field
     */
    ElevatorCommittedDirection(int value) {

        this.value = value;
    }

    /**
     * Returns the index value of the enums
     *
     * @return index value of the enums
     */
    public static ElevatorCommittedDirection getValue(int value) {
        for (ElevatorCommittedDirection direction : values()) {
            if (direction.value == value) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Invalid direction value: " + value);
    }}
