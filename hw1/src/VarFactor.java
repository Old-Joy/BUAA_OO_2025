import java.math.BigInteger;

public class VarFactor implements Factor {
    private BigInteger power;

    public VarFactor(BigInteger power) {
        this.power = power;
    }

    public Poly toPoly() {
        Poly result = new Poly();
        result.addUnit(new Unit(BigInteger.ONE, power));
        return result;
    }
}
