import com.oocourse.spec2.main.PersonInterface;
import com.oocourse.spec2.main.TagInterface;
import com.oocourse.spec2.exceptions.AcquaintanceNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

public class Person implements PersonInterface {
    private final int id;
    private final String name;
    private final int age;
    private final Map<Integer, PersonInterface> acquaintanceMap;
    private final Map<Integer, Integer> valueMap;

    // 用于 qba O(1) 查询的数据结构
    private static class AcquaintanceEntry implements Comparable<AcquaintanceEntry> {
        private final int personId;
        private int value;

        public AcquaintanceEntry(int personId, int value) {
            this.personId = personId;
            this.value = value;
        }

        @Override
        public int compareTo(AcquaintanceEntry other) {
            if (this.value != other.value) {
                return Integer.compare(other.value, this.value); // value 降序
            }
            return Integer.compare(this.personId, other.personId); // personId 升序
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            AcquaintanceEntry that = (AcquaintanceEntry) o;
            return personId == that.personId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(personId);
        }
    }

    private final TreeSet<AcquaintanceEntry> sortedAcquaintances;

    private final Map<Integer, TagInterface> tags;
    private final List<Integer> receivedArticles;

    // --- 新增字段 ---
    private final Map<Integer, TagInterface> associatedTags; // 记录此人所属的 Tag

    public Person(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.acquaintanceMap = new HashMap<>();
        this.valueMap = new HashMap<>();
        this.sortedAcquaintances = new TreeSet<>();
        this.tags = new HashMap<>(); // Person 自己拥有的 Tag
        this.receivedArticles = new ArrayList<>();
        this.associatedTags = new HashMap<>(); // 初始化所属 Tag 集合
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
    public /*@ pure @*/ boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PersonInterface) {
            return this.id == ((PersonInterface) obj).getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public /*@ pure @*/ boolean isLinked(PersonInterface person) {
        if (person == null) {
            return false;
        }
        return person.getId() == this.id || acquaintanceMap.containsKey(person.getId());
    }

    @Override
    public /*@ pure @*/ int queryValue(PersonInterface person) {
        if (person == null || person.getId() == this.id) {
            return 0;
        }
        return valueMap.getOrDefault(person.getId(), 0);
    }

    @Override
    public /*@ pure @*/ boolean containsTag(int id) {
        return tags.containsKey(id); // 指自己拥有的 Tag
    }

    @Override
    public /*@ pure @*/ TagInterface getTag(int id) {
        return tags.get(id); // 获取自己拥有的 Tag
    }

    @Override
    public /*@ safe @*/ void addTag(/*@ non_null @*/ TagInterface tag) {
        if (tag != null && !tags.containsKey(tag.getId())) {
            tags.put(tag.getId(), tag);
        }
    }

    @Override
    public /*@ safe @*/ void delTag(int id) {
        tags.remove(id);
    }

    @Override
    public /*@ pure @*/ List<Integer> getReceivedArticles() {
        return new ArrayList<>(this.receivedArticles);
    }

    @Override
    public /*@ pure @*/ List<Integer> queryReceivedArticles() {
        int size = Math.min(this.receivedArticles.size(), 5);
        return new ArrayList<>(this.receivedArticles.subList(0, size));
    }

    // --- 内部方法，用于 Network 修改关系 ---
    public /*@ safe @*/ void addAcquaintance(PersonInterface person, int val) {
        if (person != null && person.getId() != this.id
            && !acquaintanceMap.containsKey(person.getId())) {
            acquaintanceMap.put(person.getId(), person);
            valueMap.put(person.getId(), val);
            sortedAcquaintances.add(new AcquaintanceEntry(person.getId(), val));
        }
    }

    public /*@ safe @*/ void removeAcquaintance(PersonInterface person) {
        if (person != null) {
            Integer personId = person.getId();
            Integer oldValue = valueMap.remove(personId); // remove 返回旧值
            acquaintanceMap.remove(personId);
            if (oldValue != null) {
                sortedAcquaintances.remove(new AcquaintanceEntry(personId, oldValue));
            }
        }
    }

    public /*@ safe @*/ void modifyRelationValue(PersonInterface person, int newValue) {
        if (person != null) {
            Integer personId = person.getId();
            if (valueMap.containsKey(personId)) { // 确保关系存在
                Integer oldValue = valueMap.get(personId);
                sortedAcquaintances.remove(new AcquaintanceEntry(personId, oldValue));
                valueMap.put(personId, newValue);
                sortedAcquaintances.add(new AcquaintanceEntry(personId, newValue));
            }
        }
    }

    // --- 查询最佳熟人 (qba) ---
    public /*@ pure @*/ int getBestAcquaintanceIdInternal() throws AcquaintanceNotFoundException {
        if (sortedAcquaintances.isEmpty()) {
            throw new AcquaintanceNotFoundException(this.id);
        }
        return sortedAcquaintances.first().personId;
    }

    public /*@ pure @*/ boolean hasAcquaintancesInternal() {
        return !acquaintanceMap.isEmpty();
    }

    // --- 处理接收的文章 ---
    public /*@ safe @*/ void addReceivedArticle(int articleId) {
        this.receivedArticles.add(0, articleId);
    }

    public /*@ safe @*/ void removeReceivedArticle(int articleId) {
        this.receivedArticles.removeIf(id -> id.equals(articleId));
    }

    // --- 获取内部状态（用于 Network） ---
    public /*@ pure @*/ Map<Integer, PersonInterface> getAcquaintances() {
        return Collections.unmodifiableMap(this.acquaintanceMap);
    }

    public /*@ pure @*/ Map<Integer, TagInterface> getOwnedTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    public /*@ safe @*/ void associateWithTag(TagInterface tag) {
        if (tag != null) {
            associatedTags.put(tag.getId(), tag);
        }
    }

    public /*@ safe @*/ void disassociateFromTag(int tagId) {
        associatedTags.remove(tagId);
    }

    public /*@ pure @*/ Map<Integer, TagInterface> getAssociatedTags() {
        return new HashMap<>(this.associatedTags); // 返回副本防止外部修改
    }

}