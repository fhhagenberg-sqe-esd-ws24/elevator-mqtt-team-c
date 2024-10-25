package at.wielander.elevator.Model;

import at.fhhagenberg.sqelevator.IElevator;
import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;

import static at.fhhagenberg.sqelevator.IElevator.*;


class ElevatorTest {

    private static final int INITIAL_VALUE = 0;

    private static final int ELEVATOR_CAPACITY = 4000;

    private static final int TARGETED_FLOOR_ONE = 1;

    private static final int TARGETED_FLOOR_FOUR = 4;

    private HashMap<Integer, Boolean> serviceFloors;

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

        /* Arrange the service floors */
        serviceFloors = new HashMap<>();

        /* Initialise an elevator with a capacity of 4000 lbs */
        elevator = new Elevator(serviceFloors, ELEVATOR_CAPACITY);
    }

    @AfterEach
    public void teardown() {
        System.out.println("Teardown called. ");
        elevator = null;
    }

    @Test
    void testGetCommittedDirection(){
        /* Assert if the direction of the elevator is UNCOMMITTED by default */
        Assertions.assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.getCommitedDirection(),
                "Test passed: Elevator is in UNCOMMITTED state by default");
    }

    @Test
    void testGetLocation(){
        /* Assert if the acceleration of the elevator is ZERO by default */
        Assertions.assertEquals(INITIAL_VALUE, elevator.getLocation(),
                "Test Passed: Elevator starts at ground floor");
    }

    @Test
    void testGetSpeed(){
        /* Assert if the speed of the elevator is ZERO by default */
        Assertions.assertEquals(INITIAL_VALUE, elevator.getSpeed(),
                "Test passed: Elevator speed should be initialized to 0 feet");
    }

    @Test
    void testGetAcceleration(){
        /* Assert if the acceleration of the elevator is ZERO by default */
        Assertions.assertEquals(INITIAL_VALUE, elevator.getAccelleration(),
                "Test Passed: Elevator acceleration should be initialized to 0 ft/s²");
    }

    @Test
    void testGetButtonStatus(){
        /* Assert if the elevator button of ground floor is not pressed */
        Assertions.assertNull(elevator.getButtons().get(0),
                "Test passed: Button for ground floor not pressed.");
    }

    @Test
    void testGetElevatorCapacity(){
        /* Assert if the elevator doors are initially closed */
        Assertions.assertEquals(INITIAL_VALUE, elevator.getWeight(),
                "Test Passed: Elevator weight should be unloaded by default");
    }

    @Test
    void testElevatorDoorState(){
        /* Assert if the elevator doors are initially closed */
        Assertions.assertEquals(IElevator.ELEVATOR_DOORS_CLOSED, elevator.getDoorState(),
                "Test Passed: Door are CLOSED by default");
    }

    @Test
    void testSetCommitedDirection() {

        /* Change the direction of the elevator to down. Assert if the elevator is heading UP */
        elevator.setCommitedDirection(ELEVATOR_DIRECTION_UP);
        Assertions.assertEquals(ELEVATOR_DIRECTION_UP, elevator.getCommitedDirection(),
                "Test Passed: Elevator direction is moving upwards");

        /* Change the direction of the elevator to down. Assert if the elevator is heading DOWN */
        elevator.setCommitedDirection(ELEVATOR_DIRECTION_DOWN);
        Assertions.assertEquals(ELEVATOR_DIRECTION_DOWN, elevator.getCommitedDirection(),
                "Test Passed: Elevator direction is moving downwards");

        /* Change the direction of the elevator to IDLE state. Assert if the elevator is UNCOMMITTED */
        elevator.setCommitedDirection(ELEVATOR_DIRECTION_UNCOMMITTED);
        Assertions.assertEquals(ELEVATOR_DIRECTION_UNCOMMITTED, elevator.getCommitedDirection(),
                "Test Passed: Elevator direction is UNCOMMITTED and in idle state");
    }


    @Test
    void testSetServiceableFloors() {

        /* Arrange the floor setup */
        elevator.setServiceableFloors(0, true);  // Ground Floor is serviceable
        elevator.setServiceableFloors(1, true);  // First Floor is serviceable
        elevator.setServiceableFloors(2, false); // Second Floor is NOT serviceable
        elevator.setServiceableFloors(3, false); // Third Floor is NOT serviceable
        elevator.setServiceableFloors(4, true);  // Fourth Floor is serviceable

        /* Assert if only three floors are serviceable */
        Assertions.assertEquals(5, elevator.getServiceableFloors().size(),
                "Test Passed: Currently only three floors are serviceable");

        /* Assert if ground floor is serviceable */
        Assertions.assertTrue(elevator.getServiceableFloors().get(0),
                "Test Passed: Ground Floor is serviceable");

        /* Assert if first floor is serviceable */
        Assertions.assertTrue(elevator.getServiceableFloors().get(1),
                "Test Passed: First Floor is serviceable");

        /* Assert if second floor is NOT serviceable */
        Assertions.assertFalse(elevator.getServiceableFloors().get(2),
                "Test Passed: Second Floor is NOT serviceable and does not exist");

        /* Assert if third  floor is NOT serviceable */
        Assertions.assertFalse(elevator.getServiceableFloors().get(3),
                "Test Passed: Third Floor is NOT serviceable and does not exist");

        /* Assert if first floor is serviceable */
        Assertions.assertTrue(elevator.getServiceableFloors().get(4),
                "Test Passed: First Floor is serviceable");

    }

    @Test
    void testSetTargetedFloor() {

    /* Set the 1st floor as the targeted floor and assert */
    elevator.setTargetedFloor(TARGETED_FLOOR_ONE);
    Assertions.assertEquals(TARGETED_FLOOR_ONE, elevator.getTargetedFloor(),
            "Test Passed: First Floor is the targeted floor");

    /* Set the 4th floor as the targeted floor and assert */
    elevator.setTargetedFloor(TARGETED_FLOOR_FOUR);
    Assertions.assertEquals(TARGETED_FLOOR_FOUR, elevator.getTargetedFloor(),
            "Test Passed: Fourth Floor is the targeted floor");
    }
}