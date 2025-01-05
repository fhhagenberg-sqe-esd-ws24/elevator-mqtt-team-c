package at.wielander.elevator.Model;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import sqelevator.IElevator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.hivemq.HiveMQContainer;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;

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

        Host = "tcp://" + hivemqCe.getHost() + ":" + hivemqCe.getMappedPort(1883);

        System.out.println("Host addresse: " + Host);
        testClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(hivemqCe.getMappedPort(1883)) // Verwenden Sie den dynamisch gemappten Port
                .serverHost(hivemqCe.getHost()) // Verbindet sich mit 'localhost'
                .buildBlocking();

        testClient.connect();
        elevatorAPI = mock(IElevator.class);
        AtomicInteger callCount = new AtomicInteger(0);

        lenient(). when(elevatorAPI.getElevatorButton(anyInt(), anyInt())).thenAnswer(invocation -> {
            if (callCount.getAndIncrement() % 2 == 0) {
                return false; // Rückgabewert beim ersten Aufruf
            } else {
                return true;  // Rückgabewert bei allen weiteren Aufrufen
            }
        });
        
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
                "mqttAdapter", 250, elevatorAPI);

    }

    @AfterEach
    public void tearDown() throws InterruptedException {
    	MQTTAdapter.disconnect();
    	testClient.disconnect();
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

        // Verbinde den MQTT
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
    void testPeriodicUpdates() throws InterruptedException, RemoteException, ElevatorMQTTAdapter.MQTTAdapterException {
        // Sicherstellen, dass der Client verbunden ist
        if (!testClient.getState().isConnected()) {
            testClient.toBlocking().connect();
        }
        assertTrue(testClient.getState().isConnected(), "Client ist nicht verbunden");

        // MQTT starten
        MQTTAdapter.connect();
        MQTTAdapter.run();

        // Set für empfangene Nachrichten (für jedes Thema)
        Set<String> receivedValues = new HashSet<>();
        Set<String> expectedValues = new HashSet<>(Arrays.asList("0", "false", "true"));

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
                    
                    // Speicherung der empfangenen Werte
                    if (topicName.equals("elevator/0/currentFloor") || topicName.equals("elevator/1/currentFloor")) {
                        receivedValues.add(receivedMessage);
                    } else if (topicName.equals("elevator/0/speed") || topicName.equals("elevator/1/speed")) {
                        receivedValues.add(receivedMessage);
                    } else if (topicName.equals("elevator/0/weight") || topicName.equals("elevator/1/weight")) {
                        receivedValues.add(receivedMessage);
                    } else if (topicName.equals("elevator/0/doorState") || topicName.equals("elevator/1/doorState")) {
                        receivedValues.add(receivedMessage);
                    } else if (topicName.equals("elevator/0/button/0") || topicName.equals("elevator/0/button/1") ||
                             topicName.equals("elevator/0/button/2") || topicName.equals("elevator/0/button/3") ||
                             topicName.equals("elevator/1/button/0") || topicName.equals("elevator/1/button/1") ||
                             topicName.equals("elevator/1/button/2") || topicName.equals("elevator/1/button/3") ||
                             topicName.equals("floor/0/buttonDown") || topicName.equals("floor/0/buttonUp") ||
                             topicName.equals("floor/1/buttonDown") || topicName.equals("floor/1/buttonUp") ||
                             topicName.equals("floor/2/buttonDown") || topicName.equals("floor/2/buttonUp") ||
                             topicName.equals("floor/3/buttonDown") || topicName.equals("floor/3/buttonUp")) {
                        receivedValues.add(receivedMessage);
                    }
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
        Thread.sleep(3000);
        //wait until everything is subscribed so changes in the @BeforeEach stubbing can be received
        MQTTAdapter.run();

     // Warten, um sicherzustellen, dass alle Nachrichten empfangen wurden
        Thread.sleep(2000);

        // Überprüfen, ob alle erwarteten Werte empfangen wurden
        assertTrue(receivedValues.containsAll(expectedValues), "Nicht alle erwarteten Werte wurden empfangen.");

    }
    
    
    @Test
    void testMQTTAdapterWithMockedElevatorAPI() throws MqttException, InterruptedException, RemoteException {
        assertTrue(testClient.getState().isConnected(), "TestClient is not connected");

        MQTTAdapter.connect();
        MQTTAdapter.run();

        // Testnachrichten und Topics
        Map<String, String> testMessages = Map.of(
                "elevator/0/committedDirection", "1",  // Committed direction: 1
                "elevator/0/targetFloor", "3",         // Target floor: 3
                "elevator/0/floorService/2", "true"    // Floor 2 service: true
        );
        
        for (Map.Entry<String, String> entry : testMessages.entrySet()) {
            String topic = entry.getKey();
            String payload = entry.getValue();

            testClient.toAsync().publishWith()
                    .topic(topic)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send()
                    .whenComplete((publishAck, throwable) -> {
                        if (throwable != null) {
                            System.err.println("Publishing failed for topic " + topic + ": " + throwable.getMessage());
                        } else {
                            System.out.println("Nachricht veröffentlicht: " + payload + " für Topic: " + topic);
                        }
                    });
        }

        // Wartezeit, um sicherzustellen, dass alle Nachrichten verarbeitet werden
        Thread.sleep(500); // Zeit zur Verarbeitung der Nachrichten
        
     // Überprüfen, ob die Methoden mit den richtigen Parametern aufgerufen wurden
        verify(elevatorAPI).setCommittedDirection(0, 1); // Elevator 0, Direction 1
        verify(elevatorAPI).setTarget(0, 3);             // Elevator 0, Target Floor 3
        verify(elevatorAPI).setServicesFloors(0, 2, true); // Elevator 0, Floor 2, Service true
    }
}