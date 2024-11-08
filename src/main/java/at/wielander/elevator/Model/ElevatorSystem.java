package at.wielander.elevator.Model;

/**
 * Represents the control structure for monitoring various elevator configurations in the building .
 *
 * This class manages both Load and Passenger elevators in multiple configurations as required for any building. Various
 * elevator physical and behavioural attributes inherited from the Elevator class can be controlled as well as accessed
 * via the methods of MQTT Broker as well as the controller, which will be implemented in the later stages of this assignment.
 *
 * @version 0.1
 */

import at.fhhagenberg.sqelevator.IElevator;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Constructor for multiple configurations of the elevator system.
 */
public class ElevatorSystem implements IElevator {

    private final int lowestFloor;
    private final int highestFloor;
    private List<Elevator> elevators;
    private final int floorHeight;
    private boolean downButtonPress;
    private boolean upButtonPress;
    private long clockTick;
    private IElevator elevatorAPI;

    /**
     * Creates a configuration of elevators for a building
     *
     * @param numElevator  Number of elevators in the building
     * @param lowestFloor  Lowest floor accessible by the elevator
     * @param highestFloor Highest floor accessible by the elevator
     * @param capacity     Maximum capacity of the elevator in lbs
     * @param floorHeight  Height of each floor to be given in ft
     */
    public ElevatorSystem(final int numElevator, final int lowestFloor, final int highestFloor, final int capacity,
            final int floorHeight, final IElevator elevatorAPI) {

        this.elevatorAPI = elevatorAPI;
        this.lowestFloor = lowestFloor;
        this.highestFloor = highestFloor;
        elevators = new ArrayList<>();
        this.floorHeight = floorHeight;
        this.downButtonPress = false;
        this.upButtonPress = false;

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        for (int i = lowestFloor; i <= highestFloor; i++) {
            serviceableFloors.put(i, true);
        }

        for (int i = 0; i < numElevator; i++) {
            if (elevators != null) {
                elevators.add(new Elevator(serviceableFloors, capacity, this.elevatorAPI, i));
                elevators.get(i).update();
            }
        }
    }

    /**
     * Adds a new elevator with a default capacity for all servicable floors.
     * Forwards the elevatorAPI for RMI access to PLC
     * and provides a number for the elevator.
     * 
     * @param elevatorNumber number of the elevator
     * @param elevatorAPI    RMI API for the elevator
     */
    public void addElevator(final int elevatorNumber, IElevator elevatorAPI) {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        for (int i = lowestFloor; i <= highestFloor; i++) {
            serviceableFloors.put(i, true);
        }

        elevators.add(new Elevator(serviceableFloors, elevators.get(0).getCapacity(), elevatorAPI, elevatorNumber));
    }

    /**
     * Returns the direction of the elevator heading
     *
     * @param elevatorNumber - elevator number whose committed direction is being
     *                       retrieved
     * @return The committed direction (movement) of the elevator (UP / DOWN /
     *         UNCOMMITTED)
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getCommittedDirection(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getCommitedDirection();
    }

    /**
     * Returns the elevator acceleration
     *
     * @param elevatorNumber - elevator number whose acceleration is being retrieved
     * @return Acceleration of elevator in ft/s²
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorAccel(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getAcceleration();
    }

    /**
     * Returns the buttons and its respective logical states
     *
     * @param elevatorNumber - elevator number whose button status is being
     *                       retrieved
     * @param floor          - floor number button being checked on the selected
     *                       elevator
     * @return Returns Mapping of elevator buttons and the
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public boolean getElevatorButton(int elevatorNumber, int floor) throws RemoteException {

        /** check for service range of elevator */
        if (!elevators.get(elevatorNumber).getServiceableFloors().get(floor)) {
            System.err.println("Floor " + floor + " not within elevator service range.");
        }
        return elevators.get(elevatorNumber).getButtons().get(floor);
    }

    /**
     * Returns the door status of elevator
     *
     * @param elevatorNumber - elevator number whose door status is being retrieved
     * @return Door status (OPEN / OPENING / CLOSE / CLOSING) of respective elevator
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorDoorStatus(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getDoorState();
    }

    /**
     * Returns the current floor being serviced
     * 
     * @param elevatorNumber - elevator number whose location is being retrieved
     * @return Current position of elevator with respect to the floor layout of the
     *         building
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorFloor(int elevatorNumber) throws RemoteException {
        return (int) Math.round((double) elevators.get(elevatorNumber).getLocation() / (double) this.floorHeight);
    }

    /***
     * Returns the current elevator ID
     * 
     * @return interger value of elevator ID
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorNum() throws RemoteException {
        return elevators.size();
    }

    /**
     * Returns the elevator location with respect to building layout
     * 
     * @param elevatorNumber - elevator number whose location is being retrieved
     * @return Current Elevator position
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorPosition(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getLocation();
    }

    /**
     * Return speed of the elevator
     * 
     * @param elevatorNumber - elevator number whose speed is being retrieved
     * @return Speed of the elevator in ft/s
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorSpeed(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getSpeed();
    }

    /**
     * Return the weight of the elevator
     *
     * @param elevatorNumber - elevator number whose service is being retrieved
     * @return Current weight of the elevator in lbs
     * @throws RemoteException RMI Invalid exception
     */
    @Override
    public int getElevatorWeight(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getWeight();
    }

    /**
     * Returns the set capacity of the elevator
     * 
     * @param elevatorNumber - elevator number whose service is being retrieved
     * @return Max Capacity of the respective elevator in lbs
     * @throws RemoteException Throws RMI Execption
     */
    @Override
    public int getElevatorCapacity(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getCapacity();
    }

    /**
     * Returns the logical state of the down button of a respective floor
     * 
     * @param floor - floor number whose down button status is being retrieved
     * @return (TRUE or FALSE ) state for the down button
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public boolean getFloorButtonDown(int floor) throws RemoteException {
        return this.downButtonPress;
    }

    /**
     * Returns the logical state of the up button of a respective floor
     * 
     * @param floor - floor number whose Up button status is being retrieved
     * @return (TRUE or FALSE ) state for the up button
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public boolean getFloorButtonUp(int floor) throws RemoteException {
        return this.upButtonPress;
    }

    /**
     * Returns the height of the floor in the building layout
     * 
     * @return height of the floor in ft
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public int getFloorHeight() throws RemoteException {
        return this.floorHeight;
    }

    /**
     * Returns the current floor number
     * 
     * @return Integer value of floor number
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public int getFloorNum() throws RemoteException {
        return this.highestFloor - this.lowestFloor;
    }

    /**
     * Returns the servicable floors
     * 
     * @param elevatorNumber elevator number whose service is being retrieved
     * @param floor          floor whose service status by the specified elevator is
     *                       being retrieved
     * @return Returns the servicable floors
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public boolean getServicesFloors(int elevatorNumber, int floor) throws RemoteException {
        return elevators.get(elevatorNumber).getServiceableFloors().get(floor);

    }

    /**
     * Returns the targeted floor
     * 
     * @param elevatorNumber elevator number whose target floor is being retrieved
     * @return Targetted floor for the elevator to head in that direction
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public int getTarget(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getTargetedFloor();
    }

    /**
     * Sets the direction of the elevator
     * 
     * @param elevatorNumber elevator number whose committed direction is being set
     * @param direction      direction being set where up=0, down=1 and
     *                       uncommitted=2
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public void setCommittedDirection(int elevatorNumber, int direction) throws RemoteException {
        elevators.get(elevatorNumber).setCommitedDirection(direction);
    }

    /**
     * Sets the floors accessible by the elevator
     * 
     * @param elevatorNumber elevator number whose service is being defined
     * @param floor          floor whose service by the specified elevator is being
     *                       set
     * @param service        indicates whether the floor is serviced by the
     *                       specified elevator (yes=true,no=false)
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public void setServicesFloors(int elevatorNumber, int floor, boolean service) throws RemoteException {
        elevators.get(elevatorNumber).setServiceableFloors(floor, service);
    }

    /**
     * Returns set target floor of the elevator
     * 
     * @param elevatorNumber elevator number whose target floor is being set
     * @param target         floor number which the specified elevator is to target
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public void setTarget(int elevatorNumber, int target) throws RemoteException {
        elevators.get(elevatorNumber).setTargetedFloor(target);
    }

    /**
     * Returns clock tick
     * 
     * @return Clock tick
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public long getClockTick() throws RemoteException {
        return this.clockTick;
    }

    public void updateAll() {
        for (Elevator elevator : elevators) {
            elevator.update();
        }
    }
}
