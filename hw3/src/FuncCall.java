import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class FuncCall {
    private static Expr standard;
    private static ArrayList<String> formalParams = new ArrayList<>();
    private static HashMap<BigInteger, Expr> funcExprs = new HashMap<>();

    public static void addDef(String funcFirst, String funcSecond, String funcThird) {
        for (String def : new String[]{funcFirst, funcSecond, funcThird}) {
            if (def.contains("f{0}")) {
                def = def.replace("=-", "=0-");
                def = def.replace("=+", "=0+");
                Lexer lexer = new Lexer(def);
                parserBasicFunction(lexer);
            } else if (def.contains("f{1}")) {
                def = def.replace("=-", "=0-");
                def = def.replace("=+", "=0+");
                Lexer lexer = new Lexer(def);
                parserBasicFunction(lexer); // f{1}定义
            } else {
                def = def.replace("=-", "=0-");
                def = def.replace("=+", "=0+");
                Lexer lexer = new Lexer(def);
                parserThirdFunction(lexer);
            }
        }
        getAllExpr(standard);
    }

    private static void getAllExpr(Expr standard) {
        Expr recursiveExpr = standard;

        // 遍历level从2到5
        BigInteger level = new BigInteger("2");
        while (level.compareTo(BigInteger.valueOf(6)) < 0) {
            // 创建新的表达式副本
            Expr newExpr = new Expr();

            // 遍历原表达式的每个Term
            for (Term term : recursiveExpr.getTerms()) {
                Term newTerm = new Term(term.getSign());

                // 遍历Term中的每个Factor
                for (Factor factor : term.getFactors()) {
                    if (factor instanceof FuncFactor) {
                        FuncFactor funcFactor = (FuncFactor) factor;
                        // 替换f{n-1}为f{level-1}，f{n-2}为f{level-2}
                        if (funcFactor.getLevel().equals(BigInteger.valueOf(1))) {
                            BigInteger actualLevel = level.add(BigInteger.valueOf(-1));
                            ArrayList<Factor> factParams = funcFactor.getParams();
                            SubExpr actualExpr = (SubExpr) forReplace(actualLevel, factParams);
                            newTerm.addFactor(actualExpr);
                        } else if (funcFactor.getLevel().equals(BigInteger.valueOf(2))) {
                            BigInteger actualLevel = level.add(BigInteger.valueOf(-2));
                            ArrayList<Factor> factParams = funcFactor.getParams();
                            SubExpr actualExpr = (SubExpr) forReplace(actualLevel, factParams);
                            newTerm.addFactor(actualExpr);
                        }
                    } else {
                        newTerm.addFactor(factor);
                    }
                }
                newExpr.addTerm(newTerm);
            }
            // 将新表达式存入funcExprs
            funcExprs.put(level, newExpr);
            level = level.add(BigInteger.valueOf(1));
        }
    }

    private static void parserBasicFunction(Lexer lexer) {
        lexer.nextToken(); // skip 'f'
        lexer.nextToken(); // skip '{'
        if (lexer.getCurToken().getContent().equals("0") ||
            lexer.getCurToken().getContent().equals("1")) {
            final String num = lexer.getCurToken().getContent();
            lexer.nextToken(); // skip num
            lexer.nextToken(); // skip '}'
            lexer.nextToken(); // skip '('
            formalParams.add(lexer.getCurToken().getContent()); // 把一个形参放进形参容器里
            lexer.nextToken();
            if (lexer.getCurToken().getType() == Token.Type.COMA) {
                lexer.nextToken();
                formalParams.add(lexer.getCurToken().getContent()); // 把一个形参放进形参容器里
                lexer.nextToken();
            }
            lexer.nextToken(); // skip ')'
            lexer.nextToken(); // skip '='
            Parser parser = new Parser(lexer); // 解析后续表达式
            Expr expr = parser.parseExpr();
            funcExprs.put(BigInteger.valueOf(Integer.parseInt(num)), expr);
        }
    }

    private static void parserThirdFunction(Lexer lexer) {
        while (!lexer.getCurToken().getContent().equals("=")) {
            lexer.nextToken();
        }
        lexer.nextToken(); // skip '='
        standard = parserRecursiveFunction(lexer);
    }

    private static Expr parserRecursiveFunction(Lexer lexer) {
        Parser parser = new Parser(lexer);
        Expr result = parser.parseExpr();
        return result;
    }

    public static Factor forReplace(BigInteger level, ArrayList<Factor> newParams) {
        HashMap<String, Factor> paramsMap = new HashMap<>();
        for (int i = 0; i < newParams.size(); i++) {
            paramsMap.put(formalParams.get(i), newParams.get(i)); // 把形参和实参一一对应
        }
        Expr expr = funcExprs.get(level);
        return new SubExpr(myReplace(expr, paramsMap));
    }

    private static Expr myReplace(Expr expr, HashMap<String, Factor> paramsMap) {
        Expr newExpr = new Expr();
        for (Term term : expr.getTerms()) {
            Term newTerm = new Term(term.getSign());
            for (Factor factor : term.getFactors()) {
                if (factor instanceof VarFactor) {
                    VarFactor varFactor = (VarFactor) factor;
                    String string = varFactor.getVar();
                    BigInteger power = varFactor.getPower();
                    Factor realFactor = paramsMap.get(string);
                    SubExpr subExpr = new SubExpr(realFactor);
                    subExpr.changePower(power.intValue());
                    newTerm.addFactor(subExpr);
                } else if (factor instanceof SubExpr) {
                    Expr base = ((SubExpr) factor).getBase();
                    int power = ((SubExpr) factor).getPower();
                    Expr anewExpr = myReplace(base, paramsMap);
                    SubExpr newSubExpr = new SubExpr(anewExpr);
                    newSubExpr.changePower(power);
                    newTerm.addFactor(newSubExpr);
                } else if (factor instanceof SinFactor) {
                    Expr innerExpr = new Expr(((SinFactor) factor).getFactor());
                    BigInteger power = ((SinFactor) factor).getExponent();
                    Expr answerExpr = myReplace(innerExpr, paramsMap);
                    SubExpr newSubExpr = new SubExpr(answerExpr); // Sin内部的子串
                    SinFactor newSinFactor = new SinFactor(newSubExpr, power);
                    newTerm.addFactor(newSinFactor);
                } else if (factor instanceof CosFactor) {
                    Expr innerExpr = new Expr(((CosFactor) factor).getFactor());
                    BigInteger power = (((CosFactor) factor).getExponent());
                    Expr answerExpr = myReplace(innerExpr, paramsMap);
                    SubExpr newSubExpr = new SubExpr(answerExpr);
                    CosFactor newCosFactor = new CosFactor(newSubExpr, power);
                    newTerm.addFactor(newCosFactor);
                } else if (factor instanceof SimpleFuncFactor) {
                    String funcName = ((SimpleFuncFactor) factor).getFuncName();
                    ArrayList<Factor> params = ((SimpleFuncFactor) factor).getParams();
                    SimpleFuncFactor newSimpleFuncFactor = new SimpleFuncFactor(funcName, params);
                    Factor factor1 = newSimpleFuncFactor.assign();
                    Expr expr1 = new Expr(factor1);
                    Expr answerExpr = myReplace(expr1, paramsMap);
                    newTerm.addFactor(new SubExpr(answerExpr));
                } else {
                    newTerm.addFactor(factor);
                }
            }
            newExpr.addTerm(newTerm);
        }
        return newExpr;
    }
}
