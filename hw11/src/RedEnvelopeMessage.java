import com.oocourse.spec3.main.PersonInterface;
import com.oocourse.spec3.main.RedEnvelopeMessageInterface; // Assuming your classes will be in this package
import com.oocourse.spec3.main.TagInterface;

public class RedEnvelopeMessage extends Message implements RedEnvelopeMessageInterface {
    private final int money; //红包金额

    public RedEnvelopeMessage(int messageId, int luckyMoney,
        PersonInterface messagePerson1, PersonInterface messagePerson2) {
        super(messageId, luckyMoney * 5, messagePerson1, messagePerson2);
        this.money = luckyMoney;
    }

    public RedEnvelopeMessage(int messageId, int luckyMoney,
        PersonInterface messagePerson1, TagInterface messageTag) {
        super(messageId, luckyMoney * 5, messagePerson1, messageTag);
        this.money = luckyMoney;
    }

    @Override
    public /*@ pure @*/ int getMoney() {
        return money;
    }

    // getSocialValue() is inherited from Message.
    // Constructors ensure socialValue = money * 5.
}