import java.math.BigInteger;

public class Parser {
    private final Lexer lexer;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    public Expr parseExpr() {
        Expr expr = new Expr();
        if (lexer.getCurToken().getType() == Token.Type.SUB) {
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
        if (token.getType() == Token.Type.NUM) {
            return parseNumFactor();
        } else if (token.getType() == Token.Type.VAR) {
            return parseVarFactor();
        } else if (token.getType() == Token.Type.SIN) {
            return parseSinFactor();
        } else if (token.getType() == Token.Type.COS) {
            return parseCosFactor();
        } else if (token.getType() == Token.Type.FUNC) {
            return parseFuncFactor();
        } else {
            lexer.nextToken(); // skip '('
            Expr innerExpr = parseExpr();
            SubExpr subExpr = new SubExpr(innerExpr);
            if (lexer.getCurToken().getType() != Token.Type.RPAREN) {
                throw new RuntimeException("Missing closing parenthesis for function call");
            }
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

    public NumFactor parseNumFactor() {
        Token token = lexer.getCurToken();
        lexer.nextToken(); // skip NUM
        NumFactor num = new NumFactor(new BigInteger(token.getContent()));
        return num;
    }

    public VarFactor parseVarFactor() {
        lexer.nextToken(); // skip VAR
        if (!lexer.isEnd()) {
            if (lexer.getCurToken().getType() == Token.Type.POW) {
                lexer.nextToken(); // skip '^'
                Token t = lexer.getCurToken();
                VarFactor var = new VarFactor(new BigInteger(t.getContent()));
                lexer.nextToken(); // skip NUM
                return var;
            } else {
                VarFactor var = new VarFactor(BigInteger.ONE);
                return var;
            }
        } else {
            VarFactor var = new VarFactor(BigInteger.ONE);
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
        Token token = lexer.getCurToken();
        int num = Integer.parseInt(token.getContent());
        lexer.nextToken(); // skip NUM
        lexer.nextToken(); // skip '}'
        lexer.nextToken(); // skip '('
        Factor firstParams = parseFactor();
        if (lexer.getCurToken().getType() != Token.Type.RPAREN) {
            throw new RuntimeException("Missing closing parenthesis for function call");
        }
        if (!lexer.isEnd() && lexer.getCurToken().getType() == Token.Type.COMA) {
            lexer.nextToken(); // skip ','
            Factor secondParams = parseFactor();
            if (lexer.getCurToken().getType() != Token.Type.RPAREN) {
                throw new RuntimeException("Missing closing parenthesis for function call");
            }
            lexer.nextToken(); // skip ')'
            return new FuncFactor(num, firstParams, secondParams);
        } else {
            Factor secondParams = new NumFactor(BigInteger.ZERO);
            if (lexer.getCurToken().getType() != Token.Type.RPAREN) {
                throw new RuntimeException("Missing closing parenthesis for function call");
            }
            lexer.nextToken(); // skip ')'
            return new FuncFactor(num, firstParams, secondParams);
        }
    }
}
