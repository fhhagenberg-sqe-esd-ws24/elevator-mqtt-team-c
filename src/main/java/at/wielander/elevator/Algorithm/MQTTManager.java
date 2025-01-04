package at.wielander.elevator.Algorithm;

import at.wielander.elevator.Helpers.Constants.ElevatorRequest;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MQTTManager {
    private static final Logger logger = Logger.getLogger(MQTTManager.class.getName());

    private final Mqtt5AsyncClient mqttClient;
    private final Map<String, String> retainedMessages = new ConcurrentHashMap<>();
    private final Map<String, String> liveMessages = new ConcurrentHashMap<>();
    private final Map<Integer, ElevatorRequest> elevatorRequests = new ConcurrentHashMap<>();

    public MQTTManager(String brokerHost, String clientId) {
        this.mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost(brokerHost)
                .serverPort(1883)
                .identifier(clientId)
                .buildAsync();
    }

    public void connect() {
        mqttClient.connect().whenComplete((ack, throwable) -> {
            if (throwable == null) {
                logger.info("Connected to MQTT broker");
            } else {
                logger.log(Level.SEVERE, "Failed to connect to MQTT broker: " + throwable.getMessage(), throwable);
            }
        });
    }

    public void disconnect() {
        mqttClient.disconnect().whenComplete((ack, throwable) -> {
            if (throwable == null) {
                logger.info("Disconnected from MQTT broker");
            } else {
                logger.log(Level.SEVERE, "Failed to disconnect: " + throwable.getMessage(), throwable);
            }
        });
    }

    public void subscribeToTopics(String topicFilter) {
        mqttClient.subscribeWith()
                .topicFilter(topicFilter)
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable == null) {
                        logger.info("Subscribed to topic: " + topicFilter);
                    } else {
                        logger.log(Level.SEVERE, "Failed to subscribe: " + throwable.getMessage(), throwable);
                    }
                });

        mqttClient.publishes(MqttGlobalPublishFilter.ALL, publish -> {
            String topic = publish.getTopic().toString();
            String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
            if (topic.startsWith("building/info")) {
                retainedMessages.put(topic, payload);
                logger.info("Retained message received: " + topic + " -> " + payload);
            } else {
                liveMessages.put(topic, payload);
                logger.info("Live message received: " + topic + " -> " + payload);
            }
        });
    }

    public void publish(String topic, String message) {
        mqttClient.publishWith()
                .topic(topic)
                .payload(message.getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete((pubAck, throwable) -> {
                    if (throwable == null) {
                        logger.info("Published to topic: " + topic + " -> " + message);
                    } else {
                        logger.log(Level.SEVERE, "Failed to publish: " + throwable.getMessage(), throwable);
                    }
                });
    }

    public String getRetainedMessage(String topic) {
        return retainedMessages.get(topic);
    }

    public String getLiveMessage(String topic) {
        return liveMessages.get(topic);
    }
}

