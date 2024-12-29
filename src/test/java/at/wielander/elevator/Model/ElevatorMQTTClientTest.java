//package at.wielander.elevator.Model;
//
//import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
//import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
//import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.testcontainers.hivemq.HiveMQContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//import sqelevator.IElevator;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.rmi.RemoteException;
//import java.util.Optional;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@Testcontainers
//class ElevatorMQTTClientTest {
//
//    @Container
//    final HiveMQContainer testMQTTBroker = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));
//
//    @Mock
//    private IElevator elevatorAPI;
//
//    private ElevatorSystem elevatorSystem;
//    private ElevatorMQTTClient MQTTClient;
//    private Mqtt5BlockingClient testClient;
//    private String Host;
//
//    private static final int RECONNECT_DELAY = 10;
//
//    @BeforeEach
//    void setUp() throws InterruptedException {
//        testMQTTBroker.start();
//
//        Host = "tcp://" + testMQTTBroker.getHost() + ":" + testMQTTBroker.getMappedPort(1883);
//        System.out.println("Host address: " + Host);
//
//        testClient = Mqtt5Client.builder()
//                .identifier("testClient")
//                .serverPort(testMQTTBroker.getMappedPort(1883))
//                .serverHost(testMQTTBroker.getHost())
//                .buildBlocking();
//
//        testClient.connect();
//        MockitoAnnotations.openMocks(this);
//
//        elevatorSystem = new ElevatorSystem(
//                1,
//                0,
//                2,
//                4000,
//                7,
//                elevatorAPI);
//
//        MQTTClient = new ElevatorMQTTClient(Host, "testClient", elevatorSystem);
//
//        assertTrue(testClient.getState().isConnected());
//    }
//
//    @Test
//    void connectAndSubscribe() {
//        assertDoesNotThrow(() -> MQTTClient.connectAndSubscribe());
//        assertTrue(testClient.getState().isConnected());
//    }
//
//    @Test
//    void shutdown() {
//        MQTTClient.connectAndSubscribe();
//        MQTTClient.shutdown();
//        Thread.onSpinWait();
//        assertFalse(testClient.getState().isConnected());
//    }
//
//    @Test
//    void handleMessage_updatesElevatorSystem() throws Exception {
//        MQTTClient.connectAndSubscribe();
//
//        String topic = "elevator/0/currentFloor";
//        String payload = "1";
//        elevatorAPI.setTarget(0,1);
//
//        Mqtt5PublishResult publishResult = testClient.publishWith()
//                .topic(topic)
//                .payload(payload.getBytes(StandardCharsets.UTF_8))
//                .send();
//
//        TimeUnit.SECONDS.sleep(1);
//        verify(elevatorAPI).setTarget(0, 1);
//    }
//
//    @Test
//    void publishMessage_sendsToCorrectTopic() {
//        MQTTClient.connectAndSubscribe();
//
//        String topic = "elevator/0/test";
//        String payload = "test message";
//
//        assertDoesNotThrow(() -> {
//            MQTTClient.publishMessage(topic, payload);
//        });
//
//        Mqtt5PublishResult publishResult = testClient.publishWith()
//                .topic(topic)
//                .payload(payload.getBytes(StandardCharsets.UTF_8))
//                .send();
//
//        Optional<ByteBuffer> resultPayload = publishResult.getPublish().getPayload();
//
//        assertTrue(resultPayload.isPresent());
//        byte[] resultBytes = new byte[resultPayload.get().remaining()];
//        resultPayload.get().get(resultBytes);
//        assertEquals(payload, new String(resultBytes, StandardCharsets.UTF_8));
//    }
//
//    @Test
//    void scheduleReconnect_attemptsToReconnect() throws InterruptedException {
//        MQTTClient.connectAndSubscribe();
//        testClient.disconnect();
//
//        TimeUnit.SECONDS.sleep(RECONNECT_DELAY + 1);
//
//        assertFalse(testClient.getState().isConnected());
//    }
//
//    @Test
//    void handleFloorButtonStates_updatesCorrectly() throws RemoteException, InterruptedException {
//        MQTTClient.connectAndSubscribe();
//
//        String topic = "floor/0/buttonDown";
//        String payload = "true";
//
//        Mqtt5PublishResult publishResult = testClient.publishWith()
//                .topic(topic)
//                .payload(payload.getBytes(StandardCharsets.UTF_8))
//                .send();
//
//        TimeUnit.SECONDS.sleep(1);
//
//        assertTrue(elevatorSystem.getFloorButtonDown(0));
//    }
//}
