import java.math.BigInteger;

public class SinFactor implements Factor {
    private Factor factor;
    private BigInteger exponent;

    public SinFactor(Factor factor, BigInteger exponent) {
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
        unit.addSin(this.factor.toPoly(), this.exponent);
        Poly result = new Poly();
        result.addUnit(unit);
        return result;
    }

    public Expr derive() {
        if (exponent.equals(BigInteger.ZERO)) {
            return new Expr(new NumFactor(BigInteger.ZERO));
        } else {
            // dx(sin(f(x))^n) = n*sin(f(x))^(n-1)*cos(f(x))*df(x)
            Expr innerDerive = this.factor.derive(); // 对因子求导 df(x)
            Expr result = new Expr();
            for (Term term : innerDerive.getTerms()) {
                Term newTerm = new Term(term.getSign());
                newTerm.addFactor(new NumFactor(this.exponent)); // n
                if (!exponent.equals(BigInteger.ONE)) {
                    SinFactor sinPower = new SinFactor(this.factor.clone(),
                        this.exponent.subtract(BigInteger.ONE));
                    newTerm.addFactor(sinPower);
                }
                newTerm.addFactor(new CosFactor(this.factor.clone(), BigInteger.ONE));
                for (Factor factor : term.getFactors()) {
                    newTerm.addFactor(factor.clone());
                }
                result.addTerm(newTerm);
            }
            return result;
        }
    }

    public SinFactor clone() {
        return new SinFactor(factor, exponent);
    }
}
