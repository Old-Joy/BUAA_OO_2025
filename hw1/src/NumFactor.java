import java.math.BigInteger;

public class NumFactor implements Factor {
    private final BigInteger value;

    public NumFactor(BigInteger value) {
        this.value = value;
    }

    public Poly toPoly() {
        Unit unit = new Unit(value, BigInteger.ZERO);
        Poly poly = new Poly();
        poly.addUnit(unit);
        return poly;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
