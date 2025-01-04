package at.wielander.elevator.Controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by winterer on 13.01.2017.
 */
public class RMIController implements IController {
    private static Logger logger = LoggerFactory.getLogger(RMIController.class);

    public static final String RMI_PORT_PROPERTY = "elevator.rmi.port";
    public static final String RMI_NAME_PROPERTY = "elevator.rmi.name";
    public static final String RMI_NAME_DEFAULT = "ElevatorSim";

    private RMIElevator rmiElevator;
    private Registry registry;
    private String name;

    @Override
    public void setup(IElevators elevators) {
        logger.info("Setting up controller");

        //String codebase = IElevator.class.getClassLoader().getResource("").toExternalForm();
        //System.setProperty("java.rmi.server.codebase", codebase);
        int port = Integer.getInteger(RMI_PORT_PROPERTY, Registry.REGISTRY_PORT);

        name = System.getProperty(RMI_NAME_PROPERTY, RMI_NAME_DEFAULT);
        try {
            rmiElevator = new RMIElevator(elevators);

            logger.info("Exporting IElevator");
            IElevator stub = stub = (IElevator) UnicastRemoteObject.exportObject(rmiElevator, 0);

            logger.info("Starting RMI registry at port {}", port);
            registry = LocateRegistry.createRegistry(port);

            logger.info("Binding IElevator to name {}", name);
            registry.rebind(name, stub);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void dispose() {
        if (registry != null) {
            try {
                logger.info("Unbinding IElevator");
                registry.unbind(name);

                logger.info("Unexporting IElevator");
                UnicastRemoteObject.unexportObject(rmiElevator, false);
                rmiElevator = null;

                logger.info("Shutting down RMI registry");
                UnicastRemoteObject.unexportObject(registry, true);
                registry = null;
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } catch (NotBoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void tick(long tick) {
        rmiElevator.setClockTick(tick);
    }

    @Override
    public String getText() {
        return "RMI Controller";
    }
}
