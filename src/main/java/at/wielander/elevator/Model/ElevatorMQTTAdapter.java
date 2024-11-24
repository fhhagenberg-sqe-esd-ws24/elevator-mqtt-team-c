
package at.wielander.elevator.Model;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @class ElevatorMQTTAdapter
 * @brief This class is responsible for adapting the ElevatorSystem to
 *        communicate via MQTT.
 *
 *        The ElevatorMQTTAdapter class connects to an MQTT broker, subscribes
 *        to control topics, and publishes elevator states at regular intervals.
 *        It uses the Eclipse Paho MQTT client library for MQTT communication.
 *
 * @details
 *          - The constructor initializes the MQTT client, connects to the
 *          broker, subscribes to control topics, and starts publishing elevator
 *          states.
 *          - The connect() method connects the MQTT client to the broker.
 *          - The startPublishingElevatorStates() method publishes elevator
 *          states at regular intervals if there are changes.
 *          - The publish() method publishes messages to specific MQTT topics.
 *          - The subscribeToControlTopics() method subscribes to control topics
 *          to receive commands for setting targeted floors and committed
 *          directions.
 *          - The start() method reconnects to the broker, subscribes to control
 *          topics, and starts publishing elevator states.
 *
 * @note The polling interval for publishing elevator states is specified in
 *       milliseconds.
 *
 * @see ElevatorSystem
 * @see Elevator
 */
public class ElevatorMQTTAdapter {
    private MqttClient client;
    private ElevatorSystem elevatorSystem;
    private ScheduledExecutorService scheduler;
    private int pollingInterval;

    /**
     * Constructs an ElevatorMQTTAdapter with the specified ElevatorSystem, broker
     * URL, client ID, and polling interval.
     *
     * @param elevatorSystem  The ElevatorSystem instance to be monitored and
     *                        controlled.
     * @param brokerUrl       The URL of the MQTT broker to connect to.
     * @param clientId        The client ID to use when connecting to the MQTT
     *                        broker.
     * @param pollingInterval The interval in milliseconds at which elevator states
     *                        are polled and published.
     */
    public ElevatorMQTTAdapter(ElevatorSystem elevatorSystem, String brokerUrl, String clientId, int pollingInterval) {
        this.elevatorSystem = elevatorSystem;
        this.pollingInterval = pollingInterval; // in milliseconds
        try {
            this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
        this.scheduler = Executors.newScheduledThreadPool(1);
        connect();
        subscribeToControlTopics();
        startPublishingElevatorStates();
    }

    /**
     * @brief Connects to the MQTT broker.
     *
     *        This function attempts to connect to the MQTT broker using the
     *        provided broker URL and client ID.
     *        If the connection is successful, a message is printed to the console.
     *        If the connection fails, the exception stack trace is printed.
     */
    public void connect() {
        try {
            client.connect();
            System.out.println("Connected to MQTT broker.");
        } catch (MqttException e) {
            e.printStackTrace();
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
                    for (Map.Entry<Integer, Boolean> entry : elevator.getServiceableFloors().entrySet()) {
                        if (!entry.getValue().equals(previousElevator.getServiceableFloors().get(entry.getKey()))) {
                            publish("elevator/" + i + "/serviceableFloors/" + entry.getKey(),
                                    String.valueOf(entry.getValue()));
                        }
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
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        // use a retained message in order to not loose the notification
        message.setRetained(true); // Set the message as retained
        try {
            client.publish(topic, message);
            System.out.println("Published: " + topic + " -> " + messageContent);
        } catch (MqttException e) {
            e.printStackTrace();
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
            client.subscribe("elevator/+/setTargetedFloor", (topic, message) -> {
                String[] topicLevels = topic.split("/");
                int elevatorNumber = Integer.parseInt(topicLevels[1]);
                int targetFloor = Integer.parseInt(new String(message.getPayload()));
                try {
                    elevatorSystem.getElevator(elevatorNumber).setTargetedFloor(targetFloor);
                    System.out.println("Set elevator " + elevatorNumber + " target floor to " + targetFloor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            client.subscribe("elevator/+/setCommittedDirection", (topic, message) -> {
                String[] topicLevels = topic.split("/");
                int elevatorNumber = Integer.parseInt(topicLevels[1]);
                int direction = Integer.parseInt(new String(message.getPayload()));
                try {
                    elevatorSystem.getElevator(elevatorNumber).setCommittedDirection(direction);
                    System.out.println("Set elevator " + elevatorNumber + " committed direction to " + direction);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            System.out.println("Subscribed to control topics.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief Starts the ElevatorMQTTAdapter.
     *
     *        This function reconnects to the MQTT broker, subscribes to control
     *        topics, and starts publishing elevator states.
     */
    public void start() {
        connect();
        subscribeToControlTopics();
        startPublishingElevatorStates();
    }
}