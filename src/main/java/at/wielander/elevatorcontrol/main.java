package at.wielander.elevatorcontrol;

public class main {
    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:1883";  // Lokaler MQTT Broker
        String adapterClientId = "ElevatorMQTTAdapter2";
        String dashboardClientId = "DashboardClient2";

        // Starte den MQTT-Adapter
        ElevatorMQTTAdapter adapter = new ElevatorMQTTAdapter(brokerUrl, adapterClientId);
        System.out.println("adapter erstellt.");
        adapter.connect();
        System.out.println("adapter mit broker verbunden.");

        // Starte das Dashboard
        Dashboard dashboard = new Dashboard(brokerUrl, dashboardClientId);
        System.out.println("dashboard erstellt.");
        dashboard.connect();
        System.out.println("dashboard mit broker verbunden.");

        // Teste den Austausch von Nachrichten
        adapter.publishElevatorState(1, 3);  // z.B. Etage 3 für Aufzug 1
        System.out.println("adapter hat nachricht gepublished.");
        dashboard.subscribeToElevatorState();
    }
}
