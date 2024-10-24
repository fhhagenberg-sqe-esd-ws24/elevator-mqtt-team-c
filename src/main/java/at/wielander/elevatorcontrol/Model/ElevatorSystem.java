package at.wielander.elevatorcontrol.Model;

import at.fhhagenberg.sqelevator.IElevator;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElevatorSystem implements IElevator {

    private final int lowestFloor;
    private final int highestFloor;
    private List<Elevator> elevators;
    private final int floorHeight;
    private boolean downButtonPress;
    private boolean upButtonPress;
    private long clockTick;


    public ElevatorSystem(final int numElevator, final int lowestFloor, final int highestFloor, final int capacity, final int floorHeight)
    {
        this.lowestFloor = lowestFloor;
        this.highestFloor = highestFloor;
        elevators = new ArrayList<>();
        this.floorHeight = floorHeight;
        this.downButtonPress = false;
        this.upButtonPress = false;

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        for(int i = lowestFloor; i <= highestFloor; i++)
        {
            serviceableFloors.put(i, true);
        }


        for(int i = 0; i < numElevator; i++)
        {
            if(elevators != null)
            {
                elevators.add(new Elevator(serviceableFloors, capacity));
            }
        }
    }

    public void addElevator(Map<Integer, Boolean> serviceableFloors, int capacity)
    {
        elevators.add(new Elevator(serviceableFloors, capacity));
    }

    public void addElevator(int capacity)
    {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        for(int i = lowestFloor; i <= highestFloor; i++)
        {
            serviceableFloors.put(i, true);
        }

        elevators.add(new Elevator(serviceableFloors, capacity));
    }
    public void addElevator()
    {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        for(int i = lowestFloor; i <= highestFloor; i++)
        {
            serviceableFloors.put(i, true);
        }

        elevators.add(new Elevator(serviceableFloors, elevators.get(0).getCapacity()));
    }

    @Override
    public int getCommittedDirection(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getCommitedDirection();
    }

    @Override
    public int getElevatorAccel(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getAccelleration();
    }

    @Override
    public boolean getElevatorButton(int elevatorNumber, int floor) throws RemoteException {

        // check for service range of elevator
        if(!elevators.get(elevatorNumber).getServiceableFloors().get(floor))
        {
            System.err.println("Floor " + floor + " not within elevator service range.");
        }
        return elevators.get(elevatorNumber).getButtons().get(floor);
    }

    @Override
    public int getElevatorDoorStatus(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getDoorState();
    }

    @Override
    public int getElevatorFloor(int elevatorNumber) throws RemoteException {
        return (int)Math.round((double)elevators.get(elevatorNumber).getLocation() / (double)this.floorHeight);
    }

    @Override
    public int getElevatorNum() throws RemoteException {
        return elevators.size();
    }

    @Override
    public int getElevatorPosition(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getLocation();
    }

    @Override
    public int getElevatorSpeed(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getSpeed();
    }

    @Override
    public int getElevatorWeight(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getWeight();
    }

    @Override
    public int getElevatorCapacity(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getCapacity();
    }

    @Override
    public boolean getFloorButtonDown(int floor) throws RemoteException {
        return this.downButtonPress;
    }

    @Override
    public boolean getFloorButtonUp(int floor) throws RemoteException {
        return this.upButtonPress;
    }

    @Override
    public int getFloorHeight() throws RemoteException {
        return this.floorHeight;
    }

    @Override
    public int getFloorNum() throws RemoteException {
        return this.highestFloor - this.lowestFloor;
    }

    @Override
    public boolean getServicesFloors(int elevatorNumber, int floor) throws RemoteException {
        return elevators.get(elevatorNumber).getServiceableFloors().get(floor);

    }

    @Override
    public int getTarget(int elevatorNumber) throws RemoteException {
        return elevators.get(elevatorNumber).getTargetedFloor();
    }

    @Override
    public void setCommittedDirection(int elevatorNumber, int direction) throws RemoteException {
        elevators.get(elevatorNumber).setCommitedDirection(direction);
    }

    @Override
    public void setServicesFloors(int elevatorNumber, int floor, boolean service) throws RemoteException {
        elevators.get(elevatorNumber).setServiceableFloors(floor, service);
    }

    @Override
    public void setTarget(int elevatorNumber, int target) throws RemoteException {
        elevators.get(elevatorNumber).setTargetedFloor(target);
    }

    @Override
    public long getClockTick() throws RemoteException {
        return this.clockTick;
    }

    // update internal values, like speed, acceleration etc
    // also update clock
    private void updateAll()
    {
        for(int i = 0; i < elevators.size(); i++)
        {
            elevators.get(i).update();
        }
    }
}
