import java.util.ArrayList;

public class Lexer {
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int index = 0;

    public Lexer(String input) {
        int pos = 0;
        while (pos < input.length()) {
            char current = input.charAt(pos);
            switch (current) {
                case '(':
                    addToken(Token.Type.LRAREN, "(", pos);
                    break;
                case ')':
                    addToken(Token.Type.RPAREN, ")", pos);
                    break;
                case '+':
                    addToken(Token.Type.ADD, "+", pos);
                    break;
                case '*':
                    addToken(Token.Type.MUL, "*", pos);
                    break;
                case '^':
                    addToken(Token.Type.POW, "^", pos);
                    break;
                case ',':
                    addToken(Token.Type.COMA, ",", pos);
                    break;
                case '{':
                    addToken(Token.Type.LBRACE, "{", pos);
                    break;
                case '}':
                    addToken(Token.Type.RBRACE, "}", pos);
                    break;
                case '=':
                    addToken(Token.Type.EQUAL, "=", pos);
                    break;
                case 'x':
                    addToken(Token.Type.X, "x", pos);
                    break;
                case 'y':
                    addToken(Token.Type.Y, "y", pos);
                    break;
                case 'n':
                    addToken(Token.Type.N, "n", pos);
                    break;
                case 's':
                    pos = processSin(input, pos);
                    break;
                case 'c':
                    pos = processCos(input, pos);
                    break;
                case 'f':
                    pos = processFunction(input, pos);
                    break;
                case '-':
                    pos = processMinus(input, pos);
                    break;
                default:
                    pos = processNumber(input, pos);
                    break;
            }
            pos++;
        }
    }

    // 新增的辅助方法
    private void addToken(Token.Type type, String value, int pos) {
        tokens.add(new Token(type, value));
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

    private int processSin(String input, int firstPos) {
        int pos = firstPos;
        tokens.add(new Token(Token.Type.SIN, "sin"));
        return pos + 2; // 跳过后续字符
    }

    private int processCos(String input, int firstPos) {
        int pos = firstPos;
        tokens.add(new Token(Token.Type.COS, "cos"));
        return pos + 2;
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
