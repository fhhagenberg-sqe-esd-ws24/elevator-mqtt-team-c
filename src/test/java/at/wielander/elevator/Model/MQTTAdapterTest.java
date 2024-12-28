package at.wielander.elevator.Model;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.client.mqtt.MqttClientState;
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
        //mock all elevator buttons
        for (int elevator = 0; elevator <= 1; elevator++) {
            for (int button = 0; button <= 3; button++) {
                lenient().when(elevatorAPI.getElevatorButton(anyInt(), anyInt())).thenReturn(false);
            }
        }
        lenient().when(elevatorAPI.getElevatorNum()).thenReturn(2);
        lenient().when(elevatorAPI.getFloorNum()).thenReturn(5);
        lenient().when(elevatorAPI.getFloorHeight()).thenReturn(3);
        
        
        lenient().when(elevatorAPI.getElevatorFloor(anyInt())).thenReturn(0);
        lenient().when(elevatorAPI.getElevatorAccel(anyInt())).thenReturn(0);
        lenient().when(elevatorAPI.getElevatorDoorStatus(anyInt())).thenReturn(2);
        lenient().when(elevatorAPI.getElevatorPosition(anyInt())).thenReturn(0);
        lenient().when(elevatorAPI.getElevatorSpeed(anyInt())).thenReturn(0);
        lenient().when(elevatorAPI.getElevatorWeight(anyInt())).thenReturn(0);
        lenient().when(elevatorAPI.getElevatorCapacity(anyInt())).thenReturn(0);
        lenient().when(elevatorAPI.getFloorButtonDown(anyInt())).thenReturn(false);
        lenient().when(elevatorAPI.getFloorButtonUp(anyInt())).thenReturn(false);
        lenient().when(elevatorAPI.getServicesFloors(anyInt(), anyInt())).thenReturn(false);

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
                "mqttAdapter", 250);

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
    void testConnect() {
        assertDoesNotThrow(() -> {
            MQTTAdapter.connect();
            assertEquals(MqttClientState.CONNECTED, MQTTAdapter.getClientState(), "MQTT client should be connected.");
        });
    }

    @Test
    void testDisconnect() {
        assertDoesNotThrow(() -> {
            MQTTAdapter.connect();
            assertEquals(MqttClientState.CONNECTED, MQTTAdapter.getClientState(), "MQTT client should be connected.");

            // Disconnect
            MQTTAdapter.disconnect();
            assertNotEquals(MqttClientState.CONNECTED, MQTTAdapter.getClientState(), "MQTT client should be disconnected.");
        });
    }
    
    @Test
    void testReconnect() {
        assertDoesNotThrow(() -> {
            // Reconnect
            MQTTAdapter.reconnect();
            assertEquals(MqttClientState.CONNECTED, MQTTAdapter.getClientState(), "MQTT client should be reconnected.");
            MQTTAdapter.reconnect();
            assertEquals(MqttClientState.CONNECTED, MQTTAdapter.getClientState(), "MQTT client should be reconnected.");
        });
    }

    
    @Test
    void testPublishRetainedTopics() throws MqttException, InterruptedException {
        // Ensure client is connected
        assertTrue(testClient.getState().isConnected(), "Client is not connected");

        // Verbinde den MQTTAdapter
        MQTTAdapter.connect();

        // Erwartete Werte basierend auf dem ElevatorSystem-Konstruktor
        Map<String, String> expectedMessages = Map.of(
                "building/info/numberOfElevators", "2", // 2 Aufzüge
                "building/info/numberOfFloors", "5",    // 5 Stockwerke (0 bis 4)
                "building/info/floorHeight/feet", "7"  // Höhe eines Stockwerks
        );

        // Abonniere alle Topics und prüfe die Nachrichten
        CountDownLatch latch = new CountDownLatch(expectedMessages.size()); // Für Synchronisation
        for (Map.Entry<String, String> entry : expectedMessages.entrySet()) {
            String topic = entry.getKey();
            String expectedValue = entry.getValue();

            testClient.toAsync().subscribeWith()
                    .topicFilter(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback(publish -> {
                        String receivedMessage = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                        System.out.println("Nachricht empfangen: " + receivedMessage + " für Topic: " + topic);

                        // Überprüfe die Nachricht und das Topic
                       assertEquals(expectedValue, receivedMessage, "Unerwartete Nachricht für Topic: " + topic);
                       assertEquals(topic, publish.getTopic().toString(), "Unerwartetes Topic");

                        latch.countDown(); // Zähle herunter, wenn die Nachricht erfolgreich überprüft wurde
                    })
                    .send()
                    .whenComplete((subAck, throwable) -> {
                        if (throwable != null) {
                            System.err.println("Subscription failed for topic " + topic + ": " + throwable.getMessage());
                        } else {
                            System.out.println("Subscription erfolgreich für Topic: " + topic);
                        }
                    });
        }

        // Starte die Methode, die die retained Nachrichten veröffentlicht
        MQTTAdapter.run();

        // Warte, bis alle Nachrichten empfangen und geprüft wurden
       assertTrue(latch.await(5, TimeUnit.SECONDS), "Nicht alle Nachrichten wurden rechtzeitig empfangen");
    }
    
    @Test
    void testPeriodicUpdates() throws InterruptedException, RemoteException, MQTTAdapterException {
        // Sicherstellen, dass der Client verbunden ist
        if (!testClient.getState().isConnected()) {
            testClient.toBlocking().connect();
        }
        assertTrue(testClient.getState().isConnected(), "Client ist nicht verbunden");

        // MQTTAdapter starten
        MQTTAdapter.connect();
        MQTTAdapter.run();

        // Liste aller Topics vorbereiten
        List<String> topics = new ArrayList<>();
        for (int elevatorId = 0; elevatorId < 2; elevatorId++) {
            topics.add("elevator/" + elevatorId + "/currentFloor");
            topics.add("elevator/" + elevatorId + "/speed");
            topics.add("elevator/" + elevatorId + "/weight");
            topics.add("elevator/" + elevatorId + "/doorState");
            for (int buttonId = 0; buttonId < 4; buttonId++) {
                topics.add("elevator/" + elevatorId + "/button/" + buttonId);
            }
        }
        for (int floorId = 0; floorId < 4; floorId++) {
            topics.add("floor/" + floorId + "/buttonDown");
            topics.add("floor/" + floorId + "/buttonUp");
        }

        // CountDownLatch für Synchronisation
        CountDownLatch latch = new CountDownLatch(topics.size());

        // Abonnieren der Topics
        for (String topic : topics) {
            testClient.toAsync()
                .subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback(publish -> {
                    String receivedMessage = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    System.out.println("Nachricht empfangen: " + receivedMessage + " für Topic: " + publish.getTopic());
                    
                    
                    // Überprüfung der empfangenen Nachrichten basierend auf dem Topic
                    String topicName = publish.getTopic().toString();
                    if (topicName.equals("elevator/0/currentFloor") || topicName.equals("elevator/1/currentFloor")) {
                        assertEquals("0", receivedMessage, "Unerwarteter Wert für currentFloor");
                    } else if (topicName.equals("elevator/0/speed") || topicName.equals("elevator/1/speed")) {
                        assertEquals("0", receivedMessage, "Unerwarteter Wert für speed");
                    } else if (topicName.equals("elevator/0/weight") || topicName.equals("elevator/1/weight")) {
                        assertEquals("0", receivedMessage, "Unerwarteter Wert für weight");
                    } else if (topicName.equals("elevator/0/doorState") || topicName.equals("elevator/1/doorState")) {
                        assertEquals("2", receivedMessage, "Unerwarteter Wert für doorState");
                    }

                    // Überprüfung der Button-Status: immer "false" erwarten
                    else if (topicName.equals("elevator/0/button/0") || topicName.equals("elevator/0/button/1") ||
                             topicName.equals("elevator/0/button/2") || topicName.equals("elevator/0/button/3") ||
                             topicName.equals("elevator/1/button/0") || topicName.equals("elevator/1/button/1") ||
                             topicName.equals("elevator/1/button/2") || topicName.equals("elevator/1/button/3") ||
                             topicName.equals("floor/0/buttonDown") || topicName.equals("floor/0/buttonUp") ||
                             topicName.equals("floor/1/buttonDown") || topicName.equals("floor/1/buttonUp") ||
                             topicName.equals("floor/2/buttonDown") || topicName.equals("floor/2/buttonUp") ||
                             topicName.equals("floor/3/buttonDown") || topicName.equals("floor/3/buttonUp")) {
                        assertEquals("false", receivedMessage, "Button-Status sollte false sein für Topic: " + topicName);
                    }
                    
                    latch.countDown(); // Zähle herunter, wenn Nachricht empfangen wurde
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Subscription fehlgeschlagen für Topic " + topic + ": " + throwable.getMessage());
                    } else {
                        System.out.println("Subscription erfolgreich für Topic: " + topic);
                    }
                });
        }

//        // Simulierte Werte für die API-Mockings
//        lenient().when(elevatorAPI.getElevatorFloor(anyInt())).thenReturn(2);
//        lenient().when(elevatorAPI.getElevatorSpeed(anyInt())).thenReturn(3);
//        lenient().when(elevatorAPI.getElevatorWeight(anyInt())).thenReturn(15);
//        lenient().when(elevatorAPI.getElevatorDoorStatus(anyInt())).thenReturn(1);

        // Wartezeit, damit Nachrichten empfangen werden können
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Nicht alle Nachrichten wurden empfangen");

        // Test-Ende: Verbindung trennen
        testClient.toBlocking().disconnect();
    }
}