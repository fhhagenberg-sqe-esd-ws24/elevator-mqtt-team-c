package at.wielander.elevator.controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ElevatorMQTTAdapter {
    private MqttClient client;

    public ElevatorMQTTAdapter(String brokerUrl, String clientId) {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            client.connect();
            System.out.println("Connected to MQTT broker.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishElevatorState(int elevatorNumber, int currentFloor) {
        String topic = "elevator/" + elevatorNumber + "/state";
        String messageContent = "Current floor: " + currentFloor;
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        try {
            client.publish(topic, message);
            System.out.println("Published: " + topic + " -> " + messageContent);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}