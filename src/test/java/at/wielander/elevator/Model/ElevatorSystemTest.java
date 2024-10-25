package at.wielander.elevator.Model;

import org.junit.jupiter.api.*;

import java.rmi.RemoteException;

class ElevatorSystemTest {

    private static final int GROUND_FLOOR = 0;

    private static final int ZERO_ELEVATORS = 0;

    private static final int TOTAL_ELEVATORS = 5;

    private static final int HIGHEST_FLOOR = 4;

    private static final int CAPACITY_ELEVATOR = 1000;

    private static final int FLOOR_HEIGHT = 7;

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
        elevatorSystem = new ElevatorSystem(TOTAL_ELEVATORS,
                GROUND_FLOOR,
                HIGHEST_FLOOR,
                CAPACITY_ELEVATOR,
                FLOOR_HEIGHT);

    }

    @AfterEach
    public void teardown() {
        System.out.println("Teardown called. ");
    }

    @Test
    void testZeroNumberOfElevators() throws RemoteException {
        /* Assert if zero elevators were initialised */
        ElevatorSystem zeroElevatorsSystem = new ElevatorSystem(
                ZERO_ELEVATORS,
                GROUND_FLOOR,
                HIGHEST_FLOOR,
                CAPACITY_ELEVATOR,
                FLOOR_HEIGHT);
        Assertions.assertEquals(zeroElevatorsSystem.getElevatorNum(), ZERO_ELEVATORS,
                " Test Passed: Zero elevators were initialised.");
    }

    @Test
    void testMultipleElevatorSetup() throws RemoteException {
        /* Assert if five elevators were initialised */
        Assertions.assertEquals(TOTAL_ELEVATORS, elevatorSystem.getElevatorNum(),
                " Test Passed: Five elevators were initialised.");
    }

    @Test
    void getElevatorCapacity() throws RemoteException {
        /* Assert if the capacity of the elevator set to 1000 */
        Assertions.assertEquals(CAPACITY_ELEVATOR,elevatorSystem.getElevatorCapacity(2),
                "Test Passed: Elevator #2 elevators has a capacity of 1000 lbs.");

        /* Assert if the capacity of the elevator set to 1000 */
        Assertions.assertEquals(CAPACITY_ELEVATOR,elevatorSystem.getElevatorCapacity(3),
                "Test Passed: Elevator #3 elevators has a capacity of 1000 lbs.");
    }

    @Test
    void getFloorHeight() throws RemoteException {
        /* Assert if floor height is initially set to 7 ft */
        Assertions.assertEquals(FLOOR_HEIGHT,elevatorSystem.getFloorHeight(),
                "Test Passed: Floor Height is initially set at 7 feet.");
    }

    @Test
    void getFloorNum() throws RemoteException {
        /* Assert if floor height is initially set to 5 floors */
        Assertions.assertEquals(HIGHEST_FLOOR ,elevatorSystem.getFloorNum(),
                "Test Passed: The elevators services 5 elevators at start.");
    }

    @Test
    void setServicesFloors() throws RemoteException {

        /* Arrange the floor setup */
        elevatorSystem.setServicesFloors(0, 0, true);  // Ground Floor is serviceable
        elevatorSystem.setServicesFloors(1, 4, true);  // First Floor is serviceable
        elevatorSystem.setServicesFloors(2, 2, false);// Second Floor is NOT serviceable
        elevatorSystem.setServicesFloors(3, 5, false); // Third Floor is NOT serviceable
        elevatorSystem.setServicesFloors(4, 3, true);  // Fourth Floor is serviceable

        /* Assert if only first floors are serviceable */
        Assertions.assertTrue(elevatorSystem.getServicesFloors(0, 0),
                "Test Passed: This elevator is serving the ground floor");

        /* Assert if only three floors are serviceable */
        Assertions.assertTrue(elevatorSystem.getServicesFloors(1, 4),
                "Test Passed: This elevator is serving the fourth floor");

        /* Assert if only first floors are serviceable */
        Assertions.assertTrue(elevatorSystem.getServicesFloors(2, 0),
                "Test Passed: This elevator is NOT serving the second floor");

    }
}