import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;
import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.exceptions.AcquaintanceNotFoundException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet; // 新增 import
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set; // 新增 import
import java.util.TreeSet;

public class Person implements PersonInterface {
    private final int id;
    private final String name;
    private final int age;
    private final Map<Integer, PersonInterface> acquaintanceMap;
    private final Map<Integer, Integer> valueMap;

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
                return Integer.compare(other.value, this.value);
            }
            return Integer.compare(this.personId, other.personId);
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
    private int socialValue;
    private int money;
    private final List<MessageInterface> messages;

    private static class ArticleNode {
        private int articleId;
        private ArticleNode prev;
        private ArticleNode next;

        ArticleNode(int articleId) {
            this.articleId = articleId;
        }
    }

    private transient ArticleNode receivedArticlesHead;
    private transient ArticleNode receivedArticlesTail;
    private int receivedArticlesCount;
    private transient Map<Integer, Set<ArticleNode>> articleIdToNodesMap;
    // --- 结束新增部分 ---

    public Person(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.acquaintanceMap = new HashMap<>();
        this.valueMap = new HashMap<>();
        this.sortedAcquaintances = new TreeSet<>();
        this.tags = new HashMap<>();
        this.socialValue = 0;
        this.money = 0;
        this.messages = new LinkedList<>();

        // --- 初始化自定义 receivedArticles 结构 ---
        this.receivedArticlesHead = null;
        this.receivedArticlesTail = null;
        this.receivedArticlesCount = 0;
        this.articleIdToNodesMap = new HashMap<>();
        // --- 结束初始化 ---
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
        tags.remove(id);
    }

    // --- 修改 getReceivedArticles 和 queryReceivedArticles 以适应新结构 ---
    @Override
    public /*@ pure @*/ List<Integer> getReceivedArticles() {
        List<Integer> result = new LinkedList<>();
        ArticleNode current = this.receivedArticlesHead;
        while (current != null) {
            result.add(current.articleId);
            current = current.next;
        }
        return result;
    }

    @Override
    public /*@ pure @*/ List<Integer> queryReceivedArticles() {
        List<Integer> queriedArticles = new LinkedList<>();
        ArticleNode current = this.receivedArticlesHead;
        int count = 0;
        while (current != null && count < 5) {
            queriedArticles.add(current.articleId);
            current = current.next;
            count++;
        }
        return queriedArticles;
    }
    // --- 结束修改 ---

    @Override
    public void addSocialValue(int num) {
        this.socialValue += num;
    }

    @Override
    public /*@ pure @*/ int getSocialValue() {
        return this.socialValue;
    }

    @Override
    public /*@ pure @*/ List<MessageInterface> getMessages() {
        return new LinkedList<>(this.messages);
    }

    @Override
    public /*@ pure @*/ List<MessageInterface> getReceivedMessages() {
        List<MessageInterface> recentMessages = new LinkedList<>();
        int count = 0;
        for (MessageInterface msg : this.messages) {
            if (count < 5) {
                recentMessages.add(msg);
                count++;
            } else {
                break;
            }
        }
        return recentMessages;
    }

    @Override
    public void addMoney(int num) {
        this.money += num;
    }

    @Override
    public /*@ pure @*/ int getMoney() {
        return this.money;
    }

    public /*@ safe @*/ void addAcquaintance(PersonInterface person, int val) {
        if (person != null && person.getId() != this.id &&
            !acquaintanceMap.containsKey(person.getId())) {
            acquaintanceMap.put(person.getId(), person);
            valueMap.put(person.getId(), val);
            sortedAcquaintances.add(new AcquaintanceEntry(person.getId(), val));
        }
    }

    public /*@ safe @*/ void removeAcquaintance(PersonInterface person) {
        if (person != null) {
            Integer personId = person.getId();
            if (acquaintanceMap.containsKey(personId)) {
                Integer oldValue = valueMap.remove(personId);
                acquaintanceMap.remove(personId);
                if (oldValue != null) {
                    sortedAcquaintances.remove(new AcquaintanceEntry(personId, oldValue));
                }
            }
        }
    }

    public /*@ safe @*/ void modifyRelationValue(PersonInterface person, int newValue) {
        if (person != null) {
            Integer personId = person.getId();
            if (valueMap.containsKey(personId)) {
                Integer oldValue = valueMap.get(personId);
                sortedAcquaintances.remove(new AcquaintanceEntry(personId, oldValue));
                valueMap.put(personId, newValue);
                sortedAcquaintances.add(new AcquaintanceEntry(personId, newValue));
            }
        }
    }

    public /*@ pure @*/ int getBestAcquaintanceIdInternal() throws AcquaintanceNotFoundException {
        if (sortedAcquaintances.isEmpty()) {
            throw new AcquaintanceNotFoundException(this.id);
        }
        return sortedAcquaintances.first().personId;
    }

    public /*@ pure @*/ boolean hasAcquaintancesInternal() {
        return !acquaintanceMap.isEmpty();
    }

    // --- 核心优化方法：addReceivedArticleToList 和 removeReceivedArticleInternal ---
    public /*@ safe @*/ void addReceivedArticleToList(int articleId) {
        ArticleNode newNode = new ArticleNode(articleId);
        if (this.receivedArticlesHead == null) { // List is empty
            this.receivedArticlesHead = newNode;
            this.receivedArticlesTail = newNode;
        } else { // Add to front
            newNode.next = this.receivedArticlesHead;
            this.receivedArticlesHead.prev = newNode;
            this.receivedArticlesHead = newNode;
        }
        this.receivedArticlesCount++;
        this.articleIdToNodesMap.computeIfAbsent(articleId, k -> new HashSet<>()).add(newNode);
    }

    private void unlinkNode(ArticleNode node) {
        if (node == null) {
            return;
        }
        if (node.prev != null) {
            node.prev.next = node.next;
        } else { // Node is head
            this.receivedArticlesHead = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else { // Node is tail
            this.receivedArticlesTail = node.prev;
        }
        this.receivedArticlesCount--;

    }

    public /*@ safe @*/ void removeReceivedArticleInternal(int articleId) {
        if (!this.articleIdToNodesMap.containsKey(articleId)) {
            return;
        }
        Set<ArticleNode> nodesToRemove = this.articleIdToNodesMap.get(articleId);
        if (nodesToRemove == null || nodesToRemove.isEmpty()) {
            this.articleIdToNodesMap.remove(articleId);
            return;
        }
        for (ArticleNode node : nodesToRemove) {
            unlinkNode(node);
        }
        this.articleIdToNodesMap.remove(articleId);
    }

    public /*@ safe @*/ void addMessageToList(MessageInterface message) {
        if (message != null) {
            this.messages.add(0, message);
        }
    }

    public /*@ pure @*/ Map<Integer, PersonInterface> getAcquaintancesInternal() {
        return Collections.unmodifiableMap(this.acquaintanceMap);
    }

    public /*@ pure @*/ Map<Integer, TagInterface> getOwnedTagsInternal() {
        return Collections.unmodifiableMap(this.tags);
    }
}