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

public class ElevatorMQTTAdapter {
    private MqttClient client;
    private Elevator elevator;
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

    private void startPublishingElevatorStates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                elevatorSystem.updateAll();
                for (int i = 0; i < (elevatorSystem.getTotalElevators()); i++) {
                    elevator = elevatorSystem.getElevator(i);
                    publishElevatorState(i, elevator);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void publishElevatorState(int elevatorNumber, Elevator elevator) {
        publish("elevator/" + elevatorNumber + "/currentFloor", String.valueOf(elevator.getCurrentFloor()));
        publish("elevator/" + elevatorNumber + "/targetedFloor", String.valueOf(elevator.getTargetedFloor()));
        publish("elevator/" + elevatorNumber + "/speed", String.valueOf(elevator.getCurrentSpeed()));
        publish("elevator/" + elevatorNumber + "/weight", String.valueOf(elevator.getCurrentWeight()));
        publish("elevator/" + elevatorNumber + "/doorState", String.valueOf(elevator.getElevatorDoorStatus()));

        for (Map.Entry<Integer, Boolean> entry : elevator.getServiceableFloors().entrySet()) {
            publish("elevator/" + elevatorNumber + "/serviceableFloors/" + entry.getKey(),
                    String.valueOf(entry.getValue()));
        }
    }

    private void publish(String topic, String messageContent) {
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        try {
            client.publish(topic, message);
            System.out.println("Published: " + topic + " -> " + messageContent);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}