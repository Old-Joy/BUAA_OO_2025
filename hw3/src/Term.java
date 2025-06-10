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

    public ArrayList<Factor> getFactors() {
        return new ArrayList<>(factors);
    }

    public Expr derive() {
        Expr derived = new Expr();
        for (int i = 0; i < factors.size(); i++) {
            Factor factor = factors.get(i);
            Expr partialDerive = factor.derive();
            for (Term partialTerm : partialDerive.getTerms()) {
                Term newTerm = new Term(this.sign * partialTerm.getSign());
                for (Factor partialFactor : partialTerm.getFactors()) {
                    newTerm.addFactor(partialFactor.clone());
                }
                for (int j = 0; j < factors.size(); j++) {
                    if (j != i) {
                        newTerm.addFactor(factors.get(j).clone());
                    }
                }
                derived.addTerm(newTerm);
            }
        }
        return derived;
    }

    public Term clone() {
        Term term = new Term(this.sign);
        for (Factor factor : factors) {
            term.addFactor(factor);
        }
        return term;
    }

    public int getSign() {
        return sign;
    }
}
