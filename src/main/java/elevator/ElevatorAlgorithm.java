package elevator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ElevatorAlgorithm {
    private Queue<Integer> upFloorRequests;
    private Queue<Integer> downFloorRequests;
    private int currentFloor;
    private boolean movingUp;

    public ElevatorAlgorithm() {
        this.upFloorRequests = new LinkedList<>();
        this.downFloorRequests = new LinkedList<>();
        this.currentFloor = 0;
        this.movingUp = true;
    }

    public void addRequest(int floor) {
        if (floor >= 0) {
            if (floor > currentFloor) {
                upFloorRequests.add(floor);
            } else if (floor < currentFloor) {
                downFloorRequests.add(floor);
            }
        } else {
            throw new IllegalArgumentException("Invalid floor number: " + floor);
        }
        processRequests();
    }

    private void processRequests() {
        if (movingUp) {
            if (!upFloorRequests.isEmpty()) {
                List<Integer> upList = new LinkedList<>(upFloorRequests);
                Collections.sort(upList);
                upFloorRequests.clear();
                upFloorRequests.addAll(upList);

                currentFloor = upFloorRequests.poll();
                System.out.println("Elevator moving up to floor: " + currentFloor);
            } else {
                movingUp = false;
                processRequests();
            }
        } else {
            if (!downFloorRequests.isEmpty()) {
                List<Integer> downList = new LinkedList<>(downFloorRequests);
                Collections.sort(downList, Collections.reverseOrder());
                downFloorRequests.clear();
                downFloorRequests.addAll(downList);

                currentFloor = downFloorRequests.poll();
                System.out.println("Elevator moving down to floor: " + currentFloor);
            } else {
                movingUp = true;
                processRequests();
            }
        }
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public int getCommittedDirection() {
        return movingUp ? 1 : -1;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
        processRequests();
    }
}
