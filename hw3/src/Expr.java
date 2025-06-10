import java.math.BigInteger;
import java.util.ArrayList;

public class Expr implements Factor {
    private final ArrayList<Term> terms;

    public Expr() {
        this.terms = new ArrayList<>();
    }

    public Expr(Factor factor) {
        this.terms = new ArrayList<>();
        Term term = new Term(1);
        term.addFactor(factor);
        this.terms.add(term);
    }

    public Expr clone() {
        Expr expr = new Expr();
        for (Term term : terms) {
            expr.addTerm(term);
        }
        return expr;
    }

    public void addTerm(Term term) {
        terms.add(term);
    }

    public Poly toPoly() {
        Poly poly = new Poly();
        for (Term it : terms) {
            Poly temp = poly.addPoly(it.toPoly()); // 累加所有项
            poly = temp;
        }
        return poly;
    }

    public Expr assign() {
        Expr expr = new Expr();
        for (Term it : terms) {
            Term newTerm = new Term(it.getSign());
            ArrayList<Factor> factors = it.getFactors();
            for (Factor factor : factors) {
                if (factor instanceof FuncFactor) {
                    FuncFactor factorFunc = (FuncFactor) factor;
                    newTerm.addFactor(factorFunc.assign());
                } else if (factor instanceof SubExpr) {
                    SubExpr subExpr = (SubExpr) factor;
                    Expr newExpr = subExpr.getBase();
                    int power = subExpr.getPower();
                    newExpr = newExpr.assign();
                    SubExpr newSubExpr = new SubExpr(newExpr);
                    newSubExpr.changePower(power);
                    newTerm.addFactor(newSubExpr);
                } else if (factor instanceof SinFactor) {
                    SinFactor sinFactor = (SinFactor) factor;
                    BigInteger exponent = sinFactor.getExponent();
                    Expr innerExpr = new Expr(sinFactor.getFactor());
                    innerExpr = innerExpr.assign();
                    SubExpr subExpr = new SubExpr(innerExpr);
                    SinFactor newSinFactor = new SinFactor(subExpr, exponent);
                    newTerm.addFactor(newSinFactor);
                } else if (factor instanceof CosFactor) {
                    CosFactor cosFactor = (CosFactor) factor;
                    BigInteger exponent = cosFactor.getExponent();
                    Expr innerExpr = new Expr(cosFactor.getFactor());
                    innerExpr = innerExpr.assign();
                    CosFactor newCosFactor = new CosFactor(innerExpr, exponent);
                    newTerm.addFactor(newCosFactor);
                } else if (factor instanceof SimpleFuncFactor) {
                    SimpleFuncFactor simpleFuncFactor = (SimpleFuncFactor) factor;
                    Factor factor1 = simpleFuncFactor.assign();
                    newTerm.addFactor(factor1);
                } else {
                    newTerm.addFactor(factor);
                }
            }
            expr.addTerm(newTerm);
        }
        return expr;
    }

    public Expr derive() {
        Expr derived = new Expr();
        for (Term term : terms) {
            derived = derived.addExpr(term.derive());
        }
        return derived;
    }

    private Expr addExpr(Expr expr) {
        for (Term term : expr.getTerms()) {
            this.addTerm(term.clone());
        }
        return this;
    }

    public ArrayList<Term> getTerms() {
        return new ArrayList<>(terms);
    }
}
