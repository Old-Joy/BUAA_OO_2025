import com.oocourse.elevator3.TimableOutput;

import java.util.ArrayList;

public class MainClass {
    private static boolean debug;

    public static boolean getDebug() {
        return debug;
    }

    public static void main(String[] args) {
        debug = false;
        TimableOutput.initStartTimestamp();
        UnfinishedTask unfinishedTask = new UnfinishedTask();
        ArrayList<QuestTable> questTables = new ArrayList<>();
        ArrayList<Elevator> elevators = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            QuestTable questTable = new QuestTable();
            Elevator elevator = new Elevator(i, questTable, unfinishedTask);
            elevators.add(elevator);
            questTables.add(questTable);
            elevator.start();
        }
        Manager manager = new Manager(unfinishedTask, elevators, questTables);
        manager.start();
        InputThread inputThread = new InputThread(unfinishedTask);
        inputThread.start();
    }
}
