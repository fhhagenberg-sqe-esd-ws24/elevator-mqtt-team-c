# sqelevator-proj
Group assignment SQElevator


package at.fhhagenberg.sqelevator ist der full qualified name vom IElevator und darf nicht verändert werden.
im repo ist ein codeexample von Elevator auf sqelevator zu finden.
wichtig ist, das erst eine neue target, oder committed direction gesetzten werden darf, wenn der elevator die türen geoffnet hat.!!!!
bei rmiexception, oder nach langer zeit ein timeout ist, dann ist die verbindung abgelaufen. was auch sein kann ist, das der simulator stehen geblieben ist.

das elevatorsystem muss nicht zwingend mit mehreren liften funktionieren. es reicht, wenn einer entsprechend des git examples hoch und runterfahren kann, entsprechend seiner ziele.
aufgrund dessen das Felix nicht macht wurde der elevatoralgorithm vereinfach.

IElevator lässt seinen clocktick abfragen. wenn man immer alles abfragt kann es sein, das unter den calls das rmi interface geupdated wird. hier ist dann wichtig, dass am anfang und ende die clockticks abgefragt werden, dann kann man
dies überprüfen und darauf reagieren.


netzwerkverbindungsabbrüche können simuliert werden, indem die elevatorsimulation gestoppt wird und das simulationsfenster (aber nicht das programm) geschlossen wird.

# MQTT-Adapter
| **Veröffentlichungshäufigkeit** | **Themen (Topics)**                          | **Beschreibung**                      |
|----------------------------------|---------------------------------------------|---------------------------------------|
| **Regelmäßig (periodisch)**     | `elevator/{id}/currentFloor`                | Aktuelle Etage                        |
|                                  | `elevator/{id}/targetedFloor`               | Ziel-Etage                            |
|                                  | `elevator/{id}/speed`                       | Geschwindigkeit                       |
|                                  | `elevator/{id}/weight`                      | Gewicht                               |
|                                  | `elevator/{id}/doorState`                   | Türstatus                             |
|                                  | `elevator/{id}/button/{btn_id}`                      | Knopf im Lift                               |
|                                  | `floor/{id}/buttonUp`                      | Stockwerk-Knopf nach oben                               |
|                                  | `floor/{id}/buttonDown`                      | Stockwerk-Knopf nach unten                               |
| **Einmalig (retained)**          | `building/info/numberOfElevators`           | Anzahl der Aufzüge                    |
|                                  | `building/info/numberOfFloors`              | Anzahl der Etagen                     |
|                                  | `building/info/floorHeight/feet`            | Etagenhöhe                            |
|    **Subscribed**                               | `elevator/{id}/committedDirection`               | Richtung                            |
|                                  | `elevator/{id}/targetFloor`                       | Ziel-Etage                       |
|                                  | `elevator/{id}/floorService`                      | Stockwerk erreichbar                               |

# Elevator-Algorithm
Der Elevator-Algorithm ist ein einfacher Algorithmus, welcher von unten beginnend, Stock für Stock, nach oben fährt und anschließend wieder ganz nach unten.
Bei der Initialisierung wird ein MQTT-Client erstellt welcher sich mit dem Broker verbindung und sich auf die topics numberOfElevators, sowie numberOfFloors subscribed.
Für den Algorithmus sind nur ein teil der zuvor gelisteten topics der einfach heit halber verwendet worden.
Durch publishen der committedDirection und des targetedFloor wird der Lift in bewegung gesetzt und sein aktueller Zustand über currentFloor, speed und doorState abgefragt.


