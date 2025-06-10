public class Token {
    public enum Type {
        ADD, SUB, MUL, LRAREN, RPAREN, NUM, X, POW,
        SIN, COS, FUNC, COMA, LBRACE, RBRACE, Y, N, EQUAL,
        DIRIVE, SIMPLEFUNC
    }

    private final Type type;
    private final String content;

    public Token(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
