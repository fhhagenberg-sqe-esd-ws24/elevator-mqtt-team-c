package at.wielander.elevator.Algorithm;

import at.wielander.elevator.Exception.MQTTClientException;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ElevatorListener {
    private final Map<String, String> retainedMessages;
    private final Map<String, String> liveMessages;
    private final Logger logger;

    public ElevatorListener(
            Map<String, String> retainedMessages,
            Map<String, String> liveMessages,
            Logger logger) {
        this.retainedMessages = new ConcurrentHashMap<>(retainedMessages);
        this.liveMessages = new ConcurrentHashMap<>(liveMessages);
        this.logger = logger;
    }

    public void handleRetainedMessage(Mqtt5Publish publish) {
        try{
            String topic = publish.getTopic().toString();
            String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
            retainedMessages.put(topic, payload);
            logger.info("Retained message received: " + topic + " -> " + payload);
        } catch(MQTTClientException e){
            e.printStackTrace();
        }

    }

    public void handleLiveMessage(Mqtt5Publish publish) {
        String topic = publish.getTopic().toString();
        String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
        liveMessages.put(topic, payload);
        logger.info("Live message received: " + topic + " -> " + payload); }
}
