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
    
    public Expr derive() {
        if (exponent.equals(BigInteger.ZERO)) {
            return new Expr(new NumFactor(BigInteger.ZERO));
        } else {
            Expr innerDerive = this.factor.derive();
            Expr result = new Expr();
            for (Term term : innerDerive.getTerms()) {
                Term newTerm = new Term(-term.getSign()); // 负号
                newTerm.addFactor(new NumFactor(this.exponent)); // 添加系数 n
                if (!exponent.equals(BigInteger.ONE)) { // 添加 cos(u)^(n-1)
                    CosFactor cosPower = new CosFactor(this.factor.clone(),
                        exponent.subtract(BigInteger.ONE));
                    newTerm.addFactor(cosPower);
                }
                // 添加 sin(u)
                newTerm.addFactor(new SinFactor(this.factor.clone(), BigInteger.ONE));
                for (Factor factor : term.getFactors()) { // 添加 u'
                    newTerm.addFactor(factor.clone());
                }
                result.addTerm(newTerm);
            }
            return result;
        }
    }

    public CosFactor clone() {
        return new CosFactor(factor, exponent);
    }
}
