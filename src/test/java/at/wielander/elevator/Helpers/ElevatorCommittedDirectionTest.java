package at.wielander.elevator.Helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorCommittedDirectionTest {

    @Test
    void givenIntegerValue_whenObtainValue_thenExpectCommitedDirection() {
        assertEquals(ElevatorCommittedDirection.UP, ElevatorCommittedDirection.getValue(0));
        assertEquals(ElevatorCommittedDirection.DOWN, ElevatorCommittedDirection.getValue(1));
        assertEquals(ElevatorCommittedDirection.UNCOMMITTED, ElevatorCommittedDirection.getValue(2));
    }

    @Test
    void givenIntegerValue_whenWrongOrdinalGiven_thenExpectIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ElevatorCommittedDirection.getValue(3));
        assertEquals("Invalid direction value: 3", exception.getMessage());
    }
}