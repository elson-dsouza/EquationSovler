import java.math.BigDecimal;

public abstract class Operator {
    private String oper;
    private int precedence;
    private boolean leftAssoc;

    Operator(String oper, int precedence, boolean leftAssoc) {
        this.oper = oper;
        this.precedence = precedence;
        this.leftAssoc = leftAssoc;
    }

    String getOper() {
        return oper;
    }

    int getPrecedence() {
        return precedence;
    }

    boolean isLeftAssoc() {
        return leftAssoc;
    }

    public abstract BigDecimal eval(BigDecimal v1, BigDecimal v2);
}
