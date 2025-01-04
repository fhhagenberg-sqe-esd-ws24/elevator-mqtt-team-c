RMI Elevator Controller
=======================

This is a special implementation of the [controller-api](../controller-api/) that provides a remote interface
([sqelevator/IElevator.java](../../../elevatorSimulator/rmi-controller/src/main/java/sqelevator/IElevator.java)) to the controller
to enable remote access to the elevator system of the simulator via [remote method invocation (RMI)][1].

When the controller is started by the elevator simulator, it automatically starts a local RMI registry,
which listens on port `1099`, and binds the remote interface to the name `ElevatorSim`.

To change this default behaviour, see section [Change RMI configuration](#markdown-header-change-rmi-configuration) below!

## Lookup of the remote interface ##

Clients, which want to remote control the elevator system of the simulator must look up the system's remote interface:

    IElevator elevators = (IElevator) Naming.lookup("rmi://localhost/ElevatorSim");

## Change RMI configuration ##

The port of the RMI registry as well as the binding name can be influenced via system properties:

parameter       | system property   | example (default values)
----------------|-------------------|-------------------------------------
RMI port        | `elevator.rmi.port` | `-Delevator.rmi.port=1099`
Binding name    | `elevator.rmi.name` | `-Delevator.rmi.name=ElevatorSim`

[1]: https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/
[2]: https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/hello/hello-world.html
[3]: https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/faq.html