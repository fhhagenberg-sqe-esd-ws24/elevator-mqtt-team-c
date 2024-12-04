import at.wielander.elevator.Model.ElevatorMQTTAdapter;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;


import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.Container;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.testcontainers.*;

import org.testcontainers.junit.jupiter.Testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
@Testcontainers 
public class MQTTAdapterTest{

	@Container
    HiveMQContainer hiveMQContainer = new HiveMQContainer("hivemq/hivemq-ce:latest");

	
    @Mock
    private IElevator elevatorAPI;

    private ElevatorSystem elevatorSystem;

    private ElevatorMQTTAdapter MQTTAdapter;

    private MqttClient mockClient;

    @BeforeEach
    public void setup() throws IllegalAccessException, NoSuchFieldException {

        // Initialisiere das ElevatorSystem
        // Übergabe des gemockten Interfaces
        elevatorSystem = new ElevatorSystem(
                2,
                0,
                4,
                1000,
                7,
                elevatorAPI // Übergabe des gemockten Interfaces
        );

        // Definiere das Broker-URL und Client-ID für HiveMQ
        String brokerUrl = "tcp://localhost:1883";
        String clientId = "testClient";

        // Initialisiere den ElevatorMQTTAdapter mit dem ElevatorSystem
        MQTTAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                "tcp://localhost:1883",
                "testClient");

        mockClient = Mockito.mock(MqttClient.class);

    }
    
    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)  // Timeout für den Test
    void testMqttConnectionAndPublishing() throws InterruptedException {
      
    }
    /*

    @Test
    void testMQTTAdapterInitialisation() {
        // Überprüfe, ob der MQTTAdapter korrekt initialisiert wurde
        assertNotNull(MQTTAdapter);
    }

    @Test
    void testConnect() throws Exception {
        var clientField = ElevatorMQTTAdapter.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(MQTTAdapter, mockClient);

        MQTTAdapter.connect();
        verify(mockClient, times(1)).connect();
    }

    @Test
    void testElevatorLevelChange() throws Exception {
        // Mock the behavior of the elevatorAPI to change the level of an elevator
        when(elevatorAPI.getElevatorFloor(eq(0))).thenReturn(1).thenReturn(2);
        Thread.sleep(100);
        // Verify initial level
        assertEquals(1, elevatorSystem.getElevator(0).getCurrentFloor());

        Thread.sleep(150);

        // Verify the level change
        assertEquals(2, elevatorSystem.getElevator(0).getCurrentFloor());

        // Verify that the publish method was called with the updated state
        verify(mockClient, times(2)).publish(anyString(), any(MqttMessage.class));
    }

    @Test
    void testPublishMethodCalled() throws Exception {

        // Verwende Reflection, um das private Feld `client` zu setzen
        var clientField = ElevatorMQTTAdapter.class.getDeclaredField("client");
        var publishMethod = ElevatorMQTTAdapter.class.getDeclaredMethod("publish", String.class, String.class);

        clientField.setAccessible(true);
        clientField.set(MQTTAdapter, mockClient);
        MQTTAdapter.connect();

        publishMethod.setAccessible(true);
        publishMethod.invoke(MQTTAdapter, "test/topic", "Test Message");

        // Verifiziere, dass der MqttClient die publish-Methode aufgerufen hat
        verify(mockClient, times(1)).publish(eq("test/topic"), any(MqttMessage.class));
    }*/
}
