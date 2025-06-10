import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

public class Poly {
    private final ArrayList<Unit> unitList;

    public Poly() {
        this.unitList = new ArrayList<>();
    }

    public void addUnit(Unit unit) {
        unitList.add(unit);
    }

    public Poly addPoly(Poly other) {
        HashMap<Unit, Unit> unitMap = new HashMap<>();
        for (Unit unit : this.unitList) {
            Unit copy = new Unit(unit.getCoe(), unit.getExp());
            // 修正：改为putAll方式复制三角函数映射
            unit.getSinMap().forEach(copy::addSin);  // 直接使用addSin累加指数
            unit.getCosMap().forEach(copy::addCos);
            unitMap.merge(copy, copy,
                (existing, newUnit) -> {
                    existing.setCoe(existing.getCoe().add(newUnit.getCoe()));
                    return existing;
                });
        }
        for (Unit unit : other.unitList) {
            Unit copy = new Unit(unit.getCoe(), unit.getExp());
            // 修正：改为putAll方式复制三角函数映射
            unit.getSinMap().forEach(copy::addSin);  // 直接使用addSin累加指数
            unit.getCosMap().forEach(copy::addCos);
            unitMap.merge(copy, copy,
                (existing, newUnit) -> {
                    existing.setCoe(existing.getCoe().add(newUnit.getCoe()));
                    return existing;
                });
        }
        Poly result = new Poly();
        for (Unit unit : unitMap.values()) {
            if (unit.getCoe().compareTo(BigInteger.ZERO) != 0) {
                result.unitList.add(unit);
            }
        }
        return result;
    }

    public Poly multiplyPoly(Poly other) {
        HashMap<Unit, Unit> unitMap = new HashMap<>();
        for (Unit u1 : this.unitList) {
            for (Unit u2 : other.unitList) {
                BigInteger newCoe = u1.getCoe().multiply(u2.getCoe());
                BigInteger newExp = u1.getExp().add(u2.getExp());

                HashMap<Poly, BigInteger> newSinMap = new HashMap<>();
                u1.getSinMap().forEach((k, v) -> newSinMap.merge(k, v, BigInteger::add));
                u2.getSinMap().forEach((k, v) -> newSinMap.merge(k, v, BigInteger::add));

                HashMap<Poly, BigInteger> newCosMap = new HashMap<>();
                u1.getCosMap().forEach((k, v) -> newCosMap.merge(k, v, BigInteger::add));
                u2.getCosMap().forEach((k, v) -> newCosMap.merge(k, v, BigInteger::add));

                Unit productUnit = new Unit(newCoe, newExp);
                newSinMap.forEach((poly, exp) -> productUnit.addSin(poly, exp));
                newCosMap.forEach((poly, exp) -> productUnit.addCos(poly, exp));

                unitMap.merge(productUnit, productUnit,
                    (existing, newUnit) -> {
                        existing.setCoe(existing.getCoe().add(newUnit.getCoe()));
                        return existing;
                    });
            }
        }
        Poly result = new Poly();
        for (Unit unit : unitMap.values()) {
            if (unit.getCoe().compareTo(BigInteger.ZERO) != 0) {
                result.unitList.add(unit);
            }
        }
        return result;
    }

    public Poly powerPoly(int power) {
        if (power == 0) {
            Poly result = new Poly();
            result.unitList.add(new Unit(BigInteger.ONE, BigInteger.ZERO));
            return result;
        }
        if (power == 1) {
            return this;
        }

        // 优化单一项的幂运算
        if (unitList.size() == 1) {
            Unit original = unitList.get(0);
            BigInteger newCoe = original.getCoe().pow(power);
            BigInteger newExp = original.getExp().multiply(BigInteger.valueOf(power));
            Unit poweredUnit = new Unit(newCoe, newExp);
            // 直接处理三角函数指数
            original.getSinMap().forEach((poly, exp) ->
                poweredUnit.addSin(poly, exp.multiply(BigInteger.valueOf(power))));
            original.getCosMap().forEach((poly, exp) ->
                poweredUnit.addCos(poly, exp.multiply(BigInteger.valueOf(power))));

            Poly result = new Poly();
            result.addUnit(poweredUnit);
            return result;
        }

        // 通用快速幂算法（处理多项式展开）
        Poly result = new Poly();
        result.unitList.add(new Unit(BigInteger.ONE, BigInteger.ZERO));
        Poly base = this;
        int exponent = power;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result = result.multiplyPoly(base);
            }
            base = base.multiplyPoly(base);
            exponent >>= 1;
        }
        return result;
    }

    public void negate() {
        for (Unit unit : unitList) {
            unit.setCoe(unit.getCoe().negate());
        }
    }

    public boolean isZero() {
        if (unitList.isEmpty()) {
            return true;
        }
        for (Unit unit : unitList) {
            if (unit.getCoe().compareTo(BigInteger.ZERO) != 0) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        if (unitList.isEmpty()) {
            return "0";
        }
        unitList.sort((u1, u2) -> {
            boolean u1Positive = u1.getCoe().compareTo(BigInteger.ZERO) > 0;
            boolean u2Positive = u2.getCoe().compareTo(BigInteger.ZERO) > 0;
            if (u1Positive != u2Positive) {
                return u1Positive ? -1 : 1;  // 正项排在负项前面
            }
            return u2.getExp().compareTo(u1.getExp()); // 同符号保持指数降序
        });
        StringBuilder sb = new StringBuilder();
        for (Unit unit : unitList) {
            String term = unit.toString();
            if (term.equals("0")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(term.startsWith("-") ? "" : "+").append(term);
            } else {
                sb.append(term);
            }
        }
        String result = sb.toString();
        result = result.replaceAll("\\^1(?!\\d)", "")
                .replaceAll("(?<!\\^)(\\d+)\\*x\\^0\\*", "$1*")
                .replaceAll("(?<!\\^)(\\d+)\\*x\\^0", "$1")
                .replaceAll("([+-])x\\^0", "$11")
                .replaceAll("(?<![0-9])1\\*", "")
                .replaceAll("-1\\*", "-");
        if (result.isEmpty()) {
            return "0";
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        Poly other = (Poly) o;
        String thisStr = this.toString();
        String otherStr = other.toString();
        return thisStr.equals(otherStr);
    }

    @Override
    public int hashCode() {
        unitList.sort(Comparator.comparing(Unit::hashCode));
        return Objects.hash(unitList);
    }

}
