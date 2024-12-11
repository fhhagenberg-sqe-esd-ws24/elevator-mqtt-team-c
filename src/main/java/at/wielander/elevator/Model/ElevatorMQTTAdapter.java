package at.wielander.elevator.Model;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ElevatorMQTTAdapter {
    private static final int TIMEOUT_DURATION = 10;
    private MqttAsyncClient client;
    private final ElevatorSystem elevatorSystem;
    private final ScheduledExecutorService scheduler;

    /**
     * Establishes connection between the MQTT Broker and the Elevator Data Model
     *
     * @param elevatorSystem Data Model for Elevator system
     * @param brokerUrl      URL for MQTT Broker
     * @param clientId       Client ID for MQTT Broker
     */
    public ElevatorMQTTAdapter(ElevatorSystem elevatorSystem, String brokerUrl, String clientId) {
        this.elevatorSystem = elevatorSystem;
        try {
            this.client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            System.err.println("Error initializing MQTT client: " + e.getMessage());
        }
        this.scheduler = Executors.newScheduledThreadPool(1);
        connect();
        startPublishingElevatorStates();
        addShutdownHook();
    }

    /**
     * Establish connection to MQTT Broker
     */
    public void connect() {
        try {
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    System.err.println("Disconnected: " + disconnectResponse.getReasonString());
                    reconnect();
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {
                    System.err.println("MQTT Error: " + exception.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.println("Message arrived: " + topic + " -> " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttToken token) {
                    System.out.println("Delivery complete for: " + token.getTopics()[0]);
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println((reconnect ? "Reconnected" : "Connected") + " to MQTT broker: " + serverURI);
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties properties) {
                    System.out.println("Auth packet arrived. Reason code: " + reasonCode);
                }
            });

            client.connect(options);
        } catch (MqttException e) {
            System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
        }
    }

    /**
     * Reconnect to MQTT Broker
     */
    public void reconnect() {
        try {
            if (!client.isConnected()) {
                client.reconnect();
                System.out.println("Reconnected to MQTT broker.");
            }
        } catch (MqttException e) {
            System.err.println("Failed to reconnect to MQTT broker: " + e.getMessage());
            scheduler.schedule(this::reconnect, TIMEOUT_DURATION, TimeUnit.SECONDS);
        }
    }

    /**
     * Disconnect from MQTT Broker
     */
    private void disconnect() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                System.out.println("Disconnected from MQTT broker.");
            }
        } catch (MqttException e) {
            System.err.println("Failed to disconnect from MQTT broker: " + e.getMessage());
        } finally {
            scheduler.shutdownNow();
        }
    }

    /**
     * Shutdown Hook to mitigate common issues when shutting down the client
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));
    }

    /**
     * Initiate Publishing of Elevator States to MQTT Broker
     */
    private void startPublishingElevatorStates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                elevatorSystem.updateAll();

                for (int i = 0; i < elevatorSystem.getElevatorNum(); i++) {
                    publishElevatorState(i, elevatorSystem.getElevator(i));
                }

            } catch (Exception ex) {
                System.err.println("Error while publishing elevator states: " + ex.getMessage());
            }

        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Publishes Elevator States to MQTT Broker
     *
     * @param elevatorNumber Elevator ID
     * @param elevator       Elevator Data Model
     */
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
        message.setQos(1); // QOS 1 ensures at least once delivery
        try {
            client.publish(topic, message);
            System.out.println("Published: " + topic + " -> " + messageContent);
        } catch (MqttException e) {
            System.err.println("Failed to publish message: " + e.getMessage());
        }
    }
}
