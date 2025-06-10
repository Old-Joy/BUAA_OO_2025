import com.oocourse.spec1.main.PersonInterface;
import com.oocourse.spec1.main.TagInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // 如果需要 Objects.hash 来实现 hashCode
import java.util.Collections;

public class Person implements PersonInterface {

    private final int id;
    private final String name;
    private final int age;
    private final Map<Integer, PersonInterface> acquaintance;
    private final Map<Integer, Integer> value;
    private final Map<Integer, TagInterface> tags;

    public Person(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.acquaintance = new HashMap<>();
        this.value = new HashMap<>();
        this.tags = new HashMap<>();
    }

    @Override
    public /*@ pure @*/ int getId() {
        return this.id;
    }

    @Override
    public /*@ pure @*/ String getName() {
        return this.name;
    }

    @Override
    public /*@ pure @*/ int getAge() {
        return this.age;
    }

    @Override
    public /*@ pure @*/ boolean containsTag(int id) {
        return tags.containsKey(id);
    }

    @Override
    public /*@ pure @*/ TagInterface getTag(int id) {
        return tags.get(id);
    }

    @Override
    public /*@ safe @*/ void addTag(/*@ non_null @*/ TagInterface tag) {
        if (tag != null) {
            tags.put(tag.getId(), tag);
        }
    }

    @Override
    public /*@ safe @*/ void delTag(int id) {
        // 假设前置条件 containsTag(id) 成立 (根据 JML 契约)
        tags.remove(id);
    }

    @Override
    public /*@ pure @*/ boolean equals(Object obj) {
        if (this == obj) { // 优化：检查是否是同一个对象
            return true;
        }
        if (obj == null || !(obj instanceof PersonInterface)) {
            return false;
        }
        PersonInterface otherPerson = (PersonInterface) obj;
        return this.id == otherPerson.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public /*@ pure @*/ boolean isLinked(PersonInterface person) {
        if (person == null) {
            return false; // 或者抛出异常？返回 false 似乎更安全
        }
        return person.getId() == this.id || acquaintance.containsKey(person.getId());
    }

    @Override
    public /*@ pure @*/ int queryValue(PersonInterface person) {
        if (person == null) {
            return 0;
        }
        return value.getOrDefault(person.getId(), 0);
    }

    public void addAcquaintance(PersonInterface person, int value) {
        // 只有当 person 不为 null 且尚未连接时才添加 (包括自己)
        if (person != null && !isLinked(person)) {
            this.acquaintance.put(person.getId(), person);
            this.value.put(person.getId(), value);
        }
    }

    public void removeAcquaintance(PersonInterface person) {
        if (person != null) {
            this.acquaintance.remove(person.getId());
            this.value.remove(person.getId());
        }
    }

    public void modifyRelationValue(PersonInterface person, int newValue) {
        if (person != null && this.value.containsKey(person.getId())) {
            this.value.put(person.getId(), newValue);
        }
    }

    public Map<Integer, PersonInterface> getAcquaintances() {
        return Collections.unmodifiableMap(this.acquaintance);
    }

    public Map<Integer, Integer> getAcquaintanceValues() {
        return Collections.unmodifiableMap(this.value);
    }

    public Map<Integer, TagInterface> getTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    public boolean strictEquals(PersonInterface person) {
        return true;
    }
}