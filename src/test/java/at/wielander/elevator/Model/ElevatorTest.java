package at.wielander.elevator.Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static at.wielander.elevator.Model.IElevator.*;
import static org.junit.jupiter.api.Assertions.*;

class ElevatorTest {

    @Mock
    public IElevator mockIElevator;

    public Elevator elevator;

    @BeforeEach
    public void setup() {


        HashMap<Integer, Boolean> serviceFloors = new HashMap<>();
        serviceFloors.put(0, true);
        serviceFloors.put(1, true);
        serviceFloors.put(2, false);
        serviceFloors.put(3, true);

        elevator = new Elevator(
                serviceFloors,
                 4000,
                mockIElevator,
                0);
    }

    @Test
    void testGetCommittedDirection(){
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.getCommitedDirection());
    }

    @Test
    void testGetLocation(){
        assertEquals(0, elevator.getCurrentPosition());
    }

    @Test
    void testGetSpeed(){
        assertEquals(0, elevator.getCurrentSpeed());
    }

    @Test
    void testGetAcceleration(){
        assertEquals(0, elevator.getAcceleration());
    }

    @Test
    void testGetButtonStatus(){

        assertFalse(elevator.getButtonsInElevatorStatus().get(0));
    }

    @Test
    void testGetElevatorCapacity(){
        assertEquals(0, elevator.getCurrentWeight());
    }

    @Test
    void testElevatorDoorState(){
        assertEquals(IElevator.ELEVATOR_DOORS_CLOSED, elevator.getElevatorDoorStatus());
    }

    @Test
    void testSetCommitedDirection() {

        elevator.setCommittedDirection(ELEVATOR_DIRECTION_UP);
        assertEquals(ELEVATOR_DIRECTION_UP, elevator.getCommitedDirection());

        elevator.setCommittedDirection(ELEVATOR_DIRECTION_DOWN);
        assertEquals(ELEVATOR_DIRECTION_DOWN, elevator.getCommitedDirection());

        elevator.setCommittedDirection(ELEVATOR_DIRECTION_UNCOMMITTED);
        assertEquals(ELEVATOR_DIRECTION_UNCOMMITTED, elevator.getCommitedDirection());

    }


    @Test
    void testSetServiceableFloors() {
        assertEquals(4, elevator.getServiceableFloors().size());
        assertTrue(elevator.getServiceableFloors().get(0));
        assertTrue(elevator.getServiceableFloors().get(1));
        assertFalse(elevator.getServiceableFloors().get(2));
        assertTrue(elevator.getServiceableFloors().get(3));
    }

    @Test
    void testSetTargetedFloor() {

    elevator.setTargetedFloor(1);
    assertEquals(1, elevator.getTargetedFloor());

    elevator.setTargetedFloor(4);
    assertEquals(4, elevator.getTargetedFloor());
    }

}