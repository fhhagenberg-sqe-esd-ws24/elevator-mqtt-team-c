package at.wielander.elevator.Algorithm;

import at.wielander.elevator.Helpers.Constants.ElevatorDoorState;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;

public class ElevatorAlgorithm {


    private static final Logger log = LoggerFactory.getLogger(ElevatorAlgorithm.class);
    private static IElevator controller;
    protected Mqtt5AsyncClient mqttClient; // MQTT Client instance variable
    private ElevatorMQTTAdapter eMQTTAdapter; // Adapter instance variable
    private ElevatorSystem eSystem;

    private static final int LOWEST_FLOOR = 0;
    private static final int HIGHEST_FLOOR = 11;
    private static final int TOTAL_ELEVATORS = 1;
    private static final int FLOOR_HEIGHT = 10;
    private static final int TOTAL_CAPACITY = 4000;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 5000;
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String RMI_URL = "rmi://localhost/ElevatorSim";

    private final Map<String, String> retainedMessages = new ConcurrentHashMap<>();
    private final Map<String, String> liveMessages = new ConcurrentHashMap<>();


    public static void main() throws InterruptedException {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        System.out.println("Connecting to MQTT Broker at: " + BROKER_URL);

        try {
            // RMI setup
            IElevator controller = connectToRMI();

            // Elevator System Configuration
            algorithm.initialiseElevatorSystem(controller);
            algorithm.connectMQTTClient();
            algorithm.subscribeToBuildingInfo();
            algorithm.subscribeToElevatorAndFloorMessages();

            algorithm.runAlgorithm(algorithm);

        } catch (Exception e) {

            if (algorithm.eMQTTAdapter != null) {
                algorithm.eMQTTAdapter.disconnect();
            }
            System.err.println("Failed to initialise System " + e.getMessage());
        }
    }

    protected void initialiseElevatorSystem(IElevator controller) throws RemoteException {
        try {
            eSystem = new ElevatorSystem(
                    TOTAL_ELEVATORS,
                    LOWEST_FLOOR,
                    HIGHEST_FLOOR,
                    TOTAL_CAPACITY,
                    FLOOR_HEIGHT,
                    controller // RMI-Controller
            );

            // Create the MQTT Adapter
            eMQTTAdapter = new ElevatorMQTTAdapter(
                    eSystem,// Elevator System
                    ElevatorAlgorithm.BROKER_URL,       // MQTT Broker Host
                    "mqttAdapter",    // Client ID
                    50,              // Polling Interval (ms)
                    controller        // RMI-Controller
            );

            // Connect MQTT Adapter to the Broker
            eMQTTAdapter.connect();
        } catch (InterruptedException e) {
            System.err.println("Exception thrown during initialisation of MQTT: " + e.getMessage());
        }
    }

    protected void connectMQTTClient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost("localhost")
                .serverPort(1883)
                .identifier("ElevatorAlgorithmClient")
                .buildAsync();
        mqttClient.connect().whenComplete((ack, throwable) -> {
            if (throwable == null) {
                System.out.println("Connected to MQTT broker");
            } else {
                if (throwable.getMessage().contains("already connected")) {
                    System.out.println("MQTT client is already connected.");
                } else {
                    System.err.println("Failed to connect to MQTT broker: " + throwable.getMessage());
                }
            }
        });
    }

    protected static IElevator connectToRMI() {
        int retries = 0;
        long retryDelay;

        while (retries < MAX_RETRIES) {
            try{
                System.out.println("Attempting to connect to RMI server...");
                Properties properties = new Properties();
                String plcUrl = properties.getProperty("plc.url", RMI_URL);
                controller = (IElevator) Naming.lookup(plcUrl);

                System.out.println("Connected to RMI server");
                break;
            } catch (MalformedURLException e) {
                System.err.println("Malformed URL Exception thrown: " + e.getMessage());
                retries++;
                retryDelay = applyExponentialBackoff(retries);
                System.err.println("Retrying in " + retryDelay + " ms...");
                try {
                    sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (NotBoundException e) {
                System.err.println("Not bound Exception thrown: " + e.getMessage());
                retries++;
                retryDelay = applyExponentialBackoff(retries);
                System.err.println("Retrying in " + retryDelay + " ms...");
                try {
                    sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (RemoteException e) {
                System.err.println("RemoteException thrown: " + e.getMessage());
                retries++;
                retryDelay = applyExponentialBackoff(retries);
                System.err.println("Retrying in " + retryDelay + " ms...");
                try {
                    sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return controller;
    }

    private static long applyExponentialBackoff(int retries) {
        return (long) (RETRY_DELAY_MS * Math.pow(2, retries));  // Exponential backoff
    }

    private void subscribeToBuildingInfo() {
        try {
            String topicFilter = "building/info/#";
            mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();
            mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                if (topic.startsWith("building/info")) {
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    retainedMessages.put(topic, payload);
                    System.out.println("Retained message received: " + topic + " -> " + payload);
                }
            });
        } catch (Exception e) {
            System.err.println("An error occurred during subscription of retained messages: " + e.getMessage());
        }
    }

    private void subscribeToElevatorAndFloorMessages() {
        try {
            for (int elevatorId = 0; elevatorId < TOTAL_ELEVATORS; elevatorId++) {
                mqttClient.subscribeWith().topicFilter("elevator/" + elevatorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }
            for (int floorId = 1; floorId < HIGHEST_FLOOR; floorId++) {
                mqttClient.subscribeWith().topicFilter("floor/" + floorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }
            mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                if (topic.startsWith("elevator/") || topic.startsWith("floor/")) {
                    liveMessages.put(topic, payload);
                    System.out.println("Live message received: " + topic + " -> " + payload);
                }
            });
        } catch (Exception e) {
            System.err.println("Exception thrown during topic subscription: " + e.getMessage());
        }
    }



    public void runAlgorithm(ElevatorAlgorithm algorithm) throws InterruptedException {
        sleep(3000);
        algorithm.eMQTTAdapter.run();
        sleep(500);

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
        for (int elevatorId = 0; elevatorId < TOTAL_ELEVATORS; elevatorId++) {
            for (int floorId = 0; floorId < HIGHEST_FLOOR; floorId++) {
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

    private void waitForElevatorToReachTarget(
            int elevator,
            int floorRequested,
            ElevatorAlgorithm algorithm,
            int sleepTime) {
        // Variables to avoid redundant live message lookups
        String elevatorPrefix = "elevator/" + elevator;
        String currentFloorKey = elevatorPrefix + "/currentFloor";
        String doorStateKey = elevatorPrefix + "/doorState";

        try {
            // Poll for the elevator to reach the target floor and stop moving
            while (true) {
                // Retrieve the current floor
                String currentFloorStr = algorithm.liveMessages.get(currentFloorKey);
                int currentFloor = Integer.parseInt(currentFloorStr);

                if (currentFloor == floorRequested) {
                    break;
                }
                sleep(sleepTime);
            }

            long timeout = System.currentTimeMillis() + RETRY_DELAY_MS;
            while (!algorithm.liveMessages.get(doorStateKey).equals(ElevatorDoorState.OPEN.ordinal())) {
                sleep(sleepTime); // Sleep to avoid busy-waiting
            }

            // Set the committed direction to UNCOMMITTED after reaching the target and doors open
            algorithm.mqttClient.publishWith()
                    .topic(elevatorPrefix + "/committedDirection")
                    .payload("0".getBytes(StandardCharsets.UTF_8)) // 0 for UNCOMMITTED
                    .send();
        } catch (InterruptedException e) {
            log.error("Elevator reached target floor: {}", elevator);
        }
    }

    private void publishWithRetry(String topic, byte[] payload) {
        int attempts = 0;
        boolean success = false;

        while (attempts < ElevatorAlgorithm.MAX_RETRIES && !success) {
            try {
                mqttClient.publishWith()
                        .topic(topic)
                        .payload(payload)
                        .send();
                success = true;
                log.info("Message published to {}", topic);
            } catch (Exception e) {
                attempts++;
                log.error("Error publishing message to {}: {}", topic, e.getMessage());
                if (attempts < ElevatorAlgorithm.MAX_RETRIES) {
                    log.info("Retrying ({}/" + ElevatorAlgorithm.MAX_RETRIES + ") in " + ElevatorAlgorithm.RETRY_DELAY_MS + "ms...", attempts);
                    try {
                        sleep(ElevatorAlgorithm.RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Interrupted while waiting for retry");
                        break;
                    }
                }
            }
        }
        if (!success) {
            log.error("Failed to publish message to {} after " + ElevatorAlgorithm.MAX_RETRIES + " attempts.", topic);
        }
    }


    private void setElevatorTargetFloor(int elevatorId, int floorRequested, ElevatorAlgorithm algorithm) {
        try {
            String targetTopic = "elevator/" + elevatorId + "/targetFloor";
            algorithm.publishWithRetry(targetTopic, Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to set target floor for elevator {}: {}", elevatorId, e.getMessage());
        }
    }

    private void moveElevator(int elevator, int floorRequested, ElevatorAlgorithm algorithm, int direction, int sleepTime) {
        try {
            String directionTopic = "elevator/" + elevator + "/committedDirection";
            String targetTopic = "elevator/" + elevator + "/targetFloor";

            // Publish the direction
            algorithm.publishWithRetry(directionTopic, Integer.toString(direction).getBytes(StandardCharsets.UTF_8));

            // Publish the target floor
            algorithm.publishWithRetry(targetTopic, Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8));

            // Wait for elevator to reach the target floor
            waitForElevatorToReachTarget(elevator, floorRequested, algorithm, sleepTime);
        } catch (Exception e) {
            log.error("Error while moving elevator: {}", e.getMessage());
        }
    }
}