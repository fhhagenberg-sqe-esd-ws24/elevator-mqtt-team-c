package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.MqttClient;

import at.fhhagenberg.sqelevator.ElevatorExample;
import org.junit.jupiter.api.Assertions;

import at.fhhagenberg.sqelevator.IElevator;
import at.wielander.elevator.Model.*;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.rmi.Naming;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.*;

class MQTT_Adapter_Test {


    private static final int GROUND_FLOOR = 0;

    private static final int ZERO_ELEVATORS = 0;

    private static final int TOTAL_ELEVATORS = 5;

    private static final int HIGHEST_FLOOR = 4;

    private static final int CAPACITY_ELEVATOR = 1000;

    private static final int FLOOR_HEIGHT = 7;
    
    private IElevator elevatorAPI;
    private ElevatorSystem elevatorSystem;
    private ElevatorMQTTAdapter MQTTAdapter;

    @BeforeEach
    public void setup() {

        System.out.println("Setup called. Creating Elevator");

        try {
            // Mock für den Elevator-API-Controller
            elevatorAPI = Mockito.mock(IElevator.class);

            // Beispielhafte Mock-Konfiguration
            when(elevatorAPI.getElevatorNum()).thenReturn(TOTAL_ELEVATORS);
            when(elevatorAPI.getFloorHeight()).thenReturn(FLOOR_HEIGHT);

            for (int i = 0; i < TOTAL_ELEVATORS; i++) {
                when(elevatorAPI.getElevatorWeight(i)).thenReturn(CAPACITY_ELEVATOR);
                when(elevatorAPI.getElevatorPosition(i)).thenReturn(GROUND_FLOOR);
            }

            // Initialisiere das ElevatorSystem
            elevatorSystem = new ElevatorSystem(
                TOTAL_ELEVATORS,
                GROUND_FLOOR,
                HIGHEST_FLOOR,
                CAPACITY_ELEVATOR,
                FLOOR_HEIGHT,
                elevatorAPI // Übergabe des gemockten Interfaces
            );

            // Definiere das Broker-URL und Client-ID für HiveMQ
            String brokerUrl = "tcp://localhost:1883";
            String clientId = "testClient";

            // Initialisiere den ElevatorMQTTAdapter mit dem ElevatorSystem
            MQTTAdapter = new ElevatorMQTTAdapter(elevatorSystem, brokerUrl, clientId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


	@Test
	void testMQTTAdapterInitialization() {
	    // Überprüfe, ob der MQTTAdapter korrekt initialisiert wurde
	    Assertions.assertNotNull(MQTTAdapter, 
	        "Test FAILED: MQTTAdapter should be initialized.");
	}

//@Test
//void testElevatorSystemInitialization() {
//    // Überprüfe, ob das ElevatorSystem korrekt initialisiert wurde
//    Assertions.assertNotNull(elevatorSystem, 
//        "Test FAILED: ElevatorSystem should be initialized.");
//
//    // Verifiziere die Anzahl der Aufzüge
//    Assertions.assertEquals(TOTAL_ELEVATORS, elevatorSystem.getElevatorNum(),
//        "Test FAILED: ElevatorSystem should have the correct number of elevators.");
//}

//@Test
//void testPublishMethodCalled() throws Exception {
//    // Mock für MqttClient, um Publish-Aufrufe zu überprüfen
//    MqttClient mockClient = Mockito.mock(MqttClient.class);
//    ElevatorMQTTAdapter adapterWithMockClient = new ElevatorMQTTAdapter(elevatorSystem, "tcp://localhost:1883", "testClient");
//
//    // Verwende Reflection, um das private Feld `client` zu setzen
//    var clientField = ElevatorMQTTAdapter.class.getDeclaredField("client");
//    clientField.setAccessible(true);
//    clientField.set(adapterWithMockClient, mockClient);
//
//    // Rufe die `publish`-Methode auf
//    adapterWithMockClient.connect();
//    adapterWithMockClient.publish("test/topic", "Test Message");
//
//    // Verifiziere, dass der MqttClient die publish-Methode aufgerufen hat
//    verify(mockClient, times(1)).publish(eq("test/topic"), any(MqttMessage.class));
//}
}
