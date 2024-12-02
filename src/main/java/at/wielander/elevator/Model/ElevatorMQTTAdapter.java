package at.wielander.elevator.Model;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ElevatorMQTTAdapter implements MqttCallback {
    private static final int TIMEOUT_DURATION = 5;
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
            this.client.setCallback(this);
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
            System.err.println("Failed to connect to MQTT broker." + e.getMessage());
        } finally {
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
            scheduler.schedule(this::reconnect, 5, TimeUnit.SECONDS);
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

            } catch (RemoteException e) {
                System.err.println("Failed to publish elevator states to MQTT broker." + e.getMessage());
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
        message.setQos(1);
        try {
            client.publish(topic, message);
            System.out.println("Published: " + topic + " -> " + messageContent);
        } catch (MqttException e) {
            System.err.println("Failed to publish to MQTT broker." + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause){
        System.err.println("Connection to Lost! Reconnecting to MQTT ");
        scheduler.schedule(this::reconnect, 5, TimeUnit.SECONDS);
    }

    /**
     * This method is called when a message arrives from the server.
     *
     * <p>
     * This method is invoked synchronously by the MQTT client. An
     * acknowledgment is not sent back to the server until this
     * method returns cleanly.</p>
     * <p>
     * If an implementation of this method throws an <code>Exception</code>, then the
     * client will be shut down.  When the client is next re-connected, any QoS
     * 1 or 2 messages will be redelivered by the server.</p>
     * <p>
     * Any additional messages which arrive while an
     * implementation of this method is running, will build up in memory, and
     * will then back up on the network.</p>
     * <p>
     * If an application needs to persist data, then it
     * should ensure the data is persisted prior to returning from this method, as
     * after returning from this method, the message is considered to have been
     * delivered, and will not be reproducible.</p>
     * <p>
     * It is possible to send a new message within an implementation of this callback
     * (for example, a response to this message), but the implementation must not
     * disconnect the client, as it will be impossible to send an acknowledgment for
     * the message being processed, and a deadlock will occur.</p>
     *
     * @param topic   name of the topic on the message was published to
     * @param message the actual message.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String messageContent = new String(message.getPayload());
            System.out.println("Received: " + topic + " -> " + messageContent);

            if(topic.startsWith("elevator/")){
                String[] segments = topic.split("/");
                if (segments.length >= 3){
                    int elevatorID = Integer.parseInt(segments[1]);
                    String parameter = segments[2];

                }
            }

            publish(topic, messageContent);
        } catch (RuntimeException e) {
            System.err.println("Failed to process message." + e.getMessage());
        }


    }

    /**
     * Called when delivery for a message has been completed, and all
     * acknowledgments have been received. For QoS 0 messages it is
     * called once the message has been handed to the network for
     * delivery. For QoS 1 it is called when PUBACK is received and
     * for QoS 2 when PUBCOMP is received. The token will be the same
     * token as that returned when the message was published.
     *
     * @param token the delivery token associated with the message.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}