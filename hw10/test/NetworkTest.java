import com.oocourse.spec2.exceptions.*;
import com.oocourse.spec2.main.NetworkInterface;
import com.oocourse.spec2.main.PersonInterface;

import org.junit.Test; // 现在只需要 @Test 注解
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects; // 仍然用于比较 List/Map

// 假设 Person, Network, Tag, OfficialAccount 类已实现相应接口
// 假设 Network 类有 getPersons() 方法 (按指导书要求)
// 假设 Person 类有 getReceivedArticles() 方法 (按指导书要求)

public class NetworkTest {

    // 不需要 @Before，因为每个测试方法会设置自己的实例

    // --- 辅助方法：在两个网络上构建相同的状态 ---
    // 示例：你可以在每个测试内部调用这个
    private void setupIdenticalNetworks(NetworkInterface n1, NetworkInterface n2, String setupSequence) {
        // 这只是一个占位符。实际测试中，你需要具体的指令序列。
        // 例如: "ap 1 A 20; ap 2 B 25; ar 1 2 100"
        String[] commands = setupSequence.split(";\\s*"); // 按分号分割指令
        for (String command : commands) {
            applyCommand(n1, command);
            applyCommand(n2, command);
        }
    }

    // --- 辅助方法：应用单条指令字符串 (简化版) ---
    // 你需要正确地解析或使用已有的辅助方法如 addPersonToNetwork
    private void applyCommand(NetworkInterface network, String command) {
        String[] parts = command.trim().split("\\s+");
        try {
            switch (parts[0]) {
                case "ap":
                    int id = Integer.parseInt(parts[1]);
                    String name = parts[2];
                    int age = Integer.parseInt(parts[3]);
                    // 使用你自己的 Person 构造函数
                    PersonInterface person = new Person(id, name, age);
                    network.addPerson(person);
                    break;
                case "ar":
                    int id1 = Integer.parseInt(parts[1]);
                    int id2 = Integer.parseInt(parts[2]);
                    int value = Integer.parseInt(parts[3]);
                    network.addRelation(id1, id2, value);
                    break;
                // 根据测试需要添加 mr, at, att, dft, dt 等命令的处理
                case "mr":
                    network.modifyRelation(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                    break;
                // ... 其他命令
                default:
                    System.err.println("警告: applyCommand 未处理命令: " + parts[0]);
                    break; // 忽略此示例中未处理的命令
            }
        } catch (Exception e) {
            // 如果设置命令抛出意外异常，则测试失败
            fail("状态构建失败，命令 '" + command + "' 在网络 " + network.hashCode() + " 上出错: " + e);
        }
    }

    // --- 辅助方法：向特定网络实例添加 Person ---
    private PersonInterface addPersonToNetworkInstance(NetworkInterface network, int id, String name, int age) {
        PersonInterface person = new Person(id, name, age); // 使用你的 Person 类
        try { network.addPerson(person); }
        catch (EqualPersonIdException e) { fail("状态构建失败: addPerson 遇到相同 ID " + id); }
        catch (Exception e) { fail("状态构建失败: addPerson 异常 " + id + ": " + e); }
        PersonInterface p = network.getPerson(id);
        if (p == null) { fail("状态构建失败: addPerson 后 getPerson 为 null " + id); }
        return p;
    }

    // --- 辅助方法：在特定网络实例上确保 Relation 存在 ---
    private void ensureRelationInstance(NetworkInterface network, int id1, int id2, int value) {
        PersonInterface p1Check = network.getPerson(id1);
        PersonInterface p2Check = network.getPerson(id2);
        // 先检查 Person 是否存在，避免后续空指针
        if (p1Check == null) { fail("状态构建失败: ensureRelation 找不到 Person " + id1); }
        if (p2Check == null) { fail("状态构建失败: ensureRelation 找不到 Person " + id2); }

        // 使用检查过的 Person 对象调用 isLinked
        if (!p1Check.isLinked(p2Check)) {
            try {
                network.addRelation(id1, id2, value);
            } catch (PersonIdNotFoundException | EqualRelationException e) {
                fail("状态构建失败: ensureRelation 调用 addRelation 异常 (" + id1 + "," + id2 + "): " + e);
            } catch (Exception e) { // 捕获其他意外异常
                fail("状态构建失败: ensureRelation 调用 addRelation 意外异常 (" + id1 + "," + id2 + "): " + e);
            }
            // 可能修改后需要重新获取 Person 对象引用
            PersonInterface p1After = network.getPerson(id1);
            PersonInterface p2After = network.getPerson(id2);
            if (p1After == null || p2After == null) {
                fail("状态构建失败: ensureRelation 添加关系后 getPerson 为 null " + id1 + "/" + id2);
            }
            // 再次检查链接状态
            if (!p1After.isLinked(p2After) || !p2After.isLinked(p1After)) {
                fail("状态构建失败: ensureRelation 添加关系后未成功链接 " + id1 + "," + id2);
            }
        } else {
            // 如果已链接，根据需要可能要修改值。
            // 为简化，这里假设 ensureRelation 只在不存在时添加。
        }
    }

    // --- 辅助方法：在特定网络实例上修改 Relation ---
    private void modifyRelationInstance(NetworkInterface network, int id1, int id2, int value) {
        try {
            network.modifyRelation(id1, id2, value);
        } catch (Exception e) {
            fail("状态构建失败: modifyRelationInstance 失败 ("+id1+","+id2+","+value+"): " + e);
        }
    }

    // --- 核心的状态等价性比较方法 ---
    private void assertNetworksStateEquivalent(NetworkInterface n1, NetworkInterface n2, String testCaseName) {
        // 假设: getPersons() 可用且返回反映当前状态的数组。
        // 根据对 qcs 测试的简化要求，不检查 Tag, Account, Article 状态。

        PersonInterface[] persons1 = null;
        PersonInterface[] persons2 = null;
        Set<Integer> ids1 = new HashSet<>();
        Set<Integer> ids2 = new HashSet<>();
        // 使用 Map 便于通过 ID 查找 Person 对象
        Map<Integer, PersonInterface> map1 = new HashMap<>();
        Map<Integer, PersonInterface> map2 = new HashMap<>();

        try {
            // 调用被测 Network 实现提供的 getPersons() 方法
            // 如果 getPersons() 不在 NetworkInterface 中，需要进行类型转换
            // 但我们根据指导书假定它存在
            if (n1 instanceof Network) { // 如果需要，检查具体类型
                persons1 = ((Network) n1).getPersons();
            } else { fail(testCaseName + " - Purity检查错误: n1 不是预期的 Network 类型"); return; }
            if (n2 instanceof Network) {
                persons2 = ((Network) n2).getPersons();
            } else { fail(testCaseName + " - Purity检查错误: n2 不是预期的 Network 类型"); return; }


            // 处理 getPersons() 的返回结果
            if (persons1 != null) {
                for (PersonInterface p : persons1) { if (p != null) { ids1.add(p.getId()); map1.put(p.getId(), p); } }
            } else { System.err.println(testCaseName + " - 警告: n1.getPersons() 返回 null."); persons1 = new PersonInterface[0];} // 安全起见，视为空

            if (persons2 != null) {
                for (PersonInterface p : persons2) { if (p != null) { ids2.add(p.getId()); map2.put(p.getId(), p); } }
            } else { System.err.println(testCaseName + " - 警告: n2.getPersons() 返回 null."); persons2 = new PersonInterface[0];}

        } catch (Exception e) {
            fail(testCaseName + " - Purity检查错误: 调用 getPersons() 时发生异常: " + e);
            return;
        }

        // --- 1. 比较 Person 集合 ---
        // 首先检查人数和 ID 集合是否完全一致
        assertEquals(testCaseName + " - Purity失败: Person 数量不一致.", persons1.length, persons2.length);
        assertEquals(testCaseName + " - Purity失败: Person ID 集合不一致.", ids1, ids2); // Set.equals 比较内容

        // --- 2. 逐一比较每个 Person 的状态 ---
        for (int id : ids1) { // 遍历其中一个 ID 集合即可，因为它们应该相等
            PersonInterface p1 = map1.get(id);
            PersonInterface p2 = map2.get(id); // 如果 ID 集合相等，这里应该能找到

            // 基本的非空检查，理论上不应失败
            assertNotNull(testCaseName + " - Purity逻辑错误: 找不到 n1 中的 Person ID " + id, p1);
            assertNotNull(testCaseName + " - Purity逻辑错误: 找不到 n2 中的 Person ID " + id, p2);

            try {
                // 比较基本属性
                assertEquals(testCaseName + " - Purity失败: ID " + id + " 的 Name 不一致", p1.getName(), p2.getName());
                assertEquals(testCaseName + " - Purity失败: ID " + id + " 的 Age 不一致", p1.getAge(), p2.getAge());

                // 比较接收的文章列表 (使用获取完整列表的方法)
                List<Integer> articles1 = p1.getReceivedArticles();
                List<Integer> articles2 = p2.getReceivedArticles();
                assertNotNull(testCaseName + " - Purity失败: p1.getReceivedArticles 返回 null (ID " + id + ")", articles1);
                assertNotNull(testCaseName + " - Purity失败: p2.getReceivedArticles 返回 null (ID " + id + ")", articles2);
                assertEquals(testCaseName + " - Purity失败: ID " + id + " 的 Received Articles 不一致", articles1, articles2); // List.equals 检查大小、内容和顺序

                // 比较与所有其他人的链接关系和关系值
                for (int otherId : ids1) { // 再次遍历所有 ID
                    if (id == otherId) continue; // 跳过自己

                    PersonInterface other1 = map1.get(otherId);
                    PersonInterface other2 = map2.get(otherId);
                    // 基本非空检查
                    assertNotNull(testCaseName + " - Purity逻辑错误: 找不到 n1 中的 other Person ID " + otherId, other1);
                    assertNotNull(testCaseName + " - Purity逻辑错误: 找不到 n2 中的 other Person ID " + otherId, other2);

                    // 比较链接状态
                    boolean linked1 = p1.isLinked(other1);
                    boolean linked2 = p2.isLinked(other2);
                    assertEquals(testCaseName + " - Purity失败: 链接状态不一致 (" + id + "," + otherId + ")", linked1, linked2);

                    // 仅当两者都链接时比较关系值 (如果状态等价，它们应该同时链接或同时不链接)
                    if (linked1) { // 此时 linked1 == linked2 应为 true
                        int val1 = p1.queryValue(other1);
                        int val2 = p2.queryValue(other2);
                        assertEquals(testCaseName + " - Purity失败: 关系值不一致 (" + id + "," + otherId + ")", val1, val2);
                    }
                } // 结束与其他人的比较循环

            } catch (Exception e) {
                // 捕获比较过程中发生的异常 (例如调用 get/is/query 方法时)
                fail(testCaseName + " - Purity检查错误: 比较 ID " + id + " 时发生异常: " + e);
                return; // 如果单个 Person 比较失败，停止对其的检查
            }
        } // 结束所有 Person 的比较循环
    }

    // --- 使用双实例方法编写的测试用例 ---

    @Test
    public void testQueryCoupleSumEmptyNetwork() {
        NetworkInterface networkUnderTest = new Network(); // 待测实例
        NetworkInterface networkControl = new Network(); // 对照实例
        // 无需构建状态

        int expected = 0;
        int actual = networkUnderTest.queryCoupleSum(); // 在待测实例上调用
        assertEquals("空网络 功能性检查", expected, actual);

        assertNetworksStateEquivalent(networkUnderTest, networkControl, "空网络"); // Purity 检查
    }

    @Test
    public void testQueryCoupleSumSinglePerson() {
        NetworkInterface networkUnderTest = new Network();
        NetworkInterface networkControl = new Network();

        // 对两个实例执行完全相同的操作
        addPersonToNetworkInstance(networkUnderTest, 1, "Alice", 30);
        addPersonToNetworkInstance(networkControl, 1, "Alice", 30);

        int expected = 0;
        int actual = networkUnderTest.queryCoupleSum();
        assertEquals("单 Person 功能性检查", expected, actual);

        assertNetworksStateEquivalent(networkUnderTest, networkControl, "单Person");
    }

    @Test
    public void testQueryCoupleSumOneCouple() {
        NetworkInterface networkUnderTest = new Network();
        NetworkInterface networkControl = new Network();

        addPersonToNetworkInstance(networkUnderTest, 1, "Alice", 30);
        addPersonToNetworkInstance(networkControl, 1, "Alice", 30);
        addPersonToNetworkInstance(networkUnderTest, 2, "Bob", 28);
        addPersonToNetworkInstance(networkControl, 2, "Bob", 28);
        ensureRelationInstance(networkUnderTest, 1, 2, 100);
        ensureRelationInstance(networkControl, 1, 2, 100);

        int expected = 1;
        int actual = networkUnderTest.queryCoupleSum();
        assertEquals("一个 Couple 功能性检查", expected, actual);

        assertNetworksStateEquivalent(networkUnderTest, networkControl, "一个Couple");
    }

    @Test
    public void testQueryCoupleSumAfterModifyRelationRemovesRelation() {
        NetworkInterface networkUnderTest = new Network();
        NetworkInterface networkControl = new Network();

        // 构建初始状态
        addPersonToNetworkInstance(networkUnderTest, 1,"A",30);
        addPersonToNetworkInstance(networkControl, 1,"A",30);
        addPersonToNetworkInstance(networkUnderTest, 2,"B",28);
        addPersonToNetworkInstance(networkControl, 2,"B",28);
        ensureRelationInstance(networkUnderTest,1,2,100);
        ensureRelationInstance(networkControl,1,2,100);

        // 调用被测方法 (修改前)
        int resultPre = networkUnderTest.queryCoupleSum();
        assertEquals("Modify移除关系 Pre 功能性检查", 1, resultPre);
        assertNetworksStateEquivalent(networkUnderTest, networkControl, "Modify移除关系Pre");

        // 对两个网络执行相同的状态修改
        modifyRelationInstance(networkUnderTest, 1, 2, -100);
        modifyRelationInstance(networkControl, 1, 2, -100);

        // 调用被测方法 (修改后)
        int resultPost = networkUnderTest.queryCoupleSum();
        assertEquals("Modify移除关系 Post 功能性检查", 0, resultPost);
        assertNetworksStateEquivalent(networkUnderTest, networkControl, "Modify移除关系Post");
    }


    @Test
    public void testSequenceOfModificationsAffectingCouples() {
        NetworkInterface networkUnderTest = new Network();
        NetworkInterface networkControl = new Network();
        String testName = "序列修改影响Couple";

        // 步骤 1: 初始状态 C(1,2), C(3,4)
        addPersonToNetworkInstance(networkUnderTest, 1,"P1",20); addPersonToNetworkInstance(networkControl, 1,"P1",20);
        addPersonToNetworkInstance(networkUnderTest, 2,"P2",20); addPersonToNetworkInstance(networkControl, 2,"P2",20);
        addPersonToNetworkInstance(networkUnderTest, 3,"P3",20); addPersonToNetworkInstance(networkControl, 3,"P3",20);
        addPersonToNetworkInstance(networkUnderTest, 4,"P4",20); addPersonToNetworkInstance(networkControl, 4,"P4",20);
        ensureRelationInstance(networkUnderTest, 1, 2, 100); ensureRelationInstance(networkControl, 1, 2, 100);
        ensureRelationInstance(networkUnderTest, 3, 4, 100); ensureRelationInstance(networkControl, 3, 4, 100);

        assertEquals(testName + " 步骤 1 功能", 2, networkUnderTest.queryCoupleSum());
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_1");

        // 步骤 2: 添加 1-3 链接
        ensureRelationInstance(networkUnderTest, 1, 3, 110); ensureRelationInstance(networkControl, 1, 3, 110);
        assertEquals(testName + " 步骤 2 功能", 1, networkUnderTest.queryCoupleSum()); // C(1,3)
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_2");

        // 步骤 3: 添加 2-4 链接
        ensureRelationInstance(networkUnderTest, 2, 4, 120); ensureRelationInstance(networkControl, 2, 4, 120);
        assertEquals(testName + " 步骤 3 功能", 2, networkUnderTest.queryCoupleSum()); // C(1,3), C(2,4)
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_3");

        // 步骤 4: 移除 1-3 链接
        modifyRelationInstance(networkUnderTest, 1, 3, -110); modifyRelationInstance(networkControl, 1, 3, -110);
        assertEquals(testName + " 步骤 4 功能", 1, networkUnderTest.queryCoupleSum()); // C(2,4)
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_4");

        // 步骤 5: 移除 2-4 链接
        modifyRelationInstance(networkUnderTest, 2, 4, -120); modifyRelationInstance(networkControl, 2, 4, -120);
        assertEquals(testName + " 步骤 5 功能", 2, networkUnderTest.queryCoupleSum()); // C(1,2), C(3,4)
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_5");
    }

    // --- 添加更多遵循此模式的测试用例 ---
    // 将其他现有测试 (如 LargeGraph, TieBreaking, AlmostCouple, ConsecutiveCalls)
    // 转换为使用双实例设置和比较方法。

    @Test
    public void testConsecutiveCallsWithoutChange() {
        NetworkInterface networkUnderTest = new Network();
        NetworkInterface networkControl = new Network();
        String testName = "连续调用无变化";

        // 构建状态 (来自辅助方法的例子)
        int n = 20;
        for(int i=1; i<=n; i++) { addPersonToNetworkInstance(networkUnderTest, i, "N"+i, 20+(i%5)); addPersonToNetworkInstance(networkControl, i, "N"+i, 20+(i%5)); }
        ensureRelationInstance(networkUnderTest, 1, 2, 200); ensureRelationInstance(networkControl, 1, 2, 200);
        ensureRelationInstance(networkUnderTest, 3, 4, 150); ensureRelationInstance(networkControl, 3, 4, 150);
        ensureRelationInstance(networkUnderTest, 5, 6, 100); ensureRelationInstance(networkControl, 5, 6, 100);
        ensureRelationInstance(networkUnderTest, 5, 7, 100); ensureRelationInstance(networkControl, 5, 7, 100);
        ensureRelationInstance(networkUnderTest, 6, 7, 50); ensureRelationInstance(networkControl, 6, 7, 50);
        ensureRelationInstance(networkUnderTest, 6, 8, 90); ensureRelationInstance(networkControl, 6, 8, 90);
        ensureRelationInstance(networkUnderTest, 7, 8, 90); ensureRelationInstance(networkControl, 7, 8, 90);
        ensureRelationInstance(networkUnderTest, 9, 10, 120); ensureRelationInstance(networkControl, 9, 10, 120);
        ensureRelationInstance(networkUnderTest, 9, 11, 130); ensureRelationInstance(networkControl, 9, 11, 130);
        ensureRelationInstance(networkUnderTest, 10, 11, 50); ensureRelationInstance(networkControl, 10, 11, 50);
        ensureRelationInstance(networkUnderTest, 1, 5, 10); ensureRelationInstance(networkControl, 1, 5, 10);
        ensureRelationInstance(networkUnderTest, 2, 6, 10); ensureRelationInstance(networkControl, 2, 6, 10);
        ensureRelationInstance(networkUnderTest, 3, 10, 10); ensureRelationInstance(networkControl, 3, 10, 10);
        ensureRelationInstance(networkUnderTest, 4, 9, 10); ensureRelationInstance(networkControl, 4, 9, 10);
        // 预期结果: C(1,2), C(3,4), C(5,6), C(9,11) -> 4 couples

        // 调用 1
        int result1 = networkUnderTest.queryCoupleSum();
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_调用1");

        // 调用 2 (紧随其后，两个网络都无变化)
        int result2 = networkUnderTest.queryCoupleSum();
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_调用2");

        // 调用 3
        int result3 = networkUnderTest.queryCoupleSum();
        assertNetworksStateEquivalent(networkUnderTest, networkControl, testName + "_调用3");

        // 断言结果一致性
        assertEquals(testName + " 结果 1 vs 2", result1, result2);
        assertEquals(testName + " 结果 2 vs 3", result2, result3);
    }

}