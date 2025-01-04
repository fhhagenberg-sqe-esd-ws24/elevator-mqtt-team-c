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
import org.mockito.MockitoAnnotations;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.TestcontainersExtension;
import org.testcontainers.utility.DockerImageName;
import sqelevator.IElevator;

import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(TestcontainersExtension.class)
@Testcontainers
class ElevatorAlgorithmTest {

    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));

    @Mock
    private IElevator mockElevatorAPI;

    private ElevatorSystem elevatorSystem;
    private ElevatorMQTTAdapter MQTTAdapter;
    private Mqtt5BlockingClient testClient;
    private String Host;

    @BeforeEach
    public void setup() throws RemoteException, InterruptedException {
        MockitoAnnotations.initMocks(this);

        hivemqCe.start();
        Host = "tcp://" + hivemqCe.getHost() + ":" + hivemqCe.getMappedPort(1883);
        System.out.println("Host address: " + Host);
        testClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(hivemqCe.getMappedPort(1883))
                .serverHost(hivemqCe.getHost())
                .buildBlocking();
        testClient.connect();
        elevatorSystem = new ElevatorSystem(2, 0, 4, 1000, 7, mockElevatorAPI);
        MQTTAdapter = new ElevatorMQTTAdapter(elevatorSystem, Host, "mqttAdapter", 250, mockElevatorAPI);
        MQTTAdapter.connect();
        MQTTAdapter.run();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        MQTTAdapter.disconnect();
        testClient.disconnect();
        hivemqCe.stop();
    }

    @Test
    void testConnectionToMQTTBroker() {
        assertTrue(hivemqCe.isRunning());
        assertTrue(testClient.getState().isConnected());
    }

    @Test
    void testFloorRequestHandling() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getFloorButtonUp(1)).thenReturn(true);
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(0);
        when(mockElevatorAPI.getTarget(0)).thenReturn(1);

        elevatorSystem.updateAll();

        assertEquals(1, elevatorSystem.getTarget(0));
        assertEquals(0, elevatorSystem.getElevatorPosition(0));
        verify(mockElevatorAPI).getTarget(0);
        verify(mockElevatorAPI).getFloorButtonUp(1);

    }

    @Test
    void testFloorButtonRequestHandling() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        String floor1UpButtonTopic = "floor/1/buttonUp";
        testClient.publishWith()
                .topic(floor1UpButtonTopic)
                .payload("pressed".getBytes())
                .send();

        verify(mockElevatorAPI).setTarget(0, 2);

    }

    @Test
    void testFloorButtonRequestHandlingDown() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(3);

        String floor2DownButtonTopic = "floor/2/buttonDown";
        testClient.publishWith()
                .topic(floor2DownButtonTopic)
                .payload("pressed".getBytes())
                .send();

        verify(mockElevatorAPI).setTarget(0, 1);  // Elevator should go to floor 1

    }

    @Test
    void testMultipleFloorRequests() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        testClient.publishWith()
                .topic("floor/2/buttonUp")
                .payload("pressed".getBytes())
                .send();

        testClient.publishWith()
                .topic("floor/3/buttonUp")
                .payload("pressed".getBytes())
                .send();

        verify(mockElevatorAPI).setTarget(0, 2);
        verify(mockElevatorAPI).setTarget(0, 3);

    }

    @Test
    void testElevatorArrivesAtRequestedFloor() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        testClient.publishWith()
                .topic("floor/2/buttonUp")
                .payload("pressed".getBytes())
                .send();

        verify(mockElevatorAPI).setTarget(0, 2);

        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(2);
    }

    @Test
    void testElevatorIdleAfterAllRequests() throws InterruptedException, RemoteException {
        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(0);

        testClient.publishWith()
                .topic("floor/2/buttonUp")
                .payload("pressed".getBytes())
                .send();

        verify(mockElevatorAPI).setTarget(0, 2);

        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(2);

        verify(mockElevatorAPI).setTarget(0, 0);

        when(mockElevatorAPI.getElevatorPosition(0)).thenReturn(0);
    }
}
