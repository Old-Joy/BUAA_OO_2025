import com.oocourse.spec3.exceptions.AcquaintanceNotFoundException;
import com.oocourse.spec3.exceptions.ArticleIdNotFoundException;
import com.oocourse.spec3.exceptions.ContributePermissionDeniedException;
import com.oocourse.spec3.exceptions.DeleteArticlePermissionDeniedException;
import com.oocourse.spec3.exceptions.DeleteOfficialAccountPermissionDeniedException;
import com.oocourse.spec3.exceptions.EmojiIdNotFoundException;
import com.oocourse.spec3.exceptions.EqualArticleIdException;
import com.oocourse.spec3.exceptions.EqualEmojiIdException;
import com.oocourse.spec3.exceptions.EqualMessageIdException;
import com.oocourse.spec3.exceptions.EqualOfficialAccountIdException;
import com.oocourse.spec3.exceptions.EqualPersonIdException;
import com.oocourse.spec3.exceptions.EqualRelationException;
import com.oocourse.spec3.exceptions.EqualTagIdException;
import com.oocourse.spec3.exceptions.MessageIdNotFoundException;
import com.oocourse.spec3.exceptions.OfficialAccountIdNotFoundException;
import com.oocourse.spec3.exceptions.PersonIdNotFoundException;
import com.oocourse.spec3.exceptions.PathNotFoundException;
import com.oocourse.spec3.exceptions.RelationNotFoundException;
import com.oocourse.spec3.exceptions.TagIdNotFoundException;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;
import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.EmojiMessageInterface;
import com.oocourse.spec3.main.RedEnvelopeMessageInterface;
import com.oocourse.spec3.main.ForwardMessageInterface;
import com.oocourse.spec3.main.OfficialAccountInterface;
import com.oocourse.spec3.main.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Iterator;

public class Network implements NetworkInterface {
    private final Map<Integer, PersonInterface> personsMap  = new HashMap<>();
    private final Map<Integer, OfficialAccountInterface> accountsMap = new HashMap<>();
    private final Map<Integer, Integer> articleToContributorMap = new HashMap<>();
    private final Set<Integer> articlesSet = new HashSet<>();
    private long totalTripleSum = 0;
    private final Map<Integer, MessageInterface> messagesMap = new HashMap<>();
    private final List<Integer> emojiIdList = new ArrayList<>();
    private final List<Integer> emojiHeatList = new ArrayList<>();
    private final Map<Integer, Integer> emojiIdToIndexMap = new HashMap<>();
    private Map<PathCacheKey, Integer> shortestPathCache  = new HashMap<>();
    private static final int PATH_NOT_FOUND_SENTINEL = -1;
    private final Map<TagKey, TagInterface> globalTagsMap  = new HashMap<>();
    private final Map<Integer, Set<TagKey>> personIdToMemberOfTagsMap  = new HashMap<>();

    private static class PathCacheKey {
        private final int id1;
        private final int id2;

        public PathCacheKey(int id1, int id2) {
            if (id1 < id2) {
                this.id1 = id1;
                this.id2 = id2;
            } else {
                this.id1 = id2;
                this.id2 = id1; } }

        @Override public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            PathCacheKey that = (PathCacheKey) o;
            return id1 == that.id1 && id2 == that.id2; }

        @Override public int hashCode() { return Objects.hash(id1, id2); } }

    private static class TagKey {
        private final int ownerId;
        private final int tagId;

        public TagKey(int ownerId, int tagId) {
            this.ownerId = ownerId;
            this.tagId = tagId;
        }

        @Override public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            TagKey tagKey = (TagKey) o;
            return ownerId == tagKey.ownerId && tagId == tagKey.tagId; }

        @Override public int hashCode() { return Objects.hash(ownerId, tagId); } }

    public Network() { this.totalTripleSum = 0; }

    private void invalidateShortestPathCache() { this.shortestPathCache.clear(); }

    private Map<Integer, PersonInterface> getPersonNeighbors(PersonInterface person) {
        if (person instanceof Person) { return ((Person) person).getAcquaintancesInternal(); }
        else if (person != null) {
            Map<Integer, PersonInterface> neighbors = new HashMap<>();
            for (PersonInterface thatNetworkPerson : personsMap.values()) {
                if (person.getId() != thatNetworkPerson.getId()
                    && person.isLinked(thatNetworkPerson)) {
                    neighbors.put(thatNetworkPerson.getId(), thatNetworkPerson); } }
            return neighbors; }
        return Collections.emptyMap(); }

    private int countCommonNeighbors(PersonInterface p1, PersonInterface p2) {
        if (p1 == null || p2 == null) { return 0; }
        Map<Integer, PersonInterface> acq1Map = getPersonNeighbors(p1);
        Map<Integer, PersonInterface> acq2Map = getPersonNeighbors(p2);
        if (acq1Map.isEmpty() || acq2Map.isEmpty()) { return 0; }
        Set<Integer> neighbors1 = acq1Map.keySet();
        Set<Integer> neighbors2 = acq2Map.keySet();
        Set<Integer> smallerSet = (neighbors1.size() < neighbors2.size()) ? neighbors1 : neighbors2;
        Set<Integer> largerSet = (neighbors1.size() < neighbors2.size()) ? neighbors2 : neighbors1;
        int count = 0;
        for (Integer neighborId : smallerSet) { if (largerSet.contains(neighborId)) { count++; } }
        return count; }

    private void notifyCommonTagsRelationUpdate(PersonInterface p1,
        PersonInterface p2, int changeInValue) {
        if (!(p1 instanceof Person) || !(p2 instanceof Person)) { return; }
        Set<TagKey> tags1 = personIdToMemberOfTagsMap.get(p1.getId());
        Set<TagKey> tags2 = personIdToMemberOfTagsMap.get(p2.getId());
        if (tags1 == null || tags2 == null || tags1.isEmpty() || tags2.isEmpty()) { return; }
        Set<TagKey> commonTagKeys = new HashSet<>(tags1);
        commonTagKeys.retainAll(tags2);
        for (TagKey commonKey : commonTagKeys) {
            TagInterface tag = globalTagsMap.get(commonKey);
            if (tag instanceof Tag) { ((Tag) tag).updateValue(p1, p2, changeInValue); } } }

    @Override public boolean containsPerson(int id) { return personsMap.containsKey(id); }

    @Override public PersonInterface getPerson(int id) { return personsMap.get(id); }

    @Override public void addPerson(PersonInterface person)
        throws EqualPersonIdException {
        if (containsPerson(person.getId())) {
            throw new EqualPersonIdException(person.getId()); }
        personsMap.put(person.getId(), person); }

    @Override public void addRelation(int id1, int id2, int value) throws
        PersonIdNotFoundException, EqualRelationException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (p1.isLinked(p2)) { throw new EqualRelationException(id1, id2); }
        int commonNeighbors = countCommonNeighbors(p1, p2);
        if (p1 instanceof Person && p2 instanceof Person) {
            ((Person) p1).addAcquaintance(p2, value);
            ((Person) p2).addAcquaintance(p1, value); }
        this.totalTripleSum += commonNeighbors;
        notifyCommonTagsRelationUpdate(p1, p2, value);
        invalidateShortestPathCache(); }

    @Override public void modifyRelation(int id1, int id2, int value) throws
        PersonIdNotFoundException, EqualPersonIdException, RelationNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 == id2) { throw new EqualPersonIdException(id1); }
        if (!p1.isLinked(p2)) { throw new RelationNotFoundException(id1, id2); }
        Person concreteP1 = (Person) p1;
        Person concreteP2 = (Person) p2;
        int oldValue = concreteP1.queryValue(concreteP2);
        int newValue = oldValue + value;
        boolean structureChanged = false;
        if (newValue <= 0) {
            notifyCommonTagsRelationUpdate(p1, p2, -oldValue);
            structureChanged = true;
            int commonNeighborsBeforeBreak = countCommonNeighbors(concreteP1, concreteP2);
            concreteP1.removeAcquaintance(concreteP2);
            concreteP2.removeAcquaintance(concreteP1);
            this.totalTripleSum -= commonNeighborsBeforeBreak;
            Map<Integer, TagInterface> p1OwnedTags = concreteP1.getOwnedTagsInternal();
            if (p1OwnedTags != null) {
                for (TagInterface tag : p1OwnedTags.values()) {
                    if (tag.hasPerson(concreteP2)) {
                        tag.delPerson(concreteP2);
                        TagKey keyOfTagOwnedByP1 = new TagKey(p1.getId(), tag.getId());
                        Set<TagKey> p2MemberOfSet = personIdToMemberOfTagsMap.get(p2.getId());
                        if (p2MemberOfSet != null) {
                            p2MemberOfSet.remove(keyOfTagOwnedByP1);
                            if (p2MemberOfSet.isEmpty()) {
                                personIdToMemberOfTagsMap.remove(p2.getId()); } } } } }
            Map<Integer, TagInterface> p2OwnedTags = concreteP2.getOwnedTagsInternal();
            if (p2OwnedTags != null) {
                for (TagInterface tag : p2OwnedTags.values()) {
                    if (tag.hasPerson(concreteP1)) {
                        tag.delPerson(concreteP1);
                        TagKey keyOfTagOwnedByP2 = new TagKey(p2.getId(), tag.getId());
                        Set<TagKey> p1MemberOfSet = personIdToMemberOfTagsMap.get(p1.getId());
                        if (p1MemberOfSet != null) {
                            p1MemberOfSet.remove(keyOfTagOwnedByP2);
                            if (p1MemberOfSet.isEmpty()) {
                                personIdToMemberOfTagsMap.remove(p1.getId()); } } } } }
        } else {
            notifyCommonTagsRelationUpdate(p1, p2, value);
            concreteP1.modifyRelationValue(concreteP2, newValue);
            concreteP2.modifyRelationValue(concreteP1, newValue); }
        if (structureChanged) { invalidateShortestPathCache(); } }

    @Override public int queryValue(int id1, int id2) throws
        PersonIdNotFoundException, RelationNotFoundException {
        PersonInterface p1 = getPerson(id1);
        if (p1 == null) { throw new PersonIdNotFoundException(id1); }
        PersonInterface p2 = getPerson(id2);
        if (p2 == null) { throw new PersonIdNotFoundException(id2); }
        if (id1 != id2 && !p1.isLinked(p2)) {
            throw new RelationNotFoundException(id1, id2); }
        return p1.queryValue(p2); }

    @Override public boolean isCircle(int id1, int id2) throws PersonIdNotFoundException {
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
                    if (neighborId.equals(id2)) { return true; }
                    if (!visited.contains(neighborId)) {
                        visited.add(neighborId);
                        queue.offer(neighborId); } } } }
        return false; }

    @Override public int queryTripleSum() {
        if (totalTripleSum > Integer.MAX_VALUE) { return Integer.MAX_VALUE; }
        if (totalTripleSum < Integer.MIN_VALUE) { return Integer.MIN_VALUE; }
        return (int) totalTripleSum; }

    @Override public void addTag(int personId, TagInterface tag) throws
        PersonIdNotFoundException, EqualTagIdException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        TagKey newTagKey = new TagKey(personId, tag.getId());
        if (person.containsTag(tag.getId())) {
            throw new EqualTagIdException(tag.getId()); }
        globalTagsMap.put(newTagKey, tag);
        person.addTag(tag); }

    @Override public void addPersonToTag(int personId1, int personId2, int tagId) throws
        PersonIdNotFoundException, RelationNotFoundException,
        TagIdNotFoundException, EqualPersonIdException {
        PersonInterface p1 = getPerson(personId1);
        if (p1 == null) { throw new PersonIdNotFoundException(personId1); }
        PersonInterface p2 = getPerson(personId2);
        if (p2 == null) { throw new PersonIdNotFoundException(personId2); }
        if (personId1 == personId2) { throw new EqualPersonIdException(personId1); }
        if (!p2.isLinked(p1)) { throw new RelationNotFoundException(personId1, personId2); }
        if (!p2.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        TagKey targetTagKey = new TagKey(personId2, tagId);
        TagInterface tagInstance = globalTagsMap.get(targetTagKey);
        if (tagInstance == null) { throw new TagIdNotFoundException(tagId); }
        if (tagInstance.hasPerson(p1)) { throw new EqualPersonIdException(personId1); }
        if (tagInstance.getSize() < 1000) {
            tagInstance.addPerson(p1);
            personIdToMemberOfTagsMap.computeIfAbsent(personId1,
                k -> new HashSet<>()).add(targetTagKey); } }

    @Override public int queryTagValueSum(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        TagKey tagKey = new TagKey(personId, tagId);
        TagInterface tag = globalTagsMap.get(tagKey);
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        return tag.getValueSum(); }

    @Override public int queryTagAgeVar(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        TagKey tagKey = new TagKey(personId, tagId);
        TagInterface tag = globalTagsMap.get(tagKey);
        if (!person.containsTag(tagId)) { throw new TagIdNotFoundException(tagId); }
        if (tag == null) { throw new TagIdNotFoundException(tagId); }
        return tag.getAgeVar(); }

    @Override public void delPersonFromTag(int personId1, int personId2, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface p1 = getPerson(personId1);
        if (p1 == null) { throw new PersonIdNotFoundException(personId1); }
        PersonInterface p2 = getPerson(personId2);
        if (p2 == null) { throw new PersonIdNotFoundException(personId2); }
        TagKey targetTagKey = new TagKey(personId2, tagId);
        TagInterface tagInstance = globalTagsMap.get(targetTagKey);
        TagInterface tag = p2.getTag(tagId);
        if (tagInstance == null) { throw new TagIdNotFoundException(tagId); }
        if (!tagInstance.hasPerson(p1)) { throw new PersonIdNotFoundException(personId1); }
        tagInstance.delPerson(p1);
        Set<TagKey> memberOfSet = personIdToMemberOfTagsMap.get(personId1);
        if (memberOfSet != null) {
            memberOfSet.remove(targetTagKey);
            if (memberOfSet.isEmpty()) { personIdToMemberOfTagsMap.remove(personId1); } } }

    @Override public void delTag(int personId, int tagId) throws
        PersonIdNotFoundException, TagIdNotFoundException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        TagKey tagKeyToDelete = new TagKey(personId, tagId);
        TagInterface tagInstanceToDelete = globalTagsMap.get(tagKeyToDelete);
        if (tagInstanceToDelete == null || !person.containsTag(tagId)) {
            throw new TagIdNotFoundException(tagId); }
        person.delTag(tagId);
        globalTagsMap.remove(tagKeyToDelete);
        for (Map.Entry<Integer, Set<TagKey>> entry : personIdToMemberOfTagsMap.entrySet()) {
            Set<TagKey> memberSet = entry.getValue();
            memberSet.remove(tagKeyToDelete); }
        personIdToMemberOfTagsMap.values().removeIf(Set::isEmpty);
    }

    @Override public int queryBestAcquaintance(int id) throws
        PersonIdNotFoundException, AcquaintanceNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        if (person instanceof Person) {
            return ((Person) person).getBestAcquaintanceIdInternal();
        } else { throw new AcquaintanceNotFoundException(id); }
    }

    @Override public int queryCoupleSum() {
        int coupleCount = 0;
        Map<Integer, Integer> bestAcquaintanceCache = new HashMap<>();
        List<PersonInterface> currentPersons = new ArrayList<>(personsMap.values());
        for (PersonInterface p : currentPersons) {
            if (p instanceof Person && ((Person) p).hasAcquaintancesInternal()) {
                try {
                    bestAcquaintanceCache.put(p.getId(), this.queryBestAcquaintance(p.getId()));
                } catch (PersonIdNotFoundException | AcquaintanceNotFoundException e) {
                    /*do-something*/ } } }
        Set<Integer> countedIds = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : bestAcquaintanceCache.entrySet()) {
            int p1Id = entry.getKey();
            int bestForP1 = entry.getValue();
            if (!countedIds.contains(p1Id) && bestAcquaintanceCache.containsKey(bestForP1)) {
                int bestForP2 = bestAcquaintanceCache.get(bestForP1);
                if (bestForP2 == p1Id) {
                    coupleCount++;
                    countedIds.add(p1Id);
                    countedIds.add(bestForP1); } } }
        return coupleCount; }

    @Override public int queryShortestPath(int id1, int id2) throws
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
        Queue<Integer> dotForward = new LinkedList<>();
        Map<Integer, Integer> distFwd = new HashMap<>();
        dotForward.offer(id1);
        distFwd.put(id1, 0);
        Queue<Integer> dotBackward = new LinkedList<>();
        Map<Integer, Integer> distBwd = new HashMap<>();
        dotBackward.offer(id2);
        distBwd.put(id2, 0);
        int shortestPathLen = Integer.MAX_VALUE;
        while (!dotForward.isEmpty() && !dotBackward.isEmpty()) {
            if (distFwd.get(dotForward.peek()) + distBwd.get(dotBackward.peek())
                >= shortestPathLen && shortestPathLen != Integer.MAX_VALUE) {
                break; }
            if (dotForward.size() <= dotBackward.size()) {
                int u = dotForward.poll();
                Map<Integer, PersonInterface> neighborsU = getPersonNeighbors(personsMap.get(u));
                if (neighborsU != null) {
                    for (Integer v : neighborsU.keySet()) {
                        if (!distFwd.containsKey(v)) {
                            distFwd.put(v, distFwd.get(u) + 1);
                            dotForward.offer(v);
                            if (distBwd.containsKey(v)) {
                                shortestPathLen = Math.min(shortestPathLen,
                                    distFwd.get(v) + distBwd.get(v)); } } } }
            } else {
                int u = dotBackward.poll();
                Map<Integer, PersonInterface> neighborsU = getPersonNeighbors(personsMap.get(u));
                if (neighborsU != null) {
                    for (Integer v : neighborsU.keySet()) {
                        if (!distBwd.containsKey(v)) {
                            distBwd.put(v, distBwd.get(u) + 1);
                            dotBackward.offer(v);
                            if (distFwd.containsKey(v)) {
                                shortestPathLen = Math.min(shortestPathLen,
                                    distFwd.get(v) + distBwd.get(v)); } } } } } }
        if (shortestPathLen != Integer.MAX_VALUE) {
            shortestPathCache.put(cacheKey, shortestPathLen);
            return shortestPathLen;
        } else {
            shortestPathCache.put(cacheKey, PATH_NOT_FOUND_SENTINEL);
            throw new PathNotFoundException(id1, id2); } }

    @Override public boolean containsAccount(int id) { return accountsMap.containsKey(id); }

    @Override public void createOfficialAccount(int personId, int accountId, String name)
        throws PersonIdNotFoundException, EqualOfficialAccountIdException {
        PersonInterface owner = getPerson(personId);
        if (owner == null) { throw new PersonIdNotFoundException(personId); }
        if (containsAccount(accountId)) { throw new EqualOfficialAccountIdException(accountId); }
        OfficialAccountInterface newAccount = new OfficialAccount(personId, accountId, name);
        accountsMap.put(accountId, newAccount);
        if (newAccount instanceof OfficialAccount) {
            ((OfficialAccount) newAccount).addFollowerInternal(owner, 0);
        } else { newAccount.addFollower(owner); } }

    @Override public void deleteOfficialAccount(int personId, int accountId)
        throws PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        DeleteOfficialAccountPermissionDeniedException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (account.getOwnerId() != personId) {
            throw new DeleteOfficialAccountPermissionDeniedException(personId, accountId); }
        accountsMap.remove(accountId); }

    @Override public boolean containsArticle(int id) { return articlesSet.contains(id); }

    @Override public void contributeArticle(int personId, int accountId, int articleId) throws
        PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        EqualArticleIdException, ContributePermissionDeniedException {
        PersonInterface contributor = getPerson(personId);
        if (contributor == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (containsArticle(articleId)) { throw new EqualArticleIdException(articleId); }
        if (!account.containsFollower(contributor)) {
            throw new ContributePermissionDeniedException(personId, articleId); }
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
                        ((Person) follower).addReceivedArticleToList(articleId); } } }
        } else { account.addArticle(contributor, articleId); } }

    @Override public void deleteArticle(int personId, int accountId, int articleId) throws
        PersonIdNotFoundException, OfficialAccountIdNotFoundException,
        ArticleIdNotFoundException, DeleteArticlePermissionDeniedException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (!account.containsArticle(articleId)) {
            throw new ArticleIdNotFoundException(articleId); }
        if (account.getOwnerId() != personId) {
            throw new DeleteArticlePermissionDeniedException(personId, articleId); }
        Integer originalContributorId = articleToContributorMap.get(articleId);
        if (account instanceof OfficialAccount) {
            OfficialAccount concreteAccount = (OfficialAccount) account;
            concreteAccount.removeArticleInternal(articleId);
            if (originalContributorId != null) {
                PersonInterface actualContributor = getPerson(originalContributorId);
                if (actualContributor != null &&
                    concreteAccount.containsFollower(actualContributor)) {
                    concreteAccount.decrementContributionInternal(originalContributorId); } }
            List<PersonInterface> followers = concreteAccount.getFollowersInternal();
            if (followers != null) {
                for (PersonInterface follower : followers) {
                    if (follower instanceof Person) {
                        ((Person) follower).removeReceivedArticleInternal(articleId); } } }
        } else { account.removeArticle(articleId); } }

    @Override public void followOfficialAccount(int personId, int accountId) throws
        PersonIdNotFoundException, OfficialAccountIdNotFoundException, EqualPersonIdException {
        PersonInterface person = getPerson(personId);
        if (person == null) { throw new PersonIdNotFoundException(personId); }
        OfficialAccountInterface account = accountsMap.get(accountId);
        if (account == null) { throw new OfficialAccountIdNotFoundException(accountId); }
        if (account.containsFollower(person)) {
            throw new EqualPersonIdException(personId); }
        if (account instanceof OfficialAccount) {
            ((OfficialAccount) account).addFollowerInternal(person, 0);
        } else { account.addFollower(person); } }

    @Override public int queryBestContributor(int id) throws
        OfficialAccountIdNotFoundException {
        OfficialAccountInterface account = accountsMap.get(id);
        if (account == null) { throw new OfficialAccountIdNotFoundException(id); }
        return account.getBestContributor(); }

    @Override public List<Integer> queryReceivedArticles(int id)
        throws PersonIdNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        return person.queryReceivedArticles(); }

    @Override public boolean containsMessage(int id) { return messagesMap.containsKey(id); }

    @Override public void addMessage(MessageInterface message) throws
        EqualMessageIdException, EmojiIdNotFoundException,
        EqualPersonIdException, ArticleIdNotFoundException {
        if (message == null) { return; }
        if (containsMessage(message.getId())) {
            throw new EqualMessageIdException(message.getId()); }
        if (message instanceof EmojiMessageInterface) {
            EmojiMessageInterface emojiMsg = (EmojiMessageInterface) message;
            if (!containsEmojiId(emojiMsg.getEmojiId())) {
                throw new EmojiIdNotFoundException(emojiMsg.getEmojiId()); } }
        if (message instanceof ForwardMessageInterface) {
            ForwardMessageInterface fwdMsg = (ForwardMessageInterface) message;
            if (!this.containsArticle(fwdMsg.getArticleId())) {
                throw new ArticleIdNotFoundException(fwdMsg.getArticleId()); }
            List<Integer> senderReceived = message.getPerson1().getReceivedArticles();
            boolean senderHasArticle = false;
            for (Integer articleIdInList : senderReceived) {
                if (articleIdInList.equals(fwdMsg.getArticleId())) {
                    senderHasArticle = true;
                    break; } }
            if (!senderHasArticle) {
                throw new ArticleIdNotFoundException(fwdMsg.getArticleId()); } }
        if (message.getType() == 0) {
            if (message.getPerson1().equals(message.getPerson2())) {
                throw new EqualPersonIdException(message.getPerson1().getId()); } }
        messagesMap.put(message.getId(), message); }

    @Override public MessageInterface getMessage(int id) { return messagesMap.get(id); }

    @Override public void sendMessage(int id) throws
        RelationNotFoundException, MessageIdNotFoundException, TagIdNotFoundException {
        MessageInterface message = getMessage(id);
        if (message == null) { throw new MessageIdNotFoundException(id); }
        PersonInterface sender = message.getPerson1();
        if (!(sender instanceof Person)) {
            messagesMap.remove(id);
            return; }
        Person concreteSender = (Person) sender;
        if (message.getType() == 0) {
            PersonInterface receiver = message.getPerson2();
            if (receiver == null || !(receiver instanceof Person) || sender.equals(receiver)) {
                messagesMap.remove(id);
                return; }
            if (!sender.isLinked(receiver)) {
                throw new RelationNotFoundException(sender.getId(), receiver.getId()); }
            applyType0Effects(message, concreteSender, (Person) receiver);
        } else {
            TagInterface targetTag = message.getTag();
            if (targetTag == null || !(targetTag instanceof Tag)) {
                messagesMap.remove(id);
                return; }
            if (!sender.containsTag(targetTag.getId())) {
                throw new TagIdNotFoundException(targetTag.getId()); }
            applyType1Effects(message, concreteSender, (Tag) targetTag); }
        messagesMap.remove(id); }

    private void applyType0Effects(MessageInterface message,
        Person concreteSender, Person concreteReceiver) {
        concreteSender.addSocialValue(message.getSocialValue());
        concreteReceiver.addSocialValue(message.getSocialValue());
        concreteReceiver.addMessageToList(message);
        if (message instanceof EmojiMessageInterface) {
            EmojiMessageInterface emojiMsg = (EmojiMessageInterface) message;
            Integer index = emojiIdToIndexMap.get(emojiMsg.getEmojiId());
            if (index != null && index < emojiHeatList.size()) {
                emojiHeatList.set(index, emojiHeatList.get(index) + 1); }
        } else if (message instanceof RedEnvelopeMessageInterface) {
            RedEnvelopeMessageInterface redMsg = (RedEnvelopeMessageInterface) message;
            concreteSender.addMoney(-redMsg.getMoney());
            concreteReceiver.addMoney(redMsg.getMoney());
        } else if (message instanceof ForwardMessageInterface) {
            ForwardMessageInterface fwdMsg = (ForwardMessageInterface) message;
            concreteReceiver.addReceivedArticleToList(fwdMsg.getArticleId()); } }

    private void applyType1Effects(MessageInterface message, Person concreteSender,
        Tag concreteTargetTag) {
        concreteSender.addSocialValue(message.getSocialValue());
        Map<Integer, PersonInterface> tagMembers = concreteTargetTag.getPersonsInternal();
        if (message instanceof EmojiMessageInterface) {
            EmojiMessageInterface emojiMsg = (EmojiMessageInterface) message;
            Integer index = emojiIdToIndexMap.get(emojiMsg.getEmojiId());
            if (index != null && index < emojiHeatList.size()) {
                emojiHeatList.set(index, emojiHeatList.get(index) + 1); } }
        int money = 0;
        if (message instanceof RedEnvelopeMessageInterface) {
            RedEnvelopeMessageInterface redMsg = (RedEnvelopeMessageInterface) message;
            if (!tagMembers.isEmpty()) {
                money = redMsg.getMoney() / tagMembers.size();
                concreteSender.addMoney(-(money * tagMembers.size())); } }
        for (PersonInterface memberP : tagMembers.values()) {
            if (memberP instanceof Person) {
                Person conMember = (Person) memberP;
                if (!conMember.equals(concreteSender)) {
                    conMember.addSocialValue(message.getSocialValue()); }
                conMember.addMessageToList(message);
                if (message instanceof RedEnvelopeMessageInterface) { conMember.addMoney(money); }
                else if (message instanceof ForwardMessageInterface) {
                    ForwardMessageInterface fwdMsg = (ForwardMessageInterface) message;
                    conMember.addReceivedArticleToList(fwdMsg.getArticleId()); } } } }

    @Override public int querySocialValue(int id) throws PersonIdNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        return person.getSocialValue(); }

    @Override public List<MessageInterface> queryReceivedMessages(int id)
        throws PersonIdNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        return person.getReceivedMessages(); }

    @Override public boolean containsEmojiId(int id) { return emojiIdToIndexMap.containsKey(id); }

    @Override public void storeEmojiId(int id) throws EqualEmojiIdException {
        if (containsEmojiId(id)) { throw new EqualEmojiIdException(id); }
        emojiIdList.add(id);
        emojiHeatList.add(0);
        emojiIdToIndexMap.put(id, emojiIdList.size() - 1); }

    @Override public int queryMoney(int id) throws PersonIdNotFoundException {
        PersonInterface person = getPerson(id);
        if (person == null) { throw new PersonIdNotFoundException(id); }
        return person.getMoney(); }

    @Override public int queryPopularity(int id) throws EmojiIdNotFoundException {
        if (!containsEmojiId(id)) { throw new EmojiIdNotFoundException(id); }
        Integer index = emojiIdToIndexMap.get(id);
        return emojiHeatList.get(index); }

    @Override public int deleteColdEmoji(int limit) {
        List<Integer> newEmojiIdList = new ArrayList<>();
        List<Integer> newEmojiHeatList = new ArrayList<>();
        Map<Integer, Integer> newEmojiIdToIndexMap = new HashMap<>();
        Set<Integer> validEmojiIds = new HashSet<>();
        for (int i = 0; i < emojiIdList.size(); i++) {
            int currentEmojiId = emojiIdList.get(i);
            int currentHeat = emojiHeatList.get(i);
            if (currentHeat >= limit) {
                newEmojiIdList.add(currentEmojiId);
                newEmojiHeatList.add(currentHeat);
                newEmojiIdToIndexMap.put(currentEmojiId, newEmojiIdList.size() - 1);
                validEmojiIds.add(currentEmojiId); } }
        this.emojiIdList.clear();
        this.emojiIdList.addAll(newEmojiIdList);
        this.emojiHeatList.clear();
        this.emojiHeatList.addAll(newEmojiHeatList);
        this.emojiIdToIndexMap.clear();
        this.emojiIdToIndexMap.putAll(newEmojiIdToIndexMap);
        Iterator<Map.Entry<Integer, MessageInterface>> iterator = messagesMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, MessageInterface> entry = iterator.next();
            MessageInterface msg = entry.getValue();
            if (msg instanceof EmojiMessageInterface) {
                EmojiMessageInterface emojiMsg = (EmojiMessageInterface) msg;
                if (!validEmojiIds.contains(emojiMsg.getEmojiId())) { iterator.remove(); } } }
        return this.emojiIdList.size(); }

    public MessageInterface[] getMessages() {
        return this.messagesMap.values().toArray(new MessageInterface[0]); }

    public int[] getEmojiIdList() {
        return this.emojiIdList.stream().mapToInt(Integer::intValue).toArray(); }

    public int[] getEmojiHeatList() {
        return this.emojiHeatList.stream().mapToInt(Integer::intValue).toArray(); } }