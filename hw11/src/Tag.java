import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Tag implements TagInterface {

    private final int id;
    private final Map<Integer, PersonInterface> persons;
    private long cachedValueSum;

    public Tag(int id) {
        this.id = id;
        this.persons = new HashMap<>();
        this.cachedValueSum = 0;
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
        // Clamp to Integer range as per your original implementation
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
        return (int) (ageSum / persons.size()); // Integer division is fine here
    }

    @Override
    public /*@ pure @*/ int getAgeVar() {
        int size = persons.size();
        if (size == 0) {
            return 0;
        }
        int mean = getAgeMean();
        long varianceSum = 0; // Use long for sum of squares
        for (PersonInterface person : persons.values()) {
            long diff = person.getAge() - mean;
            varianceSum += diff * diff;
        }
        return (int) (varianceSum / size);
    }

    @Override
    public /*@ safe @*/ void delPerson(PersonInterface person) {
        if (person != null && persons.containsKey(person.getId())) {
            long valueToRemove = 0;
            for (PersonInterface remainingPerson : persons.values()) {
                if (remainingPerson.getId() != person.getId() && person.isLinked(remainingPerson)) {
                    valueToRemove += 2L * person.queryValue(remainingPerson);
                }
            }
            cachedValueSum -= valueToRemove;
            persons.remove(person.getId());
        }
    }

    @Override
    public /*@ pure @*/ int getSize() {
        return persons.size();
    }

    public /*@ safe @*/ void updateValue(PersonInterface p1,
        PersonInterface p2, int changeInValue) {
        if (p1 != null && p2 != null && p1.getId() != p2.getId()) {
            cachedValueSum += 2L * changeInValue;
        }
    }

    public /*@ pure @*/ Map<Integer, PersonInterface> getPersonsInternal() {
        return Collections.unmodifiableMap(this.persons);
    }
}