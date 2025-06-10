import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Update implements Task {
    private int elevatorAId;
    private int elevatorBId;
    private int transferFloor;
    private TargetFloor targetFloor;

    private AtomicInteger finishCounter = new AtomicInteger(0);
    private AtomicBoolean updateTrulyFinished = new AtomicBoolean(false);
    private final Object completionLock = new Object();

    private AtomicInteger beginCounter = new AtomicInteger(0);
    private AtomicBoolean beginPhaseSynchronized = new AtomicBoolean(false);
    private final Object beginLock = new Object();

    private AtomicInteger preCounter = new AtomicInteger(0);
    private AtomicBoolean prePhaseSynchronized = new AtomicBoolean(false);
    private final Object preLock = new Object();

    public Update(int elevatorAId, int elevatorBId, int transferFloor) {
        this.elevatorAId = elevatorAId;
        this.elevatorBId = elevatorBId;
        this.transferFloor = transferFloor;
        this.targetFloor = new TargetFloor(transferFloor);
    }

    public void signalAndWaitForPreSync() {
        synchronized (preLock) {
            int currentCount = preCounter.incrementAndGet();
            if (currentCount == 2) {
                prePhaseSynchronized.set(true);
                preLock.notifyAll();
            } else {
                while (!prePhaseSynchronized.get()) {
                    try {
                        preLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public void signalAndWaitForBeginSync() {
        synchronized (beginLock) {
            int currentCount = beginCounter.incrementAndGet();
            if (currentCount == 2) {
                beginPhaseSynchronized.set(true);
                beginLock.notifyAll();
            } else {
                while (!beginPhaseSynchronized.get()) {
                    try {
                        beginLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public void signalAndWaitForCompletion() {
        synchronized (completionLock) {
            int currentCount = finishCounter.incrementAndGet();
            if (currentCount == 2) {
                updateTrulyFinished.set(true);
                completionLock.notifyAll();
            } else {
                while (!updateTrulyFinished.get()) {
                    try {
                        completionLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    public int getElevatorAId() {
        return this.elevatorAId;
    }

    public int getElevatorBId() {
        return this.elevatorBId;
    }

    public int getTransferFloor() {
        return this.transferFloor;
    }

    public TargetFloor getTargetFloor() {
        return this.targetFloor;
    }
}
