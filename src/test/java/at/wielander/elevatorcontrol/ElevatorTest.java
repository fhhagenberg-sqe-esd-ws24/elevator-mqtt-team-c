package at.wielander.elevatorcontrol;

import at.wielander.elevatorcontrol.Model.Elevator;
import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;

import static at.fhhagenberg.sqelevator.IElevator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ElevatorTest {

    private static final int INITIAL_SERVICEABLE_FLOORS = 6;

    private static final int ELEVATOR_CAPACITY = 4000;

    private static final int TARGETED_FLOOR_ONE = 1;

    private static final int TARGETED_FLOOR_FOUR = 4;

    private Elevator elevator;

    @BeforeAll
    public static void setupOnce(){
        System.out.println("setupOnce called");
    }

    @AfterAll
    public static void tearDownOnce(){
        System.out.println("tearDownOnce called");
    }

    @BeforeEach
    public void setup() {

        System.out.println("Setup called. Creating Elevator");

        /* Arrange: Set up a building with one elevator */
        HashMap<Integer, Boolean> serviceFloors = new HashMap<>();
        serviceFloors.put(0, true);
        serviceFloors.put(1, true);
        serviceFloors.put(2, false);
        serviceFloors.put(3, false);
        serviceFloors.put(4, true);
        serviceFloors.put(5, false);

        /* Initialise an elevator with a capacity of 4000 lbs */
        elevator = new Elevator(serviceFloors, ELEVATOR_CAPACITY);

    }

    @AfterEach
    public void teardown() {
        System.out.println("Teardown called. ");
    }

    @Test
    void getCapacity() {
        assertEquals(ELEVATOR_CAPACITY, elevator.getCapacity());
    }

    @Test
    void setCommitedDirection() {
        /* Assert if the initial state of the elevator is UP */
        elevator.setCommitedDirection(ELEVATOR_DIRECTION_UP);
        Assertions.assertEquals(ELEVATOR_DIRECTION_UP, elevator.getCommitedDirection());

        /* Change the direction of the elevator to down. Assert if the elevator is heading DOWN */
        elevator.setCommitedDirection(ELEVATOR_DIRECTION_DOWN);
        Assertions.assertEquals(ELEVATOR_DIRECTION_DOWN, elevator.getCommitedDirection());

        /* Change the direction of the elevator to IDLE state. Assert if the elevator is UNCOMMITTED */
        elevator.setCommitedDirection(ELEVATOR_DIRECTION_UNCOMMITTED);
        Assertions.assertEquals(ELEVATOR_DIRECTION_UNCOMMITTED, elevator.getCommitedDirection());
    }

    @Test
    void getServiceableFloors() {

        /* Obtain the current servicable floors */
        Map<Integer, Boolean> floors = elevator.getServiceableFloors();

        /* Assert if the floors are servicable */
        Assertions.assertEquals(INITIAL_SERVICEABLE_FLOORS, floors.size());
        Assertions.assertTrue(floors.get(0));
        Assertions.assertTrue(floors.get(1));
        Assertions.assertFalse(floors.get(2));
        Assertions.assertFalse(floors.get(3));
        Assertions.assertTrue(floors.get(4));
        Assertions.assertFalse(floors.get(5));
    }

    @Test
    void setServiceableFloors() {
    /*Obtain the current servicable floors */
    Map<Integer, Boolean> floors = elevator.getServiceableFloors();

    /* Change the servicable status of floor 1st, 3rd and 5th */
    elevator.setServiceableFloors(1,false);
    elevator.setServiceableFloors(3,true);
    elevator.setServiceableFloors(4,false);

    /* Assert if the changes has been accounted for */
    Assertions.assertFalse(floors.get(1));
    Assertions.assertTrue(floors.get(3));
    Assertions.assertFalse(floors.get(4));

    }

    @Test
    void setTargetedFloor() {

    /* Set the 1st floor as the targeted floor and assert */
    elevator.setTargetedFloor(TARGETED_FLOOR_ONE);
    Assertions.assertEquals(TARGETED_FLOOR_ONE, elevator.getTargetedFloor());

    /* Set the 1st floor as the targeted floor and assert */
    elevator.setTargetedFloor(TARGETED_FLOOR_FOUR);
    Assertions.assertEquals(TARGETED_FLOOR_FOUR, elevator.getTargetedFloor());
    }

}