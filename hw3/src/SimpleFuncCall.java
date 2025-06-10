import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleFuncCall {
    private static HashMap<String, Expr> funcMap = new HashMap<>();
    private static HashMap<String, ArrayList<String>> paraMap = new HashMap<>();

    public static void addDef(ArrayList<String> simpleFuncDef) {
        for (int i = 0; i < simpleFuncDef.size(); i++) {
            String funcDef = simpleFuncDef.get(i);
            if (funcDef.charAt(0) == 'h') {
                String funcName = "h";
                Lexer lexer = new Lexer(funcDef);
                parseFuncDef(lexer, funcName);
            } else if (funcDef.charAt(0) == 'g') {
                String funcName = "g";
                Lexer lexer = new Lexer(funcDef);
                parseFuncDef(lexer, funcName);
            }
        }
    }

    private static void parseFuncDef(Lexer lexer, String funcName) {
        lexer.nextToken(); // skip 'h' | 'g'
        lexer.nextToken(); // skip '('
        ArrayList<String> parameters = new ArrayList<>();
        parameters.add(lexer.getCurToken().getContent()); // 把一个形参放进参数容器里面
        lexer.nextToken(); // skip 'x' | 'y'
        if (lexer.getCurToken().getType() == Token.Type.COMA) {
            lexer.nextToken(); // skip ','
            parameters.add(lexer.getCurToken().getContent()); // 把第二个形参放进参数容器里面
            lexer.nextToken(); // skip parameter
        }
        lexer.nextToken(); // skip ')'
        lexer.nextToken(); // skip '='
        Parser parser = new Parser(lexer);
        Expr expr = parser.parseExpr();
        funcMap.put(funcName, expr);
        paraMap.put(funcName, parameters);
    }

    public static Factor forReplace(String funcName, ArrayList<Factor> parameters) {
        ArrayList<String> params = paraMap.get(funcName); // 形参
        HashMap<String, Factor> newParams = new HashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            newParams.put(params.get(i), parameters.get(i)); // 把形参和实参一一对应
        }
        Expr expr = funcMap.get(funcName); // 取出有对应形参的函数表达式
        return new SubExpr(myReplace(expr, newParams)); // 替换
    }

    private static Expr myReplace(Expr expr, HashMap<String, Factor> newParams) {
        Expr newExpr = new Expr();
        for (Term term : expr.getTerms()) {
            Term newTerm = new Term(term.getSign());
            for (Factor factor : term.getFactors()) {
                if (factor instanceof VarFactor) {
                    VarFactor varFactor = (VarFactor) factor;
                    String string = varFactor.getVar();
                    BigInteger power = varFactor.getPower();
                    Factor realFactor = newParams.get(string);
                    SubExpr subExpr = new SubExpr(realFactor);
                    subExpr.changePower(power.intValue());
                    newTerm.addFactor(subExpr);
                } else if (factor instanceof SubExpr) {
                    Expr base = ((SubExpr) factor).getBase();
                    int power = ((SubExpr) factor).getPower();
                    Expr anewExpr = myReplace(base, newParams);
                    SubExpr newSubExpr = new SubExpr(anewExpr);
                    newSubExpr.changePower(power);
                    newTerm.addFactor(newSubExpr);
                } else if (factor instanceof SinFactor) {
                    Expr innerExpr = new Expr(((SinFactor) factor).getFactor());
                    BigInteger power = ((SinFactor) factor).getExponent();
                    Expr answerExpr = myReplace(innerExpr, newParams);
                    SubExpr newSubExpr = new SubExpr(answerExpr); // Sin内部的子串
                    SinFactor newSinFactor = new SinFactor(newSubExpr, power);
                    newTerm.addFactor(newSinFactor);
                } else if (factor instanceof CosFactor) {
                    Expr innerExpr = new Expr(((CosFactor) factor).getFactor());
                    BigInteger power = (((CosFactor) factor).getExponent());
                    Expr answerExpr = myReplace(innerExpr, newParams);
                    SubExpr newSubExpr = new SubExpr(answerExpr);
                    CosFactor newCosFactor = new CosFactor(newSubExpr, power);
                    newTerm.addFactor(newCosFactor);
                } else if (factor instanceof SimpleFuncFactor) {
                    String funcName = ((SimpleFuncFactor) factor).getFuncName();
                    ArrayList<Factor> params = ((SimpleFuncFactor) factor).getParams();
                    SimpleFuncFactor newSimpleFuncFactor = new SimpleFuncFactor(funcName, params);
                    Factor factor1 = newSimpleFuncFactor.assign();
                    Expr expr1 = new Expr(factor1);
                    Expr answerExpr = myReplace(expr1, newParams);
                    SubExpr newSubExpr = new SubExpr(answerExpr);
                    newTerm.addFactor(newSubExpr);
                } else {
                    newTerm.addFactor(factor);
                }
            }
            newExpr.addTerm(newTerm);
        }
        return newExpr;
    }
}
