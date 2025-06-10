import com.oocourse.spec2.main.PersonInterface;
import com.oocourse.spec2.main.TagInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Tag implements TagInterface {

    private final int id;
    private final Map<Integer, PersonInterface> persons;
    private long cachedValueSum; // 使用 long 防止溢出

    public Tag(int id) {
        this.id = id;
        this.persons = new HashMap<>();
        this.cachedValueSum = 0; // 初始化缓存值
    }

    @Override
    public /*@ pure @*/ int getId() {
        return this.id;
    }

    @Override
    public /*@ pure @*/ boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TagInterface)) {
            return false;
        }
        TagInterface otherTag = (TagInterface) obj;
        return this.id == otherTag.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public /*@ safe @*/ void addPerson(/*@ non_null @*/ PersonInterface person) {
        if (person != null && !persons.containsKey(person.getId())) {
            for (PersonInterface existingPerson : persons.values()) {
                if (person.isLinked(existingPerson)) {
                    cachedValueSum += 2L * person.queryValue(existingPerson);
                }
            }
            persons.put(person.getId(), person);
        }
    }

    @Override
    public /*@ pure @*/ boolean hasPerson(PersonInterface person) {
        if (person == null) {
            return false;
        }
        return persons.containsKey(person.getId());
    }

    @Override
    public /*@ pure @*/ int getValueSum() {
        if (cachedValueSum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (cachedValueSum < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) cachedValueSum;
    }

    @Override
    public /*@ pure @*/ int getAgeMean() {
        if (persons.isEmpty()) {
            return 0;
        }
        long ageSum = 0;
        for (PersonInterface person : persons.values()) {
            ageSum += person.getAge();
        }
        return (int) (ageSum / persons.size());
    }

    @Override
    public /*@ pure @*/ int getAgeVar() {
        int size = persons.size();
        if (size == 0) {
            return 0;
        }
        int mean = getAgeMean();
        long varianceSum = 0;
        for (PersonInterface person : persons.values()) {
            long diff = person.getAge() - mean;
            varianceSum += diff * diff;
        }
        if (size == 0) { return 0; }
        return (int) (varianceSum / size);
    }

    @Override
    public /*@ safe @*/ void delPerson(/*@ non_null @*/ PersonInterface person) {
        if (person != null && persons.containsKey(person.getId())) {
            long valueToRemove = 0;
            for (PersonInterface otherPerson : persons.values()) {
                if (otherPerson.getId() != person.getId() && person.isLinked(otherPerson)) {
                    valueToRemove += 2L * person.queryValue(otherPerson);
                }
            }
            cachedValueSum -= valueToRemove;
            persons.remove(person.getId()); // 现在移除
        }
    }

    @Override
    public /*@ pure @*/ int getSize() {
        return persons.size();
    }

    public /*@ safe @*/ void updateRelationValue(PersonInterface p1,
        PersonInterface p2, int change) {
        if (p1 != null && p2 != null && p1.getId() != p2.getId()) { // 避免自己和自己的关系
            cachedValueSum += 2L * change;
        }
    }

    public Map<Integer, PersonInterface> getPersonsInternal() {
        return this.persons; // 返回内部 map 的引用，注意外部修改风险
    }

    public long getCachedValueSumInternal() {
        return this.cachedValueSum;
    }
}