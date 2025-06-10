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

    public NumFactor clone() {
        return new NumFactor(value);
    }

    public Expr derive() {
        Expr expr = new Expr();
        Term term = new Term(1);
        term.addFactor(new NumFactor(BigInteger.ZERO));
        expr.addTerm(term);
        return expr;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
