import java.math.BigInteger;
import java.util.ArrayList;

public class FuncFactor implements Factor {
    private BigInteger level;
    private ArrayList<Factor> params; // 实参

    public FuncFactor(BigInteger level, ArrayList<Factor> params) {
        this.level = level;
        this.params = new ArrayList<>();
        this.params.addAll(params);
    }

    public ArrayList<Factor> getParams() {
        return new ArrayList<>(params);
    }

    public BigInteger getLevel() {
        return new BigInteger(level.toString());
    }

    public Poly toPoly() {
        return assign().toPoly();
    }

    public Factor assign() {
        ArrayList<Factor> newParams = new ArrayList<>();
        for (Factor it : params) {
            if (it instanceof FuncFactor) {
                FuncFactor factor = (FuncFactor) it;
                newParams.add(factor.assign());
            } else if (it instanceof SubExpr) {
                SubExpr subExpr = (SubExpr) it;
                Expr innerExpr = subExpr.getBase();
                int power = subExpr.getPower();
                Expr expr = innerExpr.assign();
                SubExpr newSubExpr = new SubExpr(expr);
                newSubExpr.changePower(power);
                newParams.add(newSubExpr);
            } else if (it instanceof SinFactor) {
                Factor innerFactor = ((SinFactor) it).getFactor();
                BigInteger exponent = ((SinFactor) it).getExponent();
                Expr innerExpr = new Expr(innerFactor);
                innerExpr = innerExpr.assign();
                SubExpr subExpr = new SubExpr(innerExpr);
                SinFactor newSinFactor = new SinFactor(subExpr, exponent);
                newParams.add(newSinFactor);
            } else if (it instanceof CosFactor) {
                Factor innerFactor = ((CosFactor) it).getFactor();
                BigInteger exponent = ((CosFactor) it).getExponent();
                Expr innerExpr = new Expr(innerFactor);
                innerExpr = innerExpr.assign();
                CosFactor newCosFactor = new CosFactor(innerExpr, exponent);
                newParams.add(newCosFactor);
            }
            else {
                newParams.add(it);
            }
        }
        // 得到了一个不含函数因子的参数容器
        return FuncCall.forReplace(level, newParams);
    }

}
