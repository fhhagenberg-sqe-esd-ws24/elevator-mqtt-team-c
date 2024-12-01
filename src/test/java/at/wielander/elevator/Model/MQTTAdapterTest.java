package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


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

//    @Test
//    void testElevatorSystemInitialization() throws RemoteException {
//        // Überprüfe, ob das ElevatorSystem korrekt initialisiert wurde
//        doNothing().when(MQTTAdapter).connect();
//        assertNotNull(elevatorSystem);
//
//        // Verifiziere die Anzahl der Aufzüge
//        when(elevatorSystem.getElevatorNum()).thenReturn(5);
//        assertEquals(5, elevatorSystem.getElevatorNum());
//        verify(elevatorSystem, times(1)).getElevator(0);
//    }

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
    }
}
