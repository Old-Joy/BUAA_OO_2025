import com.oocourse.spec1.main.PersonInterface;
import com.oocourse.spec1.main.TagInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // For Objects.hash

public class Tag implements TagInterface {

    private final int id;
    private final Map<Integer, PersonInterface> persons;

    public Tag(int id) {
        this.id = id;
        this.persons = new HashMap<>(); // Initialize the map
    }

    @Override
    public /*@ pure @*/ int getId() {
        return this.id;
    }

    @Override
    public /*@ pure @*/ boolean equals(Object obj) {
        if (this == obj) { // Optimization: check for self-reference
            return true;
        }
        // Handles both JML cases (null or wrong type check)
        if (obj == null || !(obj instanceof TagInterface)) {
            return false;
        }
        // Cast and compare IDs
        TagInterface otherTag = (TagInterface) obj;
        return this.id == otherTag.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public /*@ safe @*/ void addPerson(/*@ non_null @*/ PersonInterface person) {
        if (person != null) { // Added null check for robustness
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

        int mean = getAgeMean(); // Calculate the mean age first
        long varianceSum = 0;
        for (PersonInterface person : persons.values()) {
            long diff = person.getAge() - mean; // Difference from mean
            varianceSum += diff * diff;       // Sum of squares
        }

        return (int) (varianceSum / size);
    }

    @Override
    public /*@ safe @*/ void delPerson(/*@ non_null @*/ PersonInterface person) {
        if (person != null) { // Added null check for robustness
            // Remove the person using their ID as the key
            persons.remove(person.getId());
        }
    }

    @Override
    public /*@ pure @*/ int getSize() {
        return persons.size();
    }

    public Map<Integer, PersonInterface> getPersons() {
        return this.persons;
    }
}