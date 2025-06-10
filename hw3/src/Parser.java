import java.math.BigInteger;
import java.util.ArrayList;

public class Parser {
    private final Lexer lexer;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    public Expr parseExpr() {
        Expr expr = new Expr();
        if (lexer.getCurToken().getType() == Token.Type.SUB) {
            lexer.nextToken();
            expr.addTerm(parseTerm(-1));
        } else {
            expr.addTerm(parseTerm(1));
        }
        while (!lexer.isEnd() && (lexer.getCurToken().getType() == Token.Type.ADD ||
                lexer.getCurToken().getType() == Token.Type.SUB)) {
            if (lexer.getCurToken().getType() == Token.Type.ADD) {
                lexer.nextToken();
                expr.addTerm(parseTerm(1));
            } else if (lexer.getCurToken().getType() == Token.Type.SUB) {
                lexer.nextToken();
                expr.addTerm(parseTerm(-1));
            }
        }
        return expr;
    }

    public Term parseTerm(int sign) {
        Term term = new Term(sign);
        term.addFactor(parseFactor());
        while (!lexer.isEnd() && lexer.getCurToken().getType() == Token.Type.MUL) {
            lexer.nextToken();
            term.addFactor(parseFactor());
        }
        return term;
    }

    public Factor parseFactor() {
        Token token = lexer.getCurToken();
        if (token.getType() == Token.Type.NUM ||
            (token.getType() == Token.Type.SUB
            && lexer.getNextNumToken(1).getType() == Token.Type.NUM) ||
            (token.getType() == Token.Type.ADD &&
            lexer.getNextNumToken(1).getType() == Token.Type.NUM)) {
            return parseNumFactor();
        } else if (token.getType() == Token.Type.X || token.getType() == Token.Type.Y) {
            return parseVarFactor();
        } else if (token.getType() == Token.Type.SIN) {
            return parseSinFactor();
        } else if (token.getType() == Token.Type.COS) {
            return parseCosFactor();
        } else if (token.getType() == Token.Type.FUNC) {
            return parseFuncFactor();
        } else if (token.getType() == Token.Type.SIMPLEFUNC) {
            return parseSimpleFuncFactor();
        } else if (token.getType() == Token.Type.DIRIVE) {
            return parseDeriveFactor();
        } else {
            lexer.nextToken(); // skip '('
            Expr innerExpr = parseExpr();
            SubExpr subExpr = new SubExpr(innerExpr);
            lexer.nextToken(); // skip ')'
            if (!lexer.isEnd() && lexer.getCurToken().getType() == Token.Type.POW) {
                lexer.nextToken(); // skip '^'
                if (lexer.getCurToken().getType() == Token.Type.NUM) {
                    int power = Integer.parseInt(lexer.getCurToken().getContent());
                    subExpr.changePower(power);
                    lexer.nextToken(); // skip NUM
                }
            }
            return subExpr;
        }
    }

    public SimpleFuncFactor parseSimpleFuncFactor() {
        final String funcName = lexer.getCurToken().getContent(); // get 'g' or 'h'
        lexer.nextToken(); // skip 'g' or 'h'
        lexer.nextToken(); // skip '('
        Factor factor1 = parseFactor();
        ArrayList<Factor> factors = new ArrayList<>();
        factors.add(factor1);
        if (lexer.getCurToken().getType() == Token.Type.COMA) {
            lexer.nextToken();
            Factor factor2 = parseFactor();
            if (!factors.contains(factor2)) {
                factors.add(factor2);
            }
        }
        lexer.nextToken(); // skip ')'
        return new SimpleFuncFactor(funcName, factors);
    }

    public DeriveFactor parseDeriveFactor() {
        lexer.nextToken(); // skip "dx"
        lexer.nextToken(); // skip '('
        Expr expr = parseExpr();
        lexer.nextToken(); // skip ')'
        return new DeriveFactor(expr);
    }

    public NumFactor parseNumFactor() {
        Token token = lexer.getCurToken();
        if (token.getType() == Token.Type.SUB) {
            lexer.nextToken();
            Token num = lexer.getCurToken();
            lexer.nextToken();
            String numStr = num.getContent();
            NumFactor result = new NumFactor(new BigInteger("-" + numStr));
            return result;
        } else if (token.getType() == Token.Type.ADD) {
            lexer.nextToken(); // skip '+'
            Token num = lexer.getCurToken();
            lexer.nextToken();
            String numStr = num.getContent();
            NumFactor result = new NumFactor(new BigInteger(numStr));
            return result;
        } else {
            lexer.nextToken(); // skip NUM
            NumFactor result = new NumFactor(new BigInteger(token.getContent()));
            return result;
        }
    }

    public VarFactor parseVarFactor() {
        String varName = lexer.getCurToken().getContent();
        lexer.nextToken(); // skip X
        if (!lexer.isEnd()) {
            if (lexer.getCurToken().getType() == Token.Type.POW) {
                lexer.nextToken(); // skip '^'
                Token t = lexer.getCurToken();
                VarFactor var = new VarFactor(varName, new BigInteger(t.getContent()));
                lexer.nextToken(); // skip NUM
                return var;
            } else {
                VarFactor var = new VarFactor(varName, BigInteger.ONE);
                return var;
            }
        } else {
            VarFactor var = new VarFactor(varName,BigInteger.ONE);
            return var;
        }
    }

    public SinFactor parseSinFactor() {
        lexer.nextToken(); // skip "sin"
        lexer.nextToken(); // skip '('
        Factor subFactor = parseFactor();
        lexer.nextToken(); // skip ')'
        if (!lexer.isEnd() && lexer.getCurToken().getType() == Token.Type.POW) {
            lexer.nextToken(); // skip '^'
            Token t = lexer.getCurToken();
            lexer.nextToken(); // skip NUM
            return new SinFactor(subFactor, new BigInteger(t.getContent()));
        } else {
            return new SinFactor(subFactor, BigInteger.ONE);
        }
    }

    public CosFactor parseCosFactor() {
        lexer.nextToken(); // skip "cos"
        lexer.nextToken(); // skip '('
        Factor subFactor = parseFactor();
        lexer.nextToken(); // skip ')'
        if (!lexer.isEnd() && lexer.getCurToken().getType() == Token.Type.POW) {
            lexer.nextToken(); // skip '^'
            Token t = lexer.getCurToken();
            lexer.nextToken(); // skip NUM
            return new CosFactor(subFactor, new BigInteger(t.getContent()));
        } else {
            return new CosFactor(subFactor, BigInteger.ONE);
        }
    }

    public FuncFactor parseFuncFactor() {
        lexer.nextToken(); // skip "f"
        lexer.nextToken(); // skip '{'
        if (lexer.getCurToken().getType().equals(Token.Type.N)) {
            lexer.nextToken(); // skip 'n'
            lexer.nextToken(); // skip '-'
        }
        final BigInteger series = new BigInteger(lexer.getCurToken().getContent());
        lexer.nextToken(); // skip num
        lexer.nextToken(); // skip '}'
        lexer.nextToken(); // skip '('
        Factor factor1 = parseFactor();
        ArrayList<Factor> factors = new ArrayList<>();
        factors.add(factor1);
        if (lexer.getCurToken().getType() == Token.Type.COMA) {
            lexer.nextToken();
            Factor factor2 = parseFactor();
            if (!factors.contains(factor2)) {
                factors.add(factor2);
            }
        }
        lexer.nextToken(); // skip ')'
        return new FuncFactor(series, factors);
    }
}
