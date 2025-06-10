import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuncFactor implements Factor {
    private int level;
    private Factor firstParams;
    private Factor secondParams;
    private static String funcFirst;
    private static String funcSecond;
    private static String funcThird;
    private static final HashMap<Integer, String> precomputed = new HashMap<>();

    public static void addDef(String funcFirst, String funcSecond, String funcThird) {
        for (String def : new String[]{funcFirst, funcSecond, funcThird}) {
            if (def.contains("f{0}")) {
                FuncFactor.funcFirst = def;  // f{0}定义
            } else if (def.contains("f{1}")) {
                FuncFactor.funcSecond = def; // f{1}定义
            } else {
                FuncFactor.funcThird = def;  // 递推定义
            }
        }
        FuncFactor.precomputed.put(0, FuncFactor.funcFirst.split("=")[1].trim());
        FuncFactor.precomputed.put(1, FuncFactor.funcSecond.split("=")[1].trim());

        // 预处理2-5级定义
        for (int level = 2; level <= 5; level++) {
            String expr = funcThird.split("=")[1].trim()
                .replaceAll("f\\{n\\}", "f{" + (level - 1) + "}")
                .replaceAll("n-1", String.valueOf(level - 1))
                .replaceAll("n-2", String.valueOf(level - 2));

            // 新增递归展开逻辑
            Pattern pattern = Pattern.compile("f\\{(\\d+)}\\([^)]*\\)");
            Matcher matcher;
            do {
                matcher = pattern.matcher(expr);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    int k = Integer.parseInt(matcher.group(1));
                    String replacement = precomputed.containsKey(k) ?
                            "(" + precomputed.get(k) + ")" : matcher.group();
                    matcher.appendReplacement(sb, replacement);
                }
                matcher.appendTail(sb);
                expr = sb.toString();
            } while (matcher.reset(expr).find()); // 循环直到没有f{k}表达式

            precomputed.put(level, expr);
        }
    }

    public FuncFactor(int level, Factor firstParams, Factor secondParams) {
        this.level = level;
        this.firstParams = firstParams;
        this.secondParams = secondParams;
    }

    public Poly toPoly() {
        // 直接获取预计算表达式
        String expr = precomputed.get(level);
        // 替换实际参数
        String substituted = expr.replace("x", "(" + firstParams.toPoly().toString() + ")")
                .replace("y", "(" + "(" + secondParams.toPoly().toString() + ")" +")");
        // 解析并转换
        return new Parser(new Lexer(substituted)).parseExpr().toPoly();
    }
}
