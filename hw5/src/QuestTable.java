import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

public class QuestTable {
    private HashMap<Integer, ArrayList<Quest>> upQuests = new HashMap<>(); // key是起点楼层
    private HashMap<Integer, ArrayList<Quest>> downQuests = new HashMap<>();
    private boolean isEnd = false;

    public QuestTable() {
        for (int i = -4; i <= 7; i++) {
            if (i != 0) {
                upQuests.put(i, new ArrayList<>());
                downQuests.put(i, new ArrayList<>());
            }
        }
    }

    public synchronized void addQuest(Quest quest) {
        if (quest.getToFloor() > quest.getFromFloor()) {
            upQuests.get(quest.getFromFloor()).add(quest);
            if (MainClass.getDebug()) {
                System.out.println("Someone wants to get form " +
                    quest.getFromFloor() + " to " + quest.getToFloor());
            }
        } else if (quest.getToFloor() < quest.getFromFloor()) {
            downQuests.get(quest.getFromFloor()).add(quest);
            if (MainClass.getDebug()) {
                System.out.println("Someone wants to get form " +
                    quest.getFromFloor() + " to " + quest.getToFloor());
            }
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

    public synchronized Optional<Quest> peekHighestPriorityQuest(int direction, int floor) {
        ArrayList<Quest> quests = null;
        if (direction == 1) {
            quests = upQuests.get(floor);
        } else if (direction == -1) {
            quests = downQuests.get(floor);
        }
        if (quests == null || quests.isEmpty()) {
            return Optional.empty();
        }
        return quests.stream().max(Comparator.comparingInt(Quest::getPriority));
    }

    public synchronized boolean questOnThisFloor(int direction, int floor) { // 是否存在以这个楼层为起点的请求
        ArrayList<Quest> quests = null;
        if (direction == 1) {
            quests = upQuests.get(floor);
        } else if (direction == -1) {
            quests = downQuests.get(floor);
        }
        if (quests == null || quests.isEmpty()) {
            if (MainClass.getDebug()) {
                System.out.println("None quests left");
            }
            return false;
        } else {
            if (MainClass.getDebug()) {
                System.out.println("Still some quests left");
            }
            return true;
        }
    }

    public synchronized void waitForQuest() {
        try {
            wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void setEnd() {
        isEnd = true;
        notifyAll();
    }

    public synchronized boolean availableQuests(int floor) {
        if (upQuests.get(floor).isEmpty() && downQuests.get(floor).isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public synchronized boolean isEnd() {
        return isEnd;
    }

    public synchronized boolean isEmpty() {
        for (int i = -4; i <= 7; i++) {
            if (i != 0) {
                if (!upQuests.get(i).isEmpty()) {
                    return false;
                }
                if (!downQuests.get(i).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
