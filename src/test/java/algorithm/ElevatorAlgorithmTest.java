package algorithm;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import elevator.ElevatorMQTTAdapter;
import elevator.ElevatorSystem;
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

import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class ElevatorAlgorithmTest {

    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));

    private IElevator iElevatorAPI;

    private ElevatorAlgorithm elevatorAlgorithm;

    private ElevatorSystem elevatorSystem;

    @Mock
    private ElevatorMQTTAdapter mockMQTTAdapter;

    @Mock
    private Mqtt5BlockingClient mockMQTTClient;

    private String Host;

    @BeforeEach
    public void setup() throws RemoteException {

        hivemqCe.start();
        Host = "tcp://" + hivemqCe.getHost() + ":" + hivemqCe.getMappedPort(1883);

        System.out.println("Host address: " + Host);

        mockMQTTClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(hivemqCe.getMappedPort(1883))
                .serverHost(hivemqCe.getHost())
                .buildBlocking();
        mockMQTTClient.connect();

        elevatorAlgorithm = new ElevatorAlgorithm();
        elevatorAlgorithm.mqttClient = mockMQTTClient;
        elevatorAlgorithm.eMQTTAdapter = mockMQTTAdapter;

        // Lenient stubbings
        AtomicInteger callCount = new AtomicInteger(0);
        lenient(). when(iElevatorAPI.getElevatorButton(anyInt(), anyInt())).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() % 2 == 0) {
                return false;
            } else {
                return true;
            }
        });

        lenient().when(iElevatorAPI.getElevatorNum()).thenReturn(2);
        lenient().when(iElevatorAPI.getFloorNum()).thenReturn(5);
        lenient().when(iElevatorAPI.getFloorHeight()).thenReturn(3);
        lenient().when(iElevatorAPI.getElevatorFloor(anyInt())).thenReturn(0);
        lenient().when(iElevatorAPI.getElevatorAccel(anyInt())).thenReturn(0);
        lenient().when(iElevatorAPI.getElevatorDoorStatus(anyInt())).thenReturn(2);
        lenient().when(iElevatorAPI.getElevatorPosition(anyInt())).thenReturn(0);
        lenient().when(iElevatorAPI.getElevatorSpeed(anyInt())).thenReturn(0);
        lenient().when(iElevatorAPI.getElevatorWeight(anyInt())).thenReturn(0);
        lenient().when(iElevatorAPI.getElevatorCapacity(anyInt())).thenReturn(0);
        lenient().when(iElevatorAPI.getFloorButtonDown(anyInt())).thenReturn(false);
        lenient().when(iElevatorAPI.getFloorButtonUp(anyInt())).thenReturn(false);
        lenient().when(iElevatorAPI.getServicesFloors(anyInt(), anyInt())).thenReturn(false);

        // when(elevatorAPI.getTarget(1)).thenReturn(5);
        lenient().when(iElevatorAPI.getClockTick()).thenReturn(1000L);
        lenient().when(iElevatorAPI.getCommittedDirection(1)).thenReturn(1);

        // Create an elevatorSystem
        elevatorSystem = new ElevatorSystem(
                2,
                0,
                4,
                1000,
                7,
                iElevatorAPI // Pass the mocked interface
        );

        // Create the MQTT adapter
        mockMQTTAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                Host,
                "mqttAdapter", 250, iElevatorAPI);



    }

    @AfterEach
    public void tearDown() throws InterruptedException {

        mockMQTTAdapter.disconnect();
        mockMQTTClient.disconnect();
        hivemqCe.stop();
    }

    @Test
    void testTestContainerStartup() throws InterruptedException {
        mockMQTTAdapter.connect();

        assertTrue(hivemqCe.isRunning());
        assertNotNull(hivemqCe.getHost());
        assertTrue(hivemqCe.getMappedPort(1883) > 0);
    }

    @Test
    void testConnectionToMQTTBroker() {
        assertTrue(mockMQTTClient.getState().isConnected());
        assertTrue(hivemqCe.isRunning());
        assertDoesNotThrow(()-> mockMQTTAdapter.connect());
    }

    @Test
    void givenElevator_whenAtAFloor_thenExecuteDoorStateTransitions() throws InterruptedException {
        mockMQTTAdapter.connect();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockMQTTClient).publishWith();
        elevatorAlgorithm = new ElevatorAlgorithm();

        mockMQTTAdapter.run();
    }

    @Test
    void testFloorRequestHandling() throws RemoteException {
        when(iElevatorAPI.getFloorButtonUp(1)).thenReturn(true);
        when(iElevatorAPI.getFloorButtonUp(2)).thenReturn(true);
        when(iElevatorAPI.getFloorButtonUp(3)).thenReturn(true);

        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(0);
        when(iElevatorAPI.getTarget(0)).thenReturn(1);

        elevatorSystem.updateAll();

        assertEquals(1, elevatorSystem.getTarget(0));
        verify(iElevatorAPI,atLeastOnce()).getTarget(0);

        //assertTrue(mockElevatorAPI.getFloorButtonUp(1));
        //verify(mockElevatorAPI,atLeastOnce()).getFloorButtonUp(1);

        assertEquals(0, elevatorSystem.getElevatorPosition(0));
        verify(iElevatorAPI,atLeastOnce()).getElevatorPosition(0);

    }

    @Test
    void testFloorButtonRequestHandling() throws RemoteException {
        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        String floor1UpButtonTopic = "floor/1/buttonUp";
        mockMQTTClient.publishWith()
                .topic(floor1UpButtonTopic)
                .payload("pressed".getBytes())
                .send();

        verify(iElevatorAPI).setTarget(0, 2);

    }

    @Test
    void testFloorButtonRequestHandlingDown() throws RemoteException {
        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(3);

        String floor2DownButtonTopic = "floor/2/buttonDown";
        mockMQTTClient.publishWith()
                .topic(floor2DownButtonTopic)
                .payload("pressed".getBytes())
                .send();

        verify(iElevatorAPI).setTarget(0, 1);  // Elevator should go to floor 1

    }

    @Test
    void testMultipleFloorRequests() throws RemoteException {
        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        mockMQTTClient.publishWith()
                .topic("floor/2/buttonUp")
                .payload("pressed".getBytes())
                .send();

        mockMQTTClient.publishWith()
                .topic("floor/3/buttonUp")
                .payload("pressed".getBytes())
                .send();

        verify(iElevatorAPI).setTarget(0, 2);
        verify(iElevatorAPI).setTarget(0, 3);

    }

    @Test
    void testElevatorArrivesAtRequestedFloor() throws RemoteException {
        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        mockMQTTClient.publishWith()
                .topic("floor/2/buttonUp")
                .payload("pressed".getBytes())
                .send();

        verify(iElevatorAPI).setTarget(0, 2);

        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(2);
    }

    @Test
    void testElevatorIdleAfterAllRequests() throws RemoteException {
        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        mockMQTTClient.publishWith()
                .topic("floor/2/buttonUp")
                .payload("pressed".getBytes())
                .send();

        verify(iElevatorAPI).setTarget(0, 2);

        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(2);

        verify(iElevatorAPI).setTarget(0, 0);

        when(iElevatorAPI.getElevatorPosition(0)).thenReturn(0);
    }
}
