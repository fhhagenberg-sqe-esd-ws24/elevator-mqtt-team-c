package at.wielander.elevator.View;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Dashboard implements MqttCallback {

    private MqttClient client;

    public Dashboard(String brokerUrl, String clientId) {
        try {
            // Verwende MemoryPersistence
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            client.setCallback(this);  // Setze das Callback-Interface
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

    public void subscribeToElevatorState() {
        try {
            client.subscribe("elevator/#");
            System.out.println("Subscribed to elevator topics.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Message arrived: " + topic + " -> " + new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Kann leer gelassen werden, wenn nicht benötigt
    }
}
