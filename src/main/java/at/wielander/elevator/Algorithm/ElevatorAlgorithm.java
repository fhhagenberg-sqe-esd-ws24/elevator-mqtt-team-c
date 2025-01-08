package at.wielander.elevator.Algorithm;

import at.wielander.elevator.Exception.MQTTClientException;
import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sqelevator.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

public class ElevatorAlgorithm {

    public static final String ELEVATOR_TOPIC = "elevator/";
    public static final String FLOOR_TOPIC = "floor/";
    public static final String BUTTON_UP_TOPIC = "/buttonUp";
    public static final String BUTTON_DOWN_TOPIC = "/buttonDown";
    private static final Logger log = LoggerFactory.getLogger(ElevatorAlgorithm.class);
    public static final int TOTAL_ELEVATORS = 1;
    private static final int TOTAL_FLOORS = 10;
    public static final int LOWEST_FLOOR = 0;
    public static final int HIGHEST_FLOOR = 10;
    public static final int CAPACITY = 4000;
    public static final int FLOOR_HEIGHT = 10;
    public static final String COMMITTED_DIRECTION_TOPIC = "/committedDirection";
    public static final String DOOR_STATE_TOPIC = "/doorState";
    public static final String SPEED_TOPIC = "/speed";
    public static final String CURRENT_FLOOR_TOPIC = "/currentFloor";
    public static final String TARGET_FLOOR_TOPIC = "/targetFloor";
    public static final String BUTTON_TOPIC = "/button/";

    private Mqtt5AsyncClient mqttClient;
    private ElevatorMQTTAdapter eMQTTAdapter;
    private ElevatorSystem eSystem;

    private Map<String, String> retainedMessages = new HashMap<>();
    private Map<String, String> liveMessages = new HashMap<>();


    public static void main(String[] args) throws InterruptedException {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        String brokerHost = "tcp://localhost:1883"; // Lokaler Mosquitto Broker
        log.info("Connecting to MQTT Broker at: {}", brokerHost);

        try {
            // RMI setup
            Properties properties = new Properties();
            String plcUrl = properties.getProperty("plc.url", "rmi://localhost/ElevatorSim");
            IElevator controller = (IElevator) Naming.lookup(plcUrl);

            // Elevator System Configuration
            algorithm.eSystem = new ElevatorSystem(
                    TOTAL_ELEVATORS,
                    LOWEST_FLOOR,
                    HIGHEST_FLOOR,
                    CAPACITY,
                    FLOOR_HEIGHT,
                    controller // RMI-Controller
            );

            // Create the MQTT Adapter
            algorithm.eMQTTAdapter = new ElevatorMQTTAdapter(
                    algorithm.eSystem,// Elevator System
                    brokerHost,       // MQTT Broker Host
                    "mqttAdapter",    // Client ID
                    50,              // Polling Interval (ms)
                    controller        // RMI-Controller
            );

            // Connect MQTT Adapter to the Broker
            algorithm.eMQTTAdapter.connect();

            // Connect to MQTT Broker
            algorithm.mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost("localhost")
                    .serverPort(1883)
                    .identifier("ElevatorAlgorithmClient")
                    .buildAsync();



            // Liste der Topics, die wir abonnieren wollen (nur die building/info Topics)
            String topicFilter = "building/info/#"; // Filtert nur Topics unter building/info

            // Abonnieren des Topic Filters
            algorithm.mqttClient.subscribeWith()
                    .topicFilter(topicFilter) // Wildcard für alle Subtopics unter building/info
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();

            // Verarbeiten der empfangenen Nachrichten
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                // Überprüfen, ob das Topic unter "building/info" fällt
                if (topic.startsWith("building/info")) {
                    // Payload wird als String gespeichert
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    algorithm.retainedMessages.put(topic, payload); // Speichern der Payload
                    log.info("Retained message received: {} -> {}", topic, payload);
                }
            });

            // Subscribe to live messages for elevators and floors
            for (int elevatorId = LOWEST_FLOOR; elevatorId < TOTAL_ELEVATORS; elevatorId++) {
                // Abonniere alle Themen, die mit "elevator/" und der entsprechenden ID beginnen
                algorithm.mqttClient.subscribeWith().topicFilter(ELEVATOR_TOPIC + elevatorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }
            for (int floorId = LOWEST_FLOOR; floorId < TOTAL_FLOORS; floorId++) {
                // Abonniere alle Themen, die mit "floor/" und der entsprechenden ID beginnen
                algorithm.mqttClient.subscribeWith().topicFilter(FLOOR_TOPIC + floorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }

            // Verarbeiten der empfangenen Nachrichten
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                // Überprüfen, ob das Topic mit "elevator/" oder "floor/" beginnt
                if (topic.startsWith(ELEVATOR_TOPIC) || topic.startsWith(FLOOR_TOPIC)) {
                    // Die Nachricht wird in der Map liveMessages gespeichert
                    algorithm.liveMessages.put(topic, payload);
                    log.info("Live message received: {} -> {}", topic, payload);
                }
            });

            algorithm.mqttClient.connect().whenComplete((ack, throwable) -> {
                if (throwable == null) {
                    System.out.println("Connected to MQTT broker");
                } else {
                    log.error("Failed to connect to MQTT broker: {}", throwable.getMessage());
                }
            });

            algorithm.runAlgorithm(algorithm);

        } catch (MQTTClientException e) {
            algorithm.eMQTTAdapter.disconnect();
        } catch (RemoteException e) {
            log.error(" Remote Exception thrown {}",e.getMessage());
        } catch (MalformedURLException e) {
            log.error(" Malformed URL exception {}",e.getMessage());
        } catch (NotBoundException e) {
            log.error(" NotBoundException thrown {}",e.getMessage());
        }
    }

    public void runAlgorithm(ElevatorAlgorithm algorithm) throws InterruptedException {
        Thread.sleep(3000);
        algorithm.eMQTTAdapter.run();
        Thread.sleep(500);

        final int numberOfFloors = Integer.parseInt(retainedMessages.get("building/info/numberOfFloors"));
        final int sleepTime = 1;

        // Subscribe to external floor button presses (up/down) for each floor, once
        subscribeToFloorButtonPresses(algorithm, numberOfFloors);

        // Subscribe to inside elevator button presses, once
        subscribeToInsideElevatorButtonPresses(algorithm);

        // Handle button presses asynchronously
        handleButtonPresses(algorithm, sleepTime);
    }

    // Optimized method to subscribe to floor button presses
    private void subscribeToFloorButtonPresses(ElevatorAlgorithm algorithm, int numberOfFloors) {
        for (int floorId = LOWEST_FLOOR; floorId < numberOfFloors; floorId++) {
            try {
                String upButtonTopic = FLOOR_TOPIC + floorId + BUTTON_UP_TOPIC;
                String downButtonTopic = FLOOR_TOPIC + floorId + BUTTON_DOWN_TOPIC;
                // Subscribe once for each button type on all floors
                algorithm.mqttClient.subscribeWith().topicFilter(upButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
                algorithm.mqttClient.subscribeWith().topicFilter(downButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
            } catch (MQTTClientException e) {
                log.error("Failed to subscribe to button press topics for floor {}: {}", floorId, e.getMessage());
            }
        }
    }

    // Optimized method to subscribe to internal elevator button presses
    private void subscribeToInsideElevatorButtonPresses(ElevatorAlgorithm algorithm) {
        for (int elevatorId = LOWEST_FLOOR; elevatorId < TOTAL_ELEVATORS; elevatorId++) {
            for (int floorId = LOWEST_FLOOR; floorId < TOTAL_FLOORS; floorId++) {
                try {
                    String elevatorButtonTopic = ELEVATOR_TOPIC + elevatorId + BUTTON_TOPIC + floorId;
                    algorithm.mqttClient.subscribeWith().topicFilter(elevatorButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
                } catch (Exception e) {
                    log.error("Failed to subscribe to elevator button topic for elevator {} and floor {}: {}", elevatorId, floorId, e.getMessage());
                }
            }
        }
    }

    // Optimized method to handle all button presses
    private void handleButtonPresses(ElevatorAlgorithm algorithm, int sleepTime) {
        algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
            try {
                String topic = publish.getTopic().toString();

                // Handle external floor button press
                if (topic.startsWith(FLOOR_TOPIC)) {
                    int floorRequested = Integer.parseInt(topic.split("/")[1]);
                    if (topic.contains(BUTTON_UP_TOPIC)) {
                        // Move elevator up if buttonUp is pressed
                        moveElevator(floorRequested, algorithm, 1, sleepTime);
                    } else if (topic.contains(BUTTON_DOWN_TOPIC)) {
                        // Move elevator down if buttonDown is pressed
                        moveElevator(floorRequested, algorithm, TOTAL_ELEVATORS, sleepTime);
                    }
                }

                // Handle inside elevator button press
                if (topic.startsWith(ELEVATOR_TOPIC) && topic.contains(BUTTON_TOPIC)) {
                    String[] parts = topic.split("/");
                    int requestedFloor = Integer.parseInt(parts[3]);
                    // Set target floor based on inside button press
                    setElevatorTargetFloor(Integer.parseInt(parts[1]), requestedFloor, algorithm);
                }
            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage());
            }
        });
    }

    // Optimized moveElevator method (generic for both up/down)
    private void moveElevator(int floorRequested, ElevatorAlgorithm algorithm, int direction, int sleepTime) {
        try {
            String directionTopic = ELEVATOR_TOPIC + ElevatorAlgorithm.TOTAL_ELEVATORS + COMMITTED_DIRECTION_TOPIC;
            String targetTopic = ELEVATOR_TOPIC + ElevatorAlgorithm.TOTAL_ELEVATORS + TARGET_FLOOR_TOPIC;

            // Set direction (1 = UP, 2 = DOWN)
            algorithm.mqttClient.publishWith()
                    .topic(directionTopic)
                    .payload(Integer.toString(direction).getBytes(StandardCharsets.UTF_8))
                    .send();

            // Set target floor
            algorithm.mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload(Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8))
                    .send();

            // Wait for elevator to reach the target floor
            waitForElevatorToReachTarget(ElevatorAlgorithm.TOTAL_ELEVATORS, floorRequested, algorithm, sleepTime);
        } catch (Exception e) {
            log.error("Error while moving elevator: {}", e.getMessage());
        }
    }

    // Optimized method to wait for elevator to reach the target floor
    private void waitForElevatorToReachTarget(int elevator, int floorRequested, ElevatorAlgorithm algorithm, int sleepTime) throws InterruptedException {
        while (Integer.parseInt(algorithm.liveMessages.getOrDefault(ELEVATOR_TOPIC + elevator + CURRENT_FLOOR_TOPIC, "-1")) != floorRequested
                || Integer.parseInt(algorithm.liveMessages.getOrDefault(ELEVATOR_TOPIC + elevator + SPEED_TOPIC, "1")) > LOWEST_FLOOR) {
            Thread.sleep(sleepTime);
        }

        // Wait for doors to open
        while (!"1".equals(algorithm.liveMessages.getOrDefault(ELEVATOR_TOPIC + elevator + DOOR_STATE_TOPIC, ""))) {
            Thread.sleep(sleepTime);
        }

        // Set committed direction to UNCOMMITTED after reaching the target
        algorithm.mqttClient.publishWith()
                .topic(ELEVATOR_TOPIC + elevator + COMMITTED_DIRECTION_TOPIC)
                .payload("0".getBytes(StandardCharsets.UTF_8)) // 0 for UNCOMMITTED
                .send();
    }

    // Method to set the target floor
    private void setElevatorTargetFloor(int elevatorId, int floorRequested, ElevatorAlgorithm algorithm) {
        try {
            String targetTopic = ELEVATOR_TOPIC + elevatorId + TARGET_FLOOR_TOPIC;
            algorithm.mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload(Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8))
                    .send();
        } catch (Exception e) {
            log.error("Failed to set target floor for elevator {}: {}", elevatorId, e.getMessage());
        }
    }
}