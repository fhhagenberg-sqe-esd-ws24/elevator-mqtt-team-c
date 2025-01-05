package algorithm;

import at.wielander.elevator.Algorithm.ElevatorAlgorithm;
import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sqelevator.IElevator;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class ElevatorAlgorithmTest {

    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));

    @Mock
    private IElevator mockElevatorAPI;

    private ElevatorAlgorithm algorithm;

    private ElevatorSystem elevatorSystem;

    private Mqtt5BlockingClient testClient;

    private ElevatorMQTTAdapter mqttAdapter;


    private String Host;

    @BeforeEach
    public void setup() throws Exception {

        hivemqCe.start();

        Host = "tcp://" + hivemqCe.getHost() + ":" + hivemqCe.getMappedPort(1883);

        System.out.println("Host addresse: " + Host);
        testClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(hivemqCe.getMappedPort(1883)) // Verwenden Sie den dynamisch gemappten Port
                .serverHost(hivemqCe.getHost()) // Verbindet sich mit 'localhost'
                .buildBlocking();

        testClient.connect();
        mockElevatorAPI = mock(IElevator.class);
        AtomicInteger callCount = new AtomicInteger(0);

        lenient(). when(mockElevatorAPI.getElevatorButton(anyInt(), anyInt())).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() % 2 == 0) {
                return (Object) false; // Rückgabewert beim ersten Aufruf
            } else {
                return (Object) true;  // Rückgabewert bei allen weiteren Aufrufen
            }
        });

        lenient().when(mockElevatorAPI.getElevatorNum()).thenReturn(Integer.valueOf(2));
        lenient().when(mockElevatorAPI.getFloorNum()).thenReturn(Integer.valueOf(5));
        lenient().when(mockElevatorAPI.getFloorHeight()).thenReturn(Integer.valueOf(3));


        lenient().when(mockElevatorAPI.getElevatorFloor(anyInt())).thenReturn(Integer.valueOf(0));
        lenient().when(mockElevatorAPI.getElevatorAccel(anyInt())).thenReturn(Integer.valueOf(0));
        lenient().when(mockElevatorAPI.getElevatorDoorStatus(anyInt())).thenReturn(Integer.valueOf(2));
        lenient().when(mockElevatorAPI.getElevatorPosition(anyInt())).thenReturn(Integer.valueOf(0));
        lenient().when(mockElevatorAPI.getElevatorSpeed(anyInt())).thenReturn(Integer.valueOf(0));
        lenient().when(mockElevatorAPI.getElevatorWeight(anyInt())).thenReturn(Integer.valueOf(0));
        lenient().when(mockElevatorAPI.getElevatorCapacity(anyInt())).thenReturn(Integer.valueOf(0));
        lenient().when(mockElevatorAPI.getFloorButtonDown(anyInt())).thenReturn(Boolean.valueOf(false));
        lenient().when(mockElevatorAPI.getFloorButtonUp(anyInt())).thenReturn(Boolean.valueOf(false));
        lenient().when(mockElevatorAPI.getServicesFloors(anyInt(), anyInt())).thenReturn(Boolean.valueOf(false));

        // when(elevatorAPI.getTarget(1)).thenReturn(5);
        lenient().when(mockElevatorAPI.getClockTick()).thenReturn(Long.valueOf(1000L));
        lenient().when(mockElevatorAPI.getCommittedDirection(1)).thenReturn(Integer.valueOf(1));

        // Create an elevatorSystem
        elevatorSystem = new ElevatorSystem(
                2,
                0,
                4,
                1000,
                7,
                mockElevatorAPI // Pass the mocked interface
        );

        // Create the MQTT adapter
        mqttAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                Host,
                "mqttAdapter", 250, mockElevatorAPI);

        algorithm = new ElevatorAlgorithm();

    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        testClient.disconnect();
        hivemqCe.stop();
    }

    @Test
    void testTestContainerStartup() throws Exception {
        assertTrue(hivemqCe.isRunning());
        assertNotNull(hivemqCe.getHost());
        assertTrue(hivemqCe.getMappedPort(1883) > 0);
    }

    /* Initialisation */

    @Test
    void testSetupRMIController() throws Exception {

        /* Use reflection to access private member */
        Field eSystemField = ElevatorAlgorithm.class.getDeclaredField("eSystem");
        eSystemField.setAccessible(true);
        ElevatorSystem eSystem = (ElevatorSystem) eSystemField.get(algorithm);

        assertNotNull(eSystem);
        assertEquals(10, eSystem.getFloorHeight());
        assertEquals(4,eSystem.getNumberOfFloors());
        assertEquals(2,eSystem.getTotalElevators());
    }


    @Test
    void testConnectionToMQTTBroker() {

        //assertTrue(algorithm.eMQTTAdapter.getClientState().isConnected());
        assertTrue(hivemqCe.isRunning());
    }

    @Test
    void givenBrokerDisconnect_whenTestClientReconnect_thenExpectedClientReconnect(){

    }

    @Test
    void givenQOS_whenPublished_testQOS() throws InterruptedException, RemoteException {

    }

    /* AlGORITHM */

    @Test
    void givenMultipleElevators_whenNoRequest_thenExpectedPositionAtGroundFloor() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
        when(mockElevatorAPI.getElevatorPosition(1)).thenReturn(Integer.valueOf(0));
        elevatorSystem.updateAll();


        assertEquals(0, mockElevatorAPI.getElevatorPosition(0));
        assertEquals(0, mockElevatorAPI.getElevatorPosition(1));
        verify(mockElevatorAPI,atLeastOnce()).setTarget(0,0);
        verify(mockElevatorAPI,atLeastOnce()).setTarget(1,0);
    }

    @Test
    void givenMultipleElevators_whenMultipleRequests_thenExpectCorrectElevatorAssigned() throws RemoteException, InterruptedException {
        /* Setup: Elevator 0 at halfway, elevator 1 at bottom and unserviced */
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(5));
        when(mockElevatorAPI.getElevatorPosition(1)).thenReturn(Integer.valueOf(0));
        elevatorSystem.updateAll();

        verify(mockElevatorAPI).setTarget(0,6);
        verify(mockElevatorAPI).setTarget(1,2);
    }

    @Test
    void givenElevator_whenAtAFloor_thenExecuteDoorStateTransitions() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
        when(mockElevatorAPI.getElevatorFloor(0)).thenReturn(Integer.valueOf(0));
        elevatorSystem.updateAll();


        /* Elevator should move up */
        verify(mockElevatorAPI).setTarget(0, 1);
        assertEquals(0, mockElevatorAPI.getCommittedDirection(0));
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(1));
    }

    @Test
    void testFloorRequestHandling() throws RemoteException {
        when(mockElevatorAPI.getFloorButtonUp(1)).thenReturn(Boolean.valueOf(true));
        when(mockElevatorAPI.getFloorButtonUp(2)).thenReturn(Boolean.valueOf(true));
        when(mockElevatorAPI.getFloorButtonUp(3)).thenReturn(Boolean.valueOf(true));

        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
        when(mockElevatorAPI.getTarget(0)).thenReturn(Integer.valueOf(1));
        elevatorSystem.updateAll();

        assertEquals(1, elevatorSystem.getTarget(0));
        verify(mockElevatorAPI,atLeastOnce()).getTarget(0);

        //assertTrue(mockElevatorAPI.getFloorButtonUp(1));
        //verify(mockElevatorAPI,atLeastOnce()).getFloorButtonUp(1);

        assertEquals(0, elevatorSystem.getElevatorPosition(0));
        verify(mockElevatorAPI,atLeastOnce()).getElevatorPosition(0);

    }

    @Test
    void testFloorButtonRequestHandling() throws RemoteException, InterruptedException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
        elevatorSystem.updateAll();


        verify(mockElevatorAPI).setTarget(0, 2);

    }

    @Test
    void testFloorButtonRequestHandlingDown() throws RemoteException, InterruptedException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(3));
        elevatorSystem.updateAll();


        verify(mockElevatorAPI).setTarget(0, 1);  // Elevator should go to floor 1

    }

    @Test
    void testMultipleFloorRequests() throws RemoteException, InterruptedException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
        elevatorSystem.updateAll();


        verify(mockElevatorAPI).setTarget(0, 2);
        verify(mockElevatorAPI).setTarget(0, 3);

    }

    @Test
    void testElevatorArrivesAtRequestedFloor() throws RemoteException, InterruptedException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(1));
        when(mockElevatorAPI.getElevatorButton(0,2)).thenReturn(Boolean.valueOf(true));

        elevatorSystem.updateAll();

        assertEquals(1,mockElevatorAPI.getElevatorPosition(0));
        assertTrue(mockElevatorAPI.getElevatorButton(0,2));


        assertEquals(2,mockElevatorAPI.getElevatorPosition(0));
        verify(mockElevatorAPI,atLeastOnce()).getElevatorPosition(0);

    }

    @Test
    void testElevatorIdleAfterAllRequests() throws RemoteException, InterruptedException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
        elevatorSystem.updateAll();


        verify(mockElevatorAPI).setTarget(0, 2);

        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(2));

        verify(mockElevatorAPI).setTarget(0, 0);

        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(Integer.valueOf(0));
    }
}
