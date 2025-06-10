import java.math.BigInteger;

public class CosFactor implements Factor {
    private Factor factor;
    private BigInteger exponent;

    public CosFactor(Factor factor, BigInteger exponent) {
        this.factor = factor;
        this.exponent = exponent;
    }

    public Factor getFactor() {
        return factor;
    }

    public BigInteger getExponent() {
        return exponent;
    }

    public Poly toPoly() {
        Unit unit = new Unit(BigInteger.ONE, BigInteger.ZERO);
        unit.addCos(this.factor.toPoly(), this.exponent);
        Poly result = new Poly();
        result.addUnit(unit);
        return result;
    }
}
