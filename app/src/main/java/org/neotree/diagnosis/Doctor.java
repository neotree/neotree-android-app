package org.neotree.diagnosis;

import android.text.TextUtils;
import android.util.Log;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.neotree.diagnosis.type.SymptomType;
import org.neotree.model.firebase.Diagnosis;
import org.neotree.model.firebase.Symptom;
import org.neotree.grammar.BooleanExpressionLexer;
import org.neotree.grammar.BooleanExpressionParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by matteo on 21/01/2017.
 */

public class Doctor {

    private static final String TAG = Doctor.class.getSimpleName();

    private static final String RISK_COUNT = "riskCount";
    private static final String SIGN_COUNT = "signCount";

    public static DiagnosisResult diagnose(List<Diagnosis> rules, Map<String, Object> data) throws DiagnosisException {
        if (rules == null || rules.size() == 0) {
            return null;
        }

        if (data == null || data.size() == 0) {
            return null;
        }

        // Clean before starting
        data.remove(RISK_COUNT);
        data.remove(SIGN_COUNT);

        final DiagnosisResult result = new DiagnosisResult();
        for (Diagnosis rule : rules) {
            if (rule.symptoms != null && rule.symptoms.size() > 0) {
                double riskCount = 0;
                double signCount = 0;

                for (int i = 0; i < rule.symptoms.size(); i++) {
                    final Symptom symptom = rule.symptoms.get(i);
                    final SymptomType type = SymptomType.fromString(symptom.type);

                    if (!TextUtils.isEmpty(symptom.expression)) {
                        boolean isMatch = evaluateExpression(rule.name, symptom.name, symptom.expression, data);
                        if (isMatch) {
                            switch (type) {
                                case RISK:
                                    riskCount += ((TextUtils.isEmpty(symptom.weight) ? 1.0 : Double.parseDouble(symptom.weight)));
                                    break;
                                case SIGN:
                                    signCount += ((TextUtils.isEmpty(symptom.weight) ? 1.0 : Double.parseDouble(symptom.weight)));
                                    break;
                            }
                        }
                    }
                }
                data.put(RISK_COUNT, riskCount);
                data.put(SIGN_COUNT, signCount);
            }

            if (!TextUtils.isEmpty(rule.expression)) {
                boolean isMatch = evaluateExpression(rule.name, null, rule.expression, data);
                if (isMatch) {
                    ArrayList<DiagnosisManagement> managements = new ArrayList<>();

                    if (!TextUtils.isEmpty(rule.text1)) {
                        managements.add(new DiagnosisManagement(rule.text1, rule.image1));
                    }

                    if (!TextUtils.isEmpty(rule.text2)) {
                        managements.add(new DiagnosisManagement(rule.text2, rule.image2));
                    }

                    if (!TextUtils.isEmpty(rule.text3)) {
                        managements.add(new DiagnosisManagement(rule.text3, rule.image3));
                    }

                    result.addDiagnosis(rule.name, managements);
                }
            }

            // Clean after starting
            data.remove(RISK_COUNT);
            data.remove(SIGN_COUNT);
        }
        return result;
    }

    private static boolean evaluateExpression(String diagnosisName, String signName, String expression, Map<String, Object> data) throws DiagnosisException {
//        Log.w(TAG, expression);
        try {
            BooleanExpressionLexer lexer = new BooleanExpressionLexer(new ANTLRInputStream(expression.trim()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            BooleanExpressionParser parser = new BooleanExpressionParser(tokens);
            BooleanExpressionParser.RootContext tree = parser.root();

            DiagnosisExpressionEvaluator evaluator = new DiagnosisExpressionEvaluator(data);
            ParseTreeWalker.DEFAULT.walk(evaluator, tree);

            return evaluator.getEvaluationResult();
        } catch (Exception e) {
            Log.e(TAG, "Error evaluating diagnosis expression", e);
            if (signName != null) {
                throw new DiagnosisException(String.format("Error evaluating diagnosis [%s]/[%s]", diagnosisName, signName));
            } else {
                throw new DiagnosisException(String.format("Error evaluating diagnosis [%s]", diagnosisName));
            }
        }
    }


}
