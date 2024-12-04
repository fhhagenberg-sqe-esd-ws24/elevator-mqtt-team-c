package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class MQTTAdapterTest {

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
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testMQTTAdapterInitialisation() {
        // Überprüfe, ob der MQTTAdapter korrekt initialisiert wurde
        assertNotNull(MQTTAdapter);
    }


    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testConnect() throws Exception {
        var clientField = ElevatorMQTTAdapter.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(MQTTAdapter, mockClient);

        MQTTAdapter.connect();
        verify(mockClient, times(1)).connect();
    }

    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
    void testElevatorLevelChange() throws Exception {

        when(elevatorAPI.getElevatorFloor(eq(0))).thenReturn(1).thenReturn(2);
        Thread.sleep(100);

        assertEquals(1, elevatorSystem.getElevator(0).getCurrentFloor());

        Thread.sleep(150);

        assertEquals(2, elevatorSystem.getElevator(0).getCurrentFloor());

        // Verify that the publish method was called with the updated state
        verify(mockClient, times(2)).publish(anyString(), any(MqttMessage.class));
    }

    // @Test
    // void testElevatorSystemInitialization() throws RemoteException {
    // // Überprüfe, ob das ElevatorSystem korrekt initialisiert wurde
    // doNothing().when(MQTTAdapter).connect();
    // assertNotNull(elevatorSystem);
    //
    // // Verifiziere die Anzahl der Aufzüge
    // when(elevatorSystem.getElevatorNum()).thenReturn(5);
    // assertEquals(5, elevatorSystem.getElevatorNum());
    // verify(elevatorSystem, times(1)).getElevator(0);
    // }

    @Test
    @Timeout(unit = TimeUnit.MINUTES, value = 2)
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
    }
}
