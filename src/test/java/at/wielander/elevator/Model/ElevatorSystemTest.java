package at.wielander.elevator.Model;

import at.wielander.elevator.model.ElevatorSystem;
import sqelevator.IElevator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElevatorSystemTest {

        @Mock
        private IElevator mockIElevator;

        public ElevatorSystem elevatorSystem;

        @BeforeEach
        void SetUp() throws RemoteException {
                elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);
        }

        @Test
        void testGetCommittedDirection() throws RemoteException {

                assertEquals(0, elevatorSystem.getCommittedDirection(2));
                assertEquals(0, elevatorSystem.getCommittedDirection(2));
                assertEquals(0, elevatorSystem.getCommittedDirection(2));

                when(mockIElevator.getCommittedDirection(0))
                                .thenReturn(0);
                when(mockIElevator.getCommittedDirection(1))
                                .thenReturn(1);
                when(mockIElevator.getCommittedDirection(2))
                                .thenReturn(2);
                elevatorSystem.updateAll();

                assertEquals(0, elevatorSystem.getCommittedDirection(0));
                assertEquals(1, elevatorSystem.getCommittedDirection(1));
                assertEquals(2, elevatorSystem.getCommittedDirection(2));
                verify(mockIElevator, atLeast(6)).getCommittedDirection(anyInt());
        }

        @Test
        void testGetAcceleration() throws RemoteException {

                assertEquals(0, elevatorSystem.getElevatorAccel(0));
                assertEquals(0, elevatorSystem.getElevatorAccel(1));
                assertEquals(0, elevatorSystem.getElevatorAccel(2));

                when(mockIElevator.getElevatorAccel(0))
                                .thenReturn(0);
                when(mockIElevator.getElevatorAccel(1))
                                .thenReturn(5);
                when(mockIElevator.getElevatorAccel(2))
                                .thenReturn(9);
                elevatorSystem.updateAll();

                assertEquals(0, elevatorSystem.getElevatorAccel(0));
                assertEquals(5, elevatorSystem.getElevatorAccel(1));
                assertEquals(9, elevatorSystem.getElevatorAccel(2));
                verify(mockIElevator, atLeast(6)).getElevatorAccel(anyInt());
        }

        @Test
        void testGetElevatorButton() throws RemoteException {

                assertFalse(elevatorSystem.getElevatorButton(0, 0));
                assertFalse(elevatorSystem.getElevatorButton(1, 1));
                assertFalse(elevatorSystem.getElevatorButton(1, 2));

                doReturn(true).when(mockIElevator).getElevatorButton(0, 0);
                doReturn(false).when(mockIElevator).getElevatorButton(0, 1);
                doReturn(true).when(mockIElevator).getElevatorButton(0, 2);
                elevatorSystem.updateAll();

                assertTrue(elevatorSystem.getElevatorButton(0, 0));
                assertFalse(elevatorSystem.getElevatorButton(0, 1));
                assertTrue(elevatorSystem.getElevatorButton(0, 2));

                verify(mockIElevator, atLeast(3)).getElevatorButton(anyInt(), anyInt());
        }

        @Test
        void testElevatorDoorStatus() throws RemoteException {
                when(mockIElevator.getElevatorDoorStatus(anyInt()))
                                .thenReturn(1, 2, 3, 4);

                elevatorSystem.updateAll();

                assertEquals(1, elevatorSystem.getElevatorDoorStatus(0));
                assertEquals(2, elevatorSystem.getElevatorDoorStatus(1));
                assertEquals(3, elevatorSystem.getElevatorDoorStatus(2));
                verify(mockIElevator, atLeast(3)).getElevatorDoorStatus(anyInt());
        }

        @Test
        void testGetElevatorPosition() throws RemoteException {

                when(mockIElevator.getElevatorPosition(0))
                                .thenReturn(1);
                when(mockIElevator.getElevatorPosition(1))
                                .thenReturn(4);
                elevatorSystem.updateAll();

                assertEquals(1, elevatorSystem.getElevatorPosition(0),
                                "Test FAILED: Doors should be CLOSED by default");
                assertNotEquals(2, elevatorSystem.getElevatorPosition(1),
                                "Test FAILED: Doors should be CLOSED by default");
                verify(mockIElevator, atLeast(2)).getElevatorPosition(anyInt());
        }

        @Test
        void testServicesFloors() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                2,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);
                /* INIT */
                assertTrue(elevatorSystem.getServicesFloors(0, 0),
                                "Test FAILED: Floor should be SERVICEABLE by default");
                assertTrue(elevatorSystem.getServicesFloors(1, 4),
                                "Test FAILED: Floor should be UNSERVICEABLE by default");

                /* NEW CONDITION */
                elevatorSystem.setServicesFloors(0, 3, false);
                elevatorSystem.setServicesFloors(1, 4, false);
                elevatorSystem.updateAll();

                assertFalse(elevatorSystem.getServicesFloors(0, 3),
                                "Test FAILED: Floor should be SERVICEABLE by default");
                assertFalse(elevatorSystem.getServicesFloors(1, 4),
                                "Test FAILED: Floor should be UNSERVICEABLE by default");
        }

        @Test
        void testGetELElevatorFloor() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);
                /* INIT */
                assertEquals(0, elevatorSystem.getTarget(0),
                                "Test Failed: Elevator #1's should be at ground floor");
                assertEquals(0, elevatorSystem.getTarget(1),
                                "Test Failed: Elevator #2's should be at ground floor");
                assertEquals(0, elevatorSystem.getTarget(2),
                                "Test Failed: Elevator #3's should be at ground floor");

                /* NEW CONDITION */
                elevatorSystem.setTarget(0, 1);
                elevatorSystem.setTarget(1, 2);
                elevatorSystem.setTarget(2, 4);
                assertEquals(1, elevatorSystem.getTarget(0),
                                "Test Failed: Elevator #1's should be at ground floor");
                assertEquals(2, elevatorSystem.getTarget(1),
                                "Test Failed: Elevator #2's should be at the 2nd floor");
                assertEquals(4, elevatorSystem.getTarget(2),
                                "Test Failed: Elevator #3's should be at the 4nd floor");
        }

        @Test
        void testSetTargetFloor() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                /* INIT */
                assertEquals(0, elevatorSystem.getTarget(0),
                                "Test Failed: Elevator #1's should be at ground floor");
                assertEquals(0, elevatorSystem.getTarget(1),
                                "Test Failed: Elevator #2's should be at the 2nd floor");
                assertEquals(0, elevatorSystem.getTarget(2),
                                "Test Failed: Elevator #3's should be at the 4nd floor");

                /* NEW CONDITION */
                elevatorSystem.setTarget(0, 0);
                elevatorSystem.setTarget(1, 2);
                elevatorSystem.setTarget(2, 4);

                assertEquals(0, elevatorSystem.getTarget(0),
                                "Test Failed: Elevator #1's should be at ground floor");
                assertEquals(2, elevatorSystem.getTarget(1),
                                "Test Failed: Elevator #2's should be at the 2nd floor");
                assertEquals(4, elevatorSystem.getTarget(2),
                                "Test Failed: Elevator #3's should be at the 4nd floor");
        }

        @Test
        void testGetFloorHeight() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                /* INIT */
                assertEquals(7, elevatorSystem.getFloorHeight());
        }

        @Test
        void testGetFloorNum() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                /* INIT */
                assertEquals(4, elevatorSystem.getFloorNum());
        }

        @Test
        void testGetCurrentSpeed() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);
                assertEquals(0, elevatorSystem.getElevatorSpeed(0),
                                "Test Failed: Elevator #1's speed SHOULD BE 0 ft/s");
                assertEquals(0, elevatorSystem.getElevatorSpeed(1),
                                "Test Failed: Elevator #2's speed SHOULD BE 0 ft/s");
                assertEquals(0, elevatorSystem.getElevatorSpeed(2),
                                "Test Failed: Elevator #3's speed SHOULD BE 0 ft/s");

                /* NEW CONDITION */
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
                verify(mockIElevator, atLeast(6)).getElevatorSpeed(anyInt());
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

                /* INIT */
                assertEquals(0, elevatorSystem.getElevatorWeight(0),
                                "Test FAILED: Elevator' weight should be 0 lbs");
                assertEquals(0, elevatorSystem.getElevatorWeight(1),
                                "Test FAILED: Elevator' weight should be 0 lbs");
                assertEquals(0, elevatorSystem.getElevatorWeight(2),
                                "Test FAILED: Elevator' weight should be 0 lbs");

                /* NEW CONDITION */
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
                verify(mockIElevator, atLeast(6)).getElevatorWeight(anyInt());
        }

        @Test
        void testGetElevatorCapacity() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                /* INIT **/
                elevatorSystem.updateAll();

                assertEquals(4000, elevatorSystem.getElevatorCapacity(0),
                                "Test FAILED: Elevator' capacity should be 4000 lbs");
                assertEquals(4000, elevatorSystem.getElevatorCapacity(1),
                                "Test FAILED: Elevator' capacity should be 4000 lbs");
                assertEquals(4000, elevatorSystem.getElevatorCapacity(2),
                                "Test FAILED: Elevator' capacity should be 4000 lbs");
        }

        @Test
        void testGetFloorButtonDown() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                /* INIT **/
                assertFalse(elevatorSystem.getFloorButtonDown(0),
                                "Test FAILED: The floor down floor button on floor 0 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonDown(1),
                                "Test FAILED: The floor down floor button on floor 1 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonDown(2),
                                "Test FAILED: The floor down floor button on floor 2 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonDown(3),
                                "Test FAILED: The floor down floor button on floor 3 should not be pressed");

                /* CONDITION */
                when(mockIElevator.getFloorButtonDown(0)).thenReturn(false);
                when(mockIElevator.getFloorButtonDown(1)).thenReturn(true);
                when(mockIElevator.getFloorButtonDown(2)).thenReturn(true);
                when(mockIElevator.getFloorButtonDown(3)).thenReturn(false);
                elevatorSystem.updateAll();

                assertFalse(elevatorSystem.getFloorButtonDown(0),
                                "Test FAILED: The floor down floor button on floor 0 should not be pressed");
                assertTrue(elevatorSystem.getFloorButtonDown(1),
                                "Test FAILED: The floor down floor button on floor 1 should be pressed");
                assertTrue(elevatorSystem.getFloorButtonDown(2),
                                "Test FAILED: The floor down floor button on floor 2 should be pressed");
                assertFalse(elevatorSystem.getFloorButtonDown(3),
                                "Test FAILED: The floor down floor button on floor 3 should not be pressed");

                verify(mockIElevator, atLeast(4)).getFloorButtonDown(anyInt());
        }

        @Test
        void testGetFloorButtonUp() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                /* INIT **/
                assertFalse(elevatorSystem.getFloorButtonUp(0),
                                "Test FAILED: The floor down floor button on floor 0 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonUp(1),
                                "Test FAILED: The floor down floor button on floor 1 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonUp(2),
                                "Test FAILED: The floor down floor button on floor 2 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonUp(3),
                                "Test FAILED: The floor down floor button on floor 3 should not be pressed");

                /* CONDITION */
                when(mockIElevator.getFloorButtonUp(0)).thenReturn(true);
                when(mockIElevator.getFloorButtonUp(1)).thenReturn(false);
                when(mockIElevator.getFloorButtonUp(2)).thenReturn(false);
                when(mockIElevator.getFloorButtonUp(3)).thenReturn(true);
                elevatorSystem.updateAll();

                assertTrue(elevatorSystem.getFloorButtonUp(0),
                                "Test FAILED: The floor up floor button on floor 0 should not be pressed");
                assertFalse(elevatorSystem.getFloorButtonUp(1),
                                "Test FAILED: The floor down floor button on floor 1 should be pressed");
                assertFalse(elevatorSystem.getFloorButtonUp(2),
                                "Test FAILED: The floor down floor button on floor 2 should be pressed");
                assertTrue(elevatorSystem.getFloorButtonUp(3),
                                "Test FAILED: The floor down floor button on floor 3 should not be pressed");

                verify(mockIElevator, atLeast(4)).getFloorButtonUp(anyInt());

        }

        @Test
        void testSetServicesFloors() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                2,
                                0,
                                2,
                                4000,
                                7,
                                mockIElevator);

                assertTrue(elevatorSystem.getServicesFloors(0, 0));
                assertTrue(elevatorSystem.getServicesFloors(0, 1));
                assertTrue(elevatorSystem.getServicesFloors(1, 0));
                assertTrue(elevatorSystem.getServicesFloors(1, 1));
        }

        @Test
        void testSetTarget() throws RemoteException {
                ElevatorSystem elevatorSystem = new ElevatorSystem(
                                3,
                                0,
                                4,
                                4000,
                                7,
                                mockIElevator);

                assertEquals(0, elevatorSystem.getTarget(0));
                assertEquals(0, elevatorSystem.getTarget(0));
                assertEquals(0, elevatorSystem.getTarget(0));

                elevatorSystem.setTarget(0, 1);
                elevatorSystem.setTarget(1, 2);
                elevatorSystem.setTarget(2, 4);

                assertEquals(1, elevatorSystem.getTarget(0));
                assertEquals(2, elevatorSystem.getTarget(1));
                assertEquals(4, elevatorSystem.getTarget(2));
        }

        @Test
        void testGetElevator() {

                assertEquals(0, elevatorSystem.getElevator(0).elevatorNumber);
                assertEquals(1, elevatorSystem.getElevator(1).elevatorNumber);
        }

}