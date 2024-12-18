package at.wielander.elevator.Model;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal Data Model for the individual elevators.

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
 * @version 1.0
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
    protected int doorStatus;

    /** Variable for the elevator in ft/sec² */
    protected int acceleration;

    /** Variable for the elevator speed in ft/sec */
    protected int speed;

    /** Variable for the current position of the elevator in ft */
    protected int position;

    /** Variable for the buttons with the floors mapped to a logical state */
    protected ArrayList<Boolean> buttons;

    /** Variable for the current weight of the passengers in the elevator in lbs */
    protected int weight;

    /** Variable for the current capacity of the elevator in lbs */
    protected int capacity;

    /** Variable for the current serviceable floors mapped to a logical state */
    protected ArrayList<Boolean> serviceableFloors;

    /** Variable for the targeted floor */
    protected int targetedFloor;

    /** Variable for the elevator at the current floor */
    protected int currentFloor;

    /** Variable for the elevator API **/
    protected IElevator elevatorAPI;

    /** Variable for the elevator number */
    protected int elevatorNumber;

    /**
     * Constructor for internal Data Model based on IElevator interface
     *
     * @param serviceableFloors Floors to be serviced
     * @param capacity Capacity of the elevator
     * @param elevatorAPI Implements the IElevator interface
     * @param elevatorNumber Number of elevators in a system
     */
    public Elevator(ArrayList<Boolean> serviceableFloors,
                    int capacity,
                    IElevator elevatorAPI,
                    int elevatorNumber) {
        if (serviceableFloors == null || capacity <= 0) {
            throw new IllegalArgumentException("Invalid Arguments");
        }

        this.commitedDirection = IElevator.ELEVATOR_DIRECTION_UNCOMMITTED;
        this.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;
        this.acceleration = INITIAL_ACCELERATION;
        this.speed = INITIAL_SPEED;
        this.weight = INITIAL_WEIGHT;
        this.position = GROUND_FLOOR;
        this.targetedFloor = GROUND_FLOOR;
        this.currentFloor = GROUND_FLOOR;
        this.buttons = new ArrayList<Boolean>();
        this.serviceableFloors = serviceableFloors;
        this.capacity = capacity;
        this.elevatorNumber = elevatorNumber;
        this.elevatorAPI = elevatorAPI;

        // set all buttons to false within the serviceable floor range
        for (int id = 0; id < serviceableFloors.size(); id++) {
            this.buttons.add(false);
        }
    }

    /**
     * Direction in which the elevator in currently moving towards
     *
     * @return Direction of current elevator
     */
    public int getCommitedDirection() {
        return commitedDirection;
    }

    /**
     * Set the direction for the elevator
     *
     * @param committedDirection Sets the direction (UP/ DOWN /UNCOMMITTED)
     */
    public void setCommittedDirection(int committedDirection) {
        this.commitedDirection = committedDirection;
    }

    /**
     * Current acceleration of the lift in ft/s²
     *
     * @return acceleration of the elevator in ft/s²
     */
    public int getAcceleration() {
        return acceleration;
    }

    /**
     * Current the truth value for the floor buttons in the elevator
     *
     * @return Map of the buttons representing each floor and their state (TRUE /
     *         FALSE)
     */
    public ArrayList<Boolean> getButtonsInElevatorStatus() {
        return buttons;
    }

    /**
     * Current state of the doors.
     *
     * @return current state of the doors(OPEN / CLOSED / OPENING / CLOSING)
     */
    public int getElevatorDoorStatus() {
        return doorStatus;
    }

    /**
     * Current floor of the elevator to the nearest floor
     *
     * @return current floor of the elevator
     */
    public int getCurrentFloor() {
        return currentFloor;
    }

    /**
     * Current position of the elevator with respect to the ground floor
     *
     * @return position of the elevator identified by the current floor being
     *         serviced
     */
    public int getCurrentPosition() {
        return position;
    }


    /**
     * Current speed of the lift in ft/s
     *
     * @return speed of the elevator in ft/s
     */
    public int getCurrentSpeed() {
        return speed;
    }


    /**
     * Current weight of the passengers in lbs
     *
     * @return The current weight of the passengers in lbs
     */
    public int getCurrentWeight() {
        return weight;
    }

    /**
     * Capacity of the elevator
     *
     * @return returns the maximum load the elevator can handle in lbs
     */
    public int getElevatorCapacity() {
        return capacity;
    }

    /**
     * Serviceable floors for an elevator
     *
     * @return Floors as an integer mapped to a logical state (TRUE / FALSE)
     */
    public ArrayList<Boolean> getServiceableFloors() {
        return serviceableFloors;
    }

    /**
     * Setst the Serviceable floors
     *
     * @param floor   integer value of floors to be serviced
     * @param service Logic state if the floors can be accessed or not (TRUE /
     *                FALSE)
     */
    public void setServiceableFloors(int floor, boolean service) {
    	//todo throw exception if invalid value is passed floors.size() <= floor
        this.serviceableFloors.set(floor, service);
    }

    /**
     * Current targeted floor
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
     *  Updates elevator based on current states
     */
    public void update() {
        try {
            this.currentFloor = elevatorAPI.getElevatorFloor(elevatorNumber);
            this.targetedFloor = elevatorAPI.getTarget(elevatorNumber);
            this.position = elevatorAPI.getElevatorPosition(elevatorNumber);
            this.speed = elevatorAPI.getElevatorSpeed(elevatorNumber);
            this.acceleration = elevatorAPI.getElevatorAccel(elevatorNumber);
            this.weight = elevatorAPI.getElevatorWeight(elevatorNumber);
            this.doorStatus = elevatorAPI.getElevatorDoorStatus(elevatorNumber);
            this.commitedDirection = elevatorAPI.getCommittedDirection(elevatorNumber);
            for (int floor = 0; floor < buttons.size(); floor++) {
                buttons.set(floor, elevatorAPI.getElevatorButton(elevatorNumber, floor));
            }

        }catch(RemoteException e){
            e.printStackTrace();
        }
    }

}