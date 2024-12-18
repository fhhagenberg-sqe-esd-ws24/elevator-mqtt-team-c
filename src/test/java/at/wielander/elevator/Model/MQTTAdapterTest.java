package at.wielander.elevator.Model;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.hivemq.HiveMQContainer;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.MqttVersion;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import sqelevator.IElevator;

import org.eclipse.paho.mqttv5.common.MqttException;

@ExtendWith(MockitoExtension.class)
@Testcontainers
public class MQTTAdapterTest {

    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));;

    @Mock
    private IElevator elevatorAPI;

    // @InjectMocks
    private ElevatorSystem elevatorSystem;

    private ElevatorMQTTAdapter MQTTAdapter;

    private Mqtt5BlockingClient testClient;

    private String Host;

    @BeforeEach
    public void setup() throws MqttException, RemoteException {

        hivemqCe.start();

        System.out.println("Mapped port: " + hivemqCe.getMappedPort(1883)); // Ausgabe des externen Ports

        Host = "tcp://" + hivemqCe.getHost() + ":" + hivemqCe.getMappedPort(1883);

        System.out.println("Host addresse: " + Host);
        testClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(hivemqCe.getMappedPort(1883)) // Verwenden Sie den dynamisch gemappten Port
                .serverHost(hivemqCe.getHost()) // Verbindet sich mit 'localhost'
                .buildBlocking();

        testClient.connect();
        MockitoAnnotations.openMocks(this);

        lenient().when(elevatorAPI.getElevatorNum()).thenReturn(2);
        lenient().when(elevatorAPI.getElevatorFloor(1)).thenReturn(1);
        lenient().when(elevatorAPI.getElevatorAccel(1)).thenReturn(15);
        lenient().when(elevatorAPI.getElevatorDoorStatus(1)).thenReturn(2);
        lenient().when(elevatorAPI.getElevatorPosition(1)).thenReturn(1);
        lenient().when(elevatorAPI.getElevatorSpeed(1)).thenReturn(5);
        lenient().when(elevatorAPI.getElevatorWeight(1)).thenReturn(10);
        lenient().when(elevatorAPI.getElevatorCapacity(1)).thenReturn(5);
        lenient().when(elevatorAPI.getElevatorButton(1, 1)).thenReturn(true);

        lenient().when(elevatorAPI.getFloorButtonDown(1)).thenReturn(true);
        lenient().when(elevatorAPI.getFloorButtonUp(1)).thenReturn(false);
        lenient().when(elevatorAPI.getFloorNum()).thenReturn(5);
        lenient().when(elevatorAPI.getFloorHeight()).thenReturn(3);
        lenient().when(elevatorAPI.getServicesFloors(1, 1)).thenReturn(true);

        // when(elevatorAPI.getTarget(1)).thenReturn(5);
        lenient().when(elevatorAPI.getClockTick()).thenReturn(1000L);
        lenient().when(elevatorAPI.getCommittedDirection(1)).thenReturn(1);

        // Create an elevatorSystem
        elevatorSystem = new ElevatorSystem(
                2,
                0,
                4,
                1000,
                7,
                elevatorAPI // Pass the mocked interface
        );
        // Create the MQTT adapter
        MQTTAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                Host,
                "mqttAdapter", 100);

    }

    @AfterEach
    public void tearDown() {

        hivemqCe.stop();
    }

    @Test
    public void testContainerStartup() {
        assertTrue(hivemqCe.isRunning(), "HiveMQ container should be running.");
        assertNotNull(hivemqCe.getHost(), "Container host should not be null.");
        assertTrue(hivemqCe.getMappedPort(1883) > 0, "MQTT port should be greater than 0.");
    }

    @Test
    void testConnect() throws MqttException {
        // assertNotNull(testClient);
        // assertTrue(testClient.getState().isConnected());
    }

    @Test
    void testDisconnect() throws MqttException {
        // testClient.disconnect();
        // assertFalse(testClient.getState().isConnected());
    }

    @Test
    void testSimpleSubscription() throws MqttException, InterruptedException {
        // Ensure client is connected
        assertTrue(testClient.getState().isConnected(), "Client is not connected");
        MQTTAdapter.connect();
        // Subscribe to the topic and wait for subscription to be processed
        testClient.toAsync().subscribeWith().topicFilter("building/info/numberOfElevators").qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    System.out.println("Nachricht empfangen: " + message);
                    assertEquals("2", message); // Überprüfe die empfangene Nachricht
                    assertEquals("building/info/numberOfElevators", publish.getTopic().toString()); // Überprüfe das
                                                                                                    // Topic
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Subscription failed: " + throwable.getMessage());
                    } else {
                        System.out.println("Subscription erfolgreich: " + subAck);
                    }
                });

        // Run the method that publishes the message
        MQTTAdapter.run();

    }

    @Test
    void testRetainedTopics() throws MqttException, InterruptedException {
        // Ensure client is connected
        assertTrue(testClient.getState().isConnected(), "Client is not connected");

        // Connect the MQTT adapter
        MQTTAdapter.connect();

        // List of topics and their expected payloads
        Map<String, String> expectedMessages = Map.of(
                "building/info/numberOfElevators", "2",
                "building/info/numberOfFloors", "10",
                "building/info/floorHeight/feet", "12",
                "building/info/systemClockTick", "100",
                "building/info/rmiConnected", "true");

        CountDownLatch latch = new CountDownLatch(expectedMessages.size());

        // Subscribe to all topics and verify payloads
        expectedMessages.forEach((topic, expectedPayload) -> {
            testClient.toAsync().subscribeWith()
                    .topicFilter(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback(publish -> {
                        String message = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                        System.out.println("Nachricht empfangen: " + message);
                        assertEquals(expectedPayload, message, "Payload mismatch for topic: " + topic);
                        assertEquals(topic, publish.getTopic().toString(), "Topic mismatch");
                        latch.countDown();
                    })
                    .send()
                    .whenComplete((subAck, throwable) -> {
                        if (throwable != null) {
                            System.err
                                    .println("Subscription failed for topic " + topic + ": " + throwable.getMessage());
                        } else {
                            System.out.println("Subscription erfolgreich: " + subAck);
                        }
                    });
        });

        // Run the method that publishes the retained topics
        MQTTAdapter.run();

        // assertTrue(latch.await(5, TimeUnit.SECONDS), "Not all topics received

    }
}