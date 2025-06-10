import com.oocourse.elevator2.TimableOutput;

import java.util.ArrayList;

public class Manager implements Runnable {
    private Thread managerThread;
    private UnfinishedTask unfinishedTask;
    private ArrayList<Elevator> elevators;
    private ArrayList<QuestTable> questTables;

    public Manager(UnfinishedTask unfinishedTask, ArrayList<Elevator> elevators,
        ArrayList<QuestTable> questTables) {
        this.unfinishedTask = unfinishedTask;
        this.elevators = elevators;
        this.questTables = questTables;
    }

    public void start() {
        if (managerThread == null) {
            managerThread = new Thread(this, "Manager");
            managerThread.start();
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (unfinishedTask) {
                if (isEnd()) {
                    setEnd();
                    if (MainClass.getDebug()) {
                        System.out.println("Manager ended");
                    }
                    break;
                } else {
                    Sche sche = null;
                    if (unfinishedTask.isEmpty()) {
                        unfinishedTask.waitForTask();
                    } else if ((sche = unfinishedTask.getSche()) != null) {
                        QuestTable questTable = questTables.get(sche.getElevatorId() - 1);
                        questTable.setSche(sche);
                    } else {
                        Task task = unfinishedTask.getTask();
                        if (task instanceof Quest) {
                            Quest quest = (Quest) task;
                            if (!choose(quest)) {
                                unfinishedTask.addToFront(quest);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean choose(Quest quest) {
        Elevator elevator = null;
        int min = Integer.MAX_VALUE;
        for (Elevator elevator1 : elevators) {
            int score = elevator1.getScore(quest);
            if (score < min) {
                elevator = elevator1;
                min = score;
            }
        }
        if (elevator == null) {
            return false;
        }
        if (!elevator.isScheing()) {
            TimableOutput.println(String.format("RECEIVE-%d-%d",
                quest.getPersonId(), elevator.getId()));
        }
        elevator.getQuestTable().addQuest(quest);
        return true;
    }

    private boolean isEnd() {
        synchronized (this) {
            if (!unfinishedTask.isEmpty() || !unfinishedTask.isEnd()) {
                return false;
            }
            for (Elevator elevator : elevators) {
                if (elevator.getPeopleNum() != 0 || !elevator.getQuestTable().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setEnd() {
        for (int i = 0; i < 6; i++) {
            questTables.get(i).setEnd();
        }
    }
}
