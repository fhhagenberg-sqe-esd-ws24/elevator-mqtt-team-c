package at.wielander.elevator.Controller;

/**
 * Created by winterer on 13.01.2017.
 */
public interface IElevators {
    boolean getFloorButtonUp(int floor);

    boolean getFloorButtonDown(int floor);

    boolean getElevatorButton(int elevatorNumber, int floor);

    int getElevatorFloor(int elevatorNumber);

    int getElevatorPosition(int elevatorNumber);

    int getElevatorCapacity(int elevatorNumber);

    int getElevatorSpeed(int elevatorNumber);

    int getElevatorAccel(int elevatorNumber);

    int getElevatorDoorStatus(int elevatorNumber);

    void setTarget(int elevatorNumber, int target);

    int getTarget(int elevatorNumber);

    void setCommittedDirection(int elevatorNumber, int direction);

    int getCommittedDirection(int elevatorNumber);

    void setServicesFloors(int elevatorNumber, int floor, boolean service);

    boolean getServicesFloors(int elevatorNumber, int floor);

    int getElevatorNum();

    int getFloorNum();

    int getFloorHeight();

    int getElevatorWeight(int elevatorNumber);

    void setText(String controllerText);

    int getLogging();
}
