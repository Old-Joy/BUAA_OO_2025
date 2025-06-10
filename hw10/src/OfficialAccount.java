import com.oocourse.spec2.main.OfficialAccountInterface;
import com.oocourse.spec2.main.PersonInterface;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List; // 用于内部方法返回列表
import java.util.ArrayList; // 用于内部方法返回列表

public class OfficialAccount implements OfficialAccountInterface {

    private final int ownerId;
    private final int id;
    private final String name;
    // 使用 Map 存储关注者及其贡献度，key 为 follower 的 id
    private final Map<Integer, PersonInterface> followerMap; // id -> PersonInterface
    private final Map<Integer, Integer> contributionMap; // id -> contribution count
    // 使用 Set 存储文章 ID，便于快速查找和删除
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
        if (person != null && !containsFollower(person)) {
            followerMap.put(person.getId(), person);
            contributionMap.put(person.getId(), 0); // 默认贡献度为 0
        }
    }

    public /*@ safe @*/ void addFollowerInternal(
        PersonInterface person, int initialContribution) {
        if (person != null && !containsFollower(person)) {
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
    public /*@ safe @*/ void addArticle(/*@ non_null @*/ PersonInterface person, int id) {
        if (!containsArticle(id)) {
            articlesSet.add(id);
            // JML ensures 贡献度增加
            if (person != null && contributionMap.containsKey(person.getId())) {
                contributionMap.put(person.getId(), contributionMap.get(person.getId()) + 1);
            } else if (person != null) {
                System.err.println("警告: 尝试为非关注者 " +
                    person.getId() + " 在公众号 " + this.id + " 增加贡献度。");
            }
        }
    }

    public /*@ safe @*/ void addArticleInternal(int articleId) {
        if (!containsArticle(articleId)) {
            articlesSet.add(articleId);
        }
    }

    public /*@ safe @*/ void incrementContributionInternal(int personId) {
        if (contributionMap.containsKey(personId)) {
            contributionMap.put(personId, contributionMap.get(personId) + 1);
        } else {
            System.err.println("警告: 尝试为不存在于贡献图的 ID " + personId + " 在公众号 " + this.id + " 增加贡献度。");
        }
    }

    public /*@ safe @*/ void decrementContributionInternal(int personId) {
        if (contributionMap.containsKey(personId)) {
            contributionMap.put(personId, contributionMap.get(personId) - 1);
            // JML 没有规定贡献度是否可以为负，这里允许
        } else {
            System.err.println("警告: 尝试为不存在于贡献图的 ID " + personId + " 在公众号 " + this.id + " 减少贡献度。");
        }
    }

    @Override
    public /*@ pure @*/ boolean containsArticle(int id) {
        return articlesSet.contains(id);
    }

    @Override
    public /*@ safe @*/ void removeArticle(int id) {
        // JML requires containsArticle(id)
        articlesSet.remove(id);
        // 注意：JML 的 assignable 只涉及 articles，不涉及 contributions。
        // Network.deleteArticle 会处理贡献度的减少。
    }

    public /*@ safe @*/ void removeArticleInternal(int articleId) {
        articlesSet.remove(articleId);
    }

    @Override
    public /*@ pure @*/ int getBestContributor() {
        if (contributionMap.isEmpty()) {
            return contributionMap.containsKey(this.ownerId) ? this.ownerId : 0;
        }

        int maxContribution = Integer.MIN_VALUE;
        // 找到最大贡献值
        for (int contribution : contributionMap.values()) {
            if (contribution > maxContribution) {
                maxContribution = contribution;
            }
        }

        int bestId = Integer.MAX_VALUE;
        boolean found = false;
        // 找到具有最大贡献值的人中，ID 最小的那个
        for (Map.Entry<Integer, Integer> entry : contributionMap.entrySet()) {
            if (entry.getValue() == maxContribution) {
                if (entry.getKey() < bestId) {
                    bestId = entry.getKey();
                    found = true;
                }
            }
        }

        // 如果找到了（至少会找到一个，除非 map 为空，已处理）
        // JML (\result == ... bestId)，所以返回找到的 bestId
        return found ? bestId : 0; // 如果因意外情况没找到，返回 0
    }

    public List<PersonInterface> getFollowersInternal() {
        return new ArrayList<>(followerMap.values()); // 返回副本以防外部修改
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        OfficialAccount other = (OfficialAccount) obj;
        return this.id == other.id; // 基于唯一 ID 判断相等性
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id); // 基于唯一 ID 计算哈希码
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}