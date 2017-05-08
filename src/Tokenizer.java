import java.util.Iterator;
import java.util.Map;

public class Tokenizer implements Iterator<String> {

    private int pos = 0;
    private String input;
    private String previousToken;
    private static final char decimalSeparator = '.';
    private static final char minusSign = '-';
    private Map operators;

    Tokenizer(String input, Map operators) {
        this.input = input.trim();
        this.operators = operators;
    }

    @Override
    public boolean hasNext() {
        return (pos < input.length());
    }

    private char peekNextChar() {
        if (pos < (input.length() - 1)) {
            return input.charAt(pos + 1);
        } else {
            return 0;
        }
    }

    @Override
    public String next() {
        StringBuilder token = new StringBuilder();
        if (pos >= input.length()) {
            return previousToken = null;
        }

        char ch = input.charAt(pos);
        while (Character.isWhitespace(ch) && pos < input.length())
            ch = input.charAt(++pos);

        String firstVarChars = "_";
        if (Character.isDigit(ch)) {
            while ((Character.isDigit(ch) || ch == decimalSeparator || ch == 'e' || ch == 'E' || (ch == minusSign && token.length() > 0
                    && ('e'==token.charAt(token.length()-1) || 'E'==token.charAt(token.length()-1)))
                    || (ch == '+' && token.length() > 0
                    && ('e'==token.charAt(token.length()-1) || 'E'==token.charAt(token.length()-1)))
            ) && (pos < input.length())) {
                token.append(input.charAt(pos++));
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
        }

        else if (ch == minusSign
                && Character.isDigit(peekNextChar())
                && ("(".equals(previousToken) || ",".equals(previousToken)
                || previousToken == null || operators
                .containsKey(previousToken))) {
            token.append(minusSign);
            pos++;
            token.append(next());

        }

        else if (Character.isLetter(ch) || firstVarChars.indexOf(ch) >= 0) {
            String varChars = "_";
            while ((Character.isLetter(ch) || Character.isDigit(ch)
                    || varChars.indexOf(ch) >= 0 || token.length() == 0 && firstVarChars.indexOf(ch) >= 0)
                    && (pos < input.length())) {
                token.append(input.charAt(pos++));
                ch = pos == input.length() ? 0 : input.charAt(pos);
            }
        }

        else if (ch == '(' || ch == ')' || ch == ',') {
            token.append(ch);
            pos++;
        }

        else {
            while (!Character.isLetter(ch) && !Character.isDigit(ch)
                    && firstVarChars.indexOf(ch) < 0 && !Character.isWhitespace(ch)
                    && ch != '(' && ch != ')' && ch != ','
                    && (pos < input.length())) {
                token.append(input.charAt(pos));
                pos++;
                ch = pos == input.length() ? 0 : input.charAt(pos);
                if (ch == minusSign) {
                    break;
                }
            }

            if (!operators.containsKey(token.toString())) {
                try {
                    throw new ExpressionException("Unknown operator '" + token
                            + "' at position " + (pos - token.length() + 1));
                } catch (ExpressionException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return previousToken = token.toString();
    }

    int getPos() {
        return pos;
    }

}
