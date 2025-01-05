package at.wielander.elevator.Algorithm;

import at.wielander.elevator.MQTT.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import sqelevator.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ElevatorAlgorithm {

    private Map<String, String> retainedMessages = new HashMap<>();
    private Map<String, String> liveMessages = new HashMap<>();
    private Mqtt5AsyncClient mqttClient; // MQTT Client instance variable
    private ElevatorMQTTAdapter eMQTTAdapter; // Adapter instance variable
    private ElevatorSystem eSystem;
    private IElevator controller;
    private static Properties properties;

    public static void main(String[] args) throws InterruptedException {
        ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        String brokerHost = "tcp://localhost:1883";
        System.out.println("Connecting to MQTT Broker at: " + brokerHost);

        try {
            // Initialize system and establish connections
            algorithm.initialiseSystem(brokerHost);

            // Run the elevator control algorithm
            algorithm.runAlgorithm(algorithm);

        } catch (Exception e) {
            e.printStackTrace();
            if (algorithm.eMQTTAdapter != null) {
                algorithm.eMQTTAdapter.disconnect();
            }
        }
    }

    private void initialiseSystem(String brokerHost) {
        reconnectRMI(); // Ensure RMI connection

        try {
            // Elevator System configuration
            eSystem = new ElevatorSystem(
                    1, 0, 10, 1000, 10, controller
            );

            // Create and connect the MQTT Adapter
            eMQTTAdapter = new ElevatorMQTTAdapter(
                    eSystem, brokerHost, "mqttAdapter", 250, controller
            );
            eMQTTAdapter.connect();

            // Initialize MQTT client
            mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost("localhost")
                    .serverPort(1883)
                    .identifier("ElevatorAlgorithmClient")
                    .buildAsync();

            // Subscribe to relevant topics
            subscribeToTopics();

        } catch (RemoteException | InterruptedException e) {
            System.err.println("Failed to initialize system: " + e.getMessage());
        }
    }

    private void reconnectRMI() {
        while (true) {
            try {
                properties = new Properties();
                String plcUrl = properties.getProperty("plc.url", "rmi://localhost/ElevatorSim");

                // Attempt to connect to the RMI API
                controller = (IElevator) Naming.lookup(plcUrl);
                System.out.println("Connected to RMI API");
                break;
            } catch (Exception e) {
                System.err.println("Failed to connect to RMI API. Retrying in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void subscribeToTopics() {
        // Subscribe to building info topics
        String topicFilter = "building/info/#";
        mqttClient.subscribeWith()
                .topicFilter(topicFilter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();

        // Subscribe to elevator-specific topics
        for (int elevatorId = 0; elevatorId < 2; elevatorId++) {
            mqttClient.subscribeWith().topicFilter("elevator/" + elevatorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
        }

        // Subscribe to floor-specific topics
        for (int floorId = 0; floorId < 4; floorId++) {
            mqttClient.subscribeWith().topicFilter("floor/" + floorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
        }

        // Process incoming MQTT messages
        mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
            String topic = publish.getTopic().toString();
            String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

            if (topic.startsWith("elevator/") || topic.startsWith("floor/")) {
                liveMessages.put(topic, payload);
                System.out.println("Live message received: " + topic + " -> " + payload);
            } else if (topic.startsWith("building/info")) {
                retainedMessages.put(topic, payload);
                System.out.println("Retained message received: " + topic + " -> " + payload);
            }
        });
    }

    public void runAlgorithm(ElevatorAlgorithm algorithm) throws InterruptedException {
        // Wait for the number of floors to be available in retained messages
        while (!retainedMessages.containsKey("building/info/numberOfFloors")) {
            System.out.println("Waiting for retained message: building/info/numberOfFloors");
            Thread.sleep(1000);
        }

        final int numberOfFloors = Integer.parseInt(retainedMessages.get("building/info/numberOfFloors")); // Number of floors
        final int elevator = 0;
        final int sleepTime = 10;

        // Set committed direction to UP and publish to MQTT
        String directionTopic = "elevator/" + elevator + "/committedDirection";
        algorithm.mqttClient.publishWith()
                .topic(directionTopic)
                .payload("1".getBytes(StandardCharsets.UTF_8)) // 1 for UP
                .send();

        // Move from ground floor to the top floor, stopping at each floor
        for (int nextFloor = 1; nextFloor < numberOfFloors; nextFloor++) {
            String targetTopic = "elevator/" + elevator + "/targetFloor";
            algorithm.mqttClient.publishWith()
                    .topic(targetTopic)
                    .payload(Integer.toString(nextFloor).getBytes(StandardCharsets.UTF_8))
                    .send();

            while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "-1")) < nextFloor
                    || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {
                Thread.sleep(sleepTime);
            }

            while (!"1".equals(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/doorState", ""))) {
                Thread.sleep(sleepTime);
            }
        }

        // Move from the top floor to the ground floor in one move
        algorithm.mqttClient.publishWith()
                .topic(directionTopic)
                .payload("2".getBytes(StandardCharsets.UTF_8)) // 2 for DOWN
                .send();

        String targetTopic = "elevator/" + elevator + "/targetFloor";
        algorithm.mqttClient.publishWith()
                .topic(targetTopic)
                .payload("0".getBytes(StandardCharsets.UTF_8))
                .send();

        while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "1")) > 0
                || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {
            Thread.sleep(sleepTime);
        }

        // Set committed direction to UNCOMMITTED
        algorithm.mqttClient.publishWith()
                .topic(directionTopic)
                .payload("0".getBytes(StandardCharsets.UTF_8)) // 0 for UNCOMMITTED
                .send();
    }
}
