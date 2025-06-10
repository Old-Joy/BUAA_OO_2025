import com.oocourse.elevator2.TimableOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QuestTable {
    private Sche sche = null;
    private HashMap<Integer, ArrayList<Quest>> upQuests = new HashMap<>(); // key是起点楼层
    private HashMap<Integer, ArrayList<Quest>> downQuests = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private AtomicBoolean isEnd = new AtomicBoolean(false);

    public QuestTable() {
        for (int i = -4; i <= 7; i++) {
            if (i != 0) {
                upQuests.put(i, new ArrayList<>());
                downQuests.put(i, new ArrayList<>());
            }
        }
    }

    public synchronized void addQuest(Quest quest) {
        if (quest.getToFloor() >= quest.getFromFloor()) {
            upQuests.get(quest.getFromFloor()).add(quest);
        } else if (quest.getToFloor() < quest.getFromFloor()) {
            downQuests.get(quest.getFromFloor()).add(quest);
        }
        notifyAll();
    }

    public synchronized Quest getQuest(int direction, int floor) {
        ArrayList<Quest> quests = null;
        if (direction == 1) {
            quests = upQuests.get(floor);
        } else if (direction == -1) {
            quests = downQuests.get(floor);
        }
        if (quests == null || quests.isEmpty()) {
            return null;
        }
        Optional<Quest> maxQuest = quests.stream()
            .max(Comparator.comparingInt(Quest::getPriority));
        maxQuest.ifPresent(quests::remove);
        return maxQuest.orElse(null);
    }

    public synchronized boolean questOnThisFloor(int direction, int floor) { // 是否存在以这个楼层为起点的请求
        ArrayList<Quest> quests = null;
        if (direction == 1) {
            quests = upQuests.get(floor);
        } else if (direction == -1) {
            quests = downQuests.get(floor);
        }
        return !quests.isEmpty();
    }

    public synchronized void waitForQuest() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setEnd() {
        this.isEnd.set(true);
        synchronized (this) {
            notifyAll();
        }
    }

    public synchronized boolean availableQuests(int floor) {
        if (upQuests.get(floor).isEmpty() && downQuests.get(floor).isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isEnd() {
        return isEnd.get();
    }

    public synchronized boolean isEmpty() {
        for (int i = -4; i <= 7; i++) {
            if (i != 0) {
                if (!upQuests.get(i).isEmpty()) {
                    if (MainClass.getDebug()) {
                        System.out.println("Still upQuests: " + upQuests.get(i));
                    }
                    return false;
                }
                if (!downQuests.get(i).isEmpty()) {
                    if (MainClass.getDebug()) {
                        System.out.println("Still downQuests: " + downQuests.get(i));
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public synchronized int questsNumInDirection(int direction) {
        int num = 0;
        HashMap<Integer, ArrayList<Quest>> quests = null;
        if (direction == 1) {
            quests = upQuests;
        } else {
            quests = downQuests;
        }
        for (int i = -4; i < 7; i++) {
            if (i != 0) {
                num += quests.get(i).size();
            }
        }
        return num;
    }

    public synchronized int allNumOfQuests() {
        int num = 0;
        for (int i = -4; i < 7; i++) {
            if (i != 0) {
                num += upQuests.get(i).size();
                num += downQuests.get(i).size();
            }
        }
        return num;
    }

    public synchronized int questLeft(int direction, int floor) {
        int num = 0;
        if (direction == 1) {
            for (int i = -4; i < 7; i++) {
                if (i != 0) {
                    num += upQuests.get(i).size();
                }
            }
        } else if (direction == -1) {
            for (int i = -4; i < 7; i++) {
                if (i != 0) {
                    num += downQuests.get(i).size();
                }
            }
        }
        return num;
    }

    public synchronized void setSche(Sche sche) {
        this.sche = sche;
        if (sche != null) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public synchronized Sche getSche() {
        return sche;
    }

    public synchronized void receive(int id) {
        for (int i = -4; i <= 7; i++) {
            if (i != 0) {
                ArrayList<Quest> quests = upQuests.get(i);
                for (Quest quest : quests) {
                    TimableOutput.println("RECEIVE-" + quest.getPersonId() + "-" + id);
                }
                quests = downQuests.get(i);
                for (Quest quest : quests) {
                    TimableOutput.println("RECEIVE-" + quest.getPersonId() + "-" + id);
                }
            }
        }
    }
}
