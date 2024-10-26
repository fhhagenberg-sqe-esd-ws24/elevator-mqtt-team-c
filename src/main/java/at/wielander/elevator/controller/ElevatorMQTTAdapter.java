package at.wielander.elevator.controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class ElevatorMQTTAdapter {

    private MqttClient client;

    public ElevatorMQTTAdapter(String brokerUrl, String clientId) {
        try {
            // Verwende MemoryPersistence anstelle der Dateispeicherung
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            client.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishElevatorState(int elevatorId, int position) {
        String topic = "elevator/" + elevatorId + "/position";
        String messageContent = String.valueOf(position);
        MqttMessage message = new MqttMessage(messageContent.getBytes());
        try {
            client.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
