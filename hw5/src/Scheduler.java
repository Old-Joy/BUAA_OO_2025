import java.util.ArrayList;

public class Scheduler {
    private QuestTable questTable;

    public Scheduler(QuestTable questTable) {
        this.questTable = questTable;
    }

    public Operation nextOperation(int direction, int num, int floor, ArrayList<Quest> quests) {
        if (openForIn(direction, num, floor) || openForOut(floor, quests)) { // 电梯内有人要出电梯或者电梯外有人要进电梯
            return Operation.OPEN;
        } else {
            if (num != 0) { // 电梯内有人
                return Operation.CONTINUE;
            } else { // 电梯内没人
                if (questTable.isEmpty()) { // 所有电梯的任务表都为空
                    if (questTable.isEnd()) { // 所有请求都已完成
                        return Operation.END;
                    } else {
                        return Operation.WAIT;
                    }
                } else {
                    if (existQuest(direction, floor)) {
                        return Operation.CONTINUE;
                    } else {
                        return Operation.REVERSE; // 电梯转向
                    }
                }
            }
        }
    }

    private boolean openForIn(int direction, int num, int floor) {
        if (num == 6) {
            return false;
        } else {
            if (questTable.questOnThisFloor(direction, floor)) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean openForOut(int floor, ArrayList<Quest> quests) {
        for (Quest quest : quests) {
            if (quest.getToFloor() == floor) {
                return true;
            }
        }
        return false;
    }

    private boolean existQuest(int direction, int floor) {
        if (direction == 1) {
            for (int i = floor + 1; i <= 7; i++) {
                if (i != 0) {
                    if (questTable.availableQuests(i)) {
                        return true;
                    }
                }
            }
        } else {
            for (int i = floor - 1; i >= -4; i--) {
                if (i != 0) {
                    if (questTable.availableQuests(i)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
