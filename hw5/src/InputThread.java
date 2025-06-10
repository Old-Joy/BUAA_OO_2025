import com.oocourse.elevator1.ElevatorInput;
import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.util.ArrayList;

public class InputThread implements Runnable {
    private Thread inputThread;
    private ArrayList<QuestTable> questTableList; // 所有电梯的任务表

    public InputThread(ArrayList<QuestTable> questTableList) {
        this.questTableList = questTableList;
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
                    for (int i = 0; i < 6; i++) {
                        questTableList.get(i).setEnd();
                    }
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
                    int elevatorId = personRequest.getElevatorId();
                    QuestTable questTable = questTableList.get(elevatorId - 1);
                    questTable.addQuest(quest);
                    if (MainClass.getDebug()) {
                        System.out.println(String.format("INPUT-%d-%d-%s-%s", personId,
                            priority, fromFloor, toFloor));
                    }
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
