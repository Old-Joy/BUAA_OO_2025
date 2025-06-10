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

    public Expr derive() {
        if (power.equals(BigInteger.ZERO)) {
            return new Expr(new NumFactor(BigInteger.ZERO));
        } else if (power.equals(BigInteger.ONE)) {
            return new Expr(new NumFactor(BigInteger.ONE));
        } else {
            NumFactor numFactor = new NumFactor(power);
            VarFactor varFactor = new VarFactor(var, power.subtract(BigInteger.ONE));
            Term term = new Term(1);
            term.addFactor(numFactor);
            term.addFactor(varFactor);
            Expr expr = new Expr();
            expr.addTerm(term);
            return expr;
        }
    }

    public VarFactor clone() {
        return new VarFactor(var, power);
    }

    public BigInteger getPower() {
        return power;
    }
}
