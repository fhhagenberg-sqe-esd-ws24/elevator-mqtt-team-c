package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import at.fhhagenberg.sqelevator.*;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.*;

class ElevatorMQTTAdapterTest {
	private static final int GROUND_FLOOR = 0;

    private static final int ZERO_ELEVATORS = 0;

    private static final int TOTAL_ELEVATORS = 5;

    private static final int HIGHEST_FLOOR = 4;

    private static final int CAPACITY_ELEVATOR = 1000;
    
    private MqttClient mockMqttClient;

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

            // Initialisiere den Mock-MQTT-Client
            mockMqttClient = Mockito.mock(MqttClient.class);

            // Initialisiere den ElevatorMQTTAdapter mit dem ElevatorSystem
            MQTTAdapter = new ElevatorMQTTAdapter(elevatorSystem, brokerUrl, clientId);

            // Setze den Mock-MQTT-Client in den Adapter (Reflection, da `client` private ist)
            var clientField = ElevatorMQTTAdapter.class.getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(MQTTAdapter, mockMqttClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testConnect() throws Exception {
        // Rufe die connect-Methode auf
    	MQTTAdapter.connect();

        // Überprüfe, ob die Verbindung hergestellt wurde
        verify(mockMqttClient).connect();
        System.out.println("Connect method tested successfully.");
    }

//    @Test
//    void testPublishElevatorState() throws Exception {
//        // Rufe die publishElevatorState-Methode für einen Aufzug auf
//    	MQTTAdapter.publish("elevator/0/currentFloor", "1");
//
//        // Überprüfe, ob die Nachricht korrekt veröffentlicht wurde
//        verify(mockMqttClient).publish(eq("elevator/0/currentFloor"), any(MqttMessage.class));
//        System.out.println("Publish elevator state tested successfully.");
//    }
//
//    @Test
//    void testStartPublishingElevatorStates() {
//        // Simuliere das Starten der Scheduler-Aufgabe
//    	MQTTAdapter.connect();
//
//        verify(mockScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(100L), eq(TimeUnit.MILLISECONDS));
//        System.out.println("Scheduler setup tested successfully.");
//    }
}
