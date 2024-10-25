package at.wielander.elevator.Model;

import at.fhhagenberg.sqelevator.IElevator;

import java.util.HashMap;
import java.util.Map;

public class Elevator {

    private int commitedDirection;
    private int doorState;
    private int accelleration; // accel in ft/sec^2
    private int speed; // speed in ft/sec
    private Map<Integer, Boolean> buttons; // push state of button
    private int location; // height of elevator from ground
    private int weight;
    private int capacity;
    private Map<Integer, Boolean> serviceableFloors; // list of floors that the elevator can stop at
    private int targetedFloor;
    public Elevator(Map<Integer, Boolean> serviceableFloors, final int capacity)
    {
        if(serviceableFloors == null || capacity <= 0){
            throw new IllegalArgumentException("Invalid Arguments");
        }

        this.commitedDirection = IElevator.ELEVATOR_DIRECTION_UNCOMMITTED;
        this.doorState = IElevator.ELEVATOR_DOORS_CLOSED;
        this.accelleration = 0;
        this.speed = 0;
        this.weight = 0;
        this.targetedFloor = 0;
        this.buttons = new HashMap<>();

        this.serviceableFloors = serviceableFloors;
        this.capacity = capacity;

        // set all buttons to false within the serviceable floor range
        for(int id : this.serviceableFloors.keySet())
        {
            if(serviceableFloors.get(id) != null) {
                buttons.put(id, false);
            }else{
                System.err.println("Serviceable floor state at " + id + " not specified");
            }
        }
    }
    public int getCommitedDirection() {
        return this.commitedDirection;
    }

    public void setCommitedDirection(int commitedDirection) {
        this.commitedDirection = commitedDirection;
    }

    public int getDoorState() {
        return doorState;
    }

    public int getAccelleration() {
        return accelleration;
    }


    public int getSpeed() {
        return speed;
    }

    public Map<Integer, Boolean> getButtons() {
        return buttons;
    }

    public int getLocation() {
        return location;
    }

    public int getWeight() {
        return weight;
    }

    public int getCapacity() {
        return capacity;
    }

    public Map<Integer, Boolean> getServiceableFloors() {
        return serviceableFloors;
    }

    public void setServiceableFloors(int floor, boolean service) {
        this.serviceableFloors.put(floor, service);
    }

    public int getTargetedFloor() {
        return targetedFloor;
    }

    public void setTargetedFloor(int targetedFloor) {
        this.targetedFloor = targetedFloor;
    }

    // get data from mqtt and calculate new speed, acceleration etc
    public void update()
    {

    }
}
