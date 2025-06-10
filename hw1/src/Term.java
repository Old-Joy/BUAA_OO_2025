import java.math.BigInteger;
import java.util.ArrayList;

public class Term {
    private final ArrayList<Factor> factors = new ArrayList<>();
    private int sign = 1;

    public Term(int sign) {
        this.sign = sign;
    }

    public void addFactor(Factor factor) {
        factors.add(factor);
    }

    public Poly toPoly() {
        Poly poly = new Poly();
        poly.addUnit(new Unit(BigInteger.ONE, BigInteger.ZERO));
        for (Factor it : factors) {
            Poly temp = poly.multiplyPoly(it.toPoly());
            poly = temp;
        }
        if (sign == -1) {
            poly.negate();
        }
        return poly;
    }

}
