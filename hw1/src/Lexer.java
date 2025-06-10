import java.util.ArrayList;

public class Lexer {
    private final ArrayList<Token> tokens = new ArrayList<>();
    private int index = 0;

    public Lexer(String input) {
        int pos = 0;
        while (pos < input.length()) {
            if (input.charAt(pos) == '(') {
                tokens.add(new Token(Token.Type.LRAREN, "("));
                pos++;
            } else if (input.charAt(pos) == ')') {
                tokens.add(new Token(Token.Type.RPAREN, ")"));
                pos++;
            } else if (input.charAt(pos) == '+') {
                tokens.add(new Token(Token.Type.ADD, "+"));
                pos++;
            } else if (input.charAt(pos) == '-') {
                if (pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1))
                    && (pos != 0 && input.charAt(pos - 1) == '*')) {
                    pos++;
                    StringBuilder sb = new StringBuilder();
                    sb.append('-');
                    char now = input.charAt(pos);
                    while (now >= '0' && now <= '9') {
                        sb.append(now);
                        pos++;
                        if (pos >= input.length()) {
                            break;
                        }
                        now = input.charAt(pos);
                    }
                    String numStr = processZero(sb.toString());
                    tokens.add(new Token(Token.Type.NUM, numStr));
                } else {
                    tokens.add(new Token(Token.Type.SUB, "-"));
                    pos++;
                }
            } else if (input.charAt(pos) == '*') {
                tokens.add(new Token(Token.Type.MUL, "*"));
                pos++;
            } else if (input.charAt(pos) == 'x') {
                tokens.add(new Token(Token.Type.VAR, "x"));
                pos++;
            } else if (input.charAt(pos) == '^') {
                tokens.add(new Token(Token.Type.POW, "^"));
                pos++;
            } else if (input.charAt(pos) == 's') {
                if (input.charAt(pos + 1) == 'i' && input.charAt(pos + 2) == 'n') {
                    tokens.add(new Token(Token.Type.SIN, "sin"));
                    pos += 3;
                }
            } else if (input.charAt(pos) == 'c') {
                if (input.charAt(pos + 1) == 'o' && input.charAt(pos + 2) == 's') {
                    tokens.add(new Token(Token.Type.COS, "cos"));
                    pos += 3;
                }
            } else if (input.charAt(pos) == 'f') {
                tokens.add(new Token(Token.Type.FUNC, "f"));
                pos++;
            } else if (input.charAt(pos) == ',') {
                tokens.add(new Token(Token.Type.COMA, ","));
                pos++;
            } else if (input.charAt(pos) == '{') {
                tokens.add(new Token(Token.Type.LBRACE, "{"));
                pos++;
            } else if (input.charAt(pos) == '}') {
                tokens.add(new Token(Token.Type.RBRACE, "}"));
                pos++;
            } else {
                char now = input.charAt(pos);
                StringBuilder sb = new StringBuilder();
                while (now >= '0' && now <= '9') {
                    sb.append(now);
                    pos++;
                    if (pos >= input.length()) {
                        break;
                    }
                    now = input.charAt(pos);
                }
                String numStr = sb.toString();
                numStr = processZero(numStr);
                tokens.add(new Token(Token.Type.NUM, numStr));
            }
        }
    }

    public Token getCurToken() {
        return tokens.get(index);
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
