package at.wielander.elevator.Model;

import com.hivemq.client.internal.mqtt.message.MqttMessage;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.Mqtt3Message;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Custom Exception for handling MQTT Adapter errors.
 */
class MQTTAdapterException extends RuntimeException {
    public MQTTAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}


public class ElevatorMQTTAdapter {
    private static final String topicBase = "building/";
    public static final String infoTopic = topicBase + "info/";
    private static final String statusTopic = topicBase + "status/";
    private static final String controlTopic = topicBase + "control/";
    public static final String infoElevatorTopic = infoTopic + "elevator/";
    public static final String statusElevatorTopic = statusTopic + "elevator/";
    public static final String controlElevatorTopic = controlTopic + "elevator/";
    public static final String statusFloorTopic = statusTopic + "floor/";
    private static final int TIMEOUT_DURATION = 10;
    private Mqtt5AsyncClient client;
    private ElevatorSystem elevatorSystem;
    private final ScheduledExecutorService scheduler;
    private int pollingInterval;
    private  ElevatorSystem previousElevatorSystem;

    /**
     * Establishes connection between the MQTT Broker and the Elevator Data Model
     *
     * @param elevatorSystem Data Model for Elevator system
     * @param brokerUrl      URL for MQTT Broker
     * @param clientId       Client ID for MQTT Broker
     */
    public ElevatorMQTTAdapter(ElevatorSystem elevatorSystem, String brokerUrl, String clientId, int pollingInterval) {
        this.elevatorSystem = elevatorSystem;
        this.previousElevatorSystem = elevatorSystem;
        this.pollingInterval = pollingInterval;
        
        try {
        String[] urlParts = brokerUrl.replace("tcp://", "").split(":");
        String host = urlParts[0];
        int port = urlParts.length > 1 ? Integer.parseInt(urlParts[1]) : 1883; // Standard-Port 1883 für MQTT

        this.client = Mqtt5Client.builder() // Use Mqtt5Client instead of MqttClient
                .serverHost(host)
                .serverPort(port)
                .identifier(clientId)
                .buildAsync();

        this.scheduler = Executors.newScheduledThreadPool(1);
        } catch (Exception e) {
            throw new MQTTAdapterException("Failed to initialize MQTT client.", e);
        }
    }

    /**
     * Establish connection to MQTT Broker
     */
    public void connect() {
    	 try {
	        client.connectWith()
	                .cleanStart(true)
	                .send()
	                .whenComplete((connAck, throwable) -> {
	                    if (throwable != null) {
	                        System.err.println("Connection failed: " + throwable.getMessage());
	                    } else {
	                        System.out.println("Connected to MQTT broker: " + connAck.getType());
	                    }
	                });
    	 } catch (Exception e) {
             throw new MQTTAdapterException("Error during MQTT client connection.", e);
         }
    }
    
    private void handleConnectionError(Throwable throwable) {
        System.err.println("Connection failed: " + throwable.getMessage());
        scheduler.schedule(this::reconnect, TIMEOUT_DURATION, TimeUnit.SECONDS);
    }

    /**
     * Reconnect to MQTT Broker
     */
    public void reconnect() {
        if (client != null && !client.getState().isConnected()) {
        	 try {
            client.toAsync().connect()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                        	handleConnectionError(throwable);
                        } else {
                            System.out.println("Reconnected to MQTT broker.");
                        }
                    });
        	  } catch (Exception e) {
                  throw new MQTTAdapterException("Error during MQTT client reconnection.", e);
              }
        }
    }

    /**
     * Disconnect from MQTT Broker
     */
    private void disconnect() {
    	try {
        client.disconnect()
                .whenComplete((ack, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Failed to disconnect: " + throwable.getMessage());
                    } else {
                        System.out.println("Disconnected from MQTT broker.");
                    }
                });
    	 } catch (Exception e) {
             throw new MQTTAdapterException("Error during MQTT client disconnection.", e);
         }
    }
   
    /**
     * @brief Starts publishing elevator states at regular intervals.
     *
     *        This function starts a scheduled task that polls the elevator states
     *        at regular intervals and publishes the states to the MQTT broker.
     *        If there are changes in the elevator states, the new states are
     *        published.
     */
    private void startPublishingElevatorStates() {
        if (client.getState() != MqttClientState.CONNECTED) {
            throw new RuntimeException("MQTT client must be connected before publishing messages");
        }

       
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Updates all elevators
                elevatorSystem.updateAll();

                for (int i = 0; i < elevatorSystem.getTotalElevators(); i++) {
                    Elevator previousElevator = previousElevatorSystem.getElevator(i);
                    Elevator elevator = elevatorSystem.getElevator(i);

                    if (elevator.getCurrentFloor() != previousElevator.getCurrentFloor()) {
                        publish("elevator/" + i + "/currentFloor", String.valueOf(elevator.getCurrentFloor()));
                    }
                    if (elevator.getTargetedFloor() != previousElevator.getTargetedFloor()) {
                        publish("elevator/" + i + "/targetedFloor", String.valueOf(elevator.getTargetedFloor()));
                    }
                    if (elevator.getCurrentSpeed() != previousElevator.getCurrentSpeed()) {
                        publish("elevator/" + i + "/speed", String.valueOf(elevator.getCurrentSpeed()));
                    }
                    if (elevator.getCurrentWeight() != previousElevator.getCurrentWeight()) {
                        publish("elevator/" + i + "/weight", String.valueOf(elevator.getCurrentWeight()));
                    }
                    if (elevator.getElevatorDoorStatus() != previousElevator.getElevatorDoorStatus()) {
                        publish("elevator/" + i + "/doorState", String.valueOf(elevator.getElevatorDoorStatus()));
                    }
                }

                // Update the previous state for the next comparison
                previousElevatorSystem = elevatorSystem;
            } catch (Exception e) {
                throw new RuntimeException("Error while publishing elevator states", e);
            }

        }, 0, pollingInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * @brief Publishes a message to the specified MQTT topic.
     *
     *        This function publishes a message to the specified MQTT topic with the
     *        given message content.
     *        The message is set as retained to ensure that the notification is not
     *        lost.
     *
     * @param topic          The MQTT topic to publish the message to.
     * @param messageContent The content of the message to be published.
     */
    private void publish(String topic, String payload) {
        try {
            Mqtt5Publish publishMessage = Mqtt5Publish.builder()
                    .topic(topic)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .retain(false) // Nachrichten werden nicht retained
                    .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                    .build();

            client.publish(publishMessage).get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MQTTAdapterException("Publishing interrupted for topic: " + topic, e);
        } catch (Exception e) {
            throw new MQTTAdapterException("Failed to publish message to topic: " + topic, e);
        }
    }

    /**
     * @brief Subscribes to control topics for setting targeted floors and committed
     *        directions.
     *
     *        This function subscribes to the control topics for setting targeted
     *        floors and committed directions.
     *        When a message is received on these topics, the corresponding elevator
     *        is updated with the new targeted floor or committed direction.
     */
    private void subscribeToControlTopics() {
        try {
            for (int id = 0; id < elevatorSystem.getElevatorNum(); id++) {
                // Subscribe to the committed direction control topic
                subscribe(controlElevatorTopic + id + "/committedDirection");

                // Subscribe to the target floor control topic
                subscribe(controlElevatorTopic + id + "/targetFloor");

                // Subscribe to the floor services control topics
                for (int num = 0; num < elevatorSystem.getNumberOfFloors(); num++) {
                    subscribe(controlElevatorTopic + id + "/floorService/" + num);
                }
                // Set callback to handle incoming messages
                client.toAsync().publishes(MqttGlobalPublishFilter.ALL, this::handleIncomingMessage);
           
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while subscribing to control topics", e);
        }
    }
    
    // todo implement code to set the received values on the plc
    private void handleIncomingMessage(Mqtt5Publish publish) {
        String topic = publish.getTopic().toString();
        String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

        System.out.println("Received message on topic: " + topic + " with payload: " + payload);

        try {
            String[] parts = topic.split("/");

            if (topic.contains("committedDirection")) {
                int elevatorNumber = Integer.parseInt(parts[1]); // Assuming topic structure includes elevator ID as the second part
                int committedDirection = Integer.parseInt(payload);
                System.out.println("Elevator " + elevatorNumber + " committed direction: " + committedDirection);
                // Add logic to handle committed direction
            } else if (topic.contains("targetFloor")) {
                int elevatorNumber = Integer.parseInt(parts[1]);
                int targetFloor = Integer.parseInt(payload);
                System.out.println("Elevator " + elevatorNumber + " target floor: " + targetFloor);
                // Add logic to handle target floor
            } else if (topic.contains("floorService")) {
                int elevatorNumber = Integer.parseInt(parts[1]);
                int floorNumber = Integer.parseInt(parts[3]); // Assuming topic structure includes floor number as the fourth part
                boolean floorService = Boolean.parseBoolean(payload);
                System.out.println("Elevator " + elevatorNumber + " floor " + floorNumber + " service: " + floorService);
                // Add logic to handle floor service
            }
        } catch (Exception e) {
            System.err.println("Failed to process message on topic: " + topic + " - Error: " + e.getMessage());
        }
    }

    private void subscribe(String topic) {
        try {
            client.subscribeWith()
                  .topicFilter(topic)
                  .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                  .send()
                  .get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MQTTAdapterException("Subscription interrupted for topic: " + topic, e);
        } catch (Exception e) {
            throw new MQTTAdapterException("Failed to subscribe to topic: " + topic, e);
        }
    }

    /**
     * Connects to broker, subscribes to all control topics,
     * publishes all retained topics and runs the update loop.
     * 
     * @throws MqttError
     */
    public void run() {
    	try {
        // Überprüfen, ob der MQTT-Client verbunden ist
        if (client.getState() != MqttClientState.CONNECTED) {
            System.err.println("MQTT client is not connected.");
            return; // Beende die Methode, wenn der MQTT-Client nicht verbunden ist
        }
        
        subscribeToControlTopics();

        // Beide Verbindungen sind in Ordnung, also publish die retained topics
        publishRetainedTopics();
        
        // start the scheduler
        startPublishingElevatorStates();
    	 } catch (Exception e) {
             throw new MQTTAdapterException("Error during MQTT adapter operation.", e);
         }
    }

    /**
     * Publishes the retained (static) building information topics
     * 
     * @throws MqttError
     */
    public void publishRetainedTopics() {
        String payload;
        // Sicherstellen, dass der Client verbunden ist
        if (client.getState() != MqttClientState.CONNECTED) {
            throw new RuntimeException("MQTT client must be connected before publishing messages");
        }

        try {
            // Anzahl der Aufzüge
            payload = String.valueOf(elevatorSystem.getElevatorNum());
            Mqtt5Publish publishMessage = Mqtt5Publish.builder()
                    .topic(infoTopic + "numberOfElevators")
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .retain(true) // Nachricht als retained markieren
                    .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                    .build();
            client.publish(publishMessage).get(100, TimeUnit.MILLISECONDS);

            // Anzahl der Stockwerke
            payload = String.valueOf(elevatorSystem.getNumberOfFloors());
            publishMessage = Mqtt5Publish.builder()
                    .topic(infoTopic + "numberOfFloors")
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .retain(true)
                    .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                    .build();
            client.publish(publishMessage).get(100, TimeUnit.MILLISECONDS);

            // Stockwerkhöhe in Fuß
            payload = String.valueOf(elevatorSystem.getFloorHeight());
            publishMessage = Mqtt5Publish.builder()
                    .topic(infoTopic + "floorHeight/feet")
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .retain(true)
                    .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                    .build();
            client.publish(publishMessage).get(100, TimeUnit.MILLISECONDS);

            // Systemtakt
            payload = String.valueOf(elevatorSystem.getClockTick());
            publishMessage = Mqtt5Publish.builder()
                    .topic(infoTopic + "systemClockTick")
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .retain(true)
                    .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                    .build();
            client.publish(publishMessage).get(100, TimeUnit.MILLISECONDS);

            // RMI-Verbindungsstatus
            payload = String.valueOf(true);
            publishMessage = Mqtt5Publish.builder()
                    .topic(infoTopic + "rmiConnected")
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .retain(true)
                    .qos(MqttQos.AT_LEAST_ONCE) // QoS Level 1 (AT_LEAST_ONCE)
                    .build();
            client.publish(publishMessage).get(100, TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Thread-Unterbrechung wiederherstellen
            throw new RuntimeException("Thread was interrupted while publishing retained topics", e);
        } catch (Exception exc) {
            throw new RuntimeException("Error while publishing retained topics: " + exc.getMessage(), exc);
        }
    }
}