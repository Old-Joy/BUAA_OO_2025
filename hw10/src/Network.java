import com.oocourse.spec2.main.NetworkInterface;
import com.oocourse.spec2.exceptions.AcquaintanceNotFoundException;
import com.oocourse.spec2.exceptions.ArticleIdNotFoundException;
import com.oocourse.spec2.exceptions.ContributePermissionDeniedException;
import com.oocourse.spec2.exceptions.DeleteArticlePermissionDeniedException;
import com.oocourse.spec2.exceptions.DeleteOfficialAccountPermissionDeniedException;
import com.oocourse.spec2.exceptions.EqualArticleIdException;
import com.oocourse.spec2.exceptions.EqualOfficialAccountIdException;
import com.oocourse.spec2.exceptions.EqualPersonIdException;
import com.oocourse.spec2.exceptions.EqualRelationException;
import com.oocourse.spec2.exceptions.EqualTagIdException;
import com.oocourse.spec2.exceptions.OfficialAccountIdNotFoundException;
import com.oocourse.spec2.exceptions.PathNotFoundException;
import com.oocourse.spec2.exceptions.PersonIdNotFoundException;
import com.oocourse.spec2.exceptions.RelationNotFoundException;
import com.oocourse.spec2.exceptions.TagIdNotFoundException;
import com.oocourse.spec2.main.OfficialAccountInterface;
import com.oocourse.spec2.main.PersonInterface;
import com.oocourse.spec2.main.TagInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class Network implements NetworkInterface {

    private final Map<Integer, PersonInterface> personsMap;
    private final Map<Integer, OfficialAccountInterface> accountsMap;
    private final Map<Integer, Integer> articleToContributorMap;
    private final Set<Integer> articlesSet;
    private long totalTripleSum = 0;

    private static class PathCacheKey {
        private final int id1;
        private final int id2;

        public PathCacheKey(int id1, int id2) {
            if (id1 < id2) {
                this.id1 = id1;
                this.id2 = id2;
            } else {
                this.id1 = id2;
                this.id2 = id1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            PathCacheKey that = (PathCacheKey) o;
            return id1 == that.id1 && id2 == that.id2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id1, id2);
        }
    }

    private Map<PathCacheKey, Integer> shortestPathCache;
    private static final int PATH_NOT_FOUND_SENTINEL = -1; // 特殊值表示不可达

    public Network() {
        this.personsMap = new HashMap<>();
        this.accountsMap = new HashMap<>();
        this.articleToContributorMap = new HashMap<>();
        this.articlesSet = new HashSet<>();
        this.totalTripleSum = 0;
        this.shortestPathCache = new HashMap<>(); // 初始化缓存
    }

    private void invalidateShortestPathCache() {
        this.shortestPathCache.clear();
    }

    private Map<Integer, PersonInterface> getPersonNeighbors(PersonInterface person) {
        if (person instanceof Person) {
            return ((Person) person).getAcquaintances(); // 假设 Person 类有 getAcquaintances
        } else if (person != null) {
            Map<Integer, PersonInterface> neighbors = new HashMap<>();
            for (PersonInterface potentialNeighbor : personsMap.values()) {
                if (person.getId() != potentialNeighbor.getId()
                    && person.isLinked(potentialNeighbor)) {
                    neighbors.put(potentialNeighbor.getId(), potentialNeighbor);
                }
            }
            return neighbors;
        }
        return Collections.emptyMap();
    }

    private int countCommonNeighbors(PersonInterface p1, PersonInterface p2) {
        if (p1 == null || p2 == null) { return 0; }
        Map<Integer, PersonInterface> acq1 = getPersonNeighbors(p1);
        Map<Integer, PersonInterface> acq2 = getPersonNeighbors(p2);
        Set<Integer> neighbors1 = (acq1 != null) ? acq1.keySet() : Collections.emptySet();
        Set<Integer> neighbors2 = (acq2 != null) ? acq2.keySet() : Collections.emptySet();
        if (neighbors1.isEmpty() || neighbors2.isEmpty()) {
            return 0;
        }
        Set<Integer> smallerSet = (neighbors1.size() < neighbors2.size()) ? neighbors1 : neighbors2;
        Set<Integer> largerSet = (neighbors1.size() < neighbors2.size()) ? neighbors2 : neighbors1;
        int commonCount = 0;
        for (Integer neighborId : smallerSet) {
            if (largerSet.contains(neighborId)) {
                commonCount++;
            }
        }
        return commonCount;
    }

    private void notifyCommonTagsRelationUpdate(PersonInterface p1,
        PersonInterface p2, int change) {
        if (!(p1 instanceof Person) || !(p2 instanceof Person)) { return; }
        Map<Integer, TagInterface> tags1 = ((Person) p1).getAssociatedTags();
        Map<Integer, TagInterface> tags2 = ((Person) p2).getAssociatedTags();
        if (tags1 == null || tags2 == null || tags1.isEmpty() || tags2.isEmpty()) { return; }
        Set<Integer> commonTagIds = new HashSet<>(tags1.keySet());
        commonTagIds.retainAll(tags2.keySet());
        for (Integer tagId : commonTagIds) {
            TagInterface commonTag = tags1.get(tagId); // 从任一map获取即可，因为是交集
            if (commonTag instanceof Tag) { // 确保是我们自定义的 Tag 类型
                ((Tag) commonTag).updateRelationValue(p1, p2, change);
            } else {
                // System.err.println("警告: Tag " + tagId + " 不是自定义 Tag 类型，无法更新 valueSum");
            }
        }
    }

    @Override
    public boolean containsPerson(int id) {
        return personsMap.containsKey(id);
    }

    @Override
    public PersonInterface getPerson(int id) {
        return personsMap.get(id);
    }

    @Override
    public void addPerson(/*@ non_null @*/PersonInterface person) throws EqualPersonIdException {
        if (person == null) {
            return;
        }
        if (containsPerson(person.getId())) {
            throw new EqualPersonIdException(person.getId());
        }
        personsMap.put(person.getId(), person);
    }

    @Override
    public void addRelation(int id1, int id2, int value) throws
        PersonIdNotFoundException, EqualRelationException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (p1.isLinked(p2)) { // Catches both id1==id2 and id1!=id2 but linked
            throw new EqualRelationException(id1, id2);
        }
        int commonNeighbors = countCommonNeighbors(p1, p2); // 在链接前计算
        if (p1 instanceof Person && p2 instanceof Person) {
            ((Person) p1).addAcquaintance(p2, value);
            ((Person) p2).addAcquaintance(p1, value);
        } else {
            System.err.println("错误: addRelation 需要 Person 实现来更新内部熟人列表。");
            return;
        }
        this.totalTripleSum += commonNeighbors;
        notifyCommonTagsRelationUpdate(p1, p2, value); // value 是新关系的绝对值
        invalidateShortestPathCache(); // 图结构变化，清空缓存
    }

    @Override
    public void modifyRelation(int id1, int id2, int value) throws // value is delta
        PersonIdNotFoundException, EqualPersonIdException, RelationNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) { throw new EqualPersonIdException(id1); }
        if (!p1.isLinked(p2)) { throw new RelationNotFoundException(id1, id2); }
        if (!(p1 instanceof Person && p2 instanceof Person)) {
            System.err.println("错误: modifyRelation 需要 Person 实现来操作具体数据");
            return;
        }
        Person concreteP1 = (Person) p1;
        Person concreteP2 = (Person) p2;
        int oldValue = concreteP1.queryValue(concreteP2);
        int newValue = oldValue + value; // value is delta
        boolean structureChanged = false;
        if (newValue <= 0) { // 关系破裂
            notifyCommonTagsRelationUpdate(p1, p2, -oldValue); // **关键修正: 通知 Tag 贡献清零**
            structureChanged = true;
            int commonNeighbors = countCommonNeighbors(p1, p2); // 在解除链接前计算
            concreteP1.removeAcquaintance(concreteP2);
            concreteP2.removeAcquaintance(concreteP1);
            this.totalTripleSum -= commonNeighbors;
            Map<Integer, TagInterface> tags1 = concreteP1.getOwnedTags();
            if (tags1 != null) {
                for (TagInterface tag : tags1.values()) {
                    if (tag.hasPerson(concreteP2)) {
                        tag.delPerson(concreteP2);
                        // 确保 Person 也更新其 associatedTags
                        concreteP2.disassociateFromTag(tag.getId());
                    }
                }
            }
            Map<Integer, TagInterface> tags2 = concreteP2.getOwnedTags();
            if (tags2 != null) {
                for (TagInterface tag : tags2.values()) {
                    if (tag.hasPerson(concreteP1)) {
                        tag.delPerson(concreteP1);
                        concreteP1.disassociateFromTag(tag.getId());
                    }
                }
            }
        } else { // newValue > 0，关系值修改但未破裂
            notifyCommonTagsRelationUpdate(p1, p2, value); // value 是 delta
            concreteP1.modifyRelationValue(concreteP2, newValue);
            concreteP2.modifyRelationValue(concreteP1, newValue);
        }
        if (structureChanged) {
            invalidateShortestPathCache();
        }
    }

    @Override
    public int queryValue(int id1, int id2) throws
        PersonIdNotFoundException, RelationNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) {
            return 0;
        }
        if (!p1.isLinked(p2)) {
            throw new RelationNotFoundException(id1, id2);
        }
        return p1.queryValue(p2);
    }

    @Override
    public boolean isCircle(int id1, int id2) throws PersonIdNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) { return true; }
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.offer(id1);
        visited.add(id1);
        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();
            PersonInterface currentPerson = personsMap.get(currentId);
            if (currentPerson == null) { continue; }
            Map<Integer, PersonInterface> neighbors = getPersonNeighbors(currentPerson);
            if (neighbors != null) {
                for (Integer neighborId : neighbors.keySet()) {
                    if (neighborId.equals(id2)) {
                        return true;
                    }
                    if (visited.add(neighborId)) {
                        queue.offer(neighborId);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int queryTripleSum() {
        if (totalTripleSum > Integer.MAX_VALUE) { return Integer.MAX_VALUE; }
        if (totalTripleSum < Integer.MIN_VALUE) { return Integer.MIN_VALUE; }
        return (int) totalTripleSum;
    }

    @Override
    public void addTag(int personId, /*@ non_null @*/TagInterface tag) throws
        PersonIdNotFoundException, EqualTagIdException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (tag == null) { return; } // Defensive, JML guarantees non_null
        if (person.containsTag(tag.getId())) {
            throw new EqualTagIdException(tag.getId());
        }
        if (person instanceof Person) {
            ((Person) person).addTag(tag); // Person 记录拥有的 Tag
        } else {
            System.err.println("错误: addTag 需要 Person 实现");
        }
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
        if (tag.getSize() < 1000) {
            tag.addPerson(p1);
            if (p1 instanceof Person) {
                ((Person) p1).associateWithTag(tag);
            } else {
                System.err.println("错误: addPersonToTag p1 需要 Person 实现");
            }
        }
    }

    @Override
    public int queryTagValueSum(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        TagInterface tag = person.getTag(tagId);
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        return tag.getValueSum();
    }

    @Override
    public int queryTagAgeVar(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        TagInterface tag = person.getTag(tagId);
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        return tag.getAgeVar();
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
        if (!tag.hasPerson(p1)) {
            throw new PersonIdNotFoundException(personId1);
        }
        tag.delPerson(p1);
        if (p1 instanceof Person) {
            ((Person) p1).disassociateFromTag(tagId);
        } else {
            System.err.println("错误: delPersonFromTag p1 需要 Person 实现");
        }
    }

    @Override
    public void delTag(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        if (!(person instanceof Person)) {
            System.err.println("Error: delTag person 需要 Person 实现");
            return;
        }
        Person concretePerson = (Person) person;
        TagInterface tagToRemove = concretePerson.getTag(tagId);
        concretePerson.delTag(tagId);
        if (tagToRemove instanceof Tag) {
            Tag concreteTag = (Tag) tagToRemove;
            Map<Integer, PersonInterface> personsInTag = concreteTag.getPersonsInternal();
            if (personsInTag != null) {
                new HashSet<>(personsInTag.keySet()).forEach(pIdInTag -> {
                    PersonInterface personInTag = personsMap.get(pIdInTag);
                    if (personInTag instanceof Person) {
                        ((Person) personInTag).disassociateFromTag(tagId);
                    }
                });
            }
        }
    }

    @Override
    public int queryBestAcquaintance(int id) throws
        PersonIdNotFoundException, AcquaintanceNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        if (!(person instanceof Person)) {
            System.err.println("Error: queryBestAcquaintance 需要 Person 实现");
            throw new AcquaintanceNotFoundException(id); // Or other handling
        }
        return ((Person) person).getBestAcquaintanceIdInternal();
    }

    @Override
    public int queryCoupleSum() {
        int coupleCount = 0;
        Map<Integer, Integer> bestAcquaintanceCache = new HashMap<>();
        for (PersonInterface p : personsMap.values()) {
            if (p instanceof Person && ((Person) p).hasAcquaintancesInternal()) {
                try {
                    bestAcquaintanceCache.put(p.getId(), queryBestAcquaintance(p.getId()));
                } catch (PersonIdNotFoundException | AcquaintanceNotFoundException e) {
                    // This person might not exist or have no acquaintances, ignore.
                }
            }
        }
        Set<Integer> countedIds = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : bestAcquaintanceCache.entrySet()) {
            int p1Id = entry.getKey();
            int bestForP1 = entry.getValue();
            if (!countedIds.contains(p1Id) && bestAcquaintanceCache.containsKey(bestForP1)) {
                int bestIdForP2 = bestAcquaintanceCache.get(bestForP1);
                if (bestIdForP2 == p1Id) {
                    coupleCount++;
                    countedIds.add(p1Id);
                    countedIds.add(bestForP1);
                }
            }
        }
        return coupleCount;
    }

    @Override
    public int queryShortestPath(int id1, int id2) throws
        PersonIdNotFoundException, PathNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) { return 0; }
        PathCacheKey cacheKey = new PathCacheKey(id1, id2);
        if (shortestPathCache.containsKey(cacheKey)) {
            int cachedDistance = shortestPathCache.get(cacheKey);
            if (cachedDistance == PATH_NOT_FOUND_SENTINEL) {
                throw new PathNotFoundException(id1, id2); }
            return cachedDistance; }
        Queue<Integer> q1 = new LinkedList<>();
        q1.offer(id1);
        Map<Integer, Integer> d1 = new HashMap<>();
        d1.put(id1, 0);
        Queue<Integer> q2 = new LinkedList<>();
        q2.offer(id2);
        Map<Integer, Integer> d2 = new HashMap<>();
        d2.put(id2, 0);
        int pathLength = Integer.MAX_VALUE;
        while (!q1.isEmpty() && !q2.isEmpty()) {
            int currentDistFromQ1 = q1.isEmpty() ? Integer.MAX_VALUE : d1.get(q1.peek());
            int currentDistFromQ2 = q2.isEmpty() ? Integer.MAX_VALUE : d2.get(q2.peek());
            if (currentDistFromQ1 + currentDistFromQ2 >= pathLength) { break; }
            if (d1.size() <= d2.size()) {
                if (!q1.isEmpty()) {
                    int u = q1.poll();
                    Map<Integer, PersonInterface> neighborsU =
                        getPersonNeighbors(personsMap.get(u));
                    if (neighborsU != null) {
                        for (Integer v : neighborsU.keySet()) {
                            if (!d1.containsKey(v)) {
                                d1.put(v, d1.get(u) + 1);
                                q1.offer(v);
                                if (d2.containsKey(v)) {
                                    pathLength = Math.min(pathLength, d1.get(v) + d2.get(v)); } }
                        } } }
            } else {
                if (!q2.isEmpty()) {
                    int u = q2.poll();
                    if (d2.get(u) + 1 >= pathLength) { continue; }
                    Map<Integer, PersonInterface> neighborsU =
                        getPersonNeighbors(personsMap.get(u));
                    if (neighborsU != null) {
                        for (Integer v : neighborsU.keySet()) {
                            if (!d2.containsKey(v)) {
                                d2.put(v, d2.get(u) + 1);
                                q2.offer(v);
                                if (d1.containsKey(v)) {
                                    pathLength = Math.min(pathLength, d2.get(v) + d1.get(v));
                                } } } } } } }
        if (pathLength != Integer.MAX_VALUE) {
            shortestPathCache.put(cacheKey, pathLength);
            return pathLength;
        } else {
            shortestPathCache.put(cacheKey, PATH_NOT_FOUND_SENTINEL);
            throw new PathNotFoundException(id1, id2);
        }
    }

    @Override
    public boolean containsAccount(int id) {
        return accountsMap.containsKey(id);
    }

    @Override
    public void createOfficialAccount(int personId, int accountId, String name)
        throws PersonIdNotFoundException, EqualOfficialAccountIdException {
        PersonInterface owner = getPerson(personId);
        if (owner == null) { throw new PersonIdNotFoundException(personId); }
        if (containsAccount(accountId)) { throw new EqualOfficialAccountIdException(accountId); }
        OfficialAccountInterface newAccount = new OfficialAccount(personId, accountId, name);
        accountsMap.put(accountId, newAccount);
        if (newAccount instanceof OfficialAccount) {
            ((OfficialAccount) newAccount).addFollowerInternal(owner, 0);
        } else {
            System.err.println("Error: createOfficialAccount needs OfficialAccount instance");
        }
    }

    @Override
    public void deleteOfficialAccount(int personId, int accountId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        DeleteOfficialAccountPermissionDeniedException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (account.getOwnerId() != personId) {
            throw new DeleteOfficialAccountPermissionDeniedException(personId, accountId);
        }
        accountsMap.remove(accountId);
    }

    @Override
    public boolean containsArticle(int id) {
        return articlesSet.contains(id);
    }

    @Override
    public void contributeArticle(int personId, int accountId, int articleId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        EqualArticleIdException, ContributePermissionDeniedException {
        PersonInterface contributor = getPerson(personId);
        if (contributor == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (containsArticle(articleId)) { throw new EqualArticleIdException(articleId); }
        if (!account.containsFollower(contributor)) {
            throw new ContributePermissionDeniedException(personId, articleId);
        }
        articlesSet.add(articleId);
        articleToContributorMap.put(articleId, personId);
        if (account instanceof OfficialAccount) {
            OfficialAccount concreteAccount = (OfficialAccount) account;
            concreteAccount.addArticleInternal(articleId);
            concreteAccount.incrementContributionInternal(personId);
            List<PersonInterface> followers = concreteAccount.getFollowersInternal();
            if (followers != null) {
                for (PersonInterface follower : followers) {
                    if (follower instanceof Person) {
                        ((Person) follower).addReceivedArticle(articleId);
                    }
                }
            }
        } else {
            System.err.println("Error: contributeArticle needs OfficialAccount instance");
        }
    }

    @Override
    public void deleteArticle(int personId, int accountId, int articleId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        ArticleIdNotFoundException, DeleteArticlePermissionDeniedException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (!account.containsArticle(articleId)) {
            throw new ArticleIdNotFoundException(articleId);
        }
        if (account.getOwnerId() != personId) {
            throw new DeleteArticlePermissionDeniedException(personId, articleId);
        }
        Integer contributorId = articleToContributorMap.get(articleId);
        if (account instanceof OfficialAccount) {
            OfficialAccount concreteAccount = (OfficialAccount) account;
            concreteAccount.removeArticleInternal(articleId);
            if (contributorId != null) {
                PersonInterface actualContributor = getPerson(contributorId);
                if (actualContributor != null
                    && concreteAccount.containsFollower(actualContributor)) {
                    concreteAccount.decrementContributionInternal(contributorId);
                }
            }
            List<PersonInterface> followers = concreteAccount.getFollowersInternal();
            if (followers != null) {
                for (PersonInterface follower : followers) {
                    if (follower instanceof Person) {
                        ((Person) follower).removeReceivedArticle(articleId);
                    }
                }
            }
        } else {
            System.err.println("Error: deleteArticle needs OfficialAccount instance");
        }
    }

    @Override
    public void followOfficialAccount(int personId, int accountId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        EqualPersonIdException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (account.containsFollower(person)) {
            throw new EqualPersonIdException(personId);
        }
        if (account instanceof OfficialAccount) {
            ((OfficialAccount) account).addFollowerInternal(person, 0);
        } else {
            System.err.println("错误: followOfficialAccount 需要具体的 OfficialAccount 实现。");
        }
    }

    @Override
    public int queryBestContributor(int id) throws OfficialAccountIdNotFoundException {
        OfficialAccountInterface account = accountsMap.get(id);
        if (account == null) { throw new OfficialAccountIdNotFoundException(id); }
        return account.getBestContributor();
    }

    @Override
    public List<Integer> queryReceivedArticles(int id) throws PersonIdNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        return person.queryReceivedArticles();
    }

    // 用于通过 Junit 编译的桩方法
    public PersonInterface[] getPersons() {
        return null;
    }
}