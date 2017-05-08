import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public abstract class Function extends LazyFunction {

    Function(String name) {
        super(name);
    }

    public LazyNumber lazyEval(List<LazyNumber> lazyParams) {
        final List<BigDecimal> params = new ArrayList<>();
        for (LazyNumber lazyParam : lazyParams) {
            params.add(lazyParam.eval());
        }
        return () -> Function.this.eval(params);
    }

    public abstract BigDecimal eval(List<BigDecimal> parameters);
}
