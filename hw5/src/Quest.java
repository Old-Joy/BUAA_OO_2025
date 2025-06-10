public class Quest {
    private int personId;
    private int priority; // 优先级
    private int fromFloor;
    private int toFloor;

    public Quest(int personId, int priority, int fromFloor, int toFloor) {
        this.personId = personId;
        this.priority = priority;
        this.fromFloor = fromFloor;
        this.toFloor = toFloor;
    }

    public int getPersonId() {
        return personId;
    }

    public int getPriority() {
        return priority;
    }

    public int getFromFloor() {
        return fromFloor;
    }

    public int getToFloor() {
        return toFloor;
    }

    public void setFromFloor(int fromFloor) {
        this.fromFloor = fromFloor;
    }
}
