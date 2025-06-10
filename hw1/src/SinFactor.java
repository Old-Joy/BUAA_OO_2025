import java.math.BigInteger;

public class SinFactor implements Factor {
    private Factor factor;
    private BigInteger exponent;

    public SinFactor(Factor factor, BigInteger exponent) {
        this.factor = factor;
        this.exponent = exponent;
    }

    public Poly toPoly() {
        Unit unit = new Unit(BigInteger.ONE, BigInteger.ZERO);
        unit.addSin(this.factor.toPoly(), this.exponent);
        Poly result = new Poly();
        result.addUnit(unit);
        return result;
    }
}
