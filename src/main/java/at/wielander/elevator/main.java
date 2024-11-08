package at.wielander.elevator;

import at.wielander.elevator.Model.ElevatorMQTTAdapter;
import at.wielander.elevator.View.Dashboard;

public class main {
    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:1883"; // Lokaler MQTT Broker
        String adapterClientId = "ElevatorMQTTAdapter2";
        String dashboardClientId = "DashboardClient2";

        // Starte den MQTT-Adapter
        ElevatorMQTTAdapter adapter = new ElevatorMQTTAdapter(brokerUrl, adapterClientId);

        adapter.connect();

        // Starte das Dashboard
        Dashboard dashboard = new Dashboard(brokerUrl, dashboardClientId);
        dashboard.connect();

        // Teste den Austausch von Nachrichten
        dashboard.subscribeToElevatorState();

        adapter.publishElevatorState(1, 3); // z.B. Etage 3 für Aufzug 1
    }
}
