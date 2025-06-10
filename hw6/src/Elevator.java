import com.oocourse.elevator2.TimableOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Elevator implements Runnable {
    private int id;
    private int currentFloor;
    private AtomicInteger peopleNum;
    private long moveTime = 400;
    private int direction; // 1: up, -1: down
    private long lastOperation;
    private Thread elevatorThread;
    private UnfinishedTask unfinishedTask;
    private QuestTable questTable;
    private ArrayList<Quest> quests = new ArrayList<>();
    private Scheduler scheduler;
    private AtomicBoolean scheing = new AtomicBoolean(false);
    private ArrayList<Quest> unreceivedQuests = new ArrayList<>();

    public Elevator(int id, QuestTable questTable, UnfinishedTask unfinishedTask) {
        this.id = id;
        this.currentFloor = 1;
        this.peopleNum = new AtomicInteger(0);
        this.direction = 1;
        this.questTable = questTable;
        this.scheduler = new Scheduler(questTable);
        this.unfinishedTask = unfinishedTask;
    }

    public void start() {
        if (elevatorThread == null) {
            elevatorThread = new Thread(this, "Elevator-" + this.id);
            elevatorThread.start();
        }
    }

    @Override
    public void run() {
        lastOperation = System.currentTimeMillis();
        while (true) {
            Operation op = scheduler.nextOperation(direction, peopleNum.get(),
                currentFloor, quests);
            if (op == Operation.END) { // 结束
                if (MainClass.getDebug()) {
                    System.out.println("Elevator" + this.id + " ended");
                }
                break;
            } else if (op == Operation.START) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                move();
            } else if (op == Operation.CONTINUE) { // 继续移动
                move();
            } else if (op == Operation.REVERSE) { // 掉头
                this.direction = -this.direction;
            } else if (op == Operation.WAIT) { // 等待
                questTable.waitForQuest();
            } else if (op == Operation.OPEN) { // 开门
                open();
            } else {
                doScheing();
            }
        }
    }

    private void doScheing() {
        Sche sche = null;
        synchronized (unfinishedTask) {
            scheing.set(true);
            pushReceive();
            lastOperation = TimableOutput.println("SCHE-BEGIN-" + this.id);
            sche = questTable.getSche();
            questTable.setSche(null);
            moveTime = (long) (sche.getSpeed() * 1000);
        }
        int targetFloor = sche.getToFloor();
        try {
            while (this.currentFloor != targetFloor) {
                int moveDirection = Integer.compare(targetFloor, this.currentFloor);
                int nextFloor = this.currentFloor + moveDirection;
                if (nextFloor == 0) {
                    nextFloor += moveDirection;
                }
                Thread.sleep(moveTime);
                this.currentFloor = nextFloor;
                lastOperation = TimableOutput.println(String.format("ARRIVE-%s-%d",
                    getFloorName(this.currentFloor), id));
            }
            lastOperation = TimableOutput.println(String.format("OPEN-%s-%d",
                getFloorName(this.currentFloor), id));
            Iterator<Quest> it = quests.iterator();
            while (it.hasNext()) {
                Quest quest = it.next();
                String status;
                if (quest.getToFloor() == this.currentFloor) {
                    status = "S";
                } else {
                    status = "F";
                    quest.setFromFloor(this.currentFloor);
                }
                lastOperation = TimableOutput.println(String.format("OUT-%s-%d-%s-%d",
                        status, quest.getPersonId(), getFloorName(this.currentFloor), id));
                it.remove();
                peopleNum.decrementAndGet();
                if (status.equals("F")) {
                    unfinishedTask.addTask(quest);
                }
            }
            Thread.sleep(1000);
            lastOperation = TimableOutput.println(String.format("CLOSE-%s-%d",
                getFloorName(this.currentFloor), id));
            lastOperation = TimableOutput.println(String.format("SCHE-END-%d", id));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            synchronized (unfinishedTask) {
                unfinishedTask.notifyScheduler();
                questTable.receive(id);
            }
            scheing.set(false);
        }
    }

    private void open() {
        String floorName = getFloorName(currentFloor);
        TimableOutput.println(String.format("OPEN-%s-%d", floorName, id));
        out();
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        in();
        Operation opNext = scheduler.nextOperation(direction, peopleNum.get(),
            currentFloor, quests);
        if (opNext == Operation.REVERSE) {
            this.direction = -this.direction;
            in();
        }
        lastOperation = TimableOutput.println(String.format("CLOSE-%s-%d", floorName, id));
    }

    private void out() {
        Iterator<Quest> it = quests.iterator();
        while (it.hasNext()) {
            Quest quest = it.next();
            if (quest.getToFloor() == this.currentFloor) {
                it.remove();
                peopleNum.decrementAndGet();
                int personId = quest.getPersonId();
                String floorName = getFloorName(currentFloor);
                TimableOutput.println(String.format("OUT-S-%d-%s-%d", personId, floorName, id));
            }
        }
        if (peopleNum.get() == 0 && questTable.isEmpty()) {
            unfinishedTask.notifyScheduler();
        }
    }

    private void in() {
        Quest quest = null;
        while (peopleNum.get() < 6 && questTable.questOnThisFloor(direction, currentFloor)) {
            quest = questTable.getQuest(direction, currentFloor);
            quests.add(quest);
            peopleNum.incrementAndGet();
            int personId = quest.getPersonId();
            String floorName = getFloorName(currentFloor);
            TimableOutput.println(String.format("IN-%d-%s-%d", personId, floorName, id));
        }
    }

    private void move() {
        while (true) {
            if (scheduler.nextOperation(direction, peopleNum.get(), currentFloor, quests)
                == Operation.OPEN) {
                open();
            }
            long currentTime = System.currentTimeMillis();
            synchronized (questTable) {
                if (currentTime - lastOperation < 400) {
                    try {
                        questTable.wait(400 - currentTime + lastOperation);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
        }
        if (direction == 1) {
            currentFloor++;
            if (currentFloor == 0) {
                currentFloor++;
            }
        } else if (direction == -1) {
            currentFloor--;
            if (currentFloor == 0) {
                currentFloor--;
            }
        }
        String floorName = getFloorName(currentFloor);
        lastOperation = TimableOutput.println(String.format("ARRIVE-%s-%d", floorName, id));
    }

    private String getFloorName(int floor) {
        if (floor > 0) {
            return "F" + floor;
        } else {
            return "B" + (-floor);
        }
    }

    private void pushReceive() { // 取消receive
        Quest quest = null;
        for (int i = -4; i <= 7; i++) {
            if (i != 0) {
                while ((quest = questTable.getQuest(1, i)) != null) {
                    unfinishedTask.addTask(quest);
                }
                while ((quest = questTable.getQuest(-1, i)) != null) {
                    unfinishedTask.addTask(quest);
                }
            }
        }
    }

    public int getScore(Quest quest) {
        int questDir;
        int questAlongSameDirection = questTable.questsNumInDirection(this.direction);
        if (quest.getToFloor() > quest.getFromFloor()) {
            questDir = 1;
        } else {
            questDir = -1;
        }
        if (questDir == this.direction) {
            if (peopleNum.get() + questAlongSameDirection < 6) {
                if ((currentFloor > quest.getFromFloor() && questDir == 1) ||
                    (currentFloor <= quest.getToFloor() && questDir == -1)) {
                    return Math.abs(currentFloor - quest.getFromFloor());
                } else {
                    return 2 * 9 - Math.abs(currentFloor - quest.getFromFloor());
                }
            } else {
                int questLeft = questTable.questLeft(this.direction, currentFloor);
                if (peopleNum.get() + questLeft < 6 &&
                    ((currentFloor > quest.getFromFloor() && questDir == 1) ||
                    (currentFloor <= quest.getToFloor() && questDir == -1))) {
                    return Math.abs(currentFloor - quest.getFromFloor());
                } else {
                    return 2 * ((questAlongSameDirection + peopleNum.get()) / 6) *
                            9 - Math.abs(currentFloor - quest.getFromFloor());
                }
            }
        } else {
            int questOppositeDirection = questTable.questsNumInDirection(-this.direction);
            if ((questAlongSameDirection + questOppositeDirection + peopleNum.get()) == 0) {
                return Math.abs(currentFloor - quest.getFromFloor());
            }
            int baseFloor = (direction == 1) ? 7 : -4;
            return 2 * (questOppositeDirection / (6 + 1)) * 9
                    + Math.abs(baseFloor - currentFloor) +
                    Math.abs(baseFloor - quest.getFromFloor());
        }
    }

    private int getFloorNum(String floor) {
        if (floor.charAt(0) == 'B') {
            return -Integer.parseInt(floor.substring(1));
        } else {
            return Integer.parseInt(floor.substring(1));
        }
    }

    public int getPeopleNum() {
        return peopleNum.get();
    }

    public QuestTable getQuestTable() {
        return questTable;
    }

    public boolean isScheing() {
        return this.scheing.get();
    }

    public int getId() {
        return id;
    }

}
