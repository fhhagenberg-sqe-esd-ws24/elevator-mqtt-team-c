package algorithm;

import at.wielander.elevator.Helpers.Constants.ElevatorRequest;
import at.wielander.elevator.Helpers.Constants.ElevatorDoorState;
import at.wielander.elevator.Helpers.Topics.ElevatorTopics;
import sqelevator.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import elevator.*;

import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static at.wielander.elevator.Helpers.Configuration.MQTTConfiguration.*;

public class ElevatorAlgorithm {

    private static final int CLOSING_OPENING_DURATION = 5000;
    private static final int CLOSE_OPEN_DURATION = 10000;
    private static final int SLEEP_DURATION = 10;
    private static final int TOTAL_ELEVATORS = 2;
    private static final int LOWEST_FLOOR = 0;
    private static final int HIGHEST_FLOOR = 4;
    private static final int MAXIMUM_PASSENGER_CAPACITY = 4000;
    private static final int FLOOR_HEIGHT = 10;
    private static final int POLLING_INTERVAL = 250;

    private static final Logger logger = Logger.getLogger(ElevatorAlgorithm.class.getName());
    private final ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_ELEVATORS);

    private final Map<String, String> retainedMessages = new ConcurrentHashMap<>();
    private final Map<String, String> liveMessages = new ConcurrentHashMap<>();
    private final Map<Integer, ElevatorRequest> elevatorRequests = new ConcurrentHashMap<>();

    private Mqtt5AsyncClient mqttClient;
    private ElevatorMQTTAdapter eMQTTAdapter;
    private ElevatorSystem eSystem;

    private volatile boolean terminateClient = false;

    public static void main(String[] args) {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();

        logger.info("Initializing the Elevator Algorithm...");
        try {
            String brokerHost = BROKER_URL.getValue();

            algorithm.setupRMIController();
            algorithm.initializeMQTTAdapter(brokerHost);
            algorithm.setupMQTTClient();
            algorithm.subscribeToTopics();

            logger.info("Starting elevator simulator...");
            algorithm.runElevatorSimulator();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Critical failure during algorithm execution: {0}", e.getMessage());
        } finally {
            logger.info("Shutting down resources.");
            algorithm.shutdown();
        }
    }

    private void setupRMIController() throws Exception {
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing RMI Controller: {0}", e.getMessage());
            throw e;
        }
    }

    private void initializeMQTTAdapter(String brokerHost) {
        try {
            eMQTTAdapter = new ElevatorMQTTAdapter(
                    eSystem,
                    brokerHost,
                    CLIENT_ID.getValue(),
                    POLLING_INTERVAL,
                    (IElevator) Naming.lookup(RMI_CONTROLLER.getValue())
            );
            eMQTTAdapter.connect();
            logger.info("MQTT Adapter initialized and connected successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing MQTT Adapter: {0}", e.getMessage());
            throw new RuntimeException("MQTT Adapter initialization failed.", e);
        }
    }

    private void setupMQTTClient() {
        try {
            mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost("localhost")
                    .serverPort(1883)
                    .identifier("ElevatorAlgorithmClient")
                    .buildAsync();

            mqttClient.connect().whenComplete((ack, throwable) -> {
                if (throwable == null) {
                    logger.info("Successfully connected to MQTT broker.");
                } else {
                    logger.log(Level.SEVERE, "Failed to connect to MQTT broker: {0}", throwable.getMessage());
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up MQTT client: {0}", e.getMessage());
            throw new RuntimeException("MQTT Client setup failed.", e);
        }
    }

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

    private void runElevatorSimulator() {
        try {
            while (true) {
                try {
                    runAlgorithm();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Elevator simulator interrupted, safely terminating.");
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Non-critical error in simulation loop: {0}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Critical error in simulator loop: {0}", e.getMessage());
        }
    }

    private void runAlgorithm() {
        try {
            Thread.sleep(3000);
            eMQTTAdapter.run();
            Thread.sleep(500);

            int numberOfFloors = Integer.parseInt(retainedMessages.getOrDefault("building/info/numberOfFloors", "0"));
            int numberOfElevators = Integer.parseInt(retainedMessages.getOrDefault("building/info/numberOfElevators", "0"));

            handleFloorRequests(numberOfElevators, numberOfFloors);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during algorithm execution: {0}", e.getMessage());
        }
    }

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

    private void publishAsyncTargetFloor(int elevator, int targetFloor) {
        String targetTopic = ElevatorTopics.ELEVATOR_INFO_CURRENT_FLOOR.elevatorIndex(String.valueOf(elevator));
        asyncPublish(targetTopic, Integer.toString(targetFloor));
    }

    private void awaitElevatorArrival(int elevator, int targetFloor) throws InterruptedException {
        while (isElevatorMoving(elevator, targetFloor)) {
            Thread.sleep(SLEEP_DURATION);
        }
        handleDoorStateTransition(elevator);
        elevatorRequests.remove(targetFloor);
    }

    private boolean isElevatorMoving(int elevator, int targetFloor) {
        int currentFloor = Integer.parseInt(liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "-1"));
        return currentFloor < targetFloor || Integer.parseInt(liveMessages.getOrDefault("elevator/" + elevator + "/speed", "0")) > 0;
    }

    private void handleElevatorMovement(int elevator, int targetFloor) throws InterruptedException {
        // Publish target floor asynchronously
        publishAsyncTargetFloor(elevator, targetFloor);

        // Wait until the elevator reaches the target floor
        awaitElevatorArrival(elevator, targetFloor);

        // Handle the door state transitions
        handleDoorStateTransition(elevator);

    }

    private void handleFloorRequests(int numberOfElevators, int numberOfFloors) {
        try {
            List<Integer> sortedFloors = new ArrayList<>(elevatorRequests.keySet());
            Collections.sort(sortedFloors);

            List<Callable<Void>> tasks = new ArrayList<>();

            for (int elevatorNumber = 0; elevatorNumber < TOTAL_ELEVATORS; elevatorNumber++) {
                final int elevatorId = elevatorNumber;
                tasks.add(() -> {
                    synchronized (elevatorRequests){
                        processElevatorRequests(sortedFloors, elevatorId, SLEEP_DURATION);
                    }
                    return null;
                });
            }

            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Error in floor request handling", e);
            Thread.currentThread().interrupt();
        }
    }

    private void processElevatorRequests(List<Integer> sortedFloors, int elevatorId, int sleepDuration) {
        for (Integer targetFloor : sortedFloors) {
            if(Thread.currentThread().isInterrupted() || terminateClient){
                logger.log(Level.WARNING, "Elevator " + elevatorId + " was interrupted, terminating the request processing.");
                return;
            }

            try {
                if (elevatorRequests.containsKey(targetFloor)) {
                    handleElevatorMovement(elevatorId, targetFloor);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.WARNING, "Movement interrupted for elevator " + elevatorId, e);
                return;
            }
        }
    }

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

    private void handleDoorStateTransition(int elevator) throws InterruptedException {
        try {
            /* CLOSED -> OPENING */
            asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)), Integer.toString(ElevatorDoorState.OPENING.ordinal()));
            Thread.sleep(CLOSING_OPENING_DURATION);

            /* OPENING -> OPEN */
            asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)), Integer.toString(ElevatorDoorState.OPEN.ordinal()));
            Thread.sleep(CLOSE_OPEN_DURATION);

            /* OPEN -> CLOSING */
            asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)), Integer.toString(ElevatorDoorState.CLOSING.ordinal()));
            Thread.sleep(CLOSING_OPENING_DURATION);

            /* CLOSING -> CLOSED */
            asyncPublish(ElevatorTopics.ELEVATOR_CONTROL_DOOR_STATE.elevatorIndex(String.valueOf(elevator)), Integer.toString(ElevatorDoorState.CLOSED.ordinal()));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling door state transition: {0}", e.getMessage());
            throw e;
        }
    }
}
