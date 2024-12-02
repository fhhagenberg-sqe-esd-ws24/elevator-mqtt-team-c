package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ElevatorMQTTAdapter {
    private static final int TIMEOUT_DURATION = 10;
    private MqttClient client;
    private final ElevatorSystem elevatorSystem;
    private final ScheduledExecutorService scheduler;

    /**
     * Establishes connection between the MQTT Broker and the Elevator Data Model
     *
     * @param elevatorSystem Data Model for Elevator system
     * @param brokerUrl URL for MQTT BRoker
     * @param clientId Client ID for MQTT Broker
     */
    public ElevatorMQTTAdapter(ElevatorSystem elevatorSystem, String brokerUrl, String clientId) {
        this.elevatorSystem = elevatorSystem;
        try {
            this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        } catch (MqttException e) {
            System.err.println(e.getMessage());
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
            client.connect();
            System.out.println("Connected to MQTT broker.");
        } catch (MqttException e) {
            e.printStackTrace();
        }finally {
            scheduler.schedule(this::reconnect, TIMEOUT_DURATION, TimeUnit.SECONDS);
        }
    }

    /**
     *  Reconnect to MQTT Broker
     */
    public void reconnect(){
        try {
            if(!client.isConnected()){
                client.reconnect();
                System.out.println("Reconnected to MQTT broker.");
            }
        } catch (MqttException e) {
            System.err.println("Failed to reconnect to MQTT broker." + e.getMessage());
            try {
                TimeUnit.SECONDS.sleep(TIMEOUT_DURATION);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.err.println(ex.getMessage());
            }
        } finally {
            scheduler.schedule(this::reconnect, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Disconnect from MQTT Broker
     */
    private void disconnect(){
        try{
            if (client.isConnected()){
                client.disconnect();
                System.out.println("Disconnected from MQTT broker.");
            }
        } catch (MqttException e) {
            System.err.println("Failed to disconnect from MQTT broker." + e.getMessage());
        } finally{
            scheduler.shutdownNow();
        }
    }

    /**
     * Shutdown Hook to mitigate common issues when shutting down the client
     */
    public void addShutdownHook(){
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect) {});
    }

    /**
     * Initiate Publishing of Elevator States to MQTT Broker
     */
    private void startPublishingElevatorStates() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                elevatorSystem.updateAll();

                for (int i = 0; i < elevatorSystem.getElevatorNum() - 1; i++) {
                    publishElevatorState(i, elevatorSystem.getElevator(i));
                }

            } catch (Exception ex) {
                System.out.println(ex);
            }

        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Publishes Elevator States to MQTT Broker
     *
     * @param elevatorNumber Elevator ID
     * @param elevator Elevator Data Model
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
        try {
            client.publish(topic, message);
            System.out.println("Published: " + topic + " -> " + messageContent);
        } catch (MqttException e) {
            e.printStackTrace();}
    }
}