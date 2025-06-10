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

    public FuncFactor clone() {
        return new FuncFactor(level, params);
    }

    public Expr derive() {
        Expr result = new Expr(assign());
        return result.derive();
    }

    public Factor assign() {
        ArrayList<Factor> newParams = new ArrayList<>();
        for (Factor it : params) {
            if (it instanceof FuncFactor) {
                FuncFactor factor = (FuncFactor) it;
                Factor itAssign = factor.assign();
                SubExpr subExpr = new SubExpr(itAssign);
                newParams.add(subExpr);
            } else if (it instanceof SubExpr) {
                SubExpr subExpr = (SubExpr) it;
                Expr innerExpr = subExpr.getBase();
                int power = subExpr.getPower();
                Expr expr = innerExpr.assign();
                SubExpr newSubExpr = new SubExpr(expr);
                newSubExpr.changePower(power);
                newParams.add(newSubExpr);
            } else if (it instanceof SimpleFuncFactor) {
                SimpleFuncFactor factor = (SimpleFuncFactor) it;
                Factor itAssign = factor.assign();
                SubExpr subExpr = new SubExpr(itAssign);
                newParams.add(itAssign);
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
                SubExpr subExpr = new SubExpr(innerExpr);
                CosFactor newCosFactor = new CosFactor(subExpr, exponent);
                newParams.add(newCosFactor);
            } else if (it instanceof DeriveFactor) {
                Expr innerExpr = ((DeriveFactor) it).getInnerExpr();
                Expr result = innerExpr.derive();
                result = result.assign();
                SubExpr subExpr = new SubExpr(result);
                newParams.add(subExpr);
            } else {
                newParams.add(it);
            }
        }
        // 得到了一个不含函数因子的参数容器
        return FuncCall.forReplace(level, newParams);
    }

}
