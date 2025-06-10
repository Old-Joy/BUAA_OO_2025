import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Unit {
    private BigInteger coe;
    private BigInteger exp;
    private HashMap<Poly, BigInteger> sinMap;
    private HashMap<Poly, BigInteger> cosMap;

    public Unit(BigInteger coe, BigInteger exp) {
        this.coe = coe;
        this.exp = exp;
        this.sinMap = new HashMap<>();
        this.cosMap = new HashMap<>();
    }

    public void addSin(Poly sinPoly, BigInteger exp) {
        if (sinMap.containsKey(sinPoly)) {
            sinMap.put(sinPoly, sinMap.get(sinPoly).add(exp));
        } else {
            sinMap.put(sinPoly, exp);
        }
    }

    public void addCos(Poly cosPoly, BigInteger exp) {
        if (cosMap.containsKey(cosPoly)) {
            cosMap.put(cosPoly, cosMap.get(cosPoly).add(exp));
        } else {
            cosMap.put(cosPoly, exp);
        }
    }

    public String toString() {
        if (coe.equals(BigInteger.ZERO)) {
            return "0";
        } else {
            for (Map.Entry<Poly, BigInteger> entry : sinMap.entrySet()) {
                if (entry.getKey().isZero() && entry.getValue().compareTo(BigInteger.ZERO) > 0) {
                    return "0";
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append(coe);
            sb.append("*x^");
            sb.append(exp);
            for (Map.Entry<Poly, BigInteger> entry : sinMap.entrySet()) {
                if (entry.getValue().equals(BigInteger.ZERO)) {
                    continue;
                }
                String polyStr = entry.getKey().toString();
                sb.append("*");  // 添加明确的乘号分隔符
                sb.append("sin(").append(wrapTrigParam(polyStr)).append(")^")
                    .append(entry.getValue());
            }
            for (Map.Entry<Poly, BigInteger> entry : cosMap.entrySet()) {
                if (entry.getValue().equals(BigInteger.ZERO)) {
                    continue;
                }
                if (entry.getKey().isZero()) {
                    sb.append("*");  // 添加明确的乘号分隔符
                    sb.append("1");
                    continue;
                }
                String polyStr = entry.getKey().toString();
                sb.append("*");  // 添加明确的乘号分隔符
                sb.append("cos(").append(wrapTrigParam(polyStr)).append(")^")
                    .append(entry.getValue());
            }
            return sb.toString();
        }
    }

    private String wrapTrigParam(String param) {
        // 增强判断逻辑：只要包含运算符就添加括号
        return param.matches(".*[+\\-*].*") ? "(" + param + ")" : param;
    }

    @Override
    public boolean equals(Object o) {  // 修改为正确的equals重写
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Unit unit = (Unit) o;
        return exp.equals(unit.exp) &&
                compareMaps(sinMap, unit.sinMap) &&
                compareMaps(cosMap, unit.cosMap);
    }

    private boolean compareMaps(
        HashMap<Poly, BigInteger> map1,
        HashMap<Poly, BigInteger> map2) {
        if (map1.size() != map2.size()) {
            return false;
        }
        for (Map.Entry<Poly, BigInteger> entry : map1.entrySet()) {
            Poly key = entry.getKey();
            if (!map2.containsKey(key)) {
                return false;
            }
            if (!entry.getValue().equals(map2.get(key))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        // 哈希计算不包含coe
        return Objects.hash(exp, sinMap, cosMap);
    }

    public HashMap<Poly, BigInteger> getSinMap() {
        return new HashMap<>(sinMap);
    }

    public HashMap<Poly, BigInteger> getCosMap() {
        return new HashMap<>(cosMap);
    }

    public BigInteger getCoe() {
        return this.coe;
    }

    public BigInteger getExp() {
        return this.exp;
    }

    public void setCoe(BigInteger newCoe) {
        this.coe = newCoe;
    }
}
