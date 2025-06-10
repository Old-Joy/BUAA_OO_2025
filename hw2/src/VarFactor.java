import java.math.BigInteger;

public class VarFactor implements Factor {
    private String var;
    private BigInteger power;

    public VarFactor(String var, BigInteger power) {
        this.var = var;
        this.power = power;
    }

    public Poly toPoly() {
        Poly result = new Poly();
        result.addUnit(new Unit(BigInteger.ONE, power));
        return result;
    }

    public String getVar() {
        return var;
    }

    public BigInteger getPower() {
        return power;
    }
}
