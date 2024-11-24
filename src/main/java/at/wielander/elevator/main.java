package at.wielander.elevator;

import at.wielander.elevator.Model.IElevator;
import at.wielander.elevator.Model.ElevatorMQTTAdapter;
import at.wielander.elevator.Model.ElevatorSystem;
import at.wielander.elevator.View.Dashboard;
import java.rmi.Naming;

/**
 * @file main.java
 * @brief Main entry point for the Elevator MQTT application.
 *
 *        This class initializes the configuration, connects to the RMI service,
 *        sets up the elevator system,
 *        and starts the MQTT adapter and dashboard for monitoring and
 *        controlling the elevators.
 *
 *        The configuration is loaded from a properties file, and the RMI and
 *        MQTT connections are established
 *        based on the configuration properties.
 *
 *        The ElevatorSystem is initialized with a specified number of
 *        elevators, floor range, door operation time,
 *        and floor height. The ElevatorMQTTAdapter is used to connect the
 *        elevator system to the MQTT broker for
 *        message exchange.
 *
 *        The Dashboard is started to provide a graphical interface for
 *        monitoring the elevator states and
 *        subscribing to elevator state messages.
 *
 * @param args Command line arguments (not used).
 */
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
