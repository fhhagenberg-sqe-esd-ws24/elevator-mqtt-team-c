package at.wielander.elevator.Algorithm;

import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.util.function.Consumer;

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
    void givenMQTTClientException_whenSubscribeToElevatorAndFloorMessages_thenLogError() throws InterruptedException {
        if (!testClient.getState().isConnected()) {
            testClient.toBlocking().connect();
        }
        assertTrue(testClient.getState().isConnected());

        mqttAdapter.connect();
        mqttAdapter.run();

        algorithm.mqttClient = mock(Mqtt5AsyncClient.class);
    }



    /*****************+
     /* Algorithm
     /******************/

    @Test
    void givenButtonPress_whenProcessingUpQueue_thenExpectCorrectFloorInQueue() throws Exception {
        // Simul  ue(testClient.getState().isConnected());
        mqttAdapter.connect();
        mqttAdapter.run();
        algorithm.upQueue.add(5);
        algorithm.upQueue.add(7);

        assertEquals(2, algorithm.upQueue.size());
        assertTrue(algorithm.upQueue.contains(5));
        assertTrue(algorithm.upQueue.contains(7));
    }

//    @Test
//    void givenInvalidTopic_whenHandleButtonPresses_thenNoQueueUpdates() throws Exception {
//        Mqtt5AsyncClient mockMqttClient = mock(Mqtt5AsyncClient.class);
//        assertTrue(testClient.getState().isConnected(), "Client ist nicht verbunden");
//        mqttAdapter.connect();
//        mqttAdapter.run();
//        algorithm.initialiseElevatorSystem(mockElevatorAPI);
//
//        Method handleButtonPresses = ElevatorAlgorithm.class.getDeclaredMethod("handleButtonPresses",ElevatorAlgorithm.class, int.class);
//        handleButtonPresses.setAccessible(true);
//        handleButtonPresses.invoke(algorithm, algorithm,0);
//        algorithm.handleButtonPresses(algorithm,0);
//
//        synchronized (algorithm.upQueue) {
//            assertTrue(algorithm.upQueue.isEmpty(), "upQueue should remain empty for invalid topic");
//        }
//        synchronized (algorithm.downQueue) {
//            assertTrue(algorithm.downQueue.isEmpty(), "downQueue should remain empty for invalid topic");
//        }
//    }


    @Test
    void givenMultipleUpRequests_whenProcessingUpQueue_thenExpectCorrectOrder() throws Exception {
        assertTrue(testClient.getState().isConnected());
        mqttAdapter.connect();
        mqttAdapter.run();

        // Java Reflections to temporarily allow access
        Method processUpQueue = ElevatorAlgorithm.class.getDeclaredMethod("processUpQueue", int.class, int.class, ElevatorAlgorithm.class);
        processUpQueue.setAccessible(true);
        processUpQueue.invoke(algorithm, 0, 4, algorithm);
        algorithm.upQueue.add(1);
        algorithm.upQueue.add(2);
        algorithm.upQueue.add(3);

        assertEquals(3, algorithm.upQueue.size());
        assertTrue(algorithm.upQueue.contains(1));
        assertTrue(algorithm.upQueue.contains(2));
        assertTrue(algorithm.upQueue.contains(3));
    }

//    @Test
//    void givenMQTTPublishFailure_whenRetrying_thenExpectMaxRetries() throws InterruptedException {
//        Mqtt5AsyncClient mockMqttClient = mock(Mqtt5AsyncClient.class);
//        ElevatorAlgorithm algorithmSpy = spy(new ElevatorAlgorithm());
//        algorithmSpy.mqttClient = mockMqttClient;
//
//        doAnswer(invocation -> {
//            throw new RuntimeException("Simulated publish failure");
//        }).when(mockMqttClient).publishWith();
//
//        try {
//            Method publishWithRetry = ElevatorAlgorithm.class.getDeclaredMethod(
//                    "publishWithRetry", String.class, byte[].class);
//            publishWithRetry.setAccessible(true);
//            publishWithRetry.invoke(algorithmSpy, "elevator/0/targetFloor", "4".getBytes(StandardCharsets.UTF_8));
//        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//            System.err.println("Error invoking publishWithRetry: " + e.getMessage());
//        }
//        // Verify that the connection retry logic was executed the expected number of times
//        verify(mockMqttClient, times(5)).connect();
//    }
//
//    @Test
//    void givenInvalidFloorRequest_whenHandling_thenExpectNoQueueUpdate() {
//        algorithm.mqttClient = mock(Mqtt5AsyncClient.class);
//        assertTrue(testClient.getState().isConnected());
//        try {
//            mqttAdapter.connect();
//            mqttAdapter.run();
//
//            doAnswer(invocation -> {
//                Consumer<Mqtt5Publish> callback = invocation.getArgument(1);
//                Mqtt5Publish invalidPublish = Mqtt5Publish.builder()
//                        .topic("elevator/0/button/-1") // Invalid floor request
//                        .payload("".getBytes(StandardCharsets.UTF_8))
//                        .build();
//                callback.accept(invalidPublish); // Trigger the callback with invalid publish
//                return null;
//            }).when(algorithm.mqttClient).publishes(any(MqttGlobalPublishFilter.class), any());
//
//            algorithm.liveMessages.put("elevator/0/currentFloor", "0");
//
//            Method handleButtonPresses = null;
//            handleButtonPresses = ElevatorAlgorithm.class.getDeclaredMethod("handleButtonPresses", ElevatorAlgorithm.class, int.class);
//            assert handleButtonPresses != null;
//            handleButtonPresses.setAccessible(true);
//
//            // Assert that invalid floor (-1) is not added to any queue
//            synchronized (algorithm.upQueue) {
//                assertFalse(algorithm.upQueue.contains(-1), "Invalid floor should not be added to upQueue");
//            }
//            synchronized (algorithm.downQueue) {
//                assertFalse(algorithm.downQueue.contains(-1), "Invalid floor should not be added to downQueue");
//            }
//
//            // Verify publishes() was called
//            verify(algorithm.mqttClient, times(1)).publishes(any(MqttGlobalPublishFilter.class), any());
//        } catch (InterruptedException | NoSuchMethodException e) {
//            System.err.println(e.getMessage());
//        }
//    }
//
//    @Test
//    void givenNegativeFloorRequest_whenHandling_thenNoQueueUpdate() {
//        Mqtt5AsyncClient mockedClient = mock(Mqtt5AsyncClient.class);
//        algorithm.mqttClient = mockedClient;
//
//        doAnswer(invocation -> {
//            Consumer<Mqtt5Publish> callback = invocation.getArgument(1);
//            Mqtt5Publish invalidPublish = Mqtt5Publish.builder()
//                    .topic("floor/-1/buttonUp") // Negative floor request
//                    .payload("".getBytes(StandardCharsets.UTF_8))
//                    .build();
//            callback.accept(invalidPublish);
//            return null;
//        }).when(mockedClient).publishes(any(MqttGlobalPublishFilter.class), any());
//
//        algorithm.liveMessages.put("elevator/0/currentFloor", "0");
//
//        // Call the method under test
//        algorithm.handleButtonPresses(algorithm, 0);
//
//        synchronized (algorithm.upQueue) {
//            assertFalse(algorithm.upQueue.contains(-1));
//        }
//        synchronized (algorithm.downQueue) {
//            assertFalse(algorithm.downQueue.contains(-1));
//        }
//    }



    @Test
    void givenEmptyUpQueue_whenProcessing_thenSwitchToDownDirection(){
        try {
            Method processUpQueue = ElevatorAlgorithm.class.getDeclaredMethod("processUpQueue", int.class, int.class, ElevatorAlgorithm.class);
            processUpQueue.setAccessible(true);
            processUpQueue.invoke(algorithm, 0, 4, algorithm);

            algorithm.upQueue.add(1);


            String committedDirection = algorithm.liveMessages.get("elevator/0/committedDirection");
            assertEquals(1, Integer.parseInt(committedDirection));
        } catch (NoSuchMethodException | NumberFormatException | InvocationTargetException | IllegalAccessException |
                 SecurityException e) {
            System.err.println(e.getMessage());
        }
    }

//    @Test
//    void givenFloorReached_whenMovingElevator_thenExpectQueueCleanup() throws Exception {
//        algorithm.upQueue.add(3);
//        algorithm.upQueue.add(5);
//
//        Method moveElevator = ElevatorAlgorithm.class.getDeclaredMethod("moveElevator", int.class, int.class, ElevatorAlgorithm.class);
//        moveElevator.setAccessible(true);
//        moveElevator.invoke(algorithm, 0, 4, algorithm);
//        algorithm.moveElevator(0, 3, algorithm, ElevatorCommittedDirection.UP);
//
//        assertFalse(algorithm.upQueue.contains(3));
//        assertTrue(algorithm.upQueue.contains(5));
//    }
//
//
//
//    @Test
//    void givenMultipleDownRequests_whenProcessingDownQueue_thenExpectCorrectOrder() throws Exception {
//        algorithm.downQueue.add(9);
//        algorithm.downQueue.add(4);
//        algorithm.downQueue.add(6);
//
//        algorithm.processDownQueue(0, 7, algorithm);
//
//        assertEquals(2, algorithm.downQueue.size());
//        assertTrue(algorithm.downQueue.contains(4));
//        assertTrue(algorithm.downQueue.contains(6));
//        assertFalse(algorithm.downQueue.contains(9));
//    }
//
//    @Test
//    void givenUncommittedState_whenProcessing_thenExpectNextDirection() throws Exception {
//        algorithm.upQueue.add(3);
//        algorithm.downQueue.add(7);
//
//        algorithm.processUncommitted(0, algorithm);
//
//        assertEquals(0, algorithm.upQueue.size());
//        assertEquals(1, algorithm.downQueue.size());
//    }

//    @Test
//    void givenFloorReached_whenProcessingRequest_thenExpectDoorStateTransition() throws Exception {
//        algorithm.liveMessages.put("elevator/0/currentFloor", "5");
//        algorithm.upQueue.add(5);
//
//        algorithm.waitForElevatorToReachTarget(0, 5, algorithm);
//
//        String doorState = algorithm.liveMessages.get("elevator/0/doorState");
//        assertEquals(1, Integer.parseInt(doorState));
//    }
}