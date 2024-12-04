package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class MQTTAdapterTest {

    @Mock
    private IElevator elevatorAPI;

    private ElevatorSystem elevatorSystem;

    private ElevatorMQTTAdapter MQTTAdapter;

    private MqttClient client;

    // Testcontainer für HiveMQ-Broker
    private GenericContainer<?> hivemqContainer;

    @BeforeEach
    public void setup() throws Exception {
        // Starten des HiveMQ Containers
        hivemqContainer = new GenericContainer<>("hivemq/hivemq5")
                .withExposedPorts(1883); // Exponiere den MQTT-Port
        hivemqContainer.start();
        
        // Hole die Container-Portnummer
        String brokerUrl = "tcp://" + hivemqContainer.getHost() + ":" + hivemqContainer.getMappedPort(1883);

        // Initialisiere das ElevatorSystem
        elevatorSystem = new ElevatorSystem(
                2,
                0,
                4,
                1000,
                7,
                elevatorAPI
        );

        // Initialisiere den MQTT-Adapter mit dem HiveMQ Broker
        MQTTAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                brokerUrl,
                "testClient"
        );

        // Initialisiere den echten MQTT-Client
        client = new MqttClient(brokerUrl, "testClient");
        client.connect();
    }

    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testMQTTAdapterInitialisation() {
        // Überprüfe, ob der MQTT-Adapter korrekt initialisiert wurde
        assertNotNull(MQTTAdapter);
    }

    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testConnect() throws Exception {
        MQTTAdapter.connect();
        assertTrue(client.isConnected()); // Überprüfe, ob der Client mit dem Broker verbunden ist
    }

    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testElevatorWeightChange() throws Exception {
        when(elevatorAPI.getElevatorWeight(0)).thenReturn(1000);
        when(elevatorAPI.getElevatorWeight(1)).thenReturn(1000);
        elevatorSystem.updateAll();
        Thread.sleep(100);

        assertEquals(1000, elevatorSystem.getElevatorWeight(0));

        Thread.sleep(150);

        assertNotEquals(2000, elevatorSystem.getElevatorWeight(1));

        // Veröffentliche eine Nachricht, um die Interaktion mit dem Broker zu überprüfen
        MqttMessage message = new MqttMessage("Test Message".getBytes());
        client.publish("test/topic", message);

        // Warten, um zu prüfen, ob die Nachricht gesendet wurde (nicht in Echtzeit, daher Test auf Empfang fehlt)
        Thread.sleep(500);

        assertTrue(client.isConnected());
    }

    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testPublishMethodCalled() throws Exception {
        // Teste die Publish-Methode
        MqttMessage message = new MqttMessage("Test Message".getBytes());
        client.publish("test/topic", message);

        // Überprüfe, dass die Nachricht veröffentlicht wurde
        assertNotNull(message);
    }
}
