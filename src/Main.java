import java.math.BigDecimal;
import java.math.RoundingMode;

public class Main {
    public static void main(String [] args){
        Expression expression = new Expression("1+(1/3)");
        BigDecimal result;
        expression.setPrecision(2);
        result = expression.eval();
        System.out.println(result);
        result = new Expression("(3.4 + -4.1)/2").eval();
        System.out.println(result);

        BigDecimal a = new BigDecimal("2.4");
        BigDecimal b = new BigDecimal("9.235");
        result = new Expression("SQRT(a^2 + b^2)").with("a",a).and("b",b).eval();
        System.out.println(result);

        result = new Expression("2.4/PI").setPrecision(128).setRoundingMode(RoundingMode.UP).eval();
        System.out.println(result);

    }
}
