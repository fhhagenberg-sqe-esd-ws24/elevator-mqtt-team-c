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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ElevatorTest {

    @Mock
    private IElevator mockIElevator;

    private Elevator mockElevator;

    @Test
    void testGetCommittedDirection() throws RemoteException {

        /* UNCOMMITTED */
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        when(mockIElevator.getCommittedDirection(1)).
                thenReturn(2);
        mockElevator.update();

        assertEquals(2,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction should be IDLE");

        /* DOWN */
        when(mockIElevator.getCommittedDirection(1)).
                thenReturn(0);
        mockElevator.update();

        assertEquals(0,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction should be heading DOWN");

        /* UP */
        when(mockIElevator.getCommittedDirection(1)).
                thenReturn(1);
        mockElevator.update();

        assertEquals(1,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction should be heading UP");
        verify(mockIElevator, atLeast(3)).getCommittedDirection(1);

    }

    @Test
    void testSetCommittedDirection() {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* IDLE */
        mockElevator.setCommittedDirection(2);
        assertEquals(2,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction should be IDLE");

        /* DOWN */
        mockElevator.setCommittedDirection(1);
        assertEquals(1,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction should be heading DOWN");

        /* UP */
        mockElevator.setCommittedDirection(0);
        assertEquals(0,
                mockElevator.getCommitedDirection(),
                "Test FAILED: Elevator direction should be heading UP");
    }

    @Test
    void testGetAcceleration() throws RemoteException {

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        when(mockIElevator.getElevatorAccel(1))
                .thenReturn(0);
        mockElevator.update();

        assertEquals(0, mockElevator.getAcceleration(),
                "Test Failed: Elevator ACCELERATION is NOT 0 ft/s²");
        verify(mockIElevator,atLeastOnce()).getElevatorAccel(1);
    }

     @Test
     void testGetButtonsStatus() throws RemoteException {

         Map<Integer, Boolean> serviceableFloors = new HashMap<>();
         serviceableFloors.put(0, true);
         serviceableFloors.put(1, false);
         serviceableFloors.put(2, true);
         mockElevator = new Elevator(serviceableFloors,
                 4000,
                 mockIElevator,
                 1);

         /* DEFAULT */
         Map<Integer, Boolean> buttons = mockElevator.getButtonsInElevatorStatus();
         assertEquals(3, buttons.size());
         assertFalse(buttons.get(0),
                 "TEST Failed: Buttons should not be pressed initially");
         assertFalse(buttons.get(2),
                 "TEST Failed: Buttons should not be pressed initially");


         /* NEW CONDITION */
         when(mockIElevator.getElevatorButton(1, 0)).
                 thenReturn(false);
         when(mockIElevator.getElevatorButton(1, 1)).
                 thenReturn(true);
         when(mockIElevator.getElevatorButton(1, 2)).
                 thenReturn(false);
         mockElevator.update();
         buttons = mockElevator.getButtonsInElevatorStatus();
         assertFalse( buttons.get(0),
                 "TEST Failed: Button for floor 0 should not be pressed ");
         assertTrue( buttons.get(1),
                 "TEST Failed: Button for floor 0 should not be pressed ");
         assertFalse( buttons.get(2),
                 "TEST Failed: Button for floor 2 should be pressed");
         verify(mockIElevator, atLeast(3)).getElevatorButton(anyInt(),anyInt());
    }

    @Test
    void testElevatorDoorStatus() throws RemoteException {

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* CLOSED */
        when(mockIElevator.getElevatorDoorStatus(1)).
                thenReturn(2);
        mockElevator.update();

        assertEquals(2, mockElevator.getElevatorDoorStatus(),
                "Test Failed: Doors are CLOSED by default");
        verify(mockIElevator, atLeastOnce()).getElevatorDoorStatus(1);

        /* OPENING */
        when(mockIElevator.getElevatorDoorStatus(1)).
                thenReturn(3);
        mockElevator.update();

        assertEquals(3, mockElevator.getElevatorDoorStatus(),
                "Test Failed: Doors are CLOSED by default");
        verify(mockIElevator, atLeast(2)).getElevatorDoorStatus(1);

        /* OPEN */
        when(mockIElevator.getElevatorDoorStatus(1)).
                thenReturn(1);
        mockElevator.update();

        assertEquals(1, mockElevator.getElevatorDoorStatus(),
                "Test Failed: Doors are CLOSED by default");
        verify(mockIElevator,atLeast(3)).getElevatorDoorStatus(1);

        /* CLOSING */
        when(mockIElevator.getElevatorDoorStatus(1)).
                thenReturn(4);
        mockElevator.update();

        assertEquals(4, mockElevator.getElevatorDoorStatus(),
                "Test Failed: Doors are CLOSED by default");
        verify(mockIElevator,atLeast(4)).getElevatorDoorStatus(anyInt());
    }

    @Test
    void testGetCurrentFloor() throws RemoteException {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* DEFAULT */
        assertEquals(0, mockElevator.getCurrentFloor(),
                "Test Failed: FLOOR are ZERO by default");

        /* New FLOOR*/
        when(mockIElevator.getElevatorFloor(1)).
                thenReturn(2);
        mockElevator.update();
        assertEquals(2, mockElevator.getCurrentFloor(),
                "Test Failed: Elevator on 2nd floor");
        verify(mockIElevator, atLeastOnce()).getElevatorFloor(1);
    }

    @Test
    void testGetCurrentPosition() throws RemoteException {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* DEFAULT */
        assertEquals(0, mockElevator.getCurrentPosition(),
                "Test Failed: FLOOR are ZERO by default");

        /* New FLOOR*/
        when(mockIElevator.getElevatorFloor(1)).
                thenReturn(2);
        mockElevator.update();
        assertEquals(2, mockElevator.getCurrentFloor(),
                "Test Failed: Elevator on 2nd floor");
        verify(mockIElevator, atLeastOnce()).getElevatorFloor(1);
    }

    @Test
    void testGetCurrentSpeed() throws RemoteException {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        when(mockIElevator.getElevatorSpeed(1)).
                thenReturn(0);
        mockElevator.update();

        assertEquals(0, mockElevator.getCurrentSpeed(),
                "Test Failed: Elevator SPEED is NOT 0 ft/s");
        verify(mockIElevator,atLeastOnce()).getElevatorSpeed(anyInt());
    }

    @Test
    void testGetCurrentWeight() throws RemoteException {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();

        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* DEFAULT */
        assertEquals(0, mockElevator.getCurrentWeight(),
                "Test Failed: Elevator CURRENT WEIGHT is NOT 0 lbs");

        /* NEW VALUE */
        when(mockIElevator.getElevatorWeight(1)).
                thenReturn(1000);
        mockElevator.update();
        assertEquals(1000, mockElevator.getCurrentWeight(),
                "Test Failed: Elevator CURRENT WEIGHT is NOT 0 lbs");
        verify(mockIElevator,atLeastOnce()).getElevatorSpeed(anyInt());
    }

    @Test
    void testGetElevatorCapacity() throws RemoteException {
        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        serviceableFloors.put(0, true);
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* INIT */
        assertEquals(4000, mockElevator.getElevatorCapacity(),
                "Test Failed: Elevator weight should be unloaded by default");
    }

    @Test
    void testServiceableFloors() throws RemoteException {

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        serviceableFloors.put(0, true);
        serviceableFloors.put(1, false);
        serviceableFloors.put(2, true);
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* INIT */
        Map<Integer, Boolean> floors = mockElevator.getServiceableFloors();
        assertTrue(floors.get(0), "Test Failed: Floor 0 should be serviceable");
        assertFalse(floors.get(1), "Test Failed: Floor 1 should NOT be serviceable");
        assertTrue(floors.get(2), "Test Failed: Floor 2 should be serviceable");

        /* NEW CONDITION */
        mockElevator.setServiceableFloors(1, true);
        mockElevator.setServiceableFloors(2, false);

        floors = mockElevator.getServiceableFloors();
        assertTrue(floors.get(0), "Test Failed: Floor 0 should be serviceable");
        assertTrue(floors.get(1), "Test Failed: Floor 1 should be serviceable");
        assertFalse(floors.get(2), "Test Failed: Floor 2 should NOT be serviceable");

    }

    @Test
    void testGetTarget() throws RemoteException {

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* INIT */
        assertEquals(0,mockElevator.getTargetedFloor(),
                "Test Failed: Floor 1 should be serviceable");


        /* NEW CONDITION */
        when(mockIElevator.getTarget(1)).thenReturn(1);
        mockElevator.update();
        assertEquals(1,mockElevator.getTargetedFloor(),
                "Test Failed: Floor 1 should be serviceable");

        when(mockIElevator.getTarget(1)).thenReturn(2);
        mockElevator.update();
        assertEquals(2,mockElevator.getTargetedFloor(),
                "Test Failed: Floor 1 should be serviceable");

        verify(mockIElevator,atLeast(2)).getTarget(anyInt());
    }

    @Test
    void testSetTarget() throws RemoteException {

        Map<Integer, Boolean> serviceableFloors = new HashMap<>();
        mockElevator = new Elevator(serviceableFloors,
                4000,
                mockIElevator,
                1);

        /* INIT */
        assertEquals(0,mockElevator.getTargetedFloor(),
                "Test Failed: No Targeted floors");

        /* NEW CONDITION */
        mockElevator.setTargetedFloor(1);
        assertEquals(1,mockElevator.getTargetedFloor(),
                "Test Failed: Wrong Targeted floors");
        mockElevator.setTargetedFloor(2);
        assertEquals(2,mockElevator.getTargetedFloor(),
                "Test Failed: Wrong Targeted floors");
    }
}