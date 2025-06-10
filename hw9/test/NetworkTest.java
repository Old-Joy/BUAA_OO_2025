import com.oocourse.spec1.main.*; // 确认包名与你的项目一致

// 导入所有需要的类和异常
import com.oocourse.spec1.exceptions.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NetworkTest {

    private Network network; // 将被评测代码的 Network 实例注入

    @Before
    public void setUp() {
        network = new Network();
    }

    // --- 辅助方法：使用官方接口 ---
    private PersonInterface addPersonToNetwork(int id, String name, int age) {
        try {
            network.addPerson(new Person(id, name, age));
        } catch (EqualPersonIdException e) {
            Assert.fail("Test setup failed: Unexpected EqualPersonIdException for id " + id);
        } catch (Exception e) {
            Assert.fail("Test setup failed: Exception during addPerson: " + e.getMessage());
        }
        // 确保返回的是 Network 管理的对象
        PersonInterface p = network.getPerson(id);
        if (p == null) {
            Assert.fail("Test setup failed: Person added but getPerson returned null for id " + id);
        }
        return p;
    }

    private void ensureRelation(int id1, int id2, int value) {
        if (!network.containsPerson(id1) || !network.containsPerson(id2)) {
            Assert.fail("Test setup precondition failed: Person " + id1 + " or " + id2 + " not found before ensuring relation.");
        }
        PersonInterface p1 = network.getPerson(id1);
        PersonInterface p2 = network.getPerson(id2);
        if (p1 == null || p2 == null) {
            Assert.fail("Test setup failed: getPerson returned null despite containsPerson being true.");
            return;
        }
        if (!p1.isLinked(p2)) {
            try {
                network.addRelation(id1, id2, value); // 使用 Network 接口添加关系
            } catch (PersonIdNotFoundException | EqualRelationException e) {
                Assert.fail("Test setup failed: Unexpected Exception during addRelation after checks passed: " + e.getMessage());
            }
            p1 = network.getPerson(id1);
            p2 = network.getPerson(id2);
            if (p1 == null || p2 == null || !p1.isLinked(p2) || !p2.isLinked(p1)) {
                Assert.fail("Test setup failed: Relation not established bidirectionally after addRelation call for " + id1 + " and " + id2);
            }
        }
    }

    private void addNNodes(int startId, int count) {
        for (int i = 0; i < count; i++) {
            addPersonToNetwork(startId + i, "Node" + (startId + i), 20 + (i % 10));
        }
    }

    private void addCompleteGraph(int startId, int n) {
        addNNodes(startId, n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                ensureRelation(startId + i, startId + j, 10 + i + j);
            }
        }
    }

    private long combinations(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        if (k > n / 2) k = n - k;
        long res = 1;
        for (int i = 1; i <= k; ++i) {
            if (n - i + 1 <= 0) { if (res != 0) return -1; }
            else if (res > Long.MAX_VALUE / (n - i + 1)) { return -1; }
            res = res * (n - i + 1);
            if (res < 0 && (n - i + 1) > 0) { return -1; }
            if (i == 0) return -1; // Should not happen
            res /= i;
        }
        return res;
    }

    // --- 辅助方法：执行 Purity 检查 (仅检查 Person 集合和调用 strictEquals) ---
    private void assertPurityOfQueryTripleSum() {
        // 1. 获取调用前状态 (Person 引用和 ID 集合)
        PersonInterface[] personsBefore = null;
        Map<Integer, PersonInterface> beforeMap = new HashMap<>();
        Set<Integer> beforeIds = new HashSet<>();
        int initialPersonCount = 0;
        try {
            personsBefore = network.getPersons(); // 允许调用的特殊方法
            if (personsBefore != null) {
                for (PersonInterface p : personsBefore) {
                    if (p != null) {
                        initialPersonCount++;
                        beforeMap.put(p.getId(), p); // 存储调用前的引用
                        beforeIds.add(p.getId());
                    }
                }
            } else {
                personsBefore = new PersonInterface[0];
            }
        } catch (Exception e) {
            Assert.fail("Purity Check Setup Error: Exception calling network.getPersons(): " + e.getMessage());
            return;
        }

        // 2. 执行被测方法
        long result = network.queryTripleSum();

        // 3. 获取调用后状态 (Person 引用和 ID 集合)
        PersonInterface[] personsAfter = null;
        Set<Integer> afterIds = new HashSet<>();
        int finalPersonCount = 0;
        Map<Integer, PersonInterface> afterMap = new HashMap<>();
        try {
            personsAfter = network.getPersons();
            if (personsAfter != null) {
                for(PersonInterface p : personsAfter) {
                    if (p != null) {
                        finalPersonCount++;
                        afterIds.add(p.getId());
                        afterMap.put(p.getId(), p); // 存储调用后的引用
                    }
                }
            } else {
                personsAfter = new PersonInterface[0];
            }
        } catch (Exception e) {
            Assert.fail("Purity Check Error: Exception calling network.getPersons() after qts call: " + e.getMessage());
            return;
        }

        Assert.assertEquals("Purity Check Failed: Number of non-null persons changed after calling queryTripleSum.",
                initialPersonCount, finalPersonCount);
        Assert.assertEquals("Purity Check Failed: Set of person IDs changed after calling queryTripleSum.",
                beforeIds, afterIds);

        // 4.2 使用 strictEquals 检查每个 Person 的状态是否改变
        for (Integer id : beforeIds) {
            PersonInterface pBefore = beforeMap.get(id);
            PersonInterface pAfter = afterMap.get(id);
            Assert.assertNotNull("Purity Check Logic Error: Person with ID " + id + " existed before call but not found after.", pAfter);
            Assert.assertNotNull("Purity Check Logic Error: Person with ID " + id + " not found in beforeMap.", pBefore);

            boolean areStatesEqual;
            try {
                if (pBefore instanceof Person) {
                    areStatesEqual = ((Person) pBefore).strictEquals(pAfter);
                } else {
                    Assert.fail("Purity Check Error: Object retrieved for ID " + id + " is not an instance of the expected Person class locally.");
                    return;
                }
            } catch (ClassCastException e) {
                Assert.fail("Purity Check Error: Could not cast PersonInterface to Person to call strictEquals for ID " + id + ". Ensure Person class is available and implements required method. " + e.getMessage());
                return;
            } catch (Exception e) {
                Assert.fail("Purity Check Error: Exception occurred during strictEquals call for ID " + id + ": " + e.getMessage());
                return;
            }

            Assert.assertTrue("Purity Check Failed: State of Person " + id +
                    " changed after calling queryTripleSum (strictEquals returned false).", areStatesEqual);
        }
    }


    // --- 测试用例 (每个测试用例末尾都需要调用 assertPurityOfQueryTripleSum) ---

    @Test
    public void testQueryTripleSumEmptyNetwork() {
        int actualTripleSum = network.queryTripleSum();
        Assert.assertEquals(0, actualTripleSum);
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumLessThanThreePersons() {
        addPersonToNetwork(1, "Alice", 20);
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum(); // Check purity after first call

        // Arrange 2: Add 2nd person
        addPersonToNetwork(2, "Bob", 22);
        // Act & Assert 2
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum(); // Check purity after second state

        // Arrange 3: Add edge
        ensureRelation(1, 2, 10);
        // Act & Assert 3
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum(); // Check purity after third state
    }

    @Test
    public void testQueryTripleSumNoTriangleLineGraph() {
        // Arrange: 1-2-3-4
        addNNodes(1, 4);
        ensureRelation(1, 2, 10);
        ensureRelation(2, 3, 10);
        ensureRelation(3, 4, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(0, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumNoTriangleStarGraph() {
        // Arrange: 1 -- 2, 1 -- 3, 1 -- 4 (center 1)
        addNNodes(1, 4);
        ensureRelation(1, 2, 10);
        ensureRelation(1, 3, 10);
        ensureRelation(1, 4, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(0, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumNoTriangleCycleGraphC4() {
        // Arrange: 1-2-3-4-1
        addNNodes(1, 4);
        ensureRelation(1, 2, 10);
        ensureRelation(2, 3, 10);
        ensureRelation(3, 4, 10);
        ensureRelation(4, 1, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(0, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumSingleTriangleK3() {
        // Arrange: K3 (1, 2, 3)
        addCompleteGraph(1, 3);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(1, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumTwoTrianglesSharedEdgeDiamond() {
        // Arrange: Diamond shape (Triangles 1,2,3 and 1,3,4 share edge 1-3)
        addPersonToNetwork(1, "Node1", 20);
        addPersonToNetwork(2, "Node2", 20);
        addPersonToNetwork(3, "Node3", 20);
        addPersonToNetwork(4, "Node4", 20);
        ensureRelation(1, 2, 10);
        ensureRelation(2, 3, 10);
        ensureRelation(1, 3, 10); // Shared edge
        ensureRelation(1, 4, 10);
        ensureRelation(3, 4, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(2, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumTwoTrianglesSharedVertexBowtie() {
        // Arrange: Bowtie shape (Triangles 1,2,3 and 3,4,5 share vertex 3)
        addNNodes(1, 5);
        ensureRelation(1, 2, 10); ensureRelation(2, 3, 10); ensureRelation(1, 3, 10); // Tri 1
        ensureRelation(3, 4, 10); ensureRelation(4, 5, 10); ensureRelation(3, 5, 10); // Tri 2
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(2, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumCompleteGraphK4() {
        // Arrange: K4 (1, 2, 3, 4)
        addCompleteGraph(1, 4);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(4, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumCompleteGraphK5() {
        // Arrange: K5 (1, 2, 3, 4, 5)
        addCompleteGraph(1, 5);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(10, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumBipartiteGraphK3_3() {
        // Arrange: K(3,3) - Part 1: 1, 2, 3; Part 2: 4, 5, 6
        addNNodes(1, 6);
        for (int i = 1; i <= 3; i++) for (int j = 4; j <= 6; j++) ensureRelation(i, j, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(0, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumMultipleComponents() {
        // Arrange: Component 1: K3 (1,2,3); Component 2: K4 (101-104); Component 3: Line (201-203)
        addCompleteGraph(1, 3); // Comp 1: K3
        addCompleteGraph(101, 4); // Comp 2: K4
        addNNodes(201, 3); ensureRelation(201, 202, 10); ensureRelation(202, 203, 10); // Comp 3: Line
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Total = 1 + 4 + 0 = 5
        Assert.assertEquals(5, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumWithZeroAndNegativeIds() {
        // Arrange: Triangle with IDs 0, -1, -2
        addPersonToNetwork(0, "Zero", 30); addPersonToNetwork(-1, "NegOne", 30); addPersonToNetwork(-2, "NegTwo", 30);
        ensureRelation(0, -1, 5); ensureRelation(-1, -2, 5); ensureRelation(0, -2, 5);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(1, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumCycleGraphC5() {
        // Arrange: 环形图 C5: 1-2-3-4-5-1
        addNNodes(1, 5);
        ensureRelation(1, 2, 10); ensureRelation(2, 3, 10); ensureRelation(3, 4, 10); ensureRelation(4, 5, 10); ensureRelation(5, 1, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(0, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumWheelGraphW4() {
        // Arrange: 轮图 W4: 环 C4 (1-2-3-4-1) + 中心点 0 连接所有环上点
        addPersonToNetwork(0, "Center", 50); addNNodes(1, 4);
        ensureRelation(1, 2, 10); ensureRelation(2, 3, 10); ensureRelation(3, 4, 10); ensureRelation(4, 1, 10); // Cycle
        ensureRelation(0, 1, 10); ensureRelation(0, 2, 10); ensureRelation(0, 3, 10); ensureRelation(0, 4, 10); // Spokes
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Triangles (0,1,2), (0,2,3), (0,3,4), (0,4,1)
        Assert.assertEquals(4, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumWheelGraphW5() {
        // Arrange: 轮图 W5: 环 C5 (1-2-3-4-5-1) + 中心点 0 连接所有环上点
        addPersonToNetwork(0, "Center", 50); addNNodes(1, 5);
        ensureRelation(1, 2, 10); ensureRelation(2, 3, 10); ensureRelation(3, 4, 10); ensureRelation(4, 5, 10); ensureRelation(5, 1, 10); // Cycle
        ensureRelation(0, 1, 10); ensureRelation(0, 2, 10); ensureRelation(0, 3, 10); ensureRelation(0, 4, 10); ensureRelation(0, 5, 10); // Spokes
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Triangles (0,1,2), (0,2,3), (0,3,4), (0,4,5), (0,5,1)
        Assert.assertEquals(5, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumLollipopGraphK4Path3() {
        // Arrange: 棒棒糖图: K4 (1,2,3,4) 连接到路径 P3 (4-5-6)
        addCompleteGraph(1, 4); // K4 -> 4 triangles
        addPersonToNetwork(5, "P5", 20); addPersonToNetwork(6, "P6", 20);
        ensureRelation(4, 5, 10); ensureRelation(5, 6, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(4, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumBarbellGraph2K3Path2() {
        // Arrange: 哑铃图: 两个 K3 (1-3 and 101-103) 通过路径 P2 (3-4-101) 连接
        addCompleteGraph(1, 3); // K3_1 -> 1 triangle
        addCompleteGraph(101, 3); // K3_2 -> 1 triangle
        addPersonToNetwork(4, "Bridge", 30);
        ensureRelation(3, 4, 10); ensureRelation(4, 101, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Total = 1 + 1 = 2
        Assert.assertEquals(2, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumNearCompleteGraphK5MinusOneEdge() {
        // Arrange: K5 minus edge (4, 5) -> Nodes 1, 2, 3, 4, 5
        int n = 5; addNNodes(1, n);
        for (int i = 1; i <= n; i++) for (int j = i + 1; j <= n; j++) if (!(i == 4 && j == 5)) ensureRelation(i, j, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Expected = C(5,3) - 3 = 7
        Assert.assertEquals(7, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumNearCompleteGraphK6MinusOneEdge() {
        // Arrange: K6 minus edge (5, 6) -> Nodes 1..6
        int n = 6; addNNodes(1, n);
        for (int i = 1; i <= n; i++) for (int j = i + 1; j <= n; j++) if (!(i == 5 && j == 6)) ensureRelation(i, j, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Expected = C(6,3) - 4 = 16
        Assert.assertEquals(16, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumLargeBipartiteK50_50() {
        // Arrange: 大型完全二分图 K(50,50)
        int partSize = 50; addNNodes(1, partSize); addNNodes(101, partSize);
        for (int i = 1; i <= partSize; i++) for (int j = 101; j <= 100 + partSize; j++) ensureRelation(i, j, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(0, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumGraphWithHighDegreeNodes() {
        // Arrange: 中心节点 1 连接到 2..50，然后在 2..11 之间形成 K10
        int nTotal = 50, cliqueSize = 10; addPersonToNetwork(1, "Hub", 99); addNNodes(2, nTotal - 1);
        for (int i = 2; i <= nTotal; i++) ensureRelation(1, i, 10);
        for (int i = 2; i <= cliqueSize + 1; i++) for (int j = i + 1; j <= cliqueSize + 1; j++) ensureRelation(i, j, 10);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Expected = C(10,3) + C(10,2) = 120 + 45 = 165
        Assert.assertEquals(165, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumGraphWithManyIsolatedNodes() {
        // Arrange: 一个 K4 (1,2,3,4) + 96 个孤立节点 (5..100)
        int cliqueSize = 4, isolatedCount = 96; addCompleteGraph(1, cliqueSize); addNNodes(cliqueSize + 1, isolatedCount);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(4, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumGraphWithManyDegreeOneNodes() {
        // Arrange: 一个 K4 (1,2,3,4) + 48 对 度为1的节点 (5-6, 7-8, ..., 99-100)
        int cliqueSize = 4, pairCount = 48; addCompleteGraph(1, cliqueSize);
        for (int i = 0; i < pairCount; i++) { int id1 = cliqueSize+1+2*i, id2 = cliqueSize+2+2*i; addPersonToNetwork(id1,"PA"+i,1); addPersonToNetwork(id2,"PB"+i,1); ensureRelation(id1,id2,1); }
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(4, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumNearLimitCompleteGraphK100() {
        // Arrange: 100 节点完全图
        int n = 100; addCompleteGraph(1, n);
        // Act
        long actualTripleSum = network.queryTripleSum();
        // Assert Value
        long expected = combinations(n, 3); if (expected == -1) Assert.fail("Combination overflow");
        Assert.assertEquals(expected, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testQueryTripleSumNearLimitSparseGraphWithCliques() {
        // Arrange: 100 个节点，包含 3 个 K5，其余线性连接
        addCompleteGraph(1, 5); addCompleteGraph(6, 5); addCompleteGraph(11, 5); // 3 * K5
        long expected = 3 * combinations(5, 3); // 30
        addNNodes(16, 85); for (int i = 16; i < 100; i++) ensureRelation(i, i + 1, 5); // Line 16-100
        ensureRelation(5, 16, 5); ensureRelation(10, 50, 5); ensureRelation(15, 99, 5); // Connect cliques to line
        // Act
        long actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(expected, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testAddEdgeCompletesOneTriangle() {
        // Arrange
        addPersonToNetwork(1, "A", 20); addPersonToNetwork(2, "B", 20); addPersonToNetwork(3, "C", 20); addPersonToNetwork(4, "D", 20);
        ensureRelation(1, 2, 10); ensureRelation(2, 3, 10); ensureRelation(3, 4, 10);
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum(); // Check purity before change

        // Act
        ensureRelation(1, 3, 10); // Add completing edge
        // Assert
        Assert.assertEquals(1, network.queryTripleSum());
        assertPurityOfQueryTripleSum(); // Check purity after change and call
    }

    @Test
    public void testAddEdgeCompletesMultipleTriangles() {
        // Arrange
        addPersonToNetwork(1, "A", 20); addPersonToNetwork(2, "B", 20); addPersonToNetwork(3, "C", 20); addPersonToNetwork(4, "D", 20);
        ensureRelation(1, 2, 10); ensureRelation(1, 3, 10); ensureRelation(4, 2, 10); ensureRelation(4, 3, 10);
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum();

        // Act 1
        ensureRelation(2, 3, 10); // Complete (1,2,3) and (4,2,3)
        // Assert 1
        Assert.assertEquals(2, network.queryTripleSum());
        assertPurityOfQueryTripleSum();

        // Act 2
        ensureRelation(1, 4, 10); // Complete (1,2,4) and (1,3,4) -> K4
        // Assert 2
        Assert.assertEquals(4, network.queryTripleSum());
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testAddEdgeDoesNotCompleteTriangle() {
        // Arrange
        addPersonToNetwork(1, "A", 20); addPersonToNetwork(2, "B", 20); addPersonToNetwork(3, "C", 20); addPersonToNetwork(4, "D", 20);
        ensureRelation(1, 2, 10); ensureRelation(3, 4, 10);
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum();

        // Act
        ensureRelation(1, 3, 10); // Connect components
        // Assert
        Assert.assertEquals(0, network.queryTripleSum());
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testComplexStructureManualCheck() {
        // Arrange
        addNNodes(1, 8);
        ensureRelation(1,2,1); ensureRelation(2,3,1); ensureRelation(3,4,1); ensureRelation(4,5,1); ensureRelation(5,1,1); // C5
        ensureRelation(1,3,1); ensureRelation(1,4,1); ensureRelation(6,1,1); ensureRelation(6,2,1); ensureRelation(7,8,1);
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(4, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testWindmillGraphWd3_5() {
        // Arrange: 5 K3s sharing vertex 0
        int k = 3; int nCliques = 5;
        addPersonToNetwork(0, "Center", 50);
        for (int i = 0; i < nCliques; i++) {
            int v1Id = 1 + i * (k - 1); int v2Id = 2 + i * (k - 1);
            addPersonToNetwork(v1Id, "V1_" + i, 20); addPersonToNetwork(v2Id, "V2_" + i, 20);
            ensureRelation(0, v1Id, 10); ensureRelation(0, v2Id, 10); ensureRelation(v1Id, v2Id, 10);
        }
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value
        Assert.assertEquals(5, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

    @Test
    public void testOverlappingK4s() {
        // Arrange: Two K4s sharing a K3 {1,2,3}
        addCompleteGraph(1, 4); // K4_1 -> 4 triangles
        addPersonToNetwork(5, "Node5", 30);
        ensureRelation(1, 5, 10); ensureRelation(2, 5, 10); ensureRelation(3, 5, 10); // Connect 5 to form K4_2
        // Act
        int actualTripleSum = network.queryTripleSum();
        // Assert Value: Total = 4 (from K4_1) + 3 (new with node 5) = 7
        Assert.assertEquals(7, actualTripleSum);
        // Assert Purity
        assertPurityOfQueryTripleSum();
    }

}