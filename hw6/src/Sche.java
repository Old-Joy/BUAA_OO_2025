public class Sche implements Task {
    private int elevatorId;
    private double speed;
    private int toFloor;

    public Sche(int elevatorId, double speed, int toFloor) {
        this.elevatorId = elevatorId;
        this.speed = speed;
        this.toFloor = toFloor;
    }

    public int getToFloor() {
        return this.toFloor;
    }

    public int getElevatorId() {
        return this.elevatorId;
    }

    public double getSpeed() {
        return this.speed;
    }
}
