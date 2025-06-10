import java.util.concurrent.atomic.AtomicBoolean;

public class TargetFloor {
    private int floor;
    private AtomicBoolean full = new AtomicBoolean(false);

    public TargetFloor(int floor) {
        this.floor = floor;
    }

    public int getFloor() {
        return this.floor;
    }

    public synchronized void use() {
        this.full.set(true);
    }

    public synchronized void useOver() {
        this.full.set(false);
        notifyAll();
    }

    public synchronized boolean isFull() {
        return this.full.get();
    }

    public synchronized void waitForSign() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void floorNotify() {
        notifyAll();
    }
}
