package at.wielander.elevator.Model;

import at.wielander.elevator.Model.IElevator;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

//import net.bytebuddy.utility.dispatcher.JavaDispatcher.Container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.hivemq.HiveMQContainer;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MQTTAdapterTest {

    @Mock
    private IElevator elevatorAPI;

    private ElevatorSystem elevatorSystem;

    private ElevatorMQTTAdapter MQTTAdapter;

    private Mqtt5BlockingClient testClient;// todo new
    @Container
    private GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("hivemq/hivemq-ce:latest"))
            .withExposedPorts(1883);

    String Host; // todo new

    // @Container
    // private container container = new
    // container(DockerImageName.parse("hivemq/hivemq-ce:latest"));

    @BeforeEach
    public void setup() throws MqttException, RemoteException {

        // setup the container
        // Start the HiveMQ container
        container.start();

        // Prepare broker URL
        Host = "tcp://" + container.getHost() + ":" + container.getMappedPort(1883);

        testClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(1883)
                .serverHost(container.getHost())
                .buildBlocking();

        testClient.connect();

        Host = container.getHost();

        MockitoAnnotations.initMocks(this);

        when(elevatorAPI.getElevatorNum()).thenReturn(2);
        when(elevatorAPI.getElevatorFloor(1)).thenReturn(1);
        when(elevatorAPI.getElevatorAccel(1)).thenReturn(15);
        when(elevatorAPI.getElevatorDoorStatus(1)).thenReturn(2);
        when(elevatorAPI.getElevatorPosition(1)).thenReturn(1);
        when(elevatorAPI.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorAPI.getElevatorWeight(1)).thenReturn(10);
        when(elevatorAPI.getElevatorCapacity(1)).thenReturn(5);
        when(elevatorAPI.getElevatorButton(1, 1)).thenReturn(true);

        when(elevatorAPI.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorAPI.getFloorButtonUp(1)).thenReturn(false);
        when(elevatorAPI.getFloorNum()).thenReturn(5);
        when(elevatorAPI.getFloorHeight()).thenReturn(3);
        when(elevatorAPI.getServicesFloors(1, 1)).thenReturn(true);

        when(elevatorAPI.getTarget(1)).thenReturn(5);
        when(elevatorAPI.getClockTick()).thenReturn(1000L);
        when(elevatorAPI.getCommittedDirection(1)).thenReturn(1);

        // create an elevatorSystem
        elevatorSystem = new ElevatorSystem(
                2,
                0,
                4,
                1000,
                7,
                elevatorAPI // Übergabe des gemockten Interfaces
        );
        // create the mqttadapter
        MQTTAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                Host,
                "mqttAdapter");

    }

    @Test
    void testConnect() throws MqttException {
        assertNotNull(testClient);
        assertTrue(testClient.getState().isConnected());
    }

    @Test
    void testDisconnect() throws MqttException {
        testClient.disconnect();
        assertFalse(testClient.getState().isConnected());
    }
}
