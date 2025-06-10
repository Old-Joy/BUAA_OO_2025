import java.math.BigInteger;
import java.util.ArrayList;

public class SimpleFuncFactor implements Factor {
    private ArrayList<Factor> params; // 实参列表
    private String funcName;

    public SimpleFuncFactor(String funcName, ArrayList<Factor> params) {
        this.funcName = funcName;
        this.params = new ArrayList<>();
        this.params.addAll(params);
    }

    public String getFuncName() {
        return funcName;
    }

    public ArrayList<Factor> getParams() {
        return params;
    }

    public Poly toPoly() {
        return assign().toPoly();
    }

    public SimpleFuncFactor clone() {
        return new SimpleFuncFactor(funcName, params);
    }

    public Expr derive() {
        Expr result = new Expr(assign());
        return result.derive();
    }

    public Factor assign() {
        ArrayList<Factor> newParams = new ArrayList<>();
        for (Factor param : params) {
            if (param instanceof SimpleFuncFactor) {
                SimpleFuncFactor factor = (SimpleFuncFactor) param;
                Factor factorAssign = factor.assign();
                SubExpr subExpr = new SubExpr(factorAssign);
                newParams.add(subExpr);
            } else if (param instanceof FuncFactor) {
                FuncFactor factor = (FuncFactor) param;
                Factor factorAssign = factor.assign();
                SubExpr subExpr = new SubExpr(factorAssign);
                newParams.add(subExpr);
            } else if (param instanceof SubExpr) {
                SubExpr subExpr = (SubExpr) param;
                Expr innerExpr = subExpr.getBase();
                int power = subExpr.getPower();
                Expr expr = innerExpr.assign();
                SubExpr newSubExpr = new SubExpr(expr);
                newSubExpr.changePower(power);
                newParams.add(newSubExpr);
            } else if (param instanceof SinFactor) {
                Factor innerFactor = ((SinFactor) param).getFactor();
                BigInteger exponent = ((SinFactor) param).getExponent();
                Expr innerExpr = new Expr(innerFactor);
                innerExpr = innerExpr.assign();
                SubExpr subExpr = new SubExpr(innerExpr);
                SinFactor newSinFactor = new SinFactor(subExpr, exponent);
                newParams.add(newSinFactor);
            } else if (param instanceof CosFactor) {
                Factor innerFactor = ((CosFactor) param).getFactor();
                BigInteger exponent = ((CosFactor) param).getExponent();
                Expr innerExpr = new Expr(innerFactor);
                innerExpr = innerExpr.assign();
                SubExpr subExpr = new SubExpr(innerExpr);
                CosFactor newCosFactor = new CosFactor(subExpr, exponent);
                newParams.add(newCosFactor);
            } else if (param instanceof DeriveFactor) {
                Expr innerExpr = ((DeriveFactor) param).getInnerExpr();
                Expr result = innerExpr.derive();
                result = result.assign();
                SubExpr subExpr = new SubExpr(result);
                newParams.add(subExpr);
            } else {
                newParams.add(param);
            }
        }
        return SimpleFuncCall.forReplace(this.funcName, newParams);
    }
}
