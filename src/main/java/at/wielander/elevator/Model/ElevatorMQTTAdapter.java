package at.wielander.elevator.Model;

import com.hivemq.client.internal.mqtt.message.MqttMessage;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
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
    private Mqtt5AsyncClient  client;
    private ElevatorSystem elevatorSystem;
    private final ScheduledExecutorService scheduler;
    private int pollingInterval;
    /**
     * Establishes connection between the MQTT Broker and the Elevator Data Model
     *
     * @param elevatorSystem Data Model for Elevator system
     * @param brokerUrl      URL for MQTT Broker
     * @param clientId       Client ID for MQTT Broker
     */
    public ElevatorMQTTAdapter(ElevatorSystem elevatorSystem, String brokerUrl, String clientId, int pollingInterval) {
        this.elevatorSystem = elevatorSystem;
        this.pollingInterval = pollingInterval;

        String[] urlParts = brokerUrl.replace("tcp://", "").split(":");
        String host = urlParts[0];
        int port = urlParts.length > 1 ? Integer.parseInt(urlParts[1]) : 1883; // Standard-Port 1883 für MQTT

        this.client = Mqtt5Client.builder()  // Use Mqtt5Client instead of MqttClient
                .serverHost(host)
                .serverPort(port)
                .identifier(clientId)
                .buildAsync();

       this.scheduler = Executors.newScheduledThreadPool(1);

        //connect();
       // subscribeToControlTopics();
        //startPublishingElevatorStates();
    }

    /**
     * Establish connection to MQTT Broker
     */
    public void connect() {
        client.connectWith()
              .cleanStart(true)  // MQTT5 equivalent of cleanSession
              .send()
              .whenComplete((connAck, throwable) -> {
                  if (throwable != null) {
                      System.err.println("Connection failed: " + throwable.getMessage());
                  } else {
                      System.out.println("Connected to MQTT broker: " + connAck.getType());
                  }
              });
    }
    

    /**
     * Reconnect to MQTT Broker
     */
	public void reconnect() {
	    if (client != null && !client.getState().isConnected()) {
	        client.toAsync().connect()
	                .whenComplete((connAck, throwable) -> {
	                    if (throwable != null) {
	                        System.err.println("Failed to reconnect to MQTT broker: " + throwable.getMessage());
	                        scheduler.schedule(this::reconnect, TIMEOUT_DURATION, TimeUnit.SECONDS);
	                    } else {
	                        System.out.println("Reconnected to MQTT broker.");
	                    }
	                });
	    }
	}

    /**
     * Disconnect from MQTT Broker
     */
    private void disconnect() {
        client.disconnect()
              .whenComplete((ack, throwable) -> {
                  if (throwable != null) {
                      System.err.println("Failed to disconnect: " + throwable.getMessage());
                  } else {
                      System.out.println("Disconnected from MQTT broker.");
                  }
              });
    }

    /**
     * Shutdown Hook to mitigate common issues when shutting down the client
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));
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
        ElevatorSystem previousElevatorSystem = elevatorSystem;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                elevatorSystem.updateAll();
                for (int i = 0; i < (elevatorSystem.getTotalElevators()); i++) {
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
            } catch (Exception e) {
                throw new RuntimeException(e);
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
    private void publish(String topic, String messageContent) {
        client.publishWith()
              .topic(topic)
              .payload(messageContent.getBytes(StandardCharsets.UTF_8))
              .retain(true)
              .send()
              .whenComplete((ack, throwable) -> {
                  if (throwable != null) {
                      System.err.println("Publish failed: " + throwable.getMessage());
                  } else {
                      System.out.println("Published: " + topic + " -> " + messageContent);
                  }
              });
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
        client.subscribeWith()
              .topicFilter("elevator/+/setTargetedFloor")
              .callback(publish -> {
                  String topic = publish.getTopic().toString();
                  String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                  handleSetTargetedFloor(topic, payload);
              })
              .send();

        client.subscribeWith()
              .topicFilter("elevator/+/setCommittedDirection")
              .callback(publish -> {
                  String topic = publish.getTopic().toString();
                  String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                  handleSetCommittedDirection(topic, payload);
              })
              .send();

        System.out.println("Subscribed to control topics.");
    }
    
    
    private void handleSetTargetedFloor(String topic, String payload) {
        String[] topicLevels = topic.split("/");
        int elevatorNumber = Integer.parseInt(topicLevels[1]);
        int targetFloor = Integer.parseInt(payload);
        elevatorSystem.getElevator(elevatorNumber).setTargetedFloor(targetFloor);
        System.out.println("Set elevator " + elevatorNumber + " target floor to " + targetFloor);
    }

    private void handleSetCommittedDirection(String topic, String payload) {
        String[] topicLevels = topic.split("/");
        int elevatorNumber = Integer.parseInt(topicLevels[1]);
        int direction = Integer.parseInt(payload);
        elevatorSystem.getElevator(elevatorNumber).setCommittedDirection(direction);
        System.out.println("Set elevator " + elevatorNumber + " committed direction to " + direction);
    }
    
    /**
     * Connects to broker, subscribes to all control topics,  
     * publishes all retained topics and runs the update loop. 
     * @throws MqttError     
     */
    public void run() {
        // Überprüfen, ob der MQTT-Client verbunden ist
        if (client.getState() != MqttClientState.CONNECTED) {
            System.err.println("MQTT client is not connected.");
            return; // Beende die Methode, wenn der MQTT-Client nicht verbunden ist
        }


        // Beide Verbindungen sind in Ordnung, also publish die retained topics
        publishRetainedTopics();
    }
    
    /** 
     * Publishes the retained (static) building information topics 
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
                    .retain(true)  // Nachricht als retained markieren
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

        } catch (Exception exc) {
            throw new RuntimeException("Error while publishing retained topics: " + exc.getMessage(), exc);
        }
    }
}
