package at.wielander.elevator;

import at.wielander.elevator.Model.IElevator;
import at.wielander.elevator.Model.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import at.wielander.elevator.View.Dashboard;
import java.rmi.Naming;

public class main {
    public static void main(String[] args) {
        ConfigLoader config = new ConfigLoader("config.properties");

        String rmiUrl = config.getProperty("rmi.url");
        String mqttBrokerUrl = config.getProperty("mqtt.broker.url");
        String mqttClientId = config.getProperty("mqtt.client.id");
        int pollingInterval = config.getIntProperty("polling.interval");

        try {

            IElevator elevatorAPI = (IElevator) Naming.lookup(rmiUrl);
            ElevatorSystem elevatorSystem = new ElevatorSystem(
                    3, // number of elevators
                    0, // minimum floor
                    10, // maximum floor
                    4000, // door operation time
                    10, // floor height
                    elevatorAPI);
            ElevatorMQTTAdapter adapter = new ElevatorMQTTAdapter(elevatorSystem, mqttBrokerUrl, mqttClientId,
                    pollingInterval);
            adapter.connect();

            // Start the Dashboard
            Dashboard dashboard = new Dashboard(mqttBrokerUrl, mqttClientId + "_dashboard");
            dashboard.connect();

            // Test message exchange
            dashboard.subscribeToElevatorState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// public class main {
// private static IElevator ElevatorAPI;

// public static void main(String[] args) {

// ElevatorSystem buildingElevators = new ElevatorSystem(
// 3,
// 0,
// 10,
// 4000,
// 10,
// ElevatorAPI);

// String brokerUrl = "tcp://localhost:1883"; // Lokaler MQTT Broker
// String adapterClientId = "ElevatorMQTTAdapter2";
// String dashboardClientId = "DashboardClient2";

// // Starte den MQTT-Adapter
// ElevatorMQTTAdapter adapter = new ElevatorMQTTAdapter(buildingElevators,
// brokerUrl, adapterClientId);

// adapter.connect();

// // Starte das Dashboard
// Dashboard dashboard = new Dashboard(brokerUrl, dashboardClientId);
// dashboard.connect();

// // Teste den Austausch von Nachrichten
// dashboard.subscribeToElevatorState();

// //adapter.publishElevatorState(1, 3); // z.B. Etage 3 für Aufzug 1
// }
// }
