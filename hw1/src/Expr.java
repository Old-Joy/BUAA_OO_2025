import java.util.ArrayList;

public class Expr implements Factor {
    private final ArrayList<Term> terms = new ArrayList<>();

    public void addTerm(Term term) {
        terms.add(term);
    }

    public Poly toPoly() {
        Poly poly = new Poly();
        for (Term it : terms) {
            Poly temp = poly.addPoly(it.toPoly()); // 累加所有项
            poly = temp;
        }
        return poly;
    }
}
