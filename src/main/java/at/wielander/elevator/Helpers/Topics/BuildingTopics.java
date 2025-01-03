package at.wielander.elevator.Helpers.Topics;

import static at.wielander.elevator.Helpers.Topics.BaseTopics.*;

public enum BuildingTopics {
    /*  Building Information topics */

    // Total Elevators
    BUILDING_INFO_TOTAL_ELEVATORS(BUILDING.topic + INFO.topic + "totalElevators"),

    // Total Floors
    BUILDING_INFO_TOTAL_FLOORS(BUILDING.topic + INFO.topic + "totalFloors"),

    // Floor Height
    BUILDING_INFO_FLOOR_HEIGHT(BUILDING.topic + INFO.topic + "floorHeight");

    private final String topic;

    BuildingTopics(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
