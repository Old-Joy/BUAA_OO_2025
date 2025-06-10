import java.util.ArrayList;

public class SubExpr extends Expr implements Factor {
    private final ArrayList<Term> terms = new ArrayList<>();
    private int power = 1;
    private final Expr base;

    public SubExpr(Expr expr) {
        this.base = expr;
    }

    public void changePower(int power) {
        this.power = power;
    }

    public int getPower() {
        return power;
    }

    public Poly toPoly() {
        Poly basePoly = this.base.toPoly();  // 调用父类Expr的转换方法
        return basePoly.powerPoly(this.power);
    }
}
