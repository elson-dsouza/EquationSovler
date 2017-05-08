import java.math.BigDecimal;

public abstract class Operator {
    private String oper;
    private int precedence;
    private boolean leftAssoc;

    public Operator(String oper, int precedence, boolean leftAssoc) {
        this.oper = oper;
        this.precedence = precedence;
        this.leftAssoc = leftAssoc;
    }

    public String getOper() {
        return oper;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean isLeftAssoc() {
        return leftAssoc;
    }

    public abstract BigDecimal eval(BigDecimal v1, BigDecimal v2);
}
