package at.wielander.elevator.Model;

import at.fhhagenberg.sqelevator.IElevator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ElevatorSystemTest {

    @Mock
    private IElevator mockIElevator;

    @Test
    void testGetSpeed() throws RemoteException {
        ElevatorSystem elevatorSystem = new ElevatorSystem(
                3,
                0,
                4,
                4000,
                7,
                mockIElevator);
        when(mockIElevator.getElevatorSpeed(0))
                .thenReturn(0);
        when(mockIElevator.getElevatorSpeed(1))
                .thenReturn(14);
        when(mockIElevator.getElevatorSpeed(2))
                .thenReturn(20);
        elevatorSystem.updateAll();

        assertEquals(0, elevatorSystem.getElevatorSpeed(0),
                "Test Failed: Elevator #1's speed SHOULD BE 0 ft/s");
        assertEquals(14, elevatorSystem.getElevatorSpeed(1),
                "Test Failed: Elevator #2's speed SHOULD BE 14 ft/s");
        assertEquals(20, elevatorSystem.getElevatorSpeed(2),
                "Test Failed: Elevator #3's speed SHOULD BE 20 ft/s");
        verify(mockIElevator, times(2)).getElevatorSpeed(0);
    }

    @Test
    void testGetAcceleration() throws RemoteException {
        ElevatorSystem elevatorSystem = new ElevatorSystem(
                3,
                0,
                4,
                4000,
                7,
                mockIElevator);
        when(mockIElevator.getElevatorAccel(0))
                .thenReturn(0);
        when(mockIElevator.getElevatorAccel(1))
                .thenReturn(5);
        when(mockIElevator.getElevatorAccel(2))
                .thenReturn(9);
        elevatorSystem.updateAll();

        assertEquals(0, elevatorSystem.getElevatorAccel(0),
                "Test Failed: Elevator #1's speed SHOULD BE 0 ft/s");
        assertEquals(5, elevatorSystem.getElevatorAccel(1),
                "Test Failed: Elevator #2's speed SHOULD BE 14 ft/s");
        assertEquals(9, elevatorSystem.getElevatorAccel(2),
                "Test Failed: Elevator #3's speed SHOULD BE 20 ft/s");
        verify(mockIElevator,times(2)).getElevatorAccel(0);
    }

    @Test
    void testGetElevatorWeight() throws RemoteException {
        ElevatorSystem elevatorSystem = new ElevatorSystem(
                3,
                0,
                4,
                4000,
                7,
                mockIElevator);
        when(mockIElevator.getElevatorWeight(0))
                .thenReturn(0);
        when(mockIElevator.getElevatorWeight(1))
                .thenReturn(2000);
        when(mockIElevator.getElevatorWeight(2))
                .thenReturn(3500);
        elevatorSystem.updateAll();

        assertEquals(0, elevatorSystem.getElevatorWeight(0),
                "Test FAILED: Elevator' weight should be 0 lbs");
        assertEquals(2000, elevatorSystem.getElevatorWeight(1),
                "Test FAILED: Elevator' weight should be 2000 lbs");
        assertEquals(3500, elevatorSystem.getElevatorWeight(2),
                "Test FAILED: Elevator' weight should be 3500 lbs");
        verify(mockIElevator, times(2)).getElevatorWeight(0);
    }

    @Test
    void testElevatorDoorState() throws RemoteException {
        ElevatorSystem elevatorSystem = new ElevatorSystem(
                4,
                0,
                4,
                4000,
                7,
                mockIElevator);
        when(mockIElevator.getElevatorDoorStatus(0))
                .thenReturn(IElevator.ELEVATOR_DOORS_OPEN);
        when(mockIElevator.getElevatorDoorStatus(1))
                .thenReturn(IElevator.ELEVATOR_DOORS_OPENING);
        when(mockIElevator.getElevatorDoorStatus(2))
                .thenReturn(IElevator.ELEVATOR_DOORS_CLOSED);
        when(mockIElevator.getElevatorDoorStatus(3)).
                thenReturn(IElevator.ELEVATOR_DOORS_CLOSING);
        elevatorSystem.updateAll();


        assertEquals(IElevator.ELEVATOR_DOORS_OPEN, elevatorSystem.getElevatorDoorStatus(0),
                "Test FAILED: Doors should be CLOSED by default");
        assertEquals(IElevator.ELEVATOR_DOORS_OPENING, elevatorSystem.getElevatorDoorStatus(1),
                "Test FAILED: Doors should be CLOSED by default");
        assertEquals(IElevator.ELEVATOR_DOORS_CLOSED, elevatorSystem.getElevatorDoorStatus(2),
                "Test FAILED: Doors should be CLOSED by default");
        assertEquals(IElevator.ELEVATOR_DOORS_CLOSING, elevatorSystem.getElevatorDoorStatus(3),
                "Test FAILED: Doors should be CLOSED by default");
        verify(mockIElevator, times(2)).getElevatorDoorStatus(0);
    }
}