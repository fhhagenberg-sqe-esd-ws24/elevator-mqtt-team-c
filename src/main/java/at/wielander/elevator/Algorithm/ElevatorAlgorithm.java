package at.wielander.elevator.Algorithm;

import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import sqelevator.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.util.*;

public class ElevatorAlgorithm {


    private Mqtt5AsyncClient mqttClient; // MQTT Client instance variable
    private ElevatorMQTTAdapter eMQTTAdapter; // Adapter instance variable
    private ElevatorSystem eSystem;
    private IElevator controller;
    private static Properties properties;
    private Timer timer;

    private static final int MAX_RETRIES = 5;
    private static final int THREAD_SLEEP = 10000;
    private static final int GROUND_FLOOR = 0;

    private int totalElevator = -1;
    private int totalFloor = -1;
    private Vector<Integer> totalPassengers = new Vector<>();

    private Map<String, String> retainedMessages = new HashMap<>();
    private Map<String, String> liveMessages = new HashMap<>();


    public static void main(String[] args) throws InterruptedException {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        String brokerHost = "tcp://localhost:1883"; // Lokaler Mosquitto Broker
        System.out.println("Connecting to MQTT Broker at: " + brokerHost);

        try {
            // RMI setup
            properties = new Properties();
            String plcUrl = properties.getProperty("plc.url", "rmi://localhost/ElevatorSim");
            IElevator controller = (IElevator) Naming.lookup(plcUrl);

            // Elevator System Configuration
            algorithm.eSystem = new ElevatorSystem(
                    1,
                    0,
                    10,
                    4000,
                    10,
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
                    System.out.println("Retained message received: " + topic + " -> " + payload);
                }
            });

            // Subscribe to live messages for elevators and floors
            for (int elevatorId = 0; elevatorId < 2; elevatorId++) {
                // Abonniere alle Themen, die mit "elevator/" und der entsprechenden ID beginnen
                algorithm.mqttClient.subscribeWith().topicFilter("elevator/" + elevatorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }
            for (int floorId = 0; floorId < 4; floorId++) {
                // Abonniere alle Themen, die mit "floor/" und der entsprechenden ID beginnen
                algorithm.mqttClient.subscribeWith().topicFilter("floor/" + floorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }

            // Verarbeiten der empfangenen Nachrichten
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                // Überprüfen, ob das Topic mit "elevator/" oder "floor/" beginnt
                if (topic.startsWith("elevator/") || topic.startsWith("floor/")) {
                    // Die Nachricht wird in der Map liveMessages gespeichert
                    algorithm.liveMessages.put(topic, payload);
                    System.out.println("Live message received: " + topic + " -> " + payload);
                }
            });

            algorithm.mqttClient.connect().whenComplete((ack, throwable) -> {
                if (throwable == null) {
                    System.out.println("Connected to MQTT broker");
                } else {
                    System.err.println("Failed to connect to MQTT broker: " + throwable.getMessage());
                }
            });

            algorithm.runAlgorithm(algorithm, algorithm.eMQTTAdapter);

        } catch (Exception e) {

            algorithm.eMQTTAdapter.disconnect();

        }
    }

    public void runAlgorithm(ElevatorAlgorithm algorithm, ElevatorMQTTAdapter eMQTTAdapter) throws InterruptedException {
        Thread.sleep(3000);
        algorithm.eMQTTAdapter.run();
        Thread.sleep(500);

        final int numberOfFloors = Integer.parseInt(retainedMessages.get("building/info/numberOfFloors"));
        final int elevator = 0;
        final int sleepTime = 1;

        // Subscribe to external floor button presses (up/down) for each floor, once
        subscribeToFloorButtonPresses(algorithm, numberOfFloors);

        // Subscribe to inside elevator button presses, once
        subscribeToInsideElevatorButtonPresses(algorithm);

        // Handle button presses asynchronously
        handleButtonPresses(algorithm, elevator, sleepTime);
    }

    // Optimized method to subscribe to floor button presses
    private void subscribeToFloorButtonPresses(ElevatorAlgorithm algorithm, int numberOfFloors) {
        for (int floorId = 0; floorId < numberOfFloors; floorId++) {
            try {
                String upButtonTopic = "floor/" + floorId + "/buttonUp";
                String downButtonTopic = "floor/" + floorId + "/buttonDown";
                // Subscribe once for each button type on all floors
                algorithm.mqttClient.subscribeWith().topicFilter(upButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
                algorithm.mqttClient.subscribeWith().topicFilter(downButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
            } catch (Exception e) {
                System.err.println("Failed to subscribe to button press topics for floor " + floorId + ": " + e.getMessage());
            }
        }
    }

    // Optimized method to subscribe to internal elevator button presses
    private void subscribeToInsideElevatorButtonPresses(ElevatorAlgorithm algorithm) {
        for (int elevatorId = 0; elevatorId < totalElevator; elevatorId++) {
            for (int floorId = 0; floorId < totalFloor; floorId++) {
                try {
                    String elevatorButtonTopic = "elevator/" + elevatorId + "/button/" + floorId;
                    algorithm.mqttClient.subscribeWith().topicFilter(elevatorButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
                } catch (Exception e) {
                    System.err.println("Failed to subscribe to elevator button topic for elevator " + elevatorId + " and floor " + floorId + ": " + e.getMessage());
                }
            }
        }
    }

    // Optimized method to handle all button presses
    private void handleButtonPresses(ElevatorAlgorithm algorithm, int elevator, int sleepTime) {
        algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
            try {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                // Handle external floor button press
                if (topic.startsWith("floor/")) {
                    int floorRequested = Integer.parseInt(topic.split("/")[1]);
                    if (topic.contains("/buttonUp")) {
                        // Move elevator up if buttonUp is pressed
                        moveElevator(elevator, floorRequested, algorithm, 1, sleepTime);
                    } else if (topic.contains("/buttonDown")) {
                        // Move elevator down if buttonDown is pressed
                        moveElevator(elevator, floorRequested, algorithm, 2, sleepTime);
                    }
                }

                // Handle inside elevator button press
                if (topic.startsWith("elevator/") && topic.contains("/button/")) {
                    String[] parts = topic.split("/");
                    int requestedFloor = Integer.parseInt(parts[3]);
                    // Set target floor based on inside button press
                    setElevatorTargetFloor(Integer.parseInt(parts[1]), requestedFloor, algorithm);
                }
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
            }
        });
    }

    // Optimized moveElevator method (generic for both up/down)
    private void moveElevator(int elevator, int floorRequested, ElevatorAlgorithm algorithm, int direction, int sleepTime) throws InterruptedException {
        try {
            String directionTopic = "elevator/" + elevator + "/committedDirection";
            String targetTopic = "elevator/" + elevator + "/targetFloor";

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
            waitForElevatorToReachTarget(elevator, floorRequested, algorithm, sleepTime);
        } catch (Exception e) {
            System.err.println("Error while moving elevator: " + e.getMessage());
        }
    }

    // Optimized method to wait for elevator to reach the target floor
    private void waitForElevatorToReachTarget(int elevator, int floorRequested, ElevatorAlgorithm algorithm, int sleepTime) throws InterruptedException {
        while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "-1")) != floorRequested
                || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {
            Thread.sleep(sleepTime);
        }

        // Wait for doors to open
        while (!"1".equals(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/doorState", ""))) {
            Thread.sleep(sleepTime);
        }

        // Set committed direction to UNCOMMITTED after reaching the target
        algorithm.mqttClient.publishWith()
                .topic("elevator/" + elevator + "/committedDirection")
                .payload("0".getBytes(StandardCharsets.UTF_8)) // 0 for UNCOMMITTED
                .send();
    }

    // Method to set the target floor
    private void setElevatorTargetFloor(int elevatorId, int floorRequested, ElevatorAlgorithm algorithm) {
        try {
            String targetTopic = "elevator/" + elevatorId + "/targetFloor";
            algorithm.mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload(Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8))
                    .send();
        } catch (Exception e) {
            System.err.println("Failed to set target floor for elevator " + elevatorId + ": " + e.getMessage());
        }
    }
}