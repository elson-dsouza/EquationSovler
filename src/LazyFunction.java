import java.util.List;
import java.util.Locale;

public abstract class LazyFunction {
    private String name;
    private int numParams;

    public LazyFunction(String name, int numParams) {
        this.name = name.toUpperCase(Locale.ROOT);
        this.numParams = numParams;
    }

    public String getName() {
        return name;
    }

    public int getNumParams() {
        return numParams;
    }

    public boolean numParamsVaries() {
        return numParams < 0;
    }
    public abstract LazyNumber lazyEval(List<LazyNumber> lazyParams);
}
