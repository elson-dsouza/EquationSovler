import java.util.List;
import java.util.Locale;

public abstract class LazyFunction {
    private String name;

    LazyFunction(String name) {
        this.name = name.toUpperCase(Locale.ROOT);
    }

    String getName() {
        return name;
    }

    public abstract LazyNumber lazyEval(List<LazyNumber> lazyParams);
}
