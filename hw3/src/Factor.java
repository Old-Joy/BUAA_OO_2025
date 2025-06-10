public interface Factor {
    public Poly toPoly();

    public Factor clone();

    public Expr derive();
}
