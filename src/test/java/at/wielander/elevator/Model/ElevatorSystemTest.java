package at.wielander.elevator.Model;

import at.fhhagenberg.sqelevator.IElevator;

import org.junit.jupiter.api.*;

import java.rmi.RemoteException;

class ElevatorSystemTest {

    private ElevatorSystem elevatorSystem;


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

        /* Initialise a new elevator system */
        elevatorSystem = new ElevatorSystem(10,
                0,
                4,
                1000,
                7);
    }


    @AfterEach
    public void teardown() {
        System.out.println("Teardown called. ");
    }

    @Test
    void testZeroNumberOfElevators() throws RemoteException {
        /* Assert if zero elevators were initialised */
        ElevatorSystem zeroElevatorsSystem = new ElevatorSystem(
                0,
                0,
                4,
                1000,
                7);
        Assertions.assertEquals(0,zeroElevatorsSystem.getElevatorNum(),
                " Test FAILED: Zero elevators should be initialised.");

    }

    @Test
    void testMultipleElevatorSetup() throws RemoteException {
        /* Assert if five elevators were initialised */
        Assertions.assertEquals(10, elevatorSystem.getElevatorNum(),
                " Test FAILED: Five elevators should be initialised.");
    }

    @Test
    void testGetElevatorCapacity() throws RemoteException {
        /* Assert if the capacity of the elevator set to 1000 */
        Assertions.assertEquals(1000,elevatorSystem.getElevatorCapacity(2),
                "Test FAILED: Elevator #2 elevators should have capacity of 1000 lbs.");

        /* Assert if the capacity of the elevator set to 1000 */
        Assertions.assertEquals(1000,elevatorSystem.getElevatorCapacity(3),
                "Test FAILED: Elevator #3 elevators should have capacity of 1000 lbs.");

    }

    @Test
    void testGetFloorHeight() throws RemoteException {
        /* Assert if floor height is initially set to 7 ft */
        Assertions.assertEquals(7,elevatorSystem.getFloorHeight(),
                "Test FAILED: Floor Height is initially set at 7 feet.");

    }

    @Test
    void testGetFloorNum() throws RemoteException {
        Assertions.assertEquals(4 ,elevatorSystem.getFloorNum(),
                "Test FAILED: The elevators services 5 elevators at start.");
    }

    @Test
    void testSetServicesFloors() throws RemoteException {

        /* Arrange the floor setup */
        elevatorSystem.setServicesFloors(0, 0, true);  // Ground Floor is serviceable
        elevatorSystem.setServicesFloors(1, 4, true);  // First Floor is serviceable
        elevatorSystem.setServicesFloors(2, 2, false);// Second Floor is NOT serviceable
        elevatorSystem.setServicesFloors(3, 5, false); // Third Floor is NOT serviceable
        elevatorSystem.setServicesFloors(4, 3, true);  // Fourth Floor is serviceable

        /* Assert for invalid service floor for elevator */
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            elevatorSystem.setServicesFloors(-1, 0, true);
        }, "Test FAILED: Floor -1 should be INVALID");

        /* Assert for invalid floor in building for elevator */
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            elevatorSystem.setServicesFloors(5, 0, true);
        }, "Test FAILED: 5th Floor is INVALID and should NOT exist on building mapping .");

        /* Assert if floors are serviceable */
        Assertions.assertTrue(elevatorSystem.getServicesFloors(0, 0),
                "Test FAILED: This elevator should be serving the ground floor");
        Assertions.assertTrue(elevatorSystem.getServicesFloors(1, 4),
                "Test FAILED: This elevator should be serving the fourth floor");
        Assertions.assertFalse(elevatorSystem.getServicesFloors(2, 2),
                "Test FAILED: This elevator should be NOT serving the second floor");

    }

    @Test
    void testSetCommittedDirection()throws RemoteException {

        /* Assert if elevator 1 moves up */
        elevatorSystem.setCommittedDirection(1, IElevator.ELEVATOR_DIRECTION_UP);
        Assertions.assertEquals(IElevator.ELEVATOR_DIRECTION_UP,
                elevatorSystem.getCommittedDirection(1),
                "Test FAILED: Elevator 1 should head UP ");

        /* Assert if elevator 2 moves down */
        elevatorSystem.setCommittedDirection(2, IElevator.ELEVATOR_DIRECTION_DOWN);
        Assertions.assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN,
                elevatorSystem.getCommittedDirection(2),
                "Test FAILED: Elevator 2 should head DOWN");

        /* Assert if elevator 3 is IDLE / UNCOMMITTED */
        elevatorSystem.setCommittedDirection(3, IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
        Assertions.assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED,
                elevatorSystem.getCommittedDirection(3),
                "Test FAILED: Elevator 3 should be UNCOMMITTED and IDLE");

    }

    @Test
    void testSetTarget() {

        /* Initialise */
        ElevatorSystem testElevatorTargets = new ElevatorSystem(
                10,
                0,
                4,
                1000,
                7);

        /* Assert if an invalid floor on building map can be set */
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            testElevatorTargets.setTarget(-1, 5);
        }, "Test FAILED: Floor -1 should is INVALID.");

        /* Assert if INVALID elevator's target can be set */
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            testElevatorTargets.setTarget(5, 5);
        }, "Test FAILED: Elevator DOES not exist");
    }
}