import com.oocourse.spec3.main.NetworkInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.EmojiMessageInterface;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class NetworkTest {



    private PersonInterface createPerson(int id) throws Exception {
        return new Person(id, "Person" + id, 20);
    }

    private EmojiMessageInterface createEmojiMessage(int msgId, int emojiId, PersonInterface p1, PersonInterface p2) throws Exception {
        return new EmojiMessage(msgId, emojiId, p1, p2);
    }

    private MessageInterface createNormalMessage(int msgId, PersonInterface p1, PersonInterface p2) throws Exception {
        return new Message(msgId, 10, p1, p2);
    }

    private NetworkInterface createNetworkInstance() throws Exception {
        return new Network();
    }

    private void checkDeleteColdEmojiEnsures(NetworkInterface network, int limit,
                                             int[] initialEmojiIds, int[] initialEmojiHeats,
                                             MessageInterface[] initialMessages) throws Exception {
        Map<Integer, Integer> oldEmojiIdToHeatMap = new HashMap<>();
        for (int i = 0; i < initialEmojiIds.length; i++) {
            oldEmojiIdToHeatMap.put(initialEmojiIds[i], initialEmojiHeats[i]);
        }
        Map<Integer, MessageInterface> oldMsgMapById = Arrays.stream(initialMessages)
                .collect(Collectors.toMap(MessageInterface::getId, msg -> msg, (m1,m2)->m1)); // handle duplicates if any by taking first

        int returnedCount = network.deleteColdEmoji(limit);

        int[] currentEmojiIds = ((Network) network).getEmojiIdList();
        int[] currentEmojiHeats = ((Network) network).getEmojiHeatList();
        MessageInterface[] currentMessages = ((Network) network).getMessages();
        Map<Integer, Integer> currentEmojiIdToHeatMap = new HashMap<>();
        for (int i = 0; i < currentEmojiIds.length; i++) {
            currentEmojiIdToHeatMap.put(currentEmojiIds[i], currentEmojiHeats[i]);
        }

        assertEquals(currentEmojiIds.length, returnedCount);
        assertEquals(currentEmojiIds.length, currentEmojiHeats.length);

        long expectedKeptEmojiCount = 0;
        for (int heat : initialEmojiHeats) {
            if (heat >= limit) {
                expectedKeptEmojiCount++;
            }
        }
        assertEquals(expectedKeptEmojiCount, currentEmojiIds.length);

        for (int i = 0; i < initialEmojiIds.length; i++) {
            int oldId = initialEmojiIds[i];
            int oldHeat = initialEmojiHeats[i];
            if (oldHeat >= limit) {
                assertTrue("Kept emoji ID " + oldId + " not found.", currentEmojiIdToHeatMap.containsKey(oldId));
                assertEquals("Heat of kept ID " + oldId + " changed.", oldHeat, (int)currentEmojiIdToHeatMap.get(oldId));
            } else {
                assertFalse("Removed ID " + oldId + " still found.", currentEmojiIdToHeatMap.containsKey(oldId));
            }
        }
        for (int currentId : currentEmojiIds) {
            assertTrue("Current ID " + currentId + " not in old list.", oldEmojiIdToHeatMap.containsKey(currentId));
            assertEquals("Heat of current ID " + currentId + " differs.",
                    (int)oldEmojiIdToHeatMap.get(currentId), (int)currentEmojiIdToHeatMap.get(currentId));
        }

        long expectedMessagesCount = 0;
        for (MessageInterface oldMsg : initialMessages) {
            boolean shouldBeKept = true;
            if (oldMsg instanceof EmojiMessageInterface) {
                int emojiId = ((EmojiMessageInterface) oldMsg).getEmojiId();
                if (!currentEmojiIdToHeatMap.containsKey(emojiId)) {
                    shouldBeKept = false;
                }
            }
            if (shouldBeKept) {
                expectedMessagesCount++;
            }
        }
        assertEquals("Message count mismatch.", expectedMessagesCount, currentMessages.length);

        for (MessageInterface oldMsg : initialMessages) {
            boolean shouldBePresentInCurrent = !(oldMsg instanceof EmojiMessageInterface) ||
                    currentEmojiIdToHeatMap.containsKey(((EmojiMessageInterface)oldMsg).getEmojiId());

            boolean foundInCurrent = false;
            for (MessageInterface currentMsg : currentMessages) {
                if (currentMsg.getId() == oldMsg.getId()) {
                    foundInCurrent = true;
                    break;
                }
            }
            assertEquals("Msg (ID: " + oldMsg.getId() + ") presence expectation.",
                    shouldBePresentInCurrent, foundInCurrent);
        }
    }

    @Test
    public void testEmptyEmojisAndMessages() throws Exception {
        NetworkInterface network = createNetworkInstance();
        checkDeleteColdEmojiEnsures(network, 1, new int[]{}, new int[]{}, new MessageInterface[]{});
    }

    @Test
    public void testNoEmojisButHasMessages() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1);
        PersonInterface p2 = createPerson(2);
        MessageInterface nm1 = createNormalMessage(1, p1, p2);
        network.addMessage(nm1);
        checkDeleteColdEmojiEnsures(network, 1, new int[]{}, new int[]{}, new MessageInterface[]{nm1});
    }

    @Test
    public void testHasEmojisNoMessages() throws Exception {
        NetworkInterface network = createNetworkInstance();
        network.storeEmojiId(10);
        network.storeEmojiId(20);
        checkDeleteColdEmojiEnsures(network, 1, new int[]{10, 20}, new int[]{0, 0}, new MessageInterface[]{});
    }

    @Test
    public void testLimitZeroKeepsAllEmojisAndAssociatedMessages() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1);
        PersonInterface p2 = createPerson(2);
        network.storeEmojiId(10);
        network.storeEmojiId(20);
        EmojiMessageInterface em1 = createEmojiMessage(1, 10, p1, p2);
        EmojiMessageInterface em2 = createEmojiMessage(2, 20, p1, p2);
        MessageInterface nm1 = createNormalMessage(3, p1, p2);
        network.addMessage(em1);
        network.addMessage(em2);
        network.addMessage(nm1);
        checkDeleteColdEmojiEnsures(network, 0,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }

    @Test
    public void testLimitHighRemovesAllEmojisAndAssociatedMessages() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1);
        PersonInterface p2 = createPerson(2);
        network.storeEmojiId(10);
        network.storeEmojiId(20);
        EmojiMessageInterface em1 = createEmojiMessage(1, 10, p1, p2);
        EmojiMessageInterface em2 = createEmojiMessage(2, 20, p1, p2);
        MessageInterface nm1 = createNormalMessage(3, p1, p2);
        network.addMessage(em1);
        network.addMessage(em2);
        network.addMessage(nm1);
        checkDeleteColdEmojiEnsures(network, 100,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }

    @Test
    public void testMixedEmojisSomeKeptSomeRemoved() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1);
        PersonInterface p2 = createPerson(2);
        PersonInterface p3 = createPerson(3);
        PersonInterface p4 = createPerson(4);

        network.addPerson(p1); network.addPerson(p2);
        network.addPerson(p3); network.addPerson(p4);
        network.addRelation(1, 2, 10);
        network.addRelation(3, 4, 10);

        network.storeEmojiId(101);
        network.storeEmojiId(102);
        network.storeEmojiId(103);

        EmojiMessageInterface emS101a = createEmojiMessage(1, 101, p1, p2);
        EmojiMessageInterface emS101b = createEmojiMessage(2, 101, p1, p2);
        EmojiMessageInterface emS102a = createEmojiMessage(3, 102, p3, p4);

        network.addMessage(emS101a); network.sendMessage(1);
        network.addMessage(emS101b); network.sendMessage(2);
        network.addMessage(emS102a); network.sendMessage(3);

        EmojiMessageInterface emKeep101 = createEmojiMessage(4, 101, p1, p2);
        EmojiMessageInterface emKeep102 = createEmojiMessage(5, 102, p1, p2);
        EmojiMessageInterface emRemove103 = createEmojiMessage(6, 103, p1, p2);
        MessageInterface nm1 = createNormalMessage(7, p1, p2);

        network.addMessage(emKeep101);
        network.addMessage(emKeep102);
        network.addMessage(emRemove103);
        network.addMessage(nm1);

        checkDeleteColdEmojiEnsures(network, 2,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }

    @Test
    public void testLimitEqualsHeat() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1); PersonInterface p2 = createPerson(2);
        network.addPerson(p1); network.addPerson(p2);
        network.addRelation(1,2,10);

        network.storeEmojiId(201);
        EmojiMessageInterface emHeat1 = createEmojiMessage(10, 201, p1, p2);
        network.addMessage(emHeat1); network.sendMessage(10);

        EmojiMessageInterface emTest = createEmojiMessage(11, 201, p1,p2);
        MessageInterface nmTest = createNormalMessage(12, p1,p2);
        network.addMessage(emTest);
        network.addMessage(nmTest);

        checkDeleteColdEmojiEnsures(network, 1,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }

    @Test
    public void testMultipleMessagesSameEmoji() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1); PersonInterface p2 = createPerson(2);
        network.addPerson(p1); network.addPerson(p2);
        network.addRelation(1,2,10);

        network.storeEmojiId(301);
        EmojiMessageInterface emHeatMsg = createEmojiMessage(100, 301, p1, p2);
        network.addMessage(emHeatMsg); network.sendMessage(100);

        EmojiMessageInterface em1 = createEmojiMessage(101, 301, p1, p2);
        EmojiMessageInterface em2 = createEmojiMessage(102, 301, p1, p2);
        network.addMessage(em1);
        network.addMessage(em2);

        checkDeleteColdEmojiEnsures(network, 1,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());

        NetworkInterface network2 = createNetworkInstance();
        network2.addPerson(p1); network2.addPerson(p2);
        network2.storeEmojiId(301);
        EmojiMessageInterface emHeatMsg2 = createEmojiMessage(200, 301, p1, p2);
        network2.addMessage(emHeatMsg2); network2.sendMessage(200);

        EmojiMessageInterface em1_2 = createEmojiMessage(201, 301, p1, p2);
        EmojiMessageInterface em2_2 = createEmojiMessage(202, 301, p1, p2);
        network2.addMessage(em1_2);
        network2.addMessage(em2_2);

        checkDeleteColdEmojiEnsures(network2, 2,
                ((Network) network2).getEmojiIdList(), ((Network) network2).getEmojiHeatList(),
                ((Network) network2).getMessages());
    }

    @Test
    public void testNegativeLimit() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1); PersonInterface p2 = createPerson(2);
        network.addPerson(p1); network.addPerson(p2);
        network.addRelation(1,2,10);

        network.storeEmojiId(401);
        EmojiMessageInterface em1 = createEmojiMessage(1, 401, p1, p2);
        MessageInterface nm1 = createNormalMessage(2, p1, p2);
        network.addMessage(em1);
        network.addMessage(nm1);

        checkDeleteColdEmojiEnsures(network, -1,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }

    @Test
    public void testAllMessagesAreEmojiAndAllRemoved() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1); PersonInterface p2 = createPerson(2);
        network.storeEmojiId(501); network.storeEmojiId(502);
        EmojiMessageInterface em1 = createEmojiMessage(1, 501, p1, p2);
        EmojiMessageInterface em2 = createEmojiMessage(2, 502, p1, p2);
        network.addMessage(em1); network.addMessage(em2);
        checkDeleteColdEmojiEnsures(network, 1,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }

    @Test
    public void testAllMessagesAreEmojiAndAllKept() throws Exception {
        NetworkInterface network = createNetworkInstance();
        PersonInterface p1 = createPerson(1); PersonInterface p2 = createPerson(2);
        network.addPerson(p1); network.addPerson(p2);
        network.addRelation(1,2,10);

        network.storeEmojiId(601); network.storeEmojiId(602);
        EmojiMessageInterface emTmp1 = createEmojiMessage(1001, 601, p1,p2);
        EmojiMessageInterface emTmp2 = createEmojiMessage(1002, 602, p1,p2);
        network.addMessage(emTmp1); network.sendMessage(1001);
        network.addMessage(emTmp2); network.sendMessage(1002);


        EmojiMessageInterface em1 = createEmojiMessage(1, 601, p1, p2);
        EmojiMessageInterface em2 = createEmojiMessage(2, 602, p1, p2);
        network.addMessage(em1); network.addMessage(em2);
        checkDeleteColdEmojiEnsures(network, 1,
                ((Network) network).getEmojiIdList(), ((Network) network).getEmojiHeatList(),
                ((Network) network).getMessages());
    }
}