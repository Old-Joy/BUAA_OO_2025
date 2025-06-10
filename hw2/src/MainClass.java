import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String first = sc.nextLine();
        if (!first.equals("0")) {
            String funcFirst = preprocessSigns(sc.nextLine().replaceAll("\\s+", ""));
            String funcSecond = preprocessSigns(sc.nextLine().replaceAll("\\s+", ""));
            String funcThird = preprocessSigns(sc.nextLine().replaceAll("\\s+", ""));
            FuncCall.addDef(funcFirst, funcSecond, funcThird);
        }
        String input = sc.nextLine();
        input = input.replaceAll("[ \t]+", "");
        input = preprocessSigns(input);
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr expr = parser.parseExpr();
        Poly result = expr.toPoly();
        String output = result.toString();
        System.out.println(output);
    }

    private static String preprocessSigns(String input) {
        Pattern pattern = Pattern.compile("[+-]+");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String sequence = matcher.group();
            boolean negative = sequence.chars()
                .filter(c -> c == '-').count() % 2 != 0;
            matcher.appendReplacement(sb, negative ? "-" : "+");
        }
        matcher.appendTail(sb);
        String result = sb.toString();
        result = result.replaceAll("^\\+", "");
        result = result.replaceAll("^-", "0-");
        result = result.replaceAll("\\^\\+", "^");
        result = result.replaceAll("\\(\\+", "(");
        result = result.replaceAll("([*^/(])\\+", "$1");
        return result;
    }
}
