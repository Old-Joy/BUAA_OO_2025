import com.oocourse.elevator3.ElevatorInput;
import com.oocourse.elevator3.PersonRequest;
import com.oocourse.elevator3.Request;
import com.oocourse.elevator3.ScheRequest;
import com.oocourse.elevator3.UpdateRequest;

public class InputThread implements Runnable {
    private Thread inputThread;
    private UnfinishedTask unfinishedTask;

    public InputThread(UnfinishedTask unfinishedTask) {
        this.unfinishedTask = unfinishedTask;
    }

    public void start() {
        if (inputThread == null) {
            inputThread = new Thread(this);
            inputThread.start();
        }
    }

    @Override
    public void run() {
        ElevatorInput input = new ElevatorInput(System.in);
        while (true) {
            Request request = input.nextRequest();
            if (request == null) {
                try {
                    input.close();
                    unfinishedTask.setEnd();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            } else {
                if (request instanceof PersonRequest) {
                    PersonRequest personRequest = (PersonRequest) request;
                    int personId = personRequest.getPersonId();
                    int priority = personRequest.getPriority();
                    String fromFloor = personRequest.getFromFloor();
                    int fromFloorInt = parseFloor(fromFloor);
                    String toFloor = personRequest.getToFloor();
                    int toFloorInt = parseFloor(toFloor);
                    Quest quest = new Quest(personId, priority, fromFloorInt, toFloorInt);
                    unfinishedTask.addTask(quest);
                } else if (request instanceof ScheRequest) {
                    ScheRequest scheRequest = (ScheRequest) request;
                    int elevatorId = scheRequest.getElevatorId();
                    double speed = scheRequest.getSpeed();
                    String toFloor = scheRequest.getToFloor();
                    int toFloorInt = parseFloor(toFloor);
                    Sche sche = new Sche(elevatorId, speed, toFloorInt);
                    unfinishedTask.addTask(sche);
                } else {
                    UpdateRequest updateRequest = (UpdateRequest) request;
                    int elevatorAId = updateRequest.getElevatorAId();
                    int elevatorBId = updateRequest.getElevatorBId();
                    String transferFloor = updateRequest.getTransferFloor();
                    int transferFloorInt = parseFloor(transferFloor);
                    Update update = new Update(elevatorAId, elevatorBId, transferFloorInt);
                    unfinishedTask.addTask(update);
                }
            }
        }
    }

    private int parseFloor(String floor) {
        if (floor.charAt(0) == 'B') {
            return -Integer.parseInt(floor.substring(1));
        } else {
            return Integer.parseInt(floor.substring(1));
        }
    }
}
