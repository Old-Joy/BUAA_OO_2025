import com.oocourse.spec3.main.MessageInterface;
import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.TagInterface;

import java.util.Objects;

public class Message implements MessageInterface {
    private final int id;
    private int socialValue;
    private final int type;
    private final PersonInterface person1;
    private final PersonInterface person2;
    private final TagInterface tag;

    public Message(int messageId, int messageSocialValue,
        PersonInterface messagePerson1, PersonInterface messagePerson2) {
        this.id = messageId;
        this.socialValue = messageSocialValue;
        this.person1 = messagePerson1;
        this.person2 = messagePerson2;
        this.type = 0;
        this.tag = null;
    }

    public Message(int messageId, int messageSocialValue,
        PersonInterface messagePerson1, TagInterface messageTag) {
        this.id = messageId;
        this.socialValue = messageSocialValue;
        this.person1 = messagePerson1;
        this.tag = messageTag;
        this.type = 1;
        this.person2 = null;
    }

    protected Message(int messageId, PersonInterface sender,
        PersonInterface receiver, int type, int socialValueIfKnown) {
        this.id = messageId;
        this.person1 = sender;
        this.person2 = receiver;
        this.tag = null;
        this.type = type;
        this.socialValue = socialValueIfKnown;
    }

    protected Message(int messageId, PersonInterface sender,
        TagInterface tag, int type, int socialValueIfKnown) {
        this.id = messageId;
        this.person1 = sender;
        this.person2 = null;
        this.tag = tag;
        this.type = type;
        this.socialValue = socialValueIfKnown;
    }

    @Override
    public /*@ pure @*/ int getType() {
        return type;
    }

    @Override
    public /*@ pure @*/ int getId() {
        return id;
    }

    @Override
    public /*@ pure @*/ int getSocialValue() {
        return socialValue;
    }

    protected void setSocialValue(int socialValue) {
        this.socialValue = socialValue;
    }

    @Override
    public /*@ pure @*/ PersonInterface getPerson1() {
        return person1;
    }

    @Override
    public /*@ pure @*/ PersonInterface getPerson2() {
        return person2;
    }

    @Override
    public /*@ pure @*/ TagInterface getTag() {
        return tag;
    }

    @Override
    public /*@ pure @*/ boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MessageInterface)) {
            return false;
        }
        MessageInterface message = (MessageInterface) obj;
        return id == message.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}