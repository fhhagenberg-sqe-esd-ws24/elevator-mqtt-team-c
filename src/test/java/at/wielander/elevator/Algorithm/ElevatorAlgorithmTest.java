package at.wielander.elevator.Algorithm;

import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.javassist.tools.rmi.RemoteException;
import org.testcontainers.utility.DockerImageName;
import sqelevator.IElevator;

import java.lang.reflect.Method;
import java.rmi.Naming;

import static at.wielander.elevator.Algorithm.ElevatorAlgorithm.connectToRMI;
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

        // Create an elevatorSystem
        elevatorSystem = new ElevatorSystem(
                1,
                0,
                10,
                4000,
                10,
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
    public void tearDown() {
        testClient.disconnect();
        hivemqCe.stop();
    }

    @Test
    void givenTestContainer_whenCheckingStatus_ThenExpectContainerShouldBeRunning() {
        assertTrue(hivemqCe.isRunning());
        assertNotNull(hivemqCe.getHost());
        assertTrue(hivemqCe.getMappedPort(1883) > 0);
    }

    /* Initialisation */

    @Test
    void givenValidRMIService_whenConnectingToRMI_ThenExpectSuccessfulConnection() {
        try (MockedStatic<Naming> namingMock = mockStatic(Naming.class)) {
            namingMock.when(() -> Naming.lookup("rmi://localhost/ElevatorSim")).thenReturn(mockElevatorAPI);

            IElevator controller = connectToRMI();

            assertNotNull(controller);
            assertEquals(mockElevatorAPI, controller);
            namingMock.verify(() -> Naming.lookup("rmi://localhost/ElevatorSim"), atLeastOnce());
        }
    }

    @Test
    void givenRMIConnectionFails_whenRetrying_thenExpectSuccessfulConnection() {
        // Test if remote exeception can be caught
        try (MockedStatic<Naming> namingMock = mockStatic(Naming.class)) {
            namingMock.when(() -> Naming.lookup("rmi://localhost/ElevatorSim"))
                    .thenThrow(new RemoteException("First attempt RMI Connection failed"))
                    .thenThrow(new RemoteException("Second attempt RMI Connection failed"))
                    .thenThrow(new RemoteException("Third attempt RMI Connection failed"))
                    .thenThrow(new RemoteException("Fourth attempt RMI Connection failed"))
                    .thenReturn(mock(IElevator.class));

            RemoteException remoteException = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException.getMessage());

            RemoteException remoteException2 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException2.getMessage());

            RemoteException remoteException3 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException3.getMessage());

            RemoteException remoteException4 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException4.getMessage());

            IElevator controller = ElevatorAlgorithm.connectToRMI();
            assertNotNull(controller);
            namingMock.verify(() -> Naming.lookup("rmi://localhost/ElevatorSim"), atLeast(3));
        }
    }

    @Test
    void givenRMIConnectionFails_whenMaxRetriesExceeded_thenExpectConnectionFailed() {
        try (MockedStatic<Naming> namingMock = mockStatic(Naming.class)) {
            namingMock.when(() -> Naming.lookup("rmi://localhost/ElevatorSim"))
                    .thenThrow(new RemoteException("Connection failed"));

            RemoteException remoteException = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException.getMessage());

            RemoteException remoteException2 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException2.getMessage());

            RemoteException remoteException3 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException3.getMessage());

            RemoteException remoteException4 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException4.getMessage());

            RemoteException remoteException5 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException5.getMessage());

            RemoteException remoteException6 = assertThrows(RemoteException.class, ElevatorAlgorithm::connectToRMI
            );
            System.out.println(remoteException6.getMessage());

            namingMock.verify(() -> Naming.lookup("rmi://localhost/ElevatorSim"), atLeast(5));
        }
    }

    /* MQTT */

    @Test
    void givenMQTTClient_whenConnecting_thenExpectClientSuccessfulConnection() {
        algorithm.connectMQTTClient();
        assertNotNull(algorithm.mqttClient);
    }

    @Test
    void givenMQTTClient_whenReconnecting_thenExpectClientSuccessfulConnection() {
        testClient.disconnect();
        testClient.connect();
        assertTrue(testClient.getState().isConnected());
    }

    @Test
    void givenMQTTClient_whenDisconnecting_thenExpectClientDisconnects() {
        algorithm.connectMQTTClient();
        algorithm.mqttClient.disconnect();
        assertFalse(algorithm.mqttClient.getState().isConnected());
    }

    @Test
    void givenMQTTClient_whenDisconnect_thenExpectClientSuccessfulConnectsAfterReconnect() {
        testClient.disconnect();
        testClient.connect();
        assertTrue(testClient.getState().isConnected());

        testClient.disconnect();
        testClient.connect();
        assertTrue(testClient.getState().isConnected());
    }

    @Test
    void givenButtonPress_whenProcessingUpQueue_thenExpectCorrectFloorInQueue() throws Exception {
        // Simulate button press
        assertTrue(testClient.getState().isConnected());
        mqttAdapter.connect();
        mqttAdapter.run();
        algorithm.upQueue.add(5);
        algorithm.upQueue.add(7);

        assertEquals(2, algorithm.upQueue.size());
        assertTrue(algorithm.upQueue.contains(5));
        assertTrue(algorithm.upQueue.contains(7));
    }
}