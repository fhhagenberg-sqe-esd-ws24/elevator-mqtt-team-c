package algorithm;

import at.wielander.elevator.Helpers.Constants.ElevatorRequest;
import at.wielander.elevator.Helpers.Constants.ElevatorDoorState;
import at.wielander.elevator.Helpers.Topics.ElevatorTopics;
import sqelevator.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import elevator.*;

import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElevatorAlgorithm {

    private static final Logger logger = Logger.getLogger(ElevatorAlgorithm.class.getName());
    private static final int CLOSING_OPENING_DURATION = 5000;
    private static final int CLOSE_OPEN_DURATION = 10000;
    private static final int SLEEP_DURATION = 10;
    private final Map<String, String> retainedMessages = new HashMap<>();
    private final Map<String, String> liveMessages = new HashMap<>();
    private final Map<Integer, ElevatorRequest> elevatorRequests = new HashMap<>();
    private Mqtt5AsyncClient mqttClient; // MQTT-Client als Instanzvariable
    private ElevatorMQTTAdapter eMQTTAdapter; // Adapter als Instanzvariable
    private ElevatorSystem eSystem;

    public static void main(String[] args) {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        String brokerHost = "tcp://localhost:1883"; // Local Mosquitto Broker
        logger.info("Connecting to MQTT Broker at: " + brokerHost);

        try {
            // RMI setup
            IElevator controller = (IElevator) Naming.lookup("rmi://localhost/ElevatorSim");

            // Elevator System Configuration
            algorithm.eSystem = new ElevatorSystem(
                    1,
                    0,
                    3,
                    4000,
                    10,
                    controller // RMI-Controller
            );

            // Create the MQTT Adapter
            algorithm.eMQTTAdapter = new ElevatorMQTTAdapter(
                    algorithm.eSystem,          // Elevator System
                    brokerHost,       // MQTT Broker Host
                    "mqttAdapter",    // Client ID
                    250,              // Polling Interval (ms)
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

            // Subscribe to the topics
            String topicFilter = "building/info/#"; // Filter topics under "building/info"
            algorithm.mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();

            // Processing received messages
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                if (topic.startsWith("building/info")) {
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    algorithm.retainedMessages.put(topic, payload);
                    logger.info("Retained message received: " + topic + " -> " + payload);
                }
            });

            // Subscribe to elevator and floor topics
            for (int elevatorId = 0; elevatorId < 2; elevatorId++) {
                algorithm.mqttClient.subscribeWith()
                        .topicFilter(ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(elevatorId)))
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .send();
            }

            for (int floorId = 0; floorId < 4; floorId++) {
                algorithm.mqttClient.subscribeWith()
                        .topicFilter("floor/" + floorId + "/#")
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .send();
            }

            // Processing the live messages
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                if (topic.startsWith("elevator/") || topic.startsWith("floor/")) {
                    algorithm.liveMessages.put(topic, payload);
                    logger.info("Live message received: " + topic + " -> " + payload);
                }
            });

            algorithm.mqttClient.connect().whenComplete((ack, throwable) -> {
                if (throwable == null) {
                    logger.info("Connected to MQTT broker");
                } else {
                    logger.log(Level.SEVERE, "Failed to connect to MQTT broker: " + throwable.getMessage(), throwable);
                }
            });

            algorithm.runElevatorSimulator(algorithm);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred: " + e.getMessage(), e);
        } finally {
            try {
                if (algorithm.eMQTTAdapter != null) {
                    algorithm.eMQTTAdapter.disconnect();
                }
                if (algorithm.mqttClient != null) {
                    algorithm.mqttClient.disconnect();
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception during cleanup: " + e.getMessage(), e);
            }
        }
    }

    public void runElevatorSimulator(ElevatorAlgorithm algorithm) {
        while (true) {  // Infinite loop to keep processing messages
            try {
                runAlgorithm(algorithm);

                Thread.sleep(500);  // Sleep for a short period to avoid overloading the system
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in algorithm loop: " + e.getMessage(), e);
                break; // Exit the loop if any unexpected error occurs
            }
        }
    }

    public void runAlgorithm(ElevatorAlgorithm algorithm) {
        try {
            Thread.sleep(3000);
            algorithm.eMQTTAdapter.run();
            Thread.sleep(500);

            final int numberOfFloors = Integer.parseInt(retainedMessages.get("building/info/numberOfFloors"));
            final int numberOfElevators = Integer.parseInt(retainedMessages.get("building/info/numberOfElevators"));
            final int elevatorServicedFloors = Integer.parseInt(retainedMessages.get("building/info/elevatorServicedFloors"));

            handleFloorRequests(algorithm, algorithm.eSystem.getElevatorNum(), algorithm.eSystem.getElevatorNum(), SLEEP_DURATION);
            handleFloorButtonRequests();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception in runAlgorithm: " + e.getMessage(), e);
        }
    }

    private void handleFloorButtonRequests() {
        for (int indexFloor = 0; indexFloor < 4; indexFloor++) {
            // Subscribe to the up and down button press topics for each floor
            String upButtonTopic = "floor/" + indexFloor + "/buttonUp";
            String downButtonTopic = "floor/" + indexFloor + "/buttonDown";

            int finalIndexFloor = indexFloor;
            int finalIndexFloor1 = indexFloor;
            mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                if (topic.equals(upButtonTopic) || topic.equals(downButtonTopic)) {
                    logger.info("Button pressed on floor " + finalIndexFloor + ": " + topic + " -> " + payload);
                    if (payload.equals("pressed")) {
                        // If the up or down button is pressed, request an elevator to come to this floor
                        int direction = topic.equals(upButtonTopic) ? 1 : -1;
                        ElevatorRequest put = elevatorRequests.put(finalIndexFloor, ElevatorRequest.getValue(direction));

                        // Trigger the elevator to go to the requested floor (direction-based)
                        triggerElevatorRequest(finalIndexFloor, direction);
                    }
                }
            });
        }
    }

    private void triggerElevatorRequest(int floorId, int direction) {
        // Determine which elevator should respond based on the direction
        for (int elevatorId = 0; elevatorId < 2; elevatorId++) {
            // Set the target floor for the elevator based on the direction (up or down)
            String targetTopic = ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(elevatorId));
            mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload(Integer.toString(floorId).getBytes(StandardCharsets.UTF_8))
                    .send();
        }
    }

    private void handleFloorRequests(ElevatorAlgorithm algorithm, int elevator, int numberOfFloors, int sleepTime) throws InterruptedException {
        List<Integer> sortedFloors = new ArrayList<>(elevatorRequests.keySet());
        Collections.sort(sortedFloors);

        for (Integer targetFloor : sortedFloors) {
            // Set target floor for the elevator
            String targetTopic = ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(elevator));
            algorithm.mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload(Integer.toString(targetFloor).getBytes(StandardCharsets.UTF_8))
                    .send();

            // Wait for elevator to reach target floor
            while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "-1")) < targetFloor
                    || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {

                Thread.sleep(sleepTime);
            }

            // Handle door transitions after arrival
            handleDoorStateTransition(algorithm, elevator);

            elevatorRequests.remove(targetFloor);
        }

        // Return to ground floor if no requests
        if (elevatorRequests.isEmpty()) {
            String targetTopic = ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(elevator));
            algorithm.mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload("0".getBytes(StandardCharsets.UTF_8))
                    .send();

            while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "1")) > 0
                    || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {

                Thread.sleep(sleepTime);
            }

            handleDoorStateTransition(algorithm, elevator);
        }
    }

    /**
     * Handles door transitions from  CLOSED -> OPENING -> OPEN -> CLOSING -> CLOSED
     * @param algorithm elevator Algorithm
     * @param elevator elevator data model
     * @throws InterruptedException Exception
     */
    private void handleDoorStateTransition(ElevatorAlgorithm algorithm, int elevator) throws InterruptedException {
        /* CLOSED -> OPENING */
        algorithm.mqttClient.publishWith()
                .topic(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)))
                .payload(Integer.toString(ElevatorDoorState.getValue(3).ordinal()).getBytes(StandardCharsets.UTF_8))
                .send();
        Thread.sleep(CLOSING_OPENING_DURATION);

        /* OPENING -> OPEN */
        algorithm.mqttClient.publishWith()
                .topic(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)))
                .payload(Integer.toString(ElevatorDoorState.getValue(1).ordinal()).getBytes(StandardCharsets.UTF_8))
                .send();
        Thread.sleep(CLOSE_OPEN_DURATION);

        /* OPEN -> CLOSING */
        algorithm.mqttClient.publishWith()
                .topic(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)))
                .payload(Integer.toString(ElevatorDoorState.getValue(4).ordinal()).getBytes(StandardCharsets.UTF_8))
                .send();
        Thread.sleep(CLOSING_OPENING_DURATION);

        /* CLOSING -> CLOSE */
        algorithm.mqttClient.publishWith()
                .topic(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)))
                .payload(Integer.toString(ElevatorDoorState.getValue(2).ordinal()).getBytes(StandardCharsets.UTF_8))
                .send();
        Thread.sleep(CLOSE_OPEN_DURATION);
    }
}
