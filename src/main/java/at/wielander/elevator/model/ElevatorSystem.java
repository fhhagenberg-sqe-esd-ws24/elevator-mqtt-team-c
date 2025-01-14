package at.wielander.elevator.model;

import sqelevator.IElevator;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the control structure for monitoring various elevator configurations in the building .
 * <p>
 * This class manages both Load and Passenger elevators in multiple configurations as required for any building. Various
 * elevator physical and behavioural attributes inherited from the Elevator class can be controlled as well as accessed
 * via the methods of adapter Broker as well as the Controller, to be implemented in the later stages of this assignment.
 *</p>
 * @version 1.0
 */

/**
 * Constructor for multiple configurations of the elevator system.
 */
public class ElevatorSystem implements IElevator {

    /** Field for the lowest floor in the building */
    private final int lowestFloor;

    /** Field for the highest floor in the building */
    private final int highestFloor;

    /** Field for the elevators in the building */
    private final List<Elevator> elevators;

    /** Field for the floor height in the building */
    private final int floorHeight;

    /** array for the total number of buttons ('DOWN') in the building */
    private boolean[] downButtonPress;

    /** array for the total number of buttons ('UP') in the building */
    private boolean[] upButtonPress;

    /** Field for the clock ticks - to be implemented in future assignment */
    private long clockTick;

    /** Instance of IElevator interface */
    private IElevator elevatorAPI;

    private ArrayList<Boolean> serviceableFloors;

    /** Instance of button state */
    protected Boolean buttonState;

    /**
     * Creates a configuration of elevators for a building
     *
     * @param numElevator  Number of elevators in the building
     * @param lowestFloor  Lowest floor accessible by the elevator
     * @param highestFloor Highest floor accessible by the elevator
     * @param capacity     Maximum capacity of the elevator in lbs
     * @param floorHeight  Height of each floor to be given in ft
     * @throws RemoteException Remote exception
     */
    public ElevatorSystem(final int numElevator,
            final int lowestFloor,
            final int highestFloor,
            final int capacity,
            final int floorHeight,
            final IElevator elevatorAPI) throws RemoteException {
        this.floorHeight = floorHeight;
        this.downButtonPress = new boolean[highestFloor + 1];
        this.upButtonPress = new boolean[highestFloor + 1];
        this.elevatorAPI = elevatorAPI;
        this.lowestFloor = lowestFloor;
        this.highestFloor = highestFloor;

        elevators = new ArrayList<>();

        serviceableFloors = new ArrayList<>();

        for (int i = lowestFloor; i <= highestFloor; i++) {
            serviceableFloors.add(i, true);
        }

        for (int i = 0; i < numElevator; i++) {
            elevators.add(new Elevator(serviceableFloors, capacity, this.elevatorAPI, i));
            elevators.get(i).update();
        }
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
     * Returns clock tick
     *
     * @return Clock tick
     * @throws RemoteException Throws an Exception for RMI
     */
    @Override
    public long getClockTick() throws RemoteException {
        return this.clockTick;
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
        buttonState = elevators.get(elevatorNumber).getButtonsInElevatorStatus().get(floor);

        if (buttonState == null) {
            return false;
        }
        return buttonState;
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
        return elevators.get(elevatorNumber).getElevatorDoorStatus();
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
        return (int) Math
                .round((double) elevators.get(elevatorNumber).getCurrentPosition() / (double) this.floorHeight);
    }

    /***
     * Returns the current elevator ID
     *
     * @return integer value of elevator ID
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
        return elevatorAPI.getElevatorPosition(elevatorNumber);
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
        return elevators.get(elevatorNumber).getCurrentSpeed();
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
        return elevators.get(elevatorNumber).getCurrentWeight();
    }

    /**
     * Returns the set capacity of the elevator
     *
     * @param elevatorNumber - elevator number whose service is being retrieved
     * @return Max Capacity of the respective elevator in lbs
     * @throws RemoteException Throws RMI Exception
     */
    @Override
    public int getElevatorCapacity(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getElevatorCapacity();
    }

    /**
     * Returns the targeted floor
     *
     * @param elevatorNumber elevator number whose target floor is being retrieved
     * @return Targeted floor for the elevator to head in that direction
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
        elevators.get(elevatorNumber).setCommittedDirection(direction);
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
     * Returns the total number of elevators with respect to building layout
     * 
     * @return total num of elevators
     */
    public int getTotalElevators() {
        return elevators.size();
    }


   
    /**
     * Returns the logical state of the down button of a respective floor
     *
     * @param floor - floor number whose down button status is being retrieved
     * @return (TRUE or FALSE ) state for the down button
     * @throws RemoteException Throws an Exception for RMI
     */
   
    public boolean getFloorButtonDown(int floor) throws RemoteException {
        if (floor < lowestFloor || floor > highestFloor) {
            throw new RemoteException("Floor number out of range");
        }
        return this.downButtonPress[floor];
    }

    /**
     * Returns the logical state of the up button of a respective floor
     *
     * @param floor - floor number whose Up button status is being retrieved
     * @return (TRUE or FALSE ) state for the up button
     * @throws RemoteException Throws an Exception for RMI
     */

    public boolean getFloorButtonUp(int floor) throws RemoteException {
        if (floor < lowestFloor || floor > highestFloor) {
            throw new RemoteException("Floor number out of range");
        }
        return this.upButtonPress[floor];
    }

    /**
     * Returns the height of the floor in the building layout
     *
     * @return height of the floor in ft
     * @throws RemoteException Throws an Exception for RMI
     */
    public int getFloorHeight() throws RemoteException {
        return this.floorHeight;
    }

    /**
     * Returns the current floor number
     *
     * @return Integer value of floor number
     * @throws RemoteException Throws an Exception for RMI
     */
    public int getFloorNum() throws RemoteException {
        return this.highestFloor - this.lowestFloor;
    }

    /**
     * Returns the serviceable floors
     *
     * @param elevatorNumber elevator number whose service is being retrieved
     * @param floor          floor whose service status by the specified elevator is
     *                       being retrieved
     * @return Returns the serviceable floors
     * @throws RemoteException Throws an Exception for RMI
     */
    public boolean getServicesFloors(int elevatorNumber, int floor) throws RemoteException {
        return elevators.get(elevatorNumber).getServiceableFloors().get(floor);

    }


    /**
     * Return requested elevator
     * 
     * @param elevatorNumber elevator number
     *
     * @return requested elevator
     */
    public Elevator getElevator(int elevatorNumber) {
        return elevators.get(elevatorNumber);
    }

    public int getNumberOfFloors() {
        return serviceableFloors.size();
    }


    /**
     * Updates all elevators based on current states
     */
    public void updateAll() throws RemoteException {
        for (Elevator elevator : elevators) {
            elevator.update();
        }

        for (int floor = lowestFloor; floor <= highestFloor; floor++) {
            this.upButtonPress[floor] = elevatorAPI.getFloorButtonUp(floor);
            this.downButtonPress[floor] = elevatorAPI.getFloorButtonDown(floor);
        }
    }
    
    public ElevatorSystem copy() throws RemoteException {
        // Kopieren der Parameter (skalare Felder und Listen)
        ElevatorSystem copy = new ElevatorSystem(
            this.elevators.size(),    // Anzahl der Aufzüge
            this.lowestFloor,         // niedrigster Stockwerk
            this.highestFloor,        // höchster Stockwerk
            this.elevators.get(0).getElevatorCapacity(), // Kapazität (angenommen alle Aufzüge haben die gleiche Kapazität)
            this.floorHeight,         // Höhe der Etagen
            this.elevatorAPI          // API für den Aufzug (dies könnte auch kopiert werden, falls notwendig)
        );

        // Kopieren der Aufzüge (Elevators) durch Kopie jedes einzelnen Aufzugs
        for (int i = 0; i < this.elevators.size(); i++) {
            copy.elevators.add(this.elevators.get(i).copy());
        }

        // Kopieren der Schalter- und Button-Status
        copy.downButtonPress = this.downButtonPress.clone();
        copy.upButtonPress = this.upButtonPress.clone();
        copy.serviceableFloors = new ArrayList<>(this.serviceableFloors);

        return copy;
    }
}