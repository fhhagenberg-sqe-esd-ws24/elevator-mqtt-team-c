package at.wielander.elevator.Algorithm;

import java.rmi.Naming;
import java.rmi.RemoteException;

import at.wielander.elevator.Helpers.Constants.ElevatorCommittedDirection;
import at.wielander.elevator.Helpers.Constants.ElevatorRequest;
import at.wielander.elevator.Helpers.Constants.ElevatorDoorState;
import at.wielander.elevator.Helpers.Topics.ElevatorTopics;
import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import at.wielander.elevator.Controller.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static at.wielander.elevator.Helpers.Configuration.MQTTConfiguration.*;

public class ElevatorAlgorithm {

    private static final int CLOSING_OPENING_DURATION = 5000;
    private static final int CLOSE_OPEN_DURATION = 10000;
    private static final int TOTAL_ELEVATORS = 2;
    private static final int LOWEST_FLOOR = 0;
    private static final int HIGHEST_FLOOR = 4;
    private static final int MAXIMUM_PASSENGER_CAPACITY = 4000;
    private static final int FLOOR_HEIGHT = 10;
    private static final int POLLING_INTERVAL = 250;
    private static final int TOTAL_RETRY_COUNTER = 10;
    private static final int RETRY_DELAY = 5000;

    private static final Logger logger = Logger.getLogger(ElevatorAlgorithm.class.getName());
    private final ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_ELEVATORS);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Map<String, String> retainedMessages = new ConcurrentHashMap<>();
    private final Map<String, String> liveMessages = new ConcurrentHashMap<>();
    private final Map<Integer, ElevatorRequest> elevatorRequests = new ConcurrentHashMap<>();

    private Mqtt5AsyncClient mqttClient;
    private ElevatorMQTTAdapter eMQTTAdapter;
    private ElevatorSystem eSystem;

    private volatile boolean terminateClient = false;
    private int numberOfFloors;
    private int numberOfElevators;
    private ElevatorCommittedDirection currentDirection = null;
    private ElevatorCommittedDirection lastDirection = null;


    public static void main(String[] args) {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();

        try {
            algorithm.initialiseAlgorithm();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Critical failure during Algorithm execution: {0}", e.getMessage());
        } finally {
            logger.info("Shutting down resources.");
            algorithm.shutdown();
        }
    }

    public void initialiseAlgorithm() throws Exception {
        try {
            setupRMIController();
            initialiseMQTTAdapter(BROKER_URL.getValue());
            setupMQTTClient();
            subscribeToTopics();
            runElevatorSimulator();
        } catch (RemoteException e){
            logger.log(Level.WARNING, "Initialisation failed!");
        }
    }

    /**
     * Initialise the RMI controller
     * @throws Exception Exception for any errors
     */
    private void setupRMIController() throws Exception {
        int retryCounter = 0;
        boolean rmiConnected = false;

        while(retryCounter < TOTAL_RETRY_COUNTER && !rmiConnected) {
            try {
                IElevator controller = (IElevator) Naming.lookup(RMI_CONTROLLER.getValue());
                eSystem = new ElevatorSystem(
                        TOTAL_ELEVATORS,
                        LOWEST_FLOOR,
                        HIGHEST_FLOOR,
                        MAXIMUM_PASSENGER_CAPACITY,
                        FLOOR_HEIGHT,
                        controller
                );
                logger.info("RMI Controller initialized successfully.");
                rmiConnected = true;
                return;
            } catch (RemoteException e) {
                logger.log(Level.WARNING, "Connection to RMI controller unsuccessful");
                retryCounter++;
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.log(Level.SEVERE, "Reconnecting to RMI interrupted", ie);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error initializing RMI Controller: {0}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Initialise MQTT Adapter
     *
     * @param brokerHost Hostname for the MQTT Broker
     */
    private void initialiseMQTTAdapter(String brokerHost) {
        try {
            eMQTTAdapter = new ElevatorMQTTAdapter(
                    eSystem,
                    brokerHost,
                    CLIENT_ID.getValue(),
                    POLLING_INTERVAL,
                    (IElevator) Naming.lookup(RMI_CONTROLLER.getValue())
            );
            eMQTTAdapter.connect();
            eMQTTAdapter.run();
            logger.info("MQTT Adapter initialized and connected successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing MQTT Adapter: {0}", e.getMessage());
            throw new RuntimeException("MQTT Adapter initialization failed.", e);
        }
    }

    /**
     * Set up the connection to the MQTT Broker
     */
    private void setupMQTTClient() {
        try {
            mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost("localhost")
                    .serverPort(1883)
                    .identifier("ElevatorAlgorithmClient")
                    .buildAsync();

            mqttClient.connect().get();
            logger.info("Successfully connected to MQTT broker.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up MQTT client: {0}", e.getMessage());
            throw new RuntimeException("MQTT Client setup failed.", e);
        }
    }

    /**
     * Terminate the connection of the MQTT Client and MQTT Adapter
     */
    private void shutdown() {
        try {
            terminateClient = true;

            // Terminate the executor service
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warning("Executor service did not terminate in time.");
            }

            // Then disconnect from MQTT
            if (eMQTTAdapter != null) {
                eMQTTAdapter.disconnect();
            }
            if (mqttClient != null) {
                mqttClient.disconnect();
            }

            logger.info("Successfully shutdown.");
        } catch (InterruptedException exception){
            logger.warning("Shutdown interrupted, forcing termination.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during shutdown: {0}", e.getMessage());
        }
    }

    /**
     *  Subscribe to the topics from MQTT Adapter
     */
    private void subscribeToTopics() {
        try {
            mqttClient.subscribeWith()
                    .topicFilter("building/info/#")
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();

            for (int i = 0; i < TOTAL_ELEVATORS; i++) {
                mqttClient.subscribeWith()
                        .topicFilter(ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(i)))
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .send();
            }

            for (int i = LOWEST_FLOOR; i <= HIGHEST_FLOOR; i++) {
                mqttClient.subscribeWith()
                        .topicFilter("floor/" + i + "/#")
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .send();
            }

            logger.info("Successfully subscribed to MQTT topics.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error subscribing to topics: {0}", e.getMessage());
            throw new RuntimeException("Topic subscription failed.", e);
        }
    }

    /**
     * Execute the elevator Simulator
     */
    private void runElevatorSimulator() {
        try {
            scheduler.scheduleAtFixedRate(this::runAlgorithm, 0, 2, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Critical error in simulator loop: {0}", e.getMessage());
        }
    }

    /**
     * Execute the elevator Algorithm
     */
    public void runAlgorithm() {
        try {

            scheduler.schedule(() -> eMQTTAdapter.run(),3,TimeUnit.SECONDS);

            numberOfFloors = Integer.parseInt(retainedMessages.getOrDefault("building/info/numberOfFloors", "0"));
            numberOfElevators = Integer.parseInt(retainedMessages.getOrDefault("building/info/numberOfElevators", "0"));

            handleFloorRequests();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during Algorithm execution: {0}", e.getMessage());
        }
    }

    /**
     * Publishes the message to the MQTT Adapter
     * @param elevatorNumber elevator Number
     * @param targetFloor Targeted floor
     */
    private void publishAsyncTargetFloor(int elevatorNumber, int targetFloor) {
        String targetTopic = ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(elevatorNumber));
        asyncPublish(targetTopic, Integer.toString(targetFloor));
    }

    /**
     * Handles the door state transitions when the elevator arrives at a single floor
     * @param elevatorNum Elevator number
     */
    private void handleDoorStateTransition(int elevatorNum) {
        try {
            /* CLOSED -> OPENING */
            scheduler.schedule(() -> asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevatorNum)),
                    Integer.toString(ElevatorDoorState.OPENING.ordinal())), CLOSING_OPENING_DURATION, TimeUnit.MILLISECONDS);

            /* OPENING -> OPEN */
            scheduler.schedule(() -> asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevatorNum)),
                    Integer.toString(ElevatorDoorState.OPEN.ordinal())), CLOSE_OPEN_DURATION, TimeUnit.MILLISECONDS);

            /* OPEN -> CLOSING */
            scheduler.schedule(() -> asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevatorNum)),
                    Integer.toString(ElevatorDoorState.CLOSING.ordinal())), CLOSING_OPENING_DURATION, TimeUnit.MILLISECONDS);

            /* CLOSING -> CLOSED */
            scheduler.schedule(() -> asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevatorNum)),
                    Integer.toString(ElevatorDoorState.CLOSED.ordinal())), CLOSE_OPEN_DURATION, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling door state transitions: {0}", e.getMessage());
            throw e;
        }
    }

    /**
     * Handles the countdown of the arrival of the elevator at a floor
     * @param elevatorNum Elevator number
     * @param targetFloor Targeted floor
     * @throws InterruptedException Throws an exception if the process is interrupted
     */
    private void awaitElevatorArrival(int elevatorNum, int targetFloor) throws InterruptedException {

        /* Create a countdown latch */
        CountDownLatch latchAwaitElevatorArrival = new CountDownLatch(1);

        new Thread(()->{
            try{
                while(isElevatorMoving(elevatorNum, targetFloor)){

                    // Determine real-time direction
                    int currentFloor = eSystem.getElevatorPosition(elevatorNum);

                    if (currentFloor < targetFloor) {
                        currentDirection = ElevatorCommittedDirection.UP;
                    } else if (currentFloor > targetFloor) {
                        currentDirection = ElevatorCommittedDirection.DOWN;
                    } else {
                        currentDirection = ElevatorCommittedDirection.UNCOMMITTED;
                    }

                    // Publish direction only if it has changed
                    if (currentDirection != lastDirection) {
                        String directionTopic = ElevatorTopics.ELEVATOR_INFO_COMMITTED_DIRECTION.elevatorIndex(String.valueOf(elevatorNum));
                        asyncPublish(directionTopic, Integer.toString(currentDirection.ordinal()));
                        lastDirection = currentDirection;
                        logger.info("Updated direction for Elevator " + elevatorNum + ": " + currentDirection);
                    }
                }
                latchAwaitElevatorArrival.countDown();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }).start();

        // Wait for the latch to be counted down and then handle state transition
        latchAwaitElevatorArrival.await();
        synchronized (elevatorRequests){
            handleDoorStateTransition(elevatorNum);
            elevatorRequests.remove(targetFloor);
        }

    }

    /**
     * Verifies if the elevator is moving in a certain direction
     * @param elevatorNum Elevator Number
     * @param targetFloor Targeted floor
     * @return true if the elevator is not at the targeted floor
     */
    private boolean isElevatorMoving(int elevatorNum, int targetFloor) {
        int currentFloor = Integer.parseInt(liveMessages.getOrDefault("elevator/" + elevatorNum + "/currentFloor", "-1"));
        return currentFloor < targetFloor || Integer.parseInt(liveMessages.getOrDefault("elevator/" + elevatorNum + "/speed", "0")) > 0;
    }

    /**
     * Verifies if the elevator is at maximum capacity
     * @param elevatorNum elevator number
     * @return true if the elevator is a maximum capacity
     */
    private boolean isElevatorFull(int elevatorNum) {
        int currentPassengers = Integer.parseInt(liveMessages.getOrDefault("elevator/" + elevatorNum + "/currentPassengers", "0"));
        return currentPassengers >= MAXIMUM_PASSENGER_CAPACITY;
    }

    /**
     * Handles the elevator moving to the targeted floor
     * @param elevatorNum Elevator number
     * @param targetFloor targeted floor
     * @throws InterruptedException Exception thrown when the elevator
     */
    private void handleElevatorMovement(int elevatorNum, int targetFloor) throws InterruptedException {
        // Publish target floor asynchronously
        publishAsyncTargetFloor(elevatorNum, targetFloor);

        // Wait until the elevator reaches the target floor
        awaitElevatorArrival(elevatorNum, targetFloor);

        // Handle the door state transitions
        handleDoorStateTransition(elevatorNum);

    }
    /**
     * Handles the floor requests, ensuring floors are processed
     * in ascending or descending order based on the elevator's current position.
     */
    private void handleFloorRequests() {
        try {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int elevatorNumber = 0; elevatorNumber < TOTAL_ELEVATORS; elevatorNumber++) {
                final int elevatorIndex = elevatorNumber;
                tasks.add(() -> {
                    synchronized (elevatorRequests) {
                        /* Send the elevator to the lowest floor if no requests remain */
                        if (elevatorRequests.isEmpty()) {
                            logger.info("No floor requests for Elevator " + elevatorIndex + ". Moving lowest floor.");
                            sendElevatorToLowestFloor(elevatorIndex);
                        }
                        /* If the maximum capacity of the elevator has been reached */
                        else if (isElevatorFull(elevatorIndex)) {
                            logger.info("Elevator " + elevatorIndex + " is full. Skipping new requests.");
                            return null;
                        }
                        else {

                            /* obtain current direction of elevator */
                            String committedDirection = ElevatorTopics.ELEVATOR_INFO_COMMITTED_DIRECTION.elevatorIndex(String.valueOf(elevatorIndex));
                            ElevatorCommittedDirection direction = ElevatorCommittedDirection.valueOf(liveMessages.getOrDefault(committedDirection, "UNCOMMITTED"));

                            /* Sort floors into upwards and downwards based on the elevator's current position */
                            int currentFloor = Integer.parseInt(liveMessages.getOrDefault(
                                    "elevator/" + elevatorIndex + "/currentFloor", String.valueOf(LOWEST_FLOOR)
                            ));
                            List<Integer> sortedFloors = new ArrayList<>(elevatorRequests.keySet());

                            /* Convert into requests in upwards direction */
                            List<Integer> upRequests = sortedFloors.stream()
                                    .filter(floor -> floor > currentFloor)
                                    .sorted()
                                    .toList();

                            /* Convert into requests in downwards direction */
                            List<Integer> downRequests = sortedFloors.stream()
                                    .filter(floor -> floor < currentFloor)
                                    .sorted(Collections.reverseOrder())
                                    .toList();

                            /* Process upwards requests first, then downwards */
                            if (direction == ElevatorCommittedDirection.UP) {
                                processElevatorRequests(upRequests, elevatorIndex);
                                processElevatorRequests(downRequests, elevatorIndex);
                            } else if (direction == ElevatorCommittedDirection.DOWN) {
                                processElevatorRequests(downRequests, elevatorIndex);
                                processElevatorRequests(upRequests, elevatorIndex);
                            } else {
                                processElevatorRequests(upRequests, elevatorIndex);
                                processElevatorRequests(downRequests, elevatorIndex);
                            }

                            /* if the top floor has been reached */
                            if (upRequests.isEmpty() && currentFloor == HIGHEST_FLOOR) {
                                logger.info("Elevator " + elevatorIndex + " reached the top floor, transitioning to downwards requests.");
                                processElevatorRequests(downRequests, elevatorIndex);
                            }
                        }
                    }
                    return null;
                });
            }

            /* Execute all tasks concurrently */
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Error in floor request handling", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handles the movement of the elevator to process the given requests.
     *
     * @param sortedFloors    Sorted list of floors to process.
     * @param indexElevator   The elevator number.
     */
    private void processElevatorRequests(List<Integer> sortedFloors, int indexElevator) {
        for (Integer targetFloor : sortedFloors) {
            /* If the working thread is interrupted */
            if (Thread.currentThread().isInterrupted() || terminateClient) {
                logger.log(Level.WARNING, "Elevator " + indexElevator + " was interrupted, terminating request processing.");
                return;
            }

            /* Move to the targeted floor */
            try {
                if (elevatorRequests.containsKey(targetFloor)) {
                    handleElevatorMovement(indexElevator, targetFloor);
                    elevatorRequests.remove(targetFloor);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.WARNING, "Movement interrupted for elevator " + indexElevator, e);
                return;
            }
        }
    }

    /**
     * Sends the specified elevator to the lowest floor when no requests remain.
     *
     * @param indexElevator The elevator number to send to the lowest floor.
     */
    private void sendElevatorToLowestFloor(int indexElevator) {
        try {
            int currentFloor = Integer.parseInt(liveMessages.getOrDefault(
                    "elevator/" + indexElevator + "/currentFloor", String.valueOf(LOWEST_FLOOR)
            ));
            if (currentFloor != LOWEST_FLOOR) {
                logger.info("Elevator " + indexElevator + " moving back to the lowest floor.");
                handleElevatorMovement(indexElevator, LOWEST_FLOOR);
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Movement to the lowest floor interrupted for elevator " + indexElevator, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handles the asynchronous publishing of the MQTT Topics and payload to the MQTT Adapter
     * @param topic MQTT topics
     * @param payload MQTT Payload
     */
    private void asyncPublish(String topic, String payload) {
        mqttClient.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .send()
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.log(Level.SEVERE, "Error publishing message: {0}", exception.getMessage());
                    } else {
                        logger.info("Message published successfully: " + topic);
                    }
                });
    }
}