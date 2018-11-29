package org.neotree.diagnosis;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matteo on 21/01/2017.
 */

public class DiagnosisResult {

    private List<String> names;
    private List<DiagnosisManagement> managements;

    public void addDiagnosis(String name, List<DiagnosisManagement> entries) {
        if (names == null) {
            names = new ArrayList<>();
        }
        names.add(name);

        if (entries != null && entries.size() > 0) {
            if (managements == null) {
                managements = new ArrayList<>();
            }
            managements.addAll(entries);
        }
    }

    public List<String> getNames() {
        return names;
    }

    public List<DiagnosisManagement> getManagements() {
        return managements;
    }

    public int getDiagnosisCount() {
        return (names != null) ? names.size() : 0;
    }

    public int getManagementCount() {
        return (managements != null) ? managements.size() : 0;
    }

    public String getDiagnosisName(int position) {
        return (names != null && names.size() > 0) ? names.get(position) : null;
    }

    public DiagnosisManagement getDiagnosisManagement(int position) {
        return (managements != null && managements.size() > 0) ? managements.get(position) : null;
    }

}
