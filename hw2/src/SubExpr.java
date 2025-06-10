import java.util.ArrayList;

public class SubExpr extends Expr implements Factor {
    private int power = 1;
    private final Expr base;

    public SubExpr(Expr expr) {
        this.base = expr;
    }

    public SubExpr(Factor factor) {
        this.base = new Expr(factor);
    }

    public void changePower(int power) {
        this.power = power;
    }

    public int getPower() {
        return power;
    }

    public Expr getBase() {
        return base;
    }

    public Poly toPoly() {
        Poly basePoly = this.base.toPoly();  // 调用父类Expr的转换方法
        return basePoly.powerPoly(this.power);
    }

    public Expr assign() {
        Expr expr = new Expr();
        ArrayList<Term> terms = base.getTerms();
        for (Term it : terms) {
            Term newTerm = new Term(it.getSign());
            ArrayList<Factor> factors = it.getFactors();
            for (Factor factor : factors) {
                if (factor instanceof FuncFactor) {
                    FuncFactor factorFunc = (FuncFactor) factor;
                    newTerm.addFactor(factorFunc.assign());
                } else if (factor instanceof SubExpr) {
                    SubExpr subExpr = (SubExpr) factor;
                    Expr innerExpr = subExpr.getBase();
                    int power = subExpr.getPower();
                    Expr expr1 = innerExpr.assign();
                    SubExpr newSubExpr = new SubExpr(expr1);
                    newSubExpr.changePower(power);
                    newTerm.addFactor(newSubExpr);
                } else {
                    newTerm.addFactor(factor);
                }
            }
            expr.addTerm(newTerm);
        }
        return expr;
    }
}
