/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Ubiqueworks Ltd and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.neotree.player.expression;

import android.util.Log;

import org.neotree.player.ScriptPlayer;
import org.neotree.grammar.BooleanExpressionBaseListener;
import org.neotree.grammar.BooleanExpressionParser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Created by matteo on 15/07/2016.
 */
public class BooleanExpressionEvaluator extends BooleanExpressionBaseListener {

    private static final String TAG = BooleanExpressionEvaluator.class.getSimpleName();

    private ScriptPlayer mScriptPlayer;
    private Deque<LogicOp> mOpStack = new ArrayDeque<>();
    private Deque<Boolean> mValStack = new ArrayDeque<>();
    private boolean mEvaluationResult;

    private enum LogicOp {
        AND, OR
    }

    private enum Operation {
        NOP, EQ, NEQ, GT, GTE, LT, LTE
    }

    public BooleanExpressionEvaluator(ScriptPlayer scriptPlayer) {
        mScriptPlayer = scriptPlayer;
    }

    public boolean getEvaluationResult() {
        return mEvaluationResult;
    }

    @Override
    public void exitRoot(BooleanExpressionParser.RootContext ctx) {
        super.exitRoot(ctx);
        Log.d(TAG, "Expression evaluation complete");
        
        // End of evaluation, set the result
        mEvaluationResult = mValStack.pop();
    }

    @Override
    public void enterExpression(BooleanExpressionParser.ExpressionContext ctx) {
        if (ctx.binaryAnd() != null) {
            mOpStack.push(LogicOp.AND);
        } else if (ctx.binaryOr() != null) {
            mOpStack.push(LogicOp.OR);
        }
    }

    @Override
    public void exitExpression(BooleanExpressionParser.ExpressionContext ctx) {
        if (ctx.binaryAnd() != null || ctx.binaryOr() != null) {
            if (mOpStack.size() > 0 && mValStack.size() >= 2) {
                boolean v1 = mValStack.pop();
                boolean v2 = mValStack.pop();
                LogicOp op = mOpStack.pop();

                switch (op) {
                    case AND:
                        mValStack.push(v1 & v2);
                        break;
                    case OR:
                        mValStack.push(v1 | v2);
                        break;
                }
            }
        }
    }

    @Override
    public void enterAssertion(BooleanExpressionParser.AssertionContext ctx) {
        if (ctx.exception != null) {
            System.out.println(ctx.exception.getMessage());
            return;
        }

        final String key = ctx.id.getText().replaceAll("\\$", "");
        final Operation op = fromOpString(ctx.op.getText());

        Boolean result = null;
        if (ctx.booleanLiteral() != null) {
            boolean value = Boolean.valueOf(ctx.valueBool.getText());
            result = evaluateBooleanAssertion(key, op, value);
            Log.d(TAG, String.format("Boolean expr: %s %s %b -> %b", key, op, value, result));
        } else if (ctx.numberLiteral() != null) {
            Double value = Double.valueOf(ctx.valueNumber.getText());
            result = evaluateNumberAssertion(key, op, value);
            Log.d(TAG, String.format("Number expr: %s %s %.2f -> %b", key, op, value, result));
        } else if (ctx.stringLiteral() != null) {
            String value = ctx.valueString.getText();
            if (value == null || "nil".equals(value) || "null".equals(value)) {
                value = null;
            } else {
                value = value.replaceAll("\"", "").replaceAll("'", "");
            }
            result = evaluateStringAssertion(key, op, value);
            Log.d(TAG, String.format("String expr: %s %s %s -> %b", key, op, value, result));
        } else if (ctx.identifier() != null) {
            // TODO: Add identifier assertion
        }
        mValStack.push(result);
    }

    private boolean evaluateBooleanAssertion(String key, Operation operation, boolean expValue) {
        Boolean ctxValue = mScriptPlayer.getValue(key, Boolean.FALSE);
        switch (operation) {
            case EQ:
                return ctxValue == expValue;
            case NEQ:
                return ctxValue != expValue;
            default:
                return false;
        }
    }

    private boolean evaluateStringAssertion(String key, Operation operation, String expValue) {
        Object ctxValue = mScriptPlayer.getValue(key);
        boolean ctxValueNull = (ctxValue == null);
        boolean expValueNull = (expValue == null);
        switch (operation) {
            case EQ:
                // TODO: Add handling for Sets
                if (ctxValueNull && expValueNull) {
                    return true;
                } else if (ctxValueNull != expValueNull) {
                    return false;
                }

                if (ctxValue instanceof Set) {
                    return ((Set) ctxValue).contains(expValue);
                } else {
                    return ctxValue.equals(expValue);
                }
            case NEQ:
                // TODO: Add handling for Sets
                if (ctxValueNull && expValueNull) {
                    return false;
                } else if (ctxValueNull != expValueNull) {
                    return true;
                }

                if (ctxValue instanceof Set) {
                    return !((Set) ctxValue).contains(expValue);
                } else {
                    return !ctxValue.equals(expValue);
                }
            default:
                return false;
        }
    }

    private boolean evaluateNumberAssertion(String key, Operation operation, Double expValue) {
        Double ctxValue = mScriptPlayer.getValue(key);
        boolean ctxValueNull = (ctxValue == null);
        if (ctxValueNull) {
            // TODO: Assume 0 by default?
            return false;
        }

        int result = ctxValue.compareTo(expValue);
        switch (operation) {
            case EQ:
                return (result == 0);
            case NEQ:
                return (result != 0);
            case LT:
                return (result < 0);
            case LTE:
                return (result <= 0);
            case GT:
                return (result > 0);
            case GTE:
                return (result >= 0);
            default:
                return false;
        }
    }

    private Operation fromOpString(String op) {
        if ("=".equals(op)) {
            return Operation.EQ;
        } else if ("!=".equals(op)) {
            return Operation.NEQ;
        } else if ("<".equals(op)) {
            return Operation.LT;
        } else if ("<=".equals(op)) {
            return Operation.LTE;
        } else if (">".equals(op)) {
            return Operation.GT;
        } else if (">=".equals(op)) {
            return Operation.GTE;
        }
        return Operation.NOP;
    }

}
