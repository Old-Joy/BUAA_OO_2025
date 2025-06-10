public class DeriveFactor implements Factor {
    private Expr innerExpr;

    public DeriveFactor(Expr innerExpr) {
        this.innerExpr = innerExpr;
    }

    public Expr derive() {
        return innerExpr.derive().derive();
    }

    public Expr getInnerExpr() {
        return innerExpr.clone();
    }

    public DeriveFactor clone() {
        return new DeriveFactor(innerExpr.clone());
    }

    public Poly toPoly() {
        Expr expr = innerExpr.derive();
        return expr.toPoly();
    }
}
