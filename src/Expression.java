import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class Expression {

    private static final BigDecimal PI = new BigDecimal("3.14159");
    private static final BigDecimal e = new BigDecimal("2.71");
    private MathContext mc = null;
    private String expression = null;
    private List<String> rpn = null;
    private Map<String, Operator> operators = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String,LazyFunction> functions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, BigDecimal> variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final char decimalSeparator = '.';
    private static final char minusSign = '-';

    private static final LazyNumber PARAMS_START = () -> null;

    Expression(String expression) {
        this(expression, MathContext.DECIMAL32);
    }

    private Expression(String expression, MathContext defaultMathContext) {
        this.mc = defaultMathContext;
        this.expression = expression;
        addOperator(new Operator("+", 20, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.add(v2, mc);
            }
        });
        addOperator(new Operator("-", 20, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.subtract(v2, mc);
            }
        });
        addOperator(new Operator("*", 30, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.multiply(v2, mc);
            }
        });
        addOperator(new Operator("/", 30, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.divide(v2, mc);
            }
        });
        addOperator(new Operator("%", 30, true) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.remainder(v2, mc);
            }
        });
        addOperator(new Operator("^", 40, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
				int signOf2 = v2.signum();
                double dn1 = v1.doubleValue();
                v2 = v2.multiply(new BigDecimal(signOf2)); // n2 is now positive
                BigDecimal remainderOf2 = v2.remainder(BigDecimal.ONE);
                BigDecimal n2IntPart = v2.subtract(remainderOf2);
                BigDecimal intPow = v1.pow(n2IntPart.intValueExact(), mc);
                BigDecimal doublePow = new BigDecimal(Math.pow(dn1,
                        remainderOf2.doubleValue()));

                BigDecimal result = intPow.multiply(doublePow, mc);
                if (signOf2 == -1) {
                    result = BigDecimal.ONE.divide(result, mc.getPrecision(),
                            RoundingMode.HALF_UP);
                }
                return result;
            }
        });
        addOperator(new Operator("&&", 4, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                boolean b1 = !v1.equals(BigDecimal.ZERO);
                boolean b2 = !v2.equals(BigDecimal.ZERO);
                return b1 && b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("||", 2, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                boolean b1 = !v1.equals(BigDecimal.ZERO);
                boolean b2 = !v2.equals(BigDecimal.ZERO);
                return b1 || b2 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator(">", 10, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.compareTo(v2) == 1 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator(">=", 10, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.compareTo(v2) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("<", 10, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.compareTo(v2) == -1 ? BigDecimal.ONE
                        : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("<=", 10, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.compareTo(v2) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addOperator(new Operator("=", 7, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.compareTo(v2) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });
        addOperator(new Operator("==", 7, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return operators.get("=").eval(v1, v2);
            }
        });

        addOperator(new Operator("!=", 7, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return v1.compareTo(v2) != 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });
        addOperator(new Operator("<>", 7, false) {
            @Override
            public BigDecimal eval(BigDecimal v1, BigDecimal v2) {
                return operators.get("!=").eval(v1, v2);
            }
        });

        addFunction(new Function("NOT", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                boolean zero = parameters.get(0).compareTo(BigDecimal.ZERO) == 0;
                return zero ? BigDecimal.ONE : BigDecimal.ZERO;
            }
        });

        addLazyFunction(new LazyFunction("IF", 3) {
            @Override
            public LazyNumber lazyEval(List<LazyNumber> lazyParams) {
                boolean isTrue = !lazyParams.get(0).eval().equals(BigDecimal.ZERO);
                return isTrue ? lazyParams.get(1) : lazyParams.get(2);
            }
        });

        addFunction(new Function("RANDOM", 0) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.random();
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SIN", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.sin(Math.toRadians(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COS", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.cos(Math.toRadians(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("TAN", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.tan(Math.toRadians(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ASIN", 1) { // added by av
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toDegrees(Math.asin(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ACOS", 1) { // added by av
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toDegrees(Math.acos(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ATAN", 1) { // added by av
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toDegrees(Math.atan(parameters.get(0)
                        .doubleValue()));
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("SINH", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.sinh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("COSH", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.cosh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("TANH", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.tanh(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("RAD", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toRadians(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("DEG", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.toDegrees(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("MAX", -1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                if (parameters.size() == 0) {
                    try {
                        throw new ExpressionException("MAX requires at least one parameter");
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                BigDecimal max = null;
                for (BigDecimal parameter : parameters) {
                    if (max == null || parameter.compareTo(max) > 0) {
                        max = parameter;
                    }
                }
                return max;
            }
        });
        addFunction(new Function("MIN", -1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                if (parameters.size() == 0) {
                    try {
                        throw new ExpressionException("MIN requires at least one parameter");
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                BigDecimal min = null;
                for (BigDecimal parameter : parameters) {
                    if (min == null || parameter.compareTo(min) < 0) {
                        min = parameter;
                    }
                }
                return min;
            }
        });
        addFunction(new Function("ABS", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                return parameters.get(0).abs(mc);
            }
        });
        addFunction(new Function("LOG", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.log(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("LOG10", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                double d = Math.log10(parameters.get(0).doubleValue());
                return new BigDecimal(d, mc);
            }
        });
        addFunction(new Function("ROUND", 2) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal toRound = parameters.get(0);
                int precision = parameters.get(1).intValue();
                return toRound.setScale(precision, mc.getRoundingMode());
            }
        });
        addFunction(new Function("FLOOR", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.FLOOR);
            }
        });
        addFunction(new Function("CEILING", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                BigDecimal toRound = parameters.get(0);
                return toRound.setScale(0, RoundingMode.CEILING);
            }
        });
        addFunction(new Function("SQRT", 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
				BigDecimal x = parameters.get(0);
                if (x.compareTo(BigDecimal.ZERO) == 0) {
                    return new BigDecimal(0);
                }
                if (x.signum() < 0) {
                    try {
                        throw new ExpressionException(
                                "Argument to SQRT() function must not be negative");
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                BigInteger n = x.movePointRight(mc.getPrecision() << 1)
                        .toBigInteger();

                int bits = (n.bitLength() + 1) >> 1;
                BigInteger ix = n.shiftRight(bits);
                BigInteger ixPrev;

                do {
                    ixPrev = ix;
                    ix = ix.add(n.divide(ix)).shiftRight(1);
                    // Give other threads a chance to work;
                    Thread.yield();
                } while (ix.compareTo(ixPrev) != 0);

                return new BigDecimal(ix, mc.getPrecision());
            }
        });

        variables.put("e", e);
        variables.put("PI", PI);
        variables.put("TRUE", BigDecimal.ONE);
        variables.put("FALSE", BigDecimal.ZERO);

    }

    private boolean isNumber(String st) {
        if (st.charAt(0) == minusSign && st.length() == 1) return false;
        if (st.charAt(0) == '+' && st.length() == 1) return false;
        if (st.charAt(0) == 'e' ||  st.charAt(0) == 'E') return false;
        for (char ch : st.toCharArray()) {
            if (!Character.isDigit(ch) && ch != minusSign
                    && ch != decimalSeparator
                    && ch != 'e' && ch != 'E' && ch != '+')
                return false;
        }
        return true;
    }

    private List<String> shuntingYard(String expression) {
        List<String> outputQueue = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        Tokenizer tokenizer = new Tokenizer(expression, operators);

        String lastFunction = null;
        String previousToken = null;
        while (tokenizer.hasNext()) {
            String token = tokenizer.next();
            if (isNumber(token)) {
                outputQueue.add(token);
            } else if (variables.containsKey(token)) {
                outputQueue.add(token);
            } else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
                stack.push(token);
                lastFunction = token;
            } else if (Character.isLetter(token.charAt(0))) {
                stack.push(token);
            } else if (",".equals(token)) {
                if (operators.containsKey(previousToken)) {
                    try {
                        throw new ExpressionException("Missing parameter(s) for operator " + previousToken +
                                " at character position " + (tokenizer.getPos() - 1 - previousToken.length()));
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                while (!stack.isEmpty() && !"(".equals(stack.peek())) {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    try {
                        throw new ExpressionException("Parse error for function '"
                                + lastFunction + "'");
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
            } else if (operators.containsKey(token)) {
                if (",".equals(previousToken) || "(".equals(previousToken)) {
                    try {
                        throw new ExpressionException("Missing parameter(s) for operator " + token +
                                " at character position " + (tokenizer.getPos() - token.length()));
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                Operator o1 = operators.get(token);
                String token2 = stack.isEmpty() ? null : stack.peek();
                while (token2!=null &&
                        operators.containsKey(token2)
                        && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators
                        .get(token2).getPrecedence()) || (o1
                        .getPrecedence() < operators.get(token2)
                        .getPrecedence()))) {
                    outputQueue.add(stack.pop());
                    token2 = stack.isEmpty() ? null : stack.peek();
                }
                stack.push(token);
            } else if ("(".equals(token)) {
                if (previousToken != null) {
                    if (isNumber(previousToken)) {
                        try {
                            throw new ExpressionException(
                                    "Missing operator at character position "
                                            + tokenizer.getPos());
                        } catch (ExpressionException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (functions.containsKey(previousToken.toUpperCase(Locale.ROOT))) {
                        outputQueue.add(token);
                    }
                }
                stack.push(token);
            } else if (")".equals(token)) {
                if (operators.containsKey(previousToken)) {
                    try {
                        throw new ExpressionException("Missing parameter(s) for operator " + previousToken +
                                " at character position " + (tokenizer.getPos() - 1 - previousToken.length()));
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                while (!stack.isEmpty() && !"(".equals(stack.peek())) {
                    outputQueue.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    try {
                        throw new ExpressionException("Mismatched parentheses");
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                stack.pop();
                if (!stack.isEmpty()
                        && functions.containsKey(stack.peek().toUpperCase(
                        Locale.ROOT))) {
                    outputQueue.add(stack.pop());
                }
            }
            previousToken = token;
        }
        while (!stack.isEmpty()) {
            String element = stack.pop();
            if ("(".equals(element) || ")".equals(element)) {
                try {
                    throw new ExpressionException("Mismatched parentheses");
                } catch (ExpressionException e1) {
                    e1.printStackTrace();
                }
            }
            if (!operators.containsKey(element)) {
                try {
                    throw new ExpressionException("Unknown operator or function: "
                            + element);
                } catch (ExpressionException e1) {
                    e1.printStackTrace();
                }
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    BigDecimal eval() {

        Stack<LazyNumber> stack = new Stack<>();

        for (final String token : getRPN()) {
            if (operators.containsKey(token)) {
                final LazyNumber v1 = stack.pop();
                final LazyNumber v2 = stack.pop();
                LazyNumber number = () -> operators.get(token).eval(v2.eval(), v1.eval());
                stack.push(number);
            } else if (variables.containsKey(token)) {
                stack.push(() -> variables.get(token).round(mc));
            } else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
                LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
                ArrayList<LazyNumber> p = new ArrayList<>(
                        !f.numParamsVaries() ? f.getNumParams() : 0);
                while (!stack.isEmpty() && stack.peek() != PARAMS_START) {
                    p.add(0, stack.pop());
                }
                if (stack.peek() == PARAMS_START) {
                    stack.pop();
                }
                LazyNumber fResult = f.lazyEval(p);
                stack.push(fResult);
            } else if ("(".equals(token)) {
                stack.push(PARAMS_START);
            } else {
                stack.push(() -> new BigDecimal(token, mc));
            }
        }
        return stack.pop().eval().stripTrailingZeros();
    }

    Expression setPrecision(int precision) {
        this.mc = new MathContext(precision);
        return this;
    }

    Expression setRoundingMode(RoundingMode roundingMode) {
        this.mc = new MathContext(mc.getPrecision(), roundingMode);
        return this;
    }

    private void addOperator(Operator operator) {
        operators.put(operator.getOper(), operator);
    }

    private void addFunction(Function function) {
        functions.put(function.getName(), function);
    }

    private void addLazyFunction(LazyFunction function) {
        functions.put(function.getName(), function);
    }

    private Expression setVariable(String variable, BigDecimal value) {
        variables.put(variable, value);
        return this;
    }

    private Expression setVariable(String variable, String value) {
        if (isNumber(value))
            variables.put(variable, new BigDecimal(value));
        else {
            expression = expression.replaceAll("(?i)\\b" + variable + "\\b", "("
                    + value + ")");
            rpn = null;
        }
        return this;
    }

    Expression with(String variable, BigDecimal value) {
        return setVariable(variable, value);
    }

    Expression and(String variable, String value) {
        return setVariable(variable, value);
    }

    Expression and(String variable, BigDecimal value) {
        return setVariable(variable, value);
    }

    Expression with(String variable, String value) {
        return setVariable(variable, value);
    }

    private List<String> getRPN() {
        if (rpn == null) {
            rpn = shuntingYard(this.expression);
            validate(rpn);
        }
        return rpn;
    }

    private void validate(List<String> rpn) {
		Stack<Integer> stack = new Stack<>();

        stack.push(0);

        for (final String token : rpn) {
            if (operators.containsKey(token)) {
                if (stack.peek() < 2) {
                    try {
                        throw new ExpressionException("Missing parameter(s) for operator " + token);
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                stack.set(stack.size() - 1, stack.peek() - 2 + 1);
            } else if (variables.containsKey(token)) {
                stack.set(stack.size() - 1, stack.peek() + 1);
            } else if (functions.containsKey(token.toUpperCase(Locale.ROOT))) {
                LazyFunction f = functions.get(token.toUpperCase(Locale.ROOT));
                int numParams = stack.pop();
                if (!f.numParamsVaries() && numParams != f.getNumParams()) {
                    try {
                        throw new ExpressionException("Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                if (stack.size() <= 0) {
                    try {
                        throw new ExpressionException("Too many function calls, maximum scope exceeded");
                    } catch (ExpressionException e1) {
                        e1.printStackTrace();
                    }
                }
                stack.set(stack.size() - 1, stack.peek() + 1);
            } else if ("(".equals(token)) {
                stack.push(0);
            } else {
                stack.set(stack.size() - 1, stack.peek() + 1);
            }
        }

        if (stack.size() > 1) {
            try {
                throw new ExpressionException("Too many unhandled function parameter lists");
            } catch (ExpressionException e1) {
                e1.printStackTrace();
            }
        } else if (stack.peek() > 1) {
            try {
                throw new ExpressionException("Too many numbers or variables");
            } catch (ExpressionException e1) {
                e1.printStackTrace();
            }
        } else if (stack.peek() < 1) {
            try {
                throw new ExpressionException("Empty expression");
            } catch (ExpressionException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expression that = (Expression) o;
        if (this.expression == null) {
            return that.expression == null;
        } else {
            return this.expression.equals(that.expression);
        }
    }

    @Override
    public int hashCode() {
        return this.expression == null ? 0 : this.expression.hashCode();
    }

    @Override
    public String toString() {
        return this.expression;
    }

}