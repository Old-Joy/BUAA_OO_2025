import java.util.ArrayList;
import java.util.HashMap;

public class Lexer {
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int index = 0;

    private static final HashMap<Character, Token.Type> SYMBOL_MAP =
        new HashMap<Character, Token.Type>() {{
                put('(', Token.Type.LRAREN);
                put(')', Token.Type.RPAREN);
                put('+', Token.Type.ADD);
                put('*', Token.Type.MUL);
                put('^', Token.Type.POW);
                put(',', Token.Type.COMA);
                put('{', Token.Type.LBRACE);
                put('}', Token.Type.RBRACE);
                put('=', Token.Type.EQUAL);
                put('x', Token.Type.X);
                put('y', Token.Type.Y);
                put('n', Token.Type.N);
            }};

    public Lexer(String input) {
        int pos = 0;
        while (pos < input.length()) {
            char current = input.charAt(pos); // 使用映射表处理单符号token
            if (SYMBOL_MAP.containsKey(current)) {
                addToken(SYMBOL_MAP.get(current), String.valueOf(current), pos);
                pos++;
                continue;
            }
            switch (current) {
                case 's':
                case 'c':
                    pos = processTrigFunc(input, pos,
                            current == 's' ? Token.Type.SIN : Token.Type.COS);
                    break;
                case 'f':
                    addToken(Token.Type.FUNC, "f", pos);
                    break;
                case '-':
                    pos = processMinus(input, pos);
                    break;
                case 'g':
                case 'h':
                    addToken(Token.Type.SIMPLEFUNC, String.valueOf(current), pos);
                    break;
                case 'd':
                    addToken(Token.Type.DIRIVE, "dx", pos);
                    pos++; // 跳过x
                    break;
                default:
                    pos = processNumber(input, pos);
            }
            pos++;
        }
    }

    private int processTrigFunc(String input, int pos, Token.Type type) {
        addToken(type, type == Token.Type.SIN ? "sin" : "cos", pos);
        return pos + 2; // 统一跳过后续字符
    }

    // 新增的辅助方法
    private void addToken(Token.Type type, String value, int pos) {
        tokens.add(new Token(type, value));
    }

    private int processDerive(String input, int firstPos) {
        int pos = firstPos;
        tokens.add(new Token(Token.Type.DIRIVE, "dx"));
        pos++;
        return pos;
    }

    private int processSimpleFunc(String input, int firstPos) {
        int pos = firstPos;
        if (input.charAt(pos) == 'h') {
            tokens.add(new Token(Token.Type.SIMPLEFUNC, "h"));
            return pos;
        } else {
            tokens.add(new Token(Token.Type.SIMPLEFUNC, "g"));
            return pos;
        }
    }

    private int processNumber(String input, int firstPos) {
        int pos = firstPos;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            sb.append(input.charAt(pos++));
        }
        tokens.add(new Token(Token.Type.NUM, processZero(sb.toString())));
        return pos - 1; // 抵消外层pos++
    }

    private int processFunction(String input, int firstPos) {
        int pos = firstPos;
        tokens.add(new Token(Token.Type.FUNC, "f"));
        return pos; // 不需要跳过字符
    }

    private int processMinus(String input, int firstPos) {
        int pos = firstPos;
        if (isNegativeNumber(input, pos)) {
            pos = processNegativeNumber(input, pos);
        } else {
            addToken(Token.Type.SUB, "-", pos);
        }
        return pos;
    }

    private boolean isNegativeNumber(String input, int pos) {
        return (pos + 1 < input.length())
                && Character.isDigit(input.charAt(pos + 1))
                && (pos > 0 && input.charAt(pos - 1) == '*');
    }

    private int processNegativeNumber(String input, int firstPos) {
        int pos = firstPos;
        pos++; // 跳过减号
        StringBuilder sb = new StringBuilder("-");
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            sb.append(input.charAt(pos++));
        }
        tokens.add(new Token(Token.Type.NUM, processZero(sb.toString())));
        return pos - 1; // 抵消外层pos++
    }

    public Token getCurToken() {
        return tokens.get(index);
    }

    public Token getNextNumToken(int num) {
        return tokens.get(index + num);
    }

    public void nextToken() {
        index++;
    }

    public boolean isEnd() {
        return index >= tokens.size();
    }

    private String processZero(String numStr) {
        int start = 0;
        boolean negative = numStr.startsWith("-");
        if (negative) {
            start = 1;
        }
        while (start < numStr.length() - 1 && numStr.charAt(start) == '0') {
            start++;
        }
        return negative ? "-" + numStr.substring(start) : numStr.substring(start);
    }
}
