package at.wielander.elevator.Controller;

/**
 * Created by winterer on 13.01.2017.
 */
public class RMIElevator implements IElevator {
    private final IElevators api;
    private long clockTick;

    public RMIElevator(IElevators api) {
        this.api = api;
    }

    @Override
    public int getCommittedDirection(int elevatorNumber) {
        return api.getCommittedDirection(elevatorNumber);
    }

    @Override
    public int getElevatorAccel(int elevatorNumber) {
        return api.getElevatorAccel(elevatorNumber);
    }

    @Override
    public boolean getElevatorButton(int elevatorNumber, int floor) {
        return api.getElevatorButton(elevatorNumber, floor);
    }

    @Override
    public int getElevatorDoorStatus(int elevatorNumber) {
        return api.getElevatorDoorStatus(elevatorNumber);
    }

    @Override
    public int getElevatorFloor(int elevatorNumber) {
        return api.getElevatorFloor(elevatorNumber);
    }

    @Override
    public int getElevatorNum() {
        return api.getElevatorNum();
    }

    @Override
    public int getElevatorPosition(int elevatorNumber) {
        return api.getElevatorPosition(elevatorNumber);
    }

    @Override
    public int getElevatorSpeed(int elevatorNumber) {
        return api.getElevatorSpeed(elevatorNumber);
    }

    @Override
    public int getElevatorWeight(int elevatorNumber) {
        return api.getElevatorWeight(elevatorNumber);
    }

    @Override
    public int getElevatorCapacity(int elevatorNumber) {
        return api.getElevatorCapacity(elevatorNumber);
    }

    @Override
    public boolean getFloorButtonDown(int floor) {
        return api.getFloorButtonDown(floor);
    }

    @Override
    public boolean getFloorButtonUp(int floor) {
        return api.getFloorButtonUp(floor);
    }

    @Override
    public int getFloorHeight() {
        return api.getFloorHeight();
    }

    @Override
    public int getFloorNum() {
        return api.getFloorNum();
    }

    @Override
    public boolean getServicesFloors(int elevatorNumber, int floor) {
        return api.getServicesFloors(elevatorNumber, floor);
    }

    @Override
    public int getTarget(int elevatorNumber) {
        return api.getTarget(elevatorNumber);
    }

    @Override
    public void setCommittedDirection(int elevatorNumber, int direction) {
        api.setCommittedDirection(elevatorNumber, direction);
    }

    @Override
    public void setServicesFloors(int elevatorNumber, int floor, boolean service) {
        api.setServicesFloors(elevatorNumber, floor, service);
    }

    @Override
    public void setTarget(int elevatorNumber, int target) {
        api.setTarget(elevatorNumber, target);
    }

    /**
     * Returns the current clock tick. This method is synchronized with {@link #setClockTick(long)}.
     * So getting and setting the clock tick is the only thread-safe operations on this class.
     * <p>
     * <p>
     * These are the consequences:
     * <ul>
     * <li>After {@link #getClockTick()} has been called by the RMI thread, all subsequent calls of any getter method by
     * the same RMI thread will 'see' at least all the changes of the elevator model that happened before the simulation
     * thread has called {@link #setClockTick(long)} on this class.</li>
     * <li>Omitting to call {@link #getClockTick()} will suppress any synchronization at all.</li>
     * </ul>
     * </p>
     *
     * @return the clock tick
     */
    @Override
    public synchronized long getClockTick() {
        return this.clockTick;
    }

    public synchronized void setClockTick(long clockTick) {
        this.clockTick = clockTick;
    }
}
