package at.wielander.elevator.Model;

import at.fhhagenberg.sqelevator.IElevator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import static at.fhhagenberg.sqelevator.IElevator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ElevatorTest {

    @Mock
    private IElevator mockIElevator;

    private Elevator mockElevator;

    @BeforeEach
    void setUp(){
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        serviceableFloors.put(0, true);
        serviceableFloors.put(1, true);
        serviceableFloors.put(2, false);
        serviceableFloors.put(3, true);

        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);
    }


    @Test
    void testGetCommittedDirection() throws RemoteException {

        when(mockIElevator.getCommittedDirection(1)).
                thenReturn(ELEVATOR_DIRECTION_UNCOMMITTED);
        mockElevator.update();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction SHOULD BE UNCOMMITTED");
        verify(mockIElevator).getCommittedDirection(1);
    }

    @Test
    void testGetSpeed() throws RemoteException {
        when(mockIElevator.getElevatorSpeed(1)).
                thenReturn(0);
        mockElevator.update();

        assertEquals(0, mockElevator.getSpeed(),
                "Test Failed: Elevator SPEED is NOT 0 ft/s");
        verify(mockIElevator).getElevatorSpeed(1);
    }

    @Test
    void testGetAcceleration() throws RemoteException {
        when(mockIElevator.getElevatorAccel(1))
                .thenReturn(0);
        mockElevator.update();

        assertEquals(0, mockElevator.getAcceleration(),
                "Test Failed: Elevator ACCELERATION is NOT 0 ft/s²");
        verify(mockIElevator).getElevatorAccel(1);
    }

    @Test
    void testGetElevatorCapacity() throws RemoteException {
        when(mockIElevator.getElevatorWeight(1))
                .thenReturn(4000);
        mockElevator.update();


        assertEquals(4000, mockElevator.getWeight(),
                "Test Failed: Elevator weight should be unloaded by default");
        verify(mockIElevator).getElevatorWeight(1);
    }

    @Test
    void testElevatorDoorState() throws RemoteException {
        when(mockIElevator.getElevatorDoorStatus(1)).
                thenReturn(ELEVATOR_DOORS_CLOSED);
        mockElevator.update();


        assertEquals(ELEVATOR_DOORS_CLOSED, mockElevator.getDoorState(),
                "Test Failed: Doors are CLOSED by default");
        verify(mockIElevator).getElevatorDoorStatus(1);
    }

    @Test
    void testSetCommitedDirection() throws RemoteException {

        mockElevator.setCommitedDirection(ELEVATOR_DIRECTION_UP);
        when(mockIElevator.getCommittedDirection(1)).
                thenReturn(ELEVATOR_DIRECTION_UP);
        mockElevator.update();


        assertEquals(ELEVATOR_DIRECTION_UP, mockElevator.getCommitedDirection(),
                "Test Failed: Elevator direction should move upwards");
        verify(mockIElevator, atLeastOnce()).getCommittedDirection(1);

    }
}