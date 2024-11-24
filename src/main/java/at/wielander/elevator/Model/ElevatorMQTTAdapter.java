/*
 * 
 * On start, connect to the IElevator RMI interface of the elevator PLC [configurable].
On start, connect to an MQTT broker [configurable].
Poll the elevator state via its RMI interface every 250 ms [configurable].
Whenever a value changes, publishe the value via MQTT.
Subscribe to a topic to enable setting the target floor and the committed direction of elevators via MQTT messages.
Whenever the corresponding "control" message is delivered, pass it to the elevator PLC via its RMI interface (e.g. call setTargetFloor(...))
 */

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
 * This class is responsible for adapting the ElevatorSystem to communicate via
 * MQTT.
 * It updates the elevator system's elevators every 250 milliseconds. If changes
 * to
 * elevator values occur, they are published via MQTT to specific topics.
 */
public class ElevatorMQTTAdapter {
    private MqttClient client;
    private ElevatorSystem elevatorSystem;
    private ScheduledExecutorService scheduler;

    public ElevatorMQTTAdapter(ElevatorSystem elevatorSystem, String brokerUrl, String clientId) {
        this.elevatorSystem = elevatorSystem;

        try {
            this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
        this.scheduler = Executors.newScheduledThreadPool(1);
        connect();
        startPublishingElevatorStates();
    }

    public void connect() {
        try {
            client.connect();
            System.out.println("Connected to MQTT broker.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * this functions updates the elevatorSystems elevators each 250 ms. If changes
     * to elevator values occur, they are published via mqtt to specific toppic
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

        }, 0, 250, TimeUnit.MILLISECONDS);
    }

    // private void publishElevatorState(int elevatorNumber, Elevator elevator) {
    // publish("elevator/" + elevatorNumber + "/currentFloor",
    // String.valueOf(elevator.getCurrentFloor()));
    // publish("elevator/" + elevatorNumber + "/targetedFloor",
    // String.valueOf(elevator.getTargetedFloor()));
    // publish("elevator/" + elevatorNumber + "/speed",
    // String.valueOf(elevator.getCurrentSpeed()));
    // publish("elevator/" + elevatorNumber + "/weight",
    // String.valueOf(elevator.getCurrentWeight()));
    // publish("elevator/" + elevatorNumber + "/doorState",
    // String.valueOf(elevator.getElevatorDoorStatus()));

    // for (Map.Entry<Integer, Boolean> entry :
    // elevator.getServiceableFloors().entrySet()) {
    // publish("elevator/" + elevatorNumber + "/serviceableFloors/" +
    // entry.getKey(),
    // String.valueOf(entry.getValue()));
    // }
    // }

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

    public void start() {
        connect();
        subscribeToControlTopics();
        startPublishingElevatorStates();
    }
}