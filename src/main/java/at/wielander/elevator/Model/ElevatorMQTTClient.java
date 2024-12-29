package at.wielander.elevator.Model;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sqelevator.IElevator;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Custom exception class for handling MQTT client errors
class MQTTClientException extends RuntimeException {
    public MQTTClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class ElevatorMQTTClient {
    private static final Logger log = LoggerFactory.getLogger(ElevatorMQTTClient.class);
    private final Mqtt5AsyncClient client;
    private final ScheduledExecutorService scheduler;
    private static final int RECONNECT_DELAY = 10;
    private final Map<Integer, ElevatorAlgorithm> elevatorAlgorithms = new HashMap<>();
    private final ElevatorSystem elevatorSystem;
    private static IElevator elevatorAPI;

    // Constructor for initializing the MQTT client and elevator system
    public ElevatorMQTTClient(String brokerUrl, String clientId, ElevatorSystem elevatorSystem) {
        try {
            URI brokerUri = new URI(brokerUrl);
            String host = brokerUri.getHost();
            int port = brokerUri.getPort();

            // Build the asynchronous MQTT client
            this.client = Mqtt5Client.builder()
                    .serverHost(host)
                    .serverPort(port)
                    .identifier(clientId)
                    .buildAsync();

            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.elevatorSystem = elevatorSystem;
        } catch (Exception e) {
            throw new MQTTClientException("Failed to initialize MQTT client", e);
        }
    }

    // Method to connect to the MQTT broker and subscribe to relevant topics
    public void connectAndSubscribe() {
        client.connectWith()
                .cleanStart(true)
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to connect to broker: {}", throwable.getMessage());
                        scheduleReconnect();
                    } else {
                        log.info("Connected to broker.");
                        subscribeToElevatorTopics();
                    }
                });
    }

    // Method to gracefully shut down the client and its resources
    public void shutdown() {
        try {
            if (client.getState() == MqttClientState.CONNECTED) {  // Check if the client is connected
                // Attempt to disconnect from the broker
                client.disconnect()
                        .whenComplete((ack, throwable) -> {
                            if (throwable != null) {
                                log.error("Failed to disconnect: {}", throwable.getMessage());  // Log error if disconnect fails
                            } else {
                                log.info("Disconnected from broker.");  // Log success if disconnect succeeds
                            }
                        });
            } else {
                log.info("Client is already disconnected.");  // If client was already disconnected, log it
            }
        } finally {
            scheduler.shutdown();  // Shutdown the scheduler
            log.info("Scheduler has been shutdown");  // Log that the scheduler is shutting down
        }
    }

    // Subscribe to elevator-related topics
    private void subscribeToElevatorTopics() {
        try {
            int totalElevators = elevatorSystem.getElevatorNum();

            // Subscribe to topics for each elevator
            for (int id = 0; id < totalElevators; id++) {
                elevatorAlgorithms.put(id, new ElevatorAlgorithm());

                // Define the topics to subscribe to
                String[] topics = {
                        "elevator/" + id + "/currentFloor",
                        "elevator/" + id + "/speed",
                        "elevator/" + id + "/weight",
                        "elevator/" + id + "/doorState",
                        "elevator/" + id + "/button/0",
                        "floor/" + id + "/buttonUp"
                };

                // Subscribe to each topic
                for (String topic : topics) {
                    Mqtt5Subscribe subscribeMessage = Mqtt5Subscribe.builder()
                            .topicFilter(topic)
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .build();

                    // Handle subscription completion
                    client.subscribe(subscribeMessage, this::handleMessage)
                            .whenComplete((subAck, throwable) -> {
                                if (throwable != null) {
                                    log.error("Failed to subscribe to topic {}: {}", topic, throwable.getMessage());
                                } else {
                                    log.info("Successfully subscribed to topic: {}", topic);
                                }
                            });
                }
            }
        } catch (RemoteException e) {
            log.error("Error retrieving elevator number: {}", e.getMessage(), e);
        }
    }

    // Method to handle incoming messages from MQTT topics
    private void handleMessage(Mqtt5Publish publish) {
        String topic = publish.getTopic().toString();
        String messagePayload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

        log.info("Received message on topic {}: {}", topic, messagePayload);

        try {
            // Process the message based on its topic
            if (topic.contains("elevator")) {
                handleElevatorMessage(topic, messagePayload);
            } else if (topic.contains("floor")) {
                handleFloorMessage(topic, messagePayload);
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
        }
    }

    // Handle elevator-related messages, such as current floor, speed, weight, and button state
    private void handleElevatorMessage(String topic, String payload) throws RemoteException {
        String[] parts = topic.split("/");
        int elevatorId = Integer.parseInt(parts[1]);

        ElevatorAlgorithm algorithm = elevatorAlgorithms.get(elevatorId);

        // Process each type of elevator-related message
        if (topic.contains("currentFloor")) {
            int currentFloor = Integer.parseInt(payload);
            log.info("Elevator {} is at floor {}", elevatorId, currentFloor);
            algorithm.setCurrentFloor(currentFloor);
            elevatorSystem.setTarget(elevatorId, currentFloor);
        } else if (topic.contains("speed")) {
            int speed = Integer.parseInt(payload);
            log.info("Elevator {} speed is {}", elevatorId, speed);
        } else if (topic.contains("weight")) {
            int weight = Integer.parseInt(payload);
            log.info("Elevator {} weight is {}", elevatorId, weight);
        } else if (topic.contains("doorState")) {
            String doorState = payload;
            log.info("Elevator {} door state is {}", elevatorId, doorState);
        } else if (topic.contains("button")) {
            int buttonId = Integer.parseInt(parts[3]);
            boolean buttonState = Boolean.parseBoolean(payload);
            log.info("Elevator {} button {} state is {}", elevatorId, buttonId, buttonState);
            if (buttonState) {
                algorithm.addRequest(buttonId);
                publishResponse(elevatorId, algorithm.getCommittedDirection(), buttonId);
            }
        }
    }

    // Handle floor-related messages, such as button up or down states
    private void handleFloorMessage(String topic, String payload) {
        String[] parts = topic.split("/");
        int floorId = Integer.parseInt(parts[1]);

        // Process floor button down/up states
        if (topic.contains("buttonDown")) {
            boolean buttonDown = Boolean.parseBoolean(payload);
            log.info("Floor {} button down state is {}", floorId, buttonDown);
        } else if (topic.contains("buttonUp")) {
            boolean buttonUp = Boolean.parseBoolean(payload);
            log.info("Floor {} button up state is {}", floorId, buttonUp);
        }
    }

    // Publish response messages, including committed direction and target floor
    private void publishResponse(int elevatorId, int committedDirection, int targetFloor) {
        String committedDirectionTopic = "elevator/" + elevatorId + "/committedDirection";
        String targetFloorTopic = "elevator/" + elevatorId + "/targetFloor";

        // Publish committed direction and target floor messages
        publishMessage(committedDirectionTopic, String.valueOf(committedDirection));
        publishMessage(targetFloorTopic, String.valueOf(targetFloor));
    }

    // Publish a message to a specific topic
    public void publishMessage(String topic, String message) {
        client.publishWith()
                .topic(topic)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish message to topic {}: {}", topic, throwable.getMessage());
                    } else {
                        log.info("Message successfully published to topic {}", topic);  // Log successful message publication
                    }
                });
    }

    //Reconnect after a delay
    private void scheduleReconnect() {
        scheduler.schedule(this::connectAndSubscribe, RECONNECT_DELAY, TimeUnit.SECONDS);  // Schedule reconnect after delay
    }
}
