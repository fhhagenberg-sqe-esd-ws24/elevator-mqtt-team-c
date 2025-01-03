package algorithm;

import sqelevator.IElevator;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import elevator.*;

import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.util.HashMap;
import java.util.Map;

public class ElevatorAlgorithm {


	private Map<String, String> retainedMessages = new HashMap<>();
    private Map<String, String> liveMessages = new HashMap<>();
    private Mqtt5AsyncClient mqttClient; // MQTT-Client als Instanzvariable
    private ElevatorMQTTAdapter eMQTTAdapter; // Adapter als Instanzvariable
    private ElevatorSystem eSystem;


    public static void main(String[] args) throws InterruptedException {
    	ElevatorAlgorithm algorithm = new ElevatorAlgorithm();
        String brokerHost = "tcp://localhost:1883"; // Lokaler Mosquitto Broker
        System.out.println("Connecting to MQTT Broker at: " + brokerHost);

        try {
            // RMI setup
            IElevator controller = (IElevator) Naming.lookup("rmi://localhost/ElevatorSim");
            
            // Elevator System Configuration
            algorithm.eSystem = new ElevatorSystem(
                    1,        
                    0,         
                    10,         
                    1000,     
                    10,        
                    controller // RMI-Controller
            );

            // Create the MQTT Adapter
            algorithm.eMQTTAdapter = new ElevatorMQTTAdapter(
            		algorithm.eSystem,          // Elevator System
                    brokerHost,       // MQTT Broker Host
                    "mqttAdapter",    // Client ID
                    250,              // Polling Interval (ms)
                    controller        // RMI-Controller
            );

            // Connect MQTT Adapter to the Broker
            algorithm.eMQTTAdapter.connect();
            
            
            

            // Connect to MQTT Broker
            algorithm.mqttClient = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost("localhost")
                    .serverPort(1883)
                    .identifier("ElevatorAlgorithmClient")
                    .buildAsync();

          

         // Liste der Topics, die wir abonnieren wollen (nur die building/info Topics)
            String topicFilter = "building/info/#"; // Filtert nur Topics unter building/info

            // Abonnieren des Topic Filters
            algorithm.mqttClient.subscribeWith()
                    .topicFilter(topicFilter) // Wildcard für alle Subtopics unter building/info
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();

            // Verarbeiten der empfangenen Nachrichten
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                // Überprüfen, ob das Topic unter "building/info" fällt
                if (topic.startsWith("building/info")) {
                    // Payload wird als String gespeichert
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    algorithm.retainedMessages.put(topic, payload); // Speichern der Payload
                    System.out.println("Retained message received: " + topic + " -> " + payload);
                }
            });

         // Subscribe to live messages for elevators and floors
            for (int elevatorId = 0; elevatorId < 2; elevatorId++) {
                // Abonniere alle Themen, die mit "elevator/" und der entsprechenden ID beginnen
                algorithm.mqttClient.subscribeWith().topicFilter("elevator/" + elevatorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }
            for (int floorId = 0; floorId < 4; floorId++) {
                // Abonniere alle Themen, die mit "floor/" und der entsprechenden ID beginnen
                algorithm.mqttClient.subscribeWith().topicFilter("floor/" + floorId + "/#").qos(MqttQos.AT_LEAST_ONCE).send();
            }

            // Verarbeiten der empfangenen Nachrichten
            algorithm.mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
                String topic = publish.getTopic().toString();
                String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

                // Überprüfen, ob das Topic mit "elevator/" oder "floor/" beginnt
                if (topic.startsWith("elevator/") || topic.startsWith("floor/")) {
                    // Die Nachricht wird in der Map liveMessages gespeichert
                    algorithm.liveMessages.put(topic, payload);
                    System.out.println("Live message received: " + topic + " -> " + payload);
                }
            });

            algorithm.mqttClient.connect().whenComplete((ack, throwable) -> {
                if (throwable == null) {
                    System.out.println("Connected to MQTT broker");
                } else {
                    System.err.println("Failed to connect to MQTT broker: " + throwable.getMessage());
                }
            });
            
            algorithm.runAlgorithm(algorithm, algorithm.eMQTTAdapter);

        } catch (Exception e) {
        
    	 algorithm.eMQTTAdapter.disconnect();
    	
        }
    }
    

    public void runAlgorithm(ElevatorAlgorithm algorithm, ElevatorMQTTAdapter eMQTTAdapter) throws InterruptedException {
        Thread.sleep(3000);
        algorithm.eMQTTAdapter.run();
        Thread.sleep(500);
        
        final int numberOfFloors = Integer.parseInt(retainedMessages.get("building/info/numberOfFloors")); // Anzahl der Stockwerke

        final int elevator = 0;
        final int sleepTime = 10;

        // Set committed direction to UP and publish to MQTT
        String directionTopic = "elevator/" + elevator + "/committedDirection";
        algorithm.mqttClient.publishWith()
            .topic(directionTopic)
            .payload("1".getBytes(StandardCharsets.UTF_8)) // 1 für UP
            .send();

        // First: Move from ground floor to the top floor, stopping at each floor
        for (int nextFloor = 1; nextFloor < numberOfFloors; nextFloor++) {
            // Set the target floor and publish to MQTT
            String targetTopic = "elevator/" + elevator + "/targetFloor";
            algorithm.mqttClient.publishWith()
                .topic(targetTopic)
                .payload(Integer.toString(nextFloor).getBytes(StandardCharsets.UTF_8))
                .send();

            // Wait until the elevator reaches the target floor and speed is 0
            while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "-1")) < nextFloor
                    || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {
               
                Thread.sleep(sleepTime);
               
            }

         // Wait until doors are open
            while (!"1".equals(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/doorState", ""))) {
              
                Thread.sleep(sleepTime);
                
            }
        }

        // Second: Move from the top floor to the ground floor in one move

        // Set committed direction to DOWN and publish to MQTT
        algorithm.mqttClient.publishWith()
            .topic(directionTopic)
            .payload("2".getBytes(StandardCharsets.UTF_8)) // 2 für DOWN
            .send();

        // Set the target floor to ground floor (floor 0) and publish
        String targetTopic = "elevator/" + elevator + "/targetFloor";
        algorithm.mqttClient.publishWith()
            .topic(targetTopic)
            .payload("0".getBytes(StandardCharsets.UTF_8))
            .send();

        // Wait until ground floor is reached
        while (Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/currentFloor", "1")) > 0
                || Integer.parseInt(algorithm.liveMessages.getOrDefault("elevator/" + elevator + "/speed", "1")) > 0) {
       
            Thread.sleep(sleepTime);
         
        }

        // Set committed direction to UNCOMMITTED and publish to MQTT
        algorithm.mqttClient.publishWith()
            .topic(directionTopic)
            .payload("0".getBytes(StandardCharsets.UTF_8)) // 0 für UNCOMMITTED
            .send();
        
    }
}