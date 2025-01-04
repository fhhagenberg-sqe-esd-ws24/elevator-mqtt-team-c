package at.wielander.elevator.Helpers.Topics;

import algorithm.ElevatorListener;

import static at.wielander.elevator.Helpers.Topics.BaseTopics.*;

public enum ElevatorTopics {
    /* Elevator Topics */
    // Current floor
    ELEVATOR_INFO_CURRENT_FLOOR(ELEVATOR.topic + "%s/currentFloor"),
    // Current speed
    ELEVATOR_INFO_SPEED(ELEVATOR.topic + "%s/speed"),
    // current weight
    ELEVATOR_INFO_WEIGHT(ELEVATOR.topic + "%s/weight"),
    // current door state
    ELEVATOR_CONTROL_DOOR_STATE(ELEVATOR.topic + "%s/doorState"),
    // button state
    ELEVATOR_FLOOR_BUTTON_STATE(ELEVATOR.topic+ "%s/button/%s"),

    ELEVATOR_INFO_COMMITTED_DIRECTION(ELEVATOR.topic + "%s");
    private final String topic;

    ElevatorTopics(String topic) {
        this.topic = topic;
    }
    public String getTopic() {
        return topic;
    }

    public String elevatorIndex(String idx) {
        return this.topic + idx;
    }
}
