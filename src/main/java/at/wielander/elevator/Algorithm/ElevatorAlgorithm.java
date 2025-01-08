package at.wielander.elevator.Algorithm;

import at.wielander.elevator.Exception.MQTTClientException;
import at.wielander.elevator.Helpers.ElevatorCommittedDirection;
import at.wielander.elevator.Helpers.ElevatorDoorState;
import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class ElevatorAlgorithm {

    /* Datamodel */
    private static IElevator controller;
    protected Mqtt5AsyncClient mqttClient;
    private ElevatorMQTTAdapter eMQTTAdapter;
    private ElevatorSystem eSystem;

    /* Constants */
    private static final int LOWEST_FLOOR = 0;
    private static final int HIGHEST_FLOOR = 11;
    private static final int TOTAL_ELEVATORS = 1;
    private static final int FLOOR_HEIGHT = 10;
    private static final int TOTAL_CAPACITY = 4000;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 5000;
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String RMI_URL = "rmi://localhost/ElevatorSim";
    private static final int SLEEP_DURATION_S = 5;

    protected final Map<String, String> retainedMessages = new ConcurrentHashMap<>();
    protected final Map<String, String> liveMessages = new ConcurrentHashMap<>();
    protected final PriorityQueue<Integer> upQueue = new PriorityQueue<>();
    protected final PriorityQueue<Integer> downQueue = new PriorityQueue<>(Collections.reverseOrder());

    /**
     * Main function
     * @throws InterruptedException Throws an exception if process interrupted
     */
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

            // Run algorithm
            algorithm.runAlgorithm(algorithm);

        } catch (Exception e) {

            if (algorithm.eMQTTAdapter != null) {
                algorithm.eMQTTAdapter.disconnect();
            }
            System.err.println("Failed to initialise System " + e.getMessage());
        }
    }

    /**
     * Initialise elevator system
     * @param controller IElevator controller
     * @throws RemoteException Throws a remote exception if disconnected
     */
    protected void initialiseElevatorSystem(IElevator controller) throws RemoteException {
        try {
            // Create Elevator Data model
            eSystem = new ElevatorSystem(
                    TOTAL_ELEVATORS,
                    LOWEST_FLOOR,
                    HIGHEST_FLOOR,
                    TOTAL_CAPACITY,
                    FLOOR_HEIGHT,
                    controller
            );

            // Create the MQTT Adapter
            eMQTTAdapter = new ElevatorMQTTAdapter(
                    eSystem,// Elevator System
                    ElevatorAlgorithm.BROKER_URL,
                    "mqttAdapter",
                    50,
                    controller
            );

            // Connect MQTT Adapter to the Broker
            eMQTTAdapter.connect();
        } catch (InterruptedException e) {
            System.err.println("Exception thrown during initialisation of MQTT: " + e.getMessage());
        }
    }

    /**
     *  Handles the connection of the mqtt client
     */
    protected void connectMQTTClient() {
        mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost("localhost")
                .serverPort(1883)
                .identifier("ElevatorAlgorithmClient")
                .automaticReconnect()
                .initialDelay(1, TimeUnit.SECONDS)
                .maxDelay(10, TimeUnit.SECONDS)
                .applyAutomaticReconnect()
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

    /**
     * Handles the connection to the JAVA RMI Elevator Simulator with
     *
     * @return IElevator instance
     */
    protected static IElevator connectToRMI() {
        int retries = 0;
        long retryDelay;

        // Establishes RMI connection. Throws necessary exceptions if connection fails
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
                    System.err.println("Interrupted Exception thrown: " + ie.getMessage());
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
                System.err.println("Failed to connect to ElevatorSim: " + e.getMessage());
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

    /**
     * Adds a small delay when reconnecting RMI
     * @param retries retry counter
     * @return incremented counter
     */
    private static long applyExponentialBackoff(int retries) {
        return (long) (RETRY_DELAY_MS * Math.pow(2, retries));
    }

    /**
     * Subscribe to building info retained topics
     */
    private void subscribeToBuildingInfo() {
        try {
            // Subscribe to all building topics
            String topicFilter = "building/info/+";
            mqttClient.subscribeWith()
                    .topicFilter(topicFilter)
                    .qos(MqttQos.AT_MOST_ONCE)
                    .send();

            // Publishes retained elevator messages
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

    /**
     * Handles the subcription of floor and elevator topics
     */
    void subscribeToElevatorAndFloorMessages() {
        try {
            // Subscribe to all elevator topics
            for (int elevatorId = 0; elevatorId < TOTAL_ELEVATORS; elevatorId++) {
                mqttClient.subscribeWith().topicFilter("elevator/" + elevatorId + "/+").qos(MqttQos.AT_LEAST_ONCE).send();
            }

            // Subscibe to all floor topics
            for (int floorId = 1; floorId < HIGHEST_FLOOR; floorId++) {
                mqttClient.subscribeWith().topicFilter("floor/" + floorId + "/+").qos(MqttQos.AT_LEAST_ONCE).send();
            }
            // publishes all the topics and payloads
            mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                // Adds topics to live messages
                if (topic.startsWith("elevator/") || topic.startsWith("floor/")) {
                    liveMessages.put(topic, payload);
                    System.out.println("Live message received: " + topic + " -> " + payload);
                }
            });
        } catch (MQTTClientException e) {
            System.err.println("Exception thrown during topic subscription: " + e.getMessage());
        }
    }


    /**
     * Main elevator algorithm. Executes task of polling and adding requests to queues
     * @param algorithm algorithm instance
     * @throws InterruptedException Exception thrown if process is interrupted
     */
    public void runAlgorithm(ElevatorAlgorithm algorithm) throws InterruptedException {
        sleep(2000);
        algorithm.eMQTTAdapter.run();
        sleep(500);

        final int numberOfFloors = Integer.parseInt(retainedMessages.get("building/info/numberOfFloors"));
        final int elevator = 0;

        // Subscribe to external floor button presses
        subscribeToFloorButtonPresses(algorithm, numberOfFloors);

        // Subscribe to inside elevator button presses, once
        subscribeToInsideElevatorButtonPresses(algorithm);

        // Handle button presses asynchronously
        handleButtonPresses(algorithm, elevator);
    }

    /**
     * Subscibe to floor button elevator topics
      * @param algorithm    algorithm instance
     * @param numberOfFloors number of floors
     */
    private void subscribeToFloorButtonPresses(ElevatorAlgorithm algorithm, int numberOfFloors) {
        for (int floorId = 1; floorId < numberOfFloors; floorId++) {
            try {
                String upButtonTopic = "floor/" + floorId + "/buttonUp";
                String downButtonTopic = "floor/" + floorId + "/buttonDown";
                // Subscribe once for each button type on all floors
                algorithm.mqttClient.subscribeWith().topicFilter(upButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
                algorithm.mqttClient.subscribeWith().topicFilter(downButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
            } catch (MQTTClientException e) {
                System.err.println("Failed to subscribe to button press topics for floor " + floorId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Subscribe to topics for inside elevator button presses
      * @param algorithm algorithm instance
     */
    private void subscribeToInsideElevatorButtonPresses(ElevatorAlgorithm algorithm) {
        for (int elevatorId = 0; elevatorId < TOTAL_ELEVATORS; elevatorId++) {
            for (int floorId = 1; floorId < HIGHEST_FLOOR; floorId++) {
                try {
                    String elevatorButtonTopic = "elevator/" + elevatorId + "/button/" + floorId;
                    algorithm.mqttClient.subscribeWith().topicFilter(elevatorButtonTopic).qos(MqttQos.AT_LEAST_ONCE).send();
                } catch (MQTTClientException e) {
                    System.err.println("Failed to subscribe to elevator button topic for elevator " + elevatorId + " and floor " + floorId + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles all button requests
     * @param algorithm algorithm instance
     * @param elevatorNumber elevator number
     */
    void handleButtonPresses(ElevatorAlgorithm algorithm, int elevatorNumber) {
        algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
            try {
                String topic = publish.getTopic().toString();

                // Handle external floor button press
                if (topic.startsWith("floor/")) {
                    int floorRequested = Integer.parseInt(topic.split("/")[1]);
                    if (topic.contains("/buttonUp")) {
                        synchronized (upQueue) {
                            upQueue.add(floorRequested);
                            System.out.println("Added " + topic +" to queue ");
                        }
                    } else if (topic.contains("/buttonDown")) {
                        synchronized (downQueue) {
                            downQueue.add(floorRequested);
                            System.out.println("Added " + topic +" to queue ");

                        }
                    }
                }

                // Handle inside elevator button press
                if (topic.startsWith("elevator/") && topic.contains("/button/")) {
                    String[] parts = topic.split("/");
                    int requestedFloor = Integer.parseInt(parts[3]);
                    int currentFloor = Integer.parseInt(algorithm.liveMessages.get("elevator/" + elevatorNumber + "/currentFloor"));

                    // adds the requested floor to the up queue if current floor is greater, else downwards queue
                    if (requestedFloor >= LOWEST_FLOOR && requestedFloor <= HIGHEST_FLOOR) {
                        if (requestedFloor > currentFloor) {
                            synchronized (upQueue) {
                                upQueue.add(requestedFloor);
                            }
                        } else if (requestedFloor < currentFloor) {
                            synchronized (downQueue) {
                                downQueue.add(requestedFloor);
                            }
                        }
                    }
                }

                // Process requests dynamically
                processRequestQueue(elevatorNumber, algorithm);

            } catch (MQTTClientException e) {
                System.err.println("Exception thrown when handling error " + e.getMessage());
            }
        });
    }

    /**
     * Handles the processing for requests based on the current direction
     * @param elevatorNumber Elevator number
     * @param algorithm algorithm instance
     */
    private void processRequestQueue(int elevatorNumber, ElevatorAlgorithm algorithm) {
        try {
            String directionKey = "elevator/" + elevatorNumber + "/committedDirection";
            String currentFloorKey = "elevator/" + elevatorNumber + "/currentFloor";

            // Set committed direction
            int currentDirectionValue = Integer.parseInt(algorithm.liveMessages.get(directionKey));
            ElevatorCommittedDirection currentDirection = ElevatorCommittedDirection.getValue(currentDirectionValue);
            int currentFloor = Integer.parseInt(algorithm.liveMessages.get(currentFloorKey));

            // Process the request based on the current committed direction
            switch (currentDirection) {
                case UP:
                    processUpQueue(elevatorNumber, currentFloor, algorithm);
                    break;

                case DOWN:
                    processDownQueue(elevatorNumber, currentFloor, algorithm);
                    break;

                case UNCOMMITTED:
                    processUncommitted(elevatorNumber, algorithm);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error while processing request queue: " + e.getMessage());
        }
    }

    /**
     * Handles the floor request going up
     * @param elevatorNumber Elevator number
     * @param currentFloor current floor
     * @param algorithm algorithm instance
     */
    private void processUpQueue(int elevatorNumber, int currentFloor, ElevatorAlgorithm algorithm) {
        synchronized (upQueue) {
            // Remove old requestss
            while (!upQueue.isEmpty() && upQueue.peek() < currentFloor) {
                upQueue.poll();
            }
            // Poll for new request heading up
            if (!upQueue.isEmpty()) {
                int nextFloor = upQueue.poll();
                moveElevator(elevatorNumber, nextFloor, algorithm, ElevatorCommittedDirection.UP);
            } else if (!downQueue.isEmpty()) {
                // Switch to DOWN committed direction
                moveElevator(elevatorNumber, downQueue.peek(), algorithm, ElevatorCommittedDirection.DOWN);
            }
        }
    }

    /**
     * Handles the floor request heading downwards
     * @param elevatorNumber elevator number
     * @param currentFloor current floor
     * @param algorithm algorithm instance
     */
    private void processDownQueue(int elevatorNumber, int currentFloor, ElevatorAlgorithm algorithm) {
        synchronized (downQueue) {
            // Remove old requestss
            while (!downQueue.isEmpty() && downQueue.peek() > currentFloor) {
                downQueue.poll();
            }
            if (!downQueue.isEmpty()) {
                int nextFloor = downQueue.poll();
                moveElevator(elevatorNumber, nextFloor, algorithm, ElevatorCommittedDirection.DOWN);
            } else if (!upQueue.isEmpty()) {
                // Switch to UP direction if no downward requests
                moveElevator(elevatorNumber, upQueue.peek(), algorithm, ElevatorCommittedDirection.UP);
            }
        }
    }

    /**
     * Handles requests by processing request upwards before heading downwards
     * @param elevatorNumber elevator number
     * @param algorithm algorithm instance
     */
    private void processUncommitted(int elevatorNumber, ElevatorAlgorithm algorithm) {
        if (!upQueue.isEmpty()) {
            moveElevator(elevatorNumber, upQueue.peek(), algorithm, ElevatorCommittedDirection.UP);
        } else if (!downQueue.isEmpty()) {
            moveElevator(elevatorNumber, downQueue.peek(), algorithm, ElevatorCommittedDirection.DOWN);
        }
    }

    /**
     * Handles the movement of the elevator
     * @param elevatorNumber elevator number
     * @param floorRequested requested floor
     * @param algorithm algorithm instance
     * @param direction direction (either up/ down or uncommitted)
     */
    private void moveElevator(int elevatorNumber, int floorRequested, ElevatorAlgorithm algorithm, ElevatorCommittedDirection direction) {
        try {
            // create the direction and target topics
            String directionTopic = "elevator/" + elevatorNumber + "/committedDirection";
            String targetTopic = "elevator/" + elevatorNumber + "/targetFloor";

            // Publish the direction
            algorithm.publishWithRetry(directionTopic, Integer.toString(direction.ordinal()).getBytes(StandardCharsets.UTF_8));

            // Publish the target floor
            algorithm.publishWithRetry(targetTopic, Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8));

            // Set elevator target floor
            setElevatorTargetFloor(elevatorNumber, floorRequested, algorithm);

            // Wait for elevator to reach the target floor
            waitForElevatorToReachTarget(elevatorNumber, floorRequested, algorithm);
        } catch (Exception e) {
            System.err.println("Error while moving elevator to target");
        }
    }

    /**
     * Adds a delay to wait for the elevator to reach the requested floor
     * @param elevatorNumber elevator number
     * @param floorRequested requested floor
     * @param algorithm algorithm instance
     */
    private void waitForElevatorToReachTarget(
            int elevatorNumber,
            int floorRequested,
            ElevatorAlgorithm algorithm) {

        try {
            while (true) {
                // get the current floor
                String currentFloorKey = "elevator/" + elevatorNumber + "/currentFloor";
                int currentFloor = Integer.parseInt(algorithm.liveMessages.get(currentFloorKey));

                // Removes current floor from the up queue
                synchronized (upQueue) {
                    if (upQueue.contains(currentFloor)) {
                        upQueue.remove(currentFloor);
                        doorsStateTransition(elevatorNumber, algorithm);
                    }
                }

                // remove current floor from the down queue
                synchronized (downQueue) {
                    if (downQueue.contains(currentFloor)) {
                        downQueue.remove(currentFloor);
                        doorsStateTransition(elevatorNumber, algorithm);
                    }
                }

                if (currentFloor == floorRequested) {
                    doorsStateTransition(elevatorNumber, algorithm);
                    break;
                }
                sleep(SLEEP_DURATION_S);
            }
        } catch (InterruptedException e) {
            System.err.println("Failed to wait for elevator to reach target");
        }
    }

    /**
     * Handles the publishing of messages for the door state transitions
     * @param elevatorNumber elevatorNumber
     * @param algorithm algorithm instance
     */
    private void doorsStateTransition(int elevatorNumber, ElevatorAlgorithm algorithm) {
        String doorStateTopic = "elevator/" + elevatorNumber + "/doorState";
        algorithm.publishWithRetry(doorStateTopic, Integer.toString(ElevatorDoorState.OPEN.ordinal()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verify if the client is connected before publishing
     * @return true if client is connected, false otherwise
     */
    private boolean isClientConnected() {
        return mqttClient.getState().isConnected();
    }

    /**
     * Publishes the topic and payload, retries if fails
     * @param topic elevator topic
     * @param payload elevator payload
     */
    private void publishWithRetry(String topic, byte[] payload) {
        int attempts = 0;
        AtomicBoolean success = new AtomicBoolean(false);

        // Reconnects the mqtt client if disconnected
        while (attempts < ElevatorAlgorithm.MAX_RETRIES && !success.get()) {
            if (!isClientConnected()) {
                System.err.println("MQTT client not connected. Attempting to reconnect...");
                mqttClient.connect();
                try {
                    sleep(ElevatorAlgorithm.RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    System.err.println("Failed to reconnect after " + attempts + " attempts.");
                    mqttClient.connect();
                    break;
                }
                continue;
            }

            try {
                mqttClient.publishWith()
                        .topic(topic)
                        .payload(payload)
                        .send()
                        .whenComplete((ack, throwable) -> {
                            if (throwable == null) {
                                System.out.println("Successfully published to topic " + topic);
                                success.set(true); // Update success flag here in the callback
                            } else {
                                System.err.println("Publish failed: " + throwable.getMessage());
                            }
                        });

                // Block the current thread until the publishing is complete
                while (!success.get()) {
                    try {
                        Thread.sleep(100); // Small delay to wait for the callback
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (Exception e) {
                attempts++;
                System.err.println("Failed to publish on attempt " + attempts + ": " + e.getMessage());
                if (attempts < ElevatorAlgorithm.MAX_RETRIES) {
                    try {
                        sleep(ElevatorAlgorithm.RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!success.get()) {
            System.err.println("Failed to publish to topic " + topic + " after " + ElevatorAlgorithm.MAX_RETRIES + " attempts");
        }
    }

    /**
     * Sets the target floor of the elevator
     * @param elevatorNumber Elevator number
     * @param floorRequested Requested floor
     * @param algorithm Algorithm instance
     */
    private void setElevatorTargetFloor(int elevatorNumber, int floorRequested, ElevatorAlgorithm algorithm) {
        try {
            // Publishes the desired target floor
            String targetTopic = "elevator/" + elevatorNumber + "/targetFloor";
            algorithm.publishWithRetry(targetTopic, Integer.toString(floorRequested).getBytes(StandardCharsets.UTF_8));
        } catch (MQTTClientException e) {
            System.err.println("Failed to publish to elevator " + elevatorNumber);
        }
    }
}