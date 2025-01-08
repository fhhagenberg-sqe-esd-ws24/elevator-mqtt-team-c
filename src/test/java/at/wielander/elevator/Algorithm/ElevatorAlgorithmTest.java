package at.wielander.elevator.Algorithm;

import at.wielander.elevator.adapter.ElevatorMQTTAdapter;
import at.wielander.elevator.model.ElevatorSystem;
import at.wielander.elevator.algorithm.ElevatorAlgorithm;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import sqelevator.IElevator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class ElevatorAlgorithmTest {

    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));

    @Mock
    private IElevator mockElevatorAPI;

    private ElevatorAlgorithm algorithm;

    private ElevatorSystem elevatorSystem;

    private Mqtt5BlockingClient testClient;

    private ElevatorMQTTAdapter mqttAdapter;

    private String host;

    @BeforeEach
    public void setup() throws Exception {

        hivemqCe.start();

        host = "tcp://" + hivemqCe.getHost() + ":" + hivemqCe.getMappedPort(1883);

        System.out.println("Host addresse: " + host);
        testClient = Mqtt5Client.builder()
                .identifier("testClient")
                .serverPort(hivemqCe.getMappedPort(1883)) // Verwenden Sie den dynamisch gemappten Port
                .serverHost(hivemqCe.getHost()) // Verbindet sich mit 'localhost'
                .buildBlocking();

        testClient.connect();
        mockElevatorAPI = mock(IElevator.class);

        // Create an elevatorSystem
        elevatorSystem = new ElevatorSystem(
                1,
                0,
                10,
                4000,
                10,
                mockElevatorAPI // Pass the mocked interface
        );

        // Create the adapter adapter
        mqttAdapter = new ElevatorMQTTAdapter(
                elevatorSystem,
                host,
                "mqttAdapter", 250, mockElevatorAPI);

        algorithm = new ElevatorAlgorithm();
    }

    @AfterEach
    public void tearDown() {
        testClient.disconnect();
        hivemqCe.stop();
    }

    @Test
    void givenTestContainer_whenCheckingStatus_ThenExpectContainerShouldBeRunning() {
        assertTrue(hivemqCe.isRunning());
        assertNotNull(hivemqCe.getHost());
        assertTrue(hivemqCe.getMappedPort(1883) > 0);
    }

}