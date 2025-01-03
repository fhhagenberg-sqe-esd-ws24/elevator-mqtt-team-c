package algorithm;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import elevator.ElevatorMQTTAdapter;
import elevator.ElevatorSystem;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.TestcontainersExtension;
import org.testcontainers.utility.DockerImageName;
import sqelevator.IElevator;

import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(TestcontainersExtension.class)
@Testcontainers
class ElevatorAlgorithmTest {

    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));;

    @Mock
    private IElevator elevatorAPI;


    private ElevatorSystem elevatorSystem;

    private ElevatorMQTTAdapter MQTTAdapter;

    private Mqtt5BlockingClient testClient;

    private String Host;

    @BeforeEach
    public void setup() throws MqttException, RemoteException, InterruptedException {

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
        MQTTAdapter.connect();
        MQTTAdapter.run();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        MQTTAdapter.disconnect();
        testClient.disconnect();
        hivemqCe.stop();
    }

    @Test
    void testConnectionToMQTTBroker() throws InterruptedException {
        assertTrue(hivemqCe.isRunning());
        assertTrue(testClient.getState().isConnected());
    }

}