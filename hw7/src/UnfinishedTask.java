import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnfinishedTask {
    private AtomicBoolean isEnd = new AtomicBoolean(false);
    private ArrayList<Task> tasks = new ArrayList<>();

    public synchronized void addTask(Task task) {
        tasks.add(task);
        notifyAll();
    }

    public synchronized void waitForTask() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void notifyScheduler() {
        notifyAll();
    }

    public synchronized Task getTask() {
        Task task = null;
        if (!tasks.isEmpty()) {
            task = tasks.get(0);
            tasks.remove(0);
        }
        return task;
    }

    public synchronized boolean isEmpty() {
        return tasks.isEmpty();
    }

    public synchronized Sche getSche() {
        Sche sche = null;
        for (Task task : tasks) {
            if (task instanceof Sche) {
                sche = (Sche) task;
            }
        }
        if (sche == null) {
            return null;
        } else {
            tasks.remove(sche);
            return sche;
        }
    }

    public synchronized Update getUpdate() {
        Update update = null;
        for (Task task : tasks) {
            if (task instanceof Update) {
                update = (Update) task;
            }
        }
        if (update == null) {
            return null;
        } else {
            tasks.remove(update);
            return update;
        }
    }

    public void setEnd() {
        this.isEnd.set(true);
        synchronized (this) {
            notifyAll();
        }
    }

    public boolean isEnd() {
        return this.isEnd.get();
    }

    public synchronized void addToFront(Task task) {
        tasks.add(0, task);
        notifyAll();
    }
}
