package at.wielander.elevator.controller;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import at.wielander.elevator.Model.IElevator;
import java.rmi.RemoteException;

public class ElevatorAlgorithm {

    private MqttClient client;
    private IElevator controller;

    public ElevatorAlgorithm(IElevator controller) throws MqttException {
        this.controller = controller;
        String broker = "tcp://localhost:1883";
        String clientId = "ElevatorAlgorithm";
        MemoryPersistence persistence = new MemoryPersistence();
        
        client = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        client.connect(connOpts);

        client.subscribe("elevator/+/call");
    }

    public void messageArrived(String topic, MqttMessage message) throws RemoteException {
        String payload = new String(message.getPayload());
        System.out.println("Received call request: " + payload);
        
        // Extract elevator ID and floor from the topic
        String[] topicLevels = topic.split("/");
        int elevatorId = Integer.parseInt(topicLevels[1]);
        int requestedFloor = Integer.parseInt(payload);

        // Set the elevator target to the requested floor
        controller.setTarget(elevatorId, requestedFloor);
    }
}
