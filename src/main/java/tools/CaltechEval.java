package tools;

// Simple Java Math Expression Evaluator
// Usage example:
// java Evaluator "1+-min(-33,+4)*sin(0.5-0.1e-7) -atan2(3,4)  +1/(0.051e-5)"
// 1+-min(-33,+4)*sin(0.5-0.1e-7)-atan2(3,4)+1/(0.051e-5) = 1960801.7782690835
//
// (c) 2008 Julian Bunn, Caltech. Julian.Bunn@caltech.edu
// You are free to use this code however you see fit. Please let me know if
// you find any bugs, or would like to suggest enhancements

import java.util.Vector;

public class CaltechEval {

    final String expression;
    String[] Operators;
    int[] OperatorsArgs;
    boolean[] OperatorsLeftRight;
    int[] OperatorsPrecedence;
    int NOPERATORS;

    public CaltechEval(String s) {
        init();
        expression = s.replace(" ", "");
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.exit(0);
        }

        CaltechEval m = new CaltechEval(args[0]);
        System.out.println(args[0].replace(" ", "") + " = " + m.evaluate());
    }

    private void init() {
        NOPERATORS = 20;
        Operators = new String[NOPERATORS];
        OperatorsArgs = new int[NOPERATORS];
        OperatorsLeftRight = new boolean[NOPERATORS];
        OperatorsPrecedence = new int[NOPERATORS];

        Operators[0] = "+";
        OperatorsArgs[0] = 2;
        OperatorsLeftRight[0] = true;
        OperatorsPrecedence[0] = 2;

        Operators[1] = "-";
        OperatorsArgs[1] = 2;
        OperatorsLeftRight[1] = true;
        OperatorsPrecedence[1] = 2;

        Operators[2] = "*";
        OperatorsArgs[2] = 2;
        OperatorsLeftRight[2] = true;
        OperatorsPrecedence[2] = 3;

        Operators[3] = "/";
        OperatorsArgs[3] = 2;
        OperatorsLeftRight[3] = true;
        OperatorsPrecedence[3] = 3;

        Operators[4] = "sin";
        OperatorsArgs[4] = 1;
        OperatorsLeftRight[4] = false;
        OperatorsPrecedence[4] = 10;

        Operators[5] = "cos";
        OperatorsArgs[5] = 1;
        OperatorsLeftRight[5] = false;
        OperatorsPrecedence[5] = 10;

        Operators[6] = "tan";
        OperatorsArgs[6] = 1;
        OperatorsLeftRight[6] = false;
        OperatorsPrecedence[6] = 10;

        Operators[7] = "exp";
        OperatorsArgs[7] = 1;
        OperatorsLeftRight[7] = false;
        OperatorsPrecedence[7] = 10;

        Operators[8] = "sqrt";
        OperatorsArgs[8] = 1;
        OperatorsLeftRight[8] = false;
        OperatorsPrecedence[8] = 10;

        Operators[9] = "asin";
        OperatorsArgs[9] = 1;
        OperatorsLeftRight[9] = false;
        OperatorsPrecedence[9] = 10;

        Operators[10] = "acos";
        OperatorsArgs[10] = 1;
        OperatorsLeftRight[10] = false;
        OperatorsPrecedence[10] = 10;

        Operators[11] = "atan";
        OperatorsArgs[11] = 1;
        OperatorsLeftRight[11] = false;
        OperatorsPrecedence[11] = 10;

        Operators[12] = "atan2";
        OperatorsArgs[12] = 2;
        OperatorsLeftRight[12] = false;
        OperatorsPrecedence[12] = 10;

        Operators[13] = "max";
        OperatorsArgs[13] = 2;
        OperatorsLeftRight[13] = false;
        OperatorsPrecedence[13] = 10;

        Operators[14] = "min";
        OperatorsArgs[14] = 2;
        OperatorsLeftRight[14] = false;
        OperatorsPrecedence[14] = 10;

        Operators[15] = ",";
        OperatorsArgs[15] = 2;
        OperatorsLeftRight[15] = true;
        OperatorsPrecedence[15] = 1;

        Operators[16] = "u";
        OperatorsArgs[16] = 1;
        OperatorsLeftRight[16] = false;
        OperatorsPrecedence[16] = 4;

        Operators[17] = "d";
        OperatorsArgs[17] = 1;
        OperatorsLeftRight[17] = false;
        OperatorsPrecedence[17] = 4;

        Operators[18] = "n";
        OperatorsArgs[18] = 1;
        OperatorsLeftRight[18] = false;
        OperatorsPrecedence[18] = 11;

        Operators[19] = "log";
        OperatorsArgs[19] = 2;
        OperatorsLeftRight[19] = false;
        OperatorsPrecedence[19] = 11;
    }

    public double evaluate() {
        return eval(expression);
    }

    private int getOperator(String op) {
        for (int i = 0; i < NOPERATORS; i++) {
            if (op.equals(Operators[i]) && op.length() == Operators[i].length()) {
                return i;
            }
        }
        return -1;
    }

    private double doOp(String op, double arg1, double arg2) {
        if (op.equals("+")) {
            return (arg1 + arg2);
        }
        if (op.equals("-")) {
            return (arg1 - arg2);
        }
        if (op.equals("*")) {
            return (arg1 * arg2);
        }
        if (op.equals("/")) {
            return (arg1 / arg2);
        }
        if (op.equals("sin")) {
            return Math.sin(arg1);
        }
        if (op.equals("cos")) {
            return Math.cos(arg1);
        }
        if (op.equals("tan")) {
            return Math.tan(arg1);
        }
        if (op.equals("exp")) {
            return Math.exp(arg1);
        }
        if (op.equals("sqrt")) {
            return Math.sqrt(arg1);
        }
        if (op.equals("asin")) {
            return Math.asin(arg1);
        }
        if (op.equals("acos")) {
            return Math.acos(arg1);
        }
        if (op.equals("atan")) {
            return Math.atan(arg1);
        }
        if (op.equals("atan2")) {
            return Math.atan2(arg1, arg2);
        }
        if (op.equals("max")) {
            return (Math.max(arg1, arg2));
        }
        if (op.equals("min")) {
            return (Math.min(arg1, arg2));
        }
        if (op.equals("u")) {
            return (Math.ceil(arg1));
        }
        if (op.equals("d")) {
            return (Math.floor(arg1));
        }
        if (op.equals("n")) {
            return -arg1;
        }
        if (op.equals("log")) {
            return Math.log(arg2) / Math.log(arg1);
        }
        throw new RuntimeException("Invalid operator: " + op);
    }

    private boolean inOperand(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }

    private Vector<String> toTokens(String s) {
        Vector<String> tokenStrings = new Vector<>(1);
        String operand = "";
        String operator = "";
        int nd = 0;
        int nt = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            char cnext = ' ';
            // System.out.println("Looking at " + c);
            if (i < s.length() - 1) {
                cnext = s.charAt(i + 1);
            }

            if (nt > 0 && operator.equals("atan") && c == '2') {
                // System.out.println("    1");
                tokenStrings.addElement("atan2");
                operator = "";
                nt = 0;
                i++;
            } else if (inOperand(c)) {
                // System.out.println("    2");
                operand += s.charAt(i);
                nd++;
                if (nt > 0) {
                    tokenStrings.addElement(operator);
                }
                nt = 0;
                operator = "";
                i++;
            } else if (c == 'e' && nd > 0 && i < s.length() - 2
                    && ((cnext == '+' || cnext == '-') || inOperand(cnext))) {
                // System.out.println("    3");
                operand += c;
                operand += cnext;
                nd += 2;
                nt = 0;
                i += 2;
            } else if (c == '(' || c == ')') {
                // System.out.println("    4");
                if (nd > 0) {
                    tokenStrings.addElement(operand);
                }
                nd = 0;
                operand = "";
                if (nt > 0) {
                    tokenStrings.addElement(operator);
                }
                operator = "" + c;
                tokenStrings.addElement(operator);
                nt = 0;
                operator = "";
                i++;
            } else {
                // System.out.println("    5");
                if (nd > 0) {
                    tokenStrings.addElement(operand);
                }
                nd = 0;
                operand = "";
                operator += c;
                if (operator.equals("+") || operator.equals("-")
                        || operator.equals("*") || operator.equals("/")
                        || operator.equals(",")) {
                    tokenStrings.addElement(operator);
                    nt = 0;
                    operator = "";
                } else {
                    nt++;
                }
                i++;
            }
        }
        if (nd > 0) {
            tokenStrings.addElement(operand);
            nt = 0;
        }
        if (nt > 0) {
            tokenStrings.addElement(operator);
        }
        return tokenStrings;
    }

    private double eval(String s) {
//        Double D = new Double(0);
        // System.out.println("Evaluating " + s);

        Vector<String> tokens = toTokens(s);

        // System.out.print("Tokenized: ");
        // for (int it = 0; it < tokens.size(); it++)
        // {
        // System.out.print((String) tokens.elementAt(it) + " ");

        // }
        // System.out.println("");

        while (tokens.size() > 1) {
            tokens = reduceTokens(tokens);
        }

        return Double.parseDouble((String) tokens.elementAt(0));

    }

    public Vector<String> reduceTokens(Vector<String> tokens) {
//        Double D = new Double(0);
        double leftValue = 0, rightValue1, rightValue2;
        // System.out.print("Simplify Tokens :");
        // for (int i = 0; i < tokens.size(); i++)
        // System.out.print(" " + (String) tokens.elementAt(i));
        // System.out.println("");
        while (tokens.indexOf("(") != -1) {
            int ib = tokens.indexOf("(");
            Vector<String> bracketTokens = new Vector<>();
            int brackets = 1;
            for (int it = ib + 1; it < tokens.size(); it++) {
                String st = tokens.elementAt(it);
                if (st.equals(")")) {
                    brackets--;
                } else if (st.equals("(")) {
                    brackets++;
                }
                if (brackets == 0) {
                    // System.out.println("Start brackets: " + ib +
                    // " end brackets " + it);
                    for (int i3 = ib + 1; i3 < it; i3++) {
                        bracketTokens.addElement(tokens.elementAt(i3));
                    }
                    int startsize = bracketTokens.size();
                    // System.out.println("call reduceTokens with");
                    // for (int i = 0; i < bracketTokens.size(); i++)
                    // System.out.println(" " +
                    // (String)bracketTokens.elementAt(i));
                    bracketTokens = reduceTokens(bracketTokens);
                    int endsize = bracketTokens.size();
                    //System.out.println("After reduce tokens Diff "+(startsize-
                    // endsize));
                    // for (int i = 0; i < bracketTokens.size(); i++)
                    // System.out.println(" " +
                    // (String)bracketTokens.elementAt(i));
                    int ip = ib;
                    for (int i3 = 0; i3 < bracketTokens.size(); i3++) {
                        tokens.setElementAt(bracketTokens.elementAt(i3), ip++);
                    }
                    for (int i = 0; i < startsize - endsize + 2; i++) {
                        tokens.removeElementAt(ip);
                    }
                    // System.out.println("Replaced tokens ");
                    // for (int i = 0; i < tokens.size(); i++)
                    // System.out.println(" " + (String)tokens.elementAt(i));
                    break;
                }
            }
            // System.out.print("After bracket reduction: ");
            // for (int i = 0; i < tokens.size(); i++);
            // System.out.print(" " + (String) tokens.elementAt(i));
            // System.out.println("");
        }

        // treat the operators in order of precedence

        while (tokens.size() > 1) {

            // System.out.print("Iterating expression: ");
            // for (int i = 0; i < tokens.size(); i++)
            // System.out.print(" " + (String) tokens.elementAt(i));
            // System.out.println("");

            int maxprec = 0;
            int ipos = -1;
            for (int it = 0; it < tokens.size(); it++) {
                String st = (String) tokens.elementAt(it);
                int iop = getOperator(st);
                if (iop == -1) {
                    continue;
                }
                if (OperatorsPrecedence[iop] >= maxprec) {
                    maxprec = OperatorsPrecedence[iop];
                    ipos = it;
                }
            }

            if (ipos == -1) {
                return tokens; // for a simple list of operands
            }
            int it = ipos;

            String st = (String) tokens.elementAt(it);
            int iop = getOperator(st);
            // System.out.println("     Precedence Operator: "+it+" " + st);

            if (OperatorsLeftRight[iop]) {
                if (!st.equals(",")) {
                    int ipt = it - 1;
                    if (it > 0) {
                        String stleft = (String) tokens.elementAt(it - 1);
                        if (getOperator(stleft) != -1) {
                            ipt = it;
                            leftValue = 0;
                        } else {
                            leftValue = Double.parseDouble(stleft);
                        }
                    } else {
                        ipt = 0;
                        leftValue = 0.0;
                    }
                    String stright = (String) tokens.elementAt(it + 1);
                    rightValue1 = Double.parseDouble(stright);
                    // System.out.print("DoOp " + leftValue + st + rightValue1);
                    double value = doOp(st, leftValue, rightValue1);
                    stright = "" + value;
                    // System.out.println(" = " + stright);
                    tokens.setElementAt(stright, ipt);
                    // System.out.println("After Op:");
                    tokens.removeElementAt(ipt + 1);
                    if (it > 0 && ipt != it) {
                        tokens.removeElementAt(ipt + 1);
                    }
                    // for (int i = 0; i < tokens.size(); i++)
                    // System.out.println(" " + (String)tokens.elementAt(i));
                } else {
                    tokens.removeElementAt(it);
                }
            } else {
                int nargs = OperatorsArgs[iop];
                String stright = tokens.elementAt(it + 1);
                rightValue1 = Double.parseDouble(stright);
                rightValue2 = 0;
                if (nargs > 1) {
                    stright = tokens.elementAt(it + 2);
                    rightValue2 = Double.parseDouble(stright);
                }
                // System.out.print("DoOp " + st + " "+
                // rightValue1+" "+rightValue2);
                double value = doOp(st, rightValue1, rightValue2);
                stright = "" + value;
                // System.out.println(" = " + stright);
                tokens.setElementAt(stright, it);

                // System.out.println("After Op:");
                tokens.removeElementAt(it + 1);
                if (nargs > 1) {
                    tokens.removeElementAt(it + 1);
                }
                // for (int i = 0; i < tokens.size(); i++)
                // System.out.println(" " + (String)tokens.elementAt(i));
            }
        }

        // System.out.print("Return from reduceTokens with: ");
        // for (int i = 0; i < tokens.size(); i++) System.out.print(" " +
        // (String)tokens.elementAt(i));
        // System.out.println("");

        return tokens;
    }
}
