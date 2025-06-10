import com.oocourse.spec3.main.OfficialAccountInterface; // Updated import
import com.oocourse.spec3.main.PersonInterface;         // Updated import
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections; // For unmodifiable lists

public class OfficialAccount implements OfficialAccountInterface {

    private final int ownerId;
    private final int id;
    private final String name;
    private final Map<Integer, PersonInterface> followerMap;
    private final Map<Integer, Integer> contributionMap;
    private final Set<Integer> articlesSet;

    public OfficialAccount(int ownerId, int id, String name) {
        this.ownerId = ownerId;
        this.id = id;
        this.name = name;
        this.followerMap = new HashMap<>();
        this.contributionMap = new HashMap<>();
        this.articlesSet = new HashSet<>();
    }

    @Override
    public /*@ pure @*/ int getOwnerId() {
        return this.ownerId;
    }

    @Override
    public /*@ safe @*/ void addFollower(/*@ non_null @*/ PersonInterface person) {
        // JML: requires !containsFollower(person); ensures containsFollower(person);
        // JML: ensures (\exists int i; ... contributions[i] == 0);
        if (person != null && !followerMap.containsKey(person.getId())) {
            followerMap.put(person.getId(), person);
            contributionMap.put(person.getId(), 0);
        }
    }

    // Your internal method, seems fine.
    public /*@ safe @*/ void addFollowerInternal(PersonInterface person, int initialContribution) {
        if (person != null && !followerMap.containsKey(person.getId())) {
            followerMap.put(person.getId(), person);
            contributionMap.put(person.getId(), initialContribution);
        }
    }

    @Override
    public /*@ pure @*/ boolean containsFollower(PersonInterface person) {
        if (person == null) {
            return false;
        }
        return followerMap.containsKey(person.getId());
    }

    @Override
    public /*@ safe @*/ void addArticle(/*@ non_null @*/ PersonInterface person, int articleId) {
        if (!articlesSet.contains(articleId)) {
            articlesSet.add(articleId);
            if (person != null && contributionMap.containsKey(person.getId())) {
                contributionMap.put(person.getId(), contributionMap.get(person.getId()) + 1);
            }
        }
    }

    public /*@ safe @*/ void addArticleInternal(int articleId) {
        articlesSet.add(articleId);
    }

    public /*@ safe @*/ void incrementContributionInternal(int personId) {
        if (contributionMap.containsKey(personId)) {
            contributionMap.put(personId, contributionMap.get(personId) + 1);
        }
    }

    public /*@ safe @*/ void decrementContributionInternal(int personId) {
        if (contributionMap.containsKey(personId)) {
            contributionMap.put(personId, contributionMap.get(personId) - 1);
        }
    }

    @Override
    public /*@ pure @*/ boolean containsArticle(int id) {
        return articlesSet.contains(id);
    }

    @Override
    public /*@ safe @*/ void removeArticle(int id) {
        articlesSet.remove(id);
    }

    // Your internal method, seems fine.
    public /*@ safe @*/ void removeArticleInternal(int articleId) {
        articlesSet.remove(articleId);
    }

    @Override
    public /*@ pure @*/ int getBestContributor() {
        if (contributionMap.isEmpty()) {
            if (followerMap.isEmpty()) { return 0; }
        }

        int maxContribution = Integer.MIN_VALUE;
        boolean foundAny = false;
        for (int contribution : contributionMap.values()) {
            if (contribution > maxContribution) {
                maxContribution = contribution;
            }
            foundAny = true;
        }
        if (!foundAny && followerMap.containsKey(this.ownerId)) {
            return this.ownerId;
        }
        if (!foundAny) { return 0; }
        int bestId = Integer.MAX_VALUE;
        boolean foundBest = false;
        for (Map.Entry<Integer, Integer> entry : contributionMap.entrySet()) {
            if (entry.getValue() == maxContribution) {
                if (entry.getKey() < bestId) {
                    bestId = entry.getKey();
                    foundBest = true;
                }
            }
        }
        return foundBest ? bestId : 0;
    }

    public /*@ pure @*/ List<PersonInterface> getFollowersInternal() {
        return new ArrayList<>(followerMap.values());
    }

    public /*@ pure @*/ Set<Integer> getArticlesInternal() {
        return Collections.unmodifiableSet(this.articlesSet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof OfficialAccountInterface)) { // Check against interface
            return false;
        }
        OfficialAccountInterface other = (OfficialAccountInterface) obj;

        if (obj instanceof OfficialAccount) { // If concrete type for optimization
            return this.id == ((OfficialAccount) obj).id;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id);
    }

    public int getId() {
        return id;
    }
}