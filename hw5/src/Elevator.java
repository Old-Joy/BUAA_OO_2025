import com.oocourse.elevator1.TimableOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

public class Elevator implements Runnable {
    private int id;
    private int currentFloor;
    private int peopleNum;
    private long waitTime;
    private int direction; // 1: up, -1: down
    private Thread elevatorThread;
    private QuestTable questTable;
    private ArrayList<Quest> quests = new ArrayList<>();
    private Scheduler scheduler;

    public Elevator(int id, QuestTable questTable) {
        this.id = id;
        this.currentFloor = 1;
        this.peopleNum = 0;
        this.direction = 1;
        this.questTable = questTable;
        this.scheduler = new Scheduler(questTable);
    }

    public void start() {
        if (elevatorThread == null) {
            elevatorThread = new Thread(this, "Elevator-" + this.id);
            elevatorThread.start();
        }
    }

    @Override
    public void run() {
        while (true) {
            Operation op = scheduler.nextOperation(direction, peopleNum, currentFloor, quests);
            if (op == Operation.END) { // 结束
                if (MainClass.getDebug()) {
                    System.out.println("Elevator " + this.id + " has ended");
                }
                break;
            } else if (op == Operation.CONTINUE) { // 继续移动
                move();
            } else if (op == Operation.REVERSE) { // 掉头
                this.direction = -this.direction;
                if (MainClass.getDebug()) {
                    System.out.println("Elevator " + this.id + " has reversed");
                }
            } else if (op == Operation.WAIT) { // 等待
                long time = System.currentTimeMillis();
                questTable.waitForQuest();
                this.waitTime += System.currentTimeMillis() - time;
            } else { // 开门
                this.waitTime = 0;
                String floorName = getFloorName(currentFloor);
                TimableOutput.println(String.format("OPEN-%s-%d", floorName, id));
                out();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                in();
                exchange(floorName);
                Operation opNext = scheduler.nextOperation(direction, peopleNum,
                    currentFloor, quests);
                if (opNext == Operation.REVERSE) {
                    this.direction = -this.direction;
                    in();
                    exchange(floorName);
                }
                TimableOutput.println(String.format("CLOSE-%s-%d", floorName, id));
            }
        }
    }

    private void exchange(String floorName) {
        while (peopleNum == 6) {
            Optional<Quest> lowestInsideOpt = findLowestPriorityPassenger();
            if (!lowestInsideOpt.isPresent()) { // 不应该发生，但作为保护
                break;
            }
            Quest lowestInside = lowestInsideOpt.get();
            Optional<Quest> highestOutsideOpt =
                questTable.peekHighestPriorityQuest(direction, currentFloor);
            if (highestOutsideOpt.isPresent() &&
                highestOutsideOpt.get().getPriority() > lowestInside.getPriority()) {
                Quest highestOutside = highestOutsideOpt.get();
                quests.remove(lowestInside);
                peopleNum--;
                TimableOutput.println(String.format("OUT-%d-%s-%d",
                    lowestInside.getPersonId(), floorName, id));
                lowestInside.setFromFloor(currentFloor);
                questTable.addQuest(lowestInside);
                Quest personToEnter = questTable.getQuest(direction, currentFloor);
                quests.add(personToEnter);
                peopleNum++;
                TimableOutput.println(String.format("IN-%d-%s-%d",
                    personToEnter.getPersonId(), floorName, id));
            } else {
                break;
            }
        }
    }

    private void out() {
        if (MainClass.getDebug()) {
            System.out.println("Someone left the elevator" + this.id);
        }
        Iterator<Quest> it = quests.iterator();
        while (it.hasNext()) {
            Quest quest = it.next();
            if (quest.getToFloor() == this.currentFloor) {
                it.remove();
                peopleNum--;
                int personId = quest.getPersonId();
                String floorName = getFloorName(currentFloor);
                TimableOutput.println(String.format("OUT-%d-%s-%d", personId, floorName, id));
            }
        }
    }

    private void in() {
        if (MainClass.getDebug()) {
            System.out.println("Someone entered the elevator" + this.id);
        }
        Quest quest = null;
        while (peopleNum < 6 && questTable.questOnThisFloor(direction, currentFloor)) {
            quest = questTable.getQuest(direction, currentFloor);
            quests.add(quest);
            peopleNum++;
            int personId = quest.getPersonId();
            String floorName = getFloorName(currentFloor);
            TimableOutput.println(String.format("IN-%d-%s-%d", personId, floorName, id));
        }
    }

    private void move() {
        if (direction == 1) {
            currentFloor++;
            if (MainClass.getDebug()) {
                System.out.println("The elevator " + this.id + " is moving up");
            }
            if (currentFloor == 0) {
                currentFloor++;
            }
        } else if (direction == -1) {
            currentFloor--;
            if (MainClass.getDebug()) {
                System.out.println("The elevator " + this.id + " is moving down");
            }
            if (currentFloor == 0) {
                currentFloor--;
            }
        }
        try {
            if (waitTime > 400) {
                waitTime = 400;
            }
            Thread.sleep(400 - waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String floorName = getFloorName(currentFloor);
        TimableOutput.println(String.format("ARRIVE-%s-%d", floorName, id));
        waitTime = 0;
    }

    private Optional<Quest> findLowestPriorityPassenger() {
        if (quests.isEmpty()) {
            return Optional.empty();
        }
        // 使用Stream API找到优先级最低的乘客
        return quests.stream().min(Comparator.comparingInt(Quest::getPriority));
    }

    private String getFloorName(int floor) {
        if (floor > 0) {
            return "F" + floor;
        } else {
            return "B" + (-floor);
        }
    }
}
