package at.wielander.elevator.Helpers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorDoorStateTest {

    @Test
    void givenIntegerValue_whenObtainValue_thenExpectDoorState() {
        assertEquals(ElevatorDoorState.OPEN, ElevatorDoorState.getValue(1));
        assertEquals(ElevatorDoorState.CLOSED, ElevatorDoorState.getValue(2));
        assertEquals(ElevatorDoorState.OPENING, ElevatorDoorState.getValue(3));
        assertEquals(ElevatorDoorState.CLOSING, ElevatorDoorState.getValue(4));

    }

    @Test
    void givenIntegerValue_whenWrongOrdinalGiven_thenExpectIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> ElevatorDoorState.getValue(0));
        assertEquals("Invalid door state value: 0", exception.getMessage());
    }

}