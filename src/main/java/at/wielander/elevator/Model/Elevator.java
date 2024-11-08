package at.wielander.elevator.Model;

import at.fhhagenberg.sqelevator.IElevator;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal Data Model for the individual elevators.
 *
 * This class represents the physical elevator and its attributes. The behaviors
 * are inherited
 * from the provided interface {@link IElevator} as follows:
 *
 * <ul>
 * <li><i>Elevator capacity:</i> The maximum number of passengers that can fit
 * in an elevator.</li>
 * <li><i>Elevator speed:</i> The maximum speed an elevator can travel at in
 * feet/sec. It is assumed to be the same for
 * all elevators.</li>
 * <li><i>Elevator acceleration:</i> The rate at which the elevator can increase
 * or decrease
 * speed in ft/sec². It is assumed to be the same for all elevators. The higher
 * the acceleration,
 * the faster the elevator can accelerate and decelerate, allowing its average
 * speed to be faster.</li>
 * <li><i>Number of floors:</i> The number of floors in the building, including
 * the ground floor. It is
 * assumed there are no floors below ground level. All elevators service the
 * ground floor but may, as described below, not necessarily service all other
 * floors. Each floor has
 * an up and a down button for the purposes of passengers calling the elevator.
 * Floor numbering starts
 * at 0 for floor 1.</li>
 * <li><i>Floor height:</i> The height of the floors in feet. It is assumed that
 * each floor is
 * the same height.</li>
 * <li><i>Number of elevators:</i> The number of elevators in the building.
 * Elevator numbering starts
 * at zero for elevator 1.</li>
 * </ul>
 *
 * @version 0.1
 */
public class Elevator {

    /** Constant state variable for the initial position in ft */
    private static final int GROUND_FLOOR = 0;

    /** Constant state variable for the initial speed in ft/s */
    private static final int INITIAL_SPEED = 0;

    /** Constant state variable for the initial acceleration in ft/s² */
    private static final int INITIAL_ACCELERATION = 0;

    /** Constant state variable for the initial capacity in lbs */
    private static final int INITIAL_WEIGHT = 0;

    /** Variable for the direction of the elevator */
    private int commitedDirection;

    /** Variable of the state of the doors */
    protected int doorState;

    /** Variable for the elevator in ft/sec² */
    protected int acceleration;

    /** Variable for the elevator speed in ft/sec */
    protected int speed;

    /** Variable for the current position of the elevator in ft */
    private int position;

    /** Variable for the buttons with the floors mapped to a logical state */
    protected Map<Integer, Boolean> buttons;

    /** Variable for the current weight of the passengers in the elevator in lbs */
    protected int weight;

    /** Variable for the current capacity of the elevator in lbs */
    protected int capacity;

    /** Variable for the current serviceable floors mapped to a logical state */
    protected Map<Integer, Boolean> serviceableFloors;

    /** Variable for the targeted floor */
    protected int targetedFloor;

    private int currentFloor;

    private IElevator elevatorAPI;

    private int elevatorNumber;

    /**
     * Default constructor for the elevators. Initialise the variables required for
     * the base elevator
     *
     * @param serviceableFloors Floors that are included in the elevators service
     *                          plan
     * @param capacity          Capacity of the elevator in lbs
     */
    public Elevator(Map<Integer, Boolean> serviceableFloors, int capacity, IElevator elevatorAPI, int elevatorNumber) {
        if (serviceableFloors == null || capacity <= 0) {
            throw new IllegalArgumentException("Invalid Arguments");
        }

        this.commitedDirection = IElevator.ELEVATOR_DIRECTION_UNCOMMITTED;
        this.doorState = IElevator.ELEVATOR_DOORS_CLOSED;
        this.acceleration = INITIAL_ACCELERATION;
        this.speed = INITIAL_SPEED;
        this.weight = INITIAL_WEIGHT;
        this.targetedFloor = GROUND_FLOOR;
        this.buttons = new HashMap<>();
        this.serviceableFloors = serviceableFloors;
        this.capacity = capacity;

        this.currentFloor = 0;
        this.elevatorNumber = elevatorNumber;
        this.elevatorAPI = elevatorAPI;

        // set all buttons to false within the serviceable floor range
        for (int id : this.serviceableFloors.keySet()) {
            if (serviceableFloors.get(id) != null) {
                buttons.put(id, false);
            } else {
                System.err.println("Serviceable floor state at " + id + " not specified");
            }
        }
    }

    /**
     * Returns the direction in which the elevator in currently moving towards (UP/
     * DOWN /UNCOMMITTED)
     * 
     * @return Direction of current elevator
     */
    public int getCommitedDirection() {
        return this.commitedDirection;
    }

    /**
     * Sets the direction in which the elevator should head
     *
     * @param commitedDirection Sets the direction (UP/ DOWN /UNCOMMITTED)
     */
    public void setCommitedDirection(int commitedDirection) {
        this.commitedDirection = commitedDirection;
    }

    /**
     * Obtain the current state of the doors.
     *
     * @return current state of the doors(OPEN / CLOSED / OPENING / CLOSING)
     */
    public int getDoorState() {
        return doorState;
    }

    /**
     * Method to return the current acceleration of the lift in ft/s²
     *
     * @return acceleration of the elevator in ft/s²
     */
    public int getAcceleration() {
        return acceleration;
    }

    /**
     * Method to return the current speed of the lift in ft/s
     *
     * @return speed of the elevator in ft/s
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Return the truth value fo the buttons mapped to a logical state
     * representing the buttons either as pressed or depressed.
     *
     * @return Map of the buttons representing each floor and their state (TRUE /
     *         FALSE)
     */
    public Map<Integer, Boolean> getButtons() {
        return buttons;
    }

    /**
     * Returns the current position of the elevator with respect to the ground floor
     *
     * @return position of the elevator identified by the current floor being
     *         serviced
     */
    public int getLocation() {
        return position;
    }

    /**
     * Method to obtain the current weight of the passengers in lbs
     *
     * @return The current weight of the passengers in lbs
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Returns the capacity of the elevator
     *
     * @return returns the maximum load the elevator can handle in lbs
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns a list of serviceable floors mapped to a truth value
     *
     * @return Floors as an integer mapped to a logical state (TRUE / FALSE)
     */
    public Map<Integer, Boolean> getServiceableFloors() {
        return serviceableFloors;
    }

    /**
     * Sets the serviceable floors
     *
     * @param floor   integer value of floors to be serviced
     * @param service Logic state if the floors can be accessed or not (TRUE /
     *                FALSE)
     */
    public void setServiceableFloors(int floor, boolean service) {
        this.serviceableFloors.put(floor, service);
    }

    /**
     * Return the current targeted floors
     *
     * @return current targeted floors
     */
    public int getTargetedFloor() {
        return targetedFloor;
    }

    /**
     * Sets the current targeted floor
     * 
     * @param targetedFloor Sets the current targeted floor as integer
     */
    public void setTargetedFloor(int targetedFloor) {
        this.targetedFloor = targetedFloor;
    }

    /**
     * Returns the current floor of the elevator
     * 
     * @return current floor of the elevator
     */
    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getElevatorNumber() {
        return elevatorNumber;
    }

    /**
     * Uses the IElevator API to update the elevator's state
     */
    public void update() {
        try {
            this.currentFloor = elevatorAPI.getElevatorFloor(elevatorNumber);
            this.targetedFloor = elevatorAPI.getTarget(elevatorNumber);
            this.speed = elevatorAPI.getElevatorSpeed(elevatorNumber);
            this.weight = elevatorAPI.getElevatorWeight(elevatorNumber);
            this.doorState = elevatorAPI.getElevatorDoorStatus(elevatorNumber);
            // Update other attributes as needed
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
