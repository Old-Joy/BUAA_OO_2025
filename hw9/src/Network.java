import com.oocourse.spec1.main.NetworkInterface;
import com.oocourse.spec1.main.PersonInterface;
import com.oocourse.spec1.main.TagInterface;
import com.oocourse.spec1.exceptions.AcquaintanceNotFoundException;
import com.oocourse.spec1.exceptions.EqualPersonIdException;
import com.oocourse.spec1.exceptions.EqualRelationException;
import com.oocourse.spec1.exceptions.EqualTagIdException;
import com.oocourse.spec1.exceptions.PersonIdNotFoundException;
import com.oocourse.spec1.exceptions.RelationNotFoundException;
import com.oocourse.spec1.exceptions.TagIdNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList; // isCircle 需要
import java.util.Map;
import java.util.Queue;   // isCircle 需要
import java.util.Set;

public class Network implements NetworkInterface {

    private final Map<Integer, PersonInterface> personsMap;
    private long totalTripleSum = 0; // 使用 long 防止溢出

    public Network() {
        this.personsMap = new HashMap<>();
        this.totalTripleSum = 0; // 初始化为 0
    }

    private int countCommonNeighbors(int uid, int vid) {
        PersonInterface p1 = personsMap.get(uid);
        PersonInterface p2 = personsMap.get(vid);

        if (!(p1 instanceof Person) || !(p2 instanceof Person)) {
            System.err.println("错误: countCommonNeighbors 需要具体的 Person 实例来获取邻居。");
            // 在无法获取邻居的情况下，无法准确计算，返回 0 或抛异常
            return 0;
        }

        Map<Integer, PersonInterface> acqMap1 = ((Person) p1).getAcquaintances(); // 获取副本/视图
        Map<Integer, PersonInterface> acqMap2 = ((Person) p2).getAcquaintances(); // 获取副本/视图

        // 获取 keySet 视图进行比较
        Set<Integer> neighborsOfU = (acqMap1 != null) ? acqMap1.keySet() : Collections.emptySet();
        Set<Integer> neighborsOfV = (acqMap2 != null) ? acqMap2.keySet() : Collections.emptySet();

        if (neighborsOfU.isEmpty() || neighborsOfV.isEmpty()) {
            return 0;
        }

        int commonCount = 0;
        Set<Integer> smallerSet = (neighborsOfU.size() < neighborsOfV.size())
            ? neighborsOfU : neighborsOfV;
        Set<Integer> largerSet = (neighborsOfU.size() < neighborsOfV.size())
            ? neighborsOfV : neighborsOfU;
        for (Integer wid : smallerSet) {
            if (largerSet.contains(wid)) {
                commonCount++;
            }
        }
        return commonCount;
    }

    @Override
    public void addPerson(PersonInterface person) throws EqualPersonIdException {
        if (person == null) { return; }
        if (containsPerson(person.getId())) { throw new EqualPersonIdException(person.getId()); }
        personsMap.put(person.getId(), person);
        // 添加孤立节点不影响三元环数
    }

    @Override
    public void addRelation(int id1, int id2, int value) throws
        PersonIdNotFoundException, EqualRelationException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (p1.isLinked(p2)) { throw new EqualRelationException(id1, id2); }

        // 1. 在添加边之前，计算将形成多少新的三元环
        int commonNeighbors = countCommonNeighbors(id1, id2);

        // 2. 修改 Person 内部状态
        if (p1 instanceof Person && p2 instanceof Person) {
            ((Person) p1).addAcquaintance(p2, value);
            ((Person) p2).addAcquaintance(p1, value);
        } else {
            System.err.println("错误: addRelation 时 Person 类型不匹配。");
            return; // 或抛异常
        }

        // 3. 更新三元环总数
        this.totalTripleSum += commonNeighbors;
    }

    @Override
    public void modifyRelation(int id1, int id2, int value) throws PersonIdNotFoundException,
        EqualPersonIdException, RelationNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) { throw new EqualPersonIdException(id1); }
        if (!p1.isLinked(p2)) { throw new RelationNotFoundException(id1, id2); }
        if (!(p1 instanceof Person) || !(p2 instanceof Person)) { return; }

        Person concreteP1 = (Person) p1;
        Person concreteP2 = (Person) p2;
        int currentVal = concreteP1.queryValue(concreteP2);
        int newVal = currentVal + value;

        if (newVal <= 0) {
            // *** 关系删除 ***
            // 1. 在删除边之前，计算将破坏多少三元环
            int commonNeighbors = countCommonNeighbors(id1, id2);

            // 2. 修改 Person 内部状态
            concreteP1.removeAcquaintance(concreteP2);
            concreteP2.removeAcquaintance(concreteP1);

            // 3. 更新三元环总数
            this.totalTripleSum -= commonNeighbors;

            Map<Integer, TagInterface> tags1 = concreteP1.getTags();
            if (tags1 != null) {
                for (TagInterface tag : tags1.values()) {
                    if (tag.hasPerson(concreteP2)) {
                        tag.delPerson(concreteP2);
                    }
                }
            }
            Map<Integer, TagInterface> tags2 = concreteP2.getTags();
            if (tags2 != null) {
                for (TagInterface tag : tags2.values()) {
                    if (tag.hasPerson(concreteP1)) {
                        tag.delPerson(concreteP1);
                    }
                }
            }
        } else {
            concreteP1.modifyRelationValue(concreteP2, newVal);
            concreteP2.modifyRelationValue(concreteP1, newVal);
        }
    }

    @Override
    public /*@ pure @*/ int queryTripleSum() {
        // 检查 long 到 int 的转换是否安全
        if (totalTripleSum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (totalTripleSum < 0) { // 理论上不应小于0，但作为检查
            System.err.println("Warning: totalTripleSum became negative!");
            return 0;
        }
        return (int) totalTripleSum;
    }

    @Override
    public /*@ pure @*/ boolean isCircle(int id1, int id2) throws PersonIdNotFoundException {
        if (!containsPerson(id1)) { throw new PersonIdNotFoundException(id1); }
        if (!containsPerson(id2)) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) { return true; }

        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(id1);
        visited.add(id1);

        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();
            PersonInterface currentPerson = personsMap.get(currentId);
            if (currentPerson == null) { continue; } // 防御性

            // 直接访问 Person 邻居 (依赖 Person.getAcquaintances)
            if (currentPerson instanceof Person) {
                Map<Integer, PersonInterface> acqMap =
                    ((Person) currentPerson).getAcquaintances();
                if (acqMap != null) {
                    for (Integer neighborId : acqMap.keySet()) { // 直接用 keySet 视图
                        if (neighborId == id2) { return true; }
                        if (visited.add(neighborId)) { // add 返回 true 如果添加成功 (即之前不存在)
                            queue.offer(neighborId);
                        }
                    }
                }
            } else {
                // Fallback (慢) - 如果必须支持非 Person 实现
                System.err.println("Warning: isCircle using isLinked check for node " + currentId);
                for (PersonInterface neighbor : personsMap.values()) {
                    int neighborId = neighbor.getId();
                    if (currentId != neighborId && currentPerson.isLinked(neighbor)) {
                        if (neighborId == id2) { return true; }
                        if (visited.add(neighborId)) {
                            queue.offer(neighborId);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public /*@ pure @*/ boolean containsPerson(int id) {
        return personsMap.containsKey(id);
    }

    @Override
    public /*@ pure @*/ PersonInterface getPerson(int id) {
        return personsMap.get(id);
    }

    @Override
    public /*@ pure @*/ int queryValue(int id1, int id2) throws
        PersonIdNotFoundException, RelationNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (!p1.isLinked(p2)) { throw new RelationNotFoundException(id1, id2); }
        return p1.queryValue(p2);
    }

    @Override
    public /*@ pure @*/ int queryTagAgeVar(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        TagInterface tag = person.getTag(tagId);
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        return tag.getAgeVar();
    }

    @Override
    public /*@ pure @*/ int queryBestAcquaintance(int id) throws
        PersonIdNotFoundException, AcquaintanceNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        if (!(person instanceof Person)) { throw new AcquaintanceNotFoundException(id); }
        Person concretePerson = (Person) person;
        Map<Integer, Integer> acquaintanceValues =
            concretePerson.getAcquaintanceValues(); // 依赖 Person getter
        if (acquaintanceValues.isEmpty()) { throw new AcquaintanceNotFoundException(id); }
        int maxValue = Integer.MIN_VALUE;
        for (int value : acquaintanceValues.values()) {
            if (value > maxValue) { maxValue = value; }
        }
        int bestId = Integer.MAX_VALUE;
        boolean firstMaxFound = false;
        for (Map.Entry<Integer, Integer> entry : acquaintanceValues.entrySet()) {
            if (entry.getValue() == maxValue) {
                if (!firstMaxFound || entry.getKey() < bestId) {
                    bestId = entry.getKey();
                    firstMaxFound = true;
                }
            }
        }
        if (bestId == Integer.MAX_VALUE) { throw new AcquaintanceNotFoundException(id); }
        return bestId;
    }

    @Override
    public void addTag(int personId, TagInterface tag)
        throws PersonIdNotFoundException, EqualTagIdException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (tag == null) { return; }
        if (person.containsTag(tag.getId())) { throw new EqualTagIdException(tag.getId()); }
        person.addTag(tag);
    }

    @Override
    public void addPersonToTag(int personId1, int personId2, int tagId) throws
        PersonIdNotFoundException, RelationNotFoundException,
        TagIdNotFoundException, EqualPersonIdException {
        PersonInterface p1 = getPerson(personId1);
        if (p1 == null) { throw new PersonIdNotFoundException(personId1); }
        PersonInterface p2 = getPerson(personId2);
        if (p2 == null) { throw new PersonIdNotFoundException(personId2); }
        if (personId1 == personId2) { throw new EqualPersonIdException(personId1); }
        if (!p2.isLinked(p1)) { throw new RelationNotFoundException(personId1, personId2); }
        if (!p2.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        TagInterface tag = p2.getTag(tagId);
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        if (tag.hasPerson(p1)) { throw new EqualPersonIdException(personId1); }
        if (tag.getSize() <= 999) { tag.addPerson(p1); }
    }

    @Override
    public void delPersonFromTag(int personId1, int personId2, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface p1 = getPerson(personId1);
        if (p1 == null) { throw new PersonIdNotFoundException(personId1); }
        PersonInterface p2 = getPerson(personId2);
        if (p2 == null) { throw new PersonIdNotFoundException(personId2); }
        if (!p2.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        TagInterface tag = p2.getTag(tagId);
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        if (!tag.hasPerson(p1)) { throw new PersonIdNotFoundException(personId1); }
        tag.delPerson(p1);
    }

    @Override
    public void delTag(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        person.delTag(tagId);
    }

    // JUnit 测试需要的桩方法 (恢复为 null)
    public PersonInterface[] getPersons() {
        return null;
    }
}