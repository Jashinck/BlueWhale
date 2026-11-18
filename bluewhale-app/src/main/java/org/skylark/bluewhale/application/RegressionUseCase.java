package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.BudgetSpec;
import org.skylark.bluewhale.domain.context.ContextBundle;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.RepresentationDepth;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.ResolveQuery;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.governance.RegressionCase;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.RegressionCaseRepository;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RegressionUseCase {

    private final RegressionCaseRepository caseRepository;
    private final ResolveContextUseCase resolve;
    private final ContextUnitRepository unitRepository;

    public RegressionUseCase(RegressionCaseRepository caseRepository, ResolveContextUseCase resolve,
                             ContextUnitRepository unitRepository) {
        this.caseRepository = caseRepository;
        this.resolve = resolve;
        this.unitRepository = unitRepository;
    }

    public RegressionReport runDefectSet(Scope scope) {
        UnitId runId = UnitId.generate();
        int passed = 0;
        int failed = 0;
        for (RegressionCase regressionCase : caseRepository.findAll()) {
            ContextBundle bundle = resolve.resolve(new ResolveQuery("regression-" + regressionCase.id(),
                    scope, "TASK", regressionCase.query(), new BudgetSpec(2000),
                    Set.of(RepresentationLine.values()), RepresentationDepth.OVERVIEW));
            if (passes(regressionCase, bundle)) {
                passed++;
            } else {
                failed++;
            }
        }
        return new RegressionReport(runId, passed, failed);
    }

    private boolean passes(RegressionCase regressionCase, ContextBundle bundle) {
        if (regressionCase.expectEmpty()) {
            return bundle.sections().isEmpty();
        }
        for (ContextBundle.Section section : bundle.sections()) {
            if (section.unitId().equals(regressionCase.expectedUnitId().toString())) {
                return true;
            }
            ContextUnit recalled = unitRepository.findById(UnitId.of(section.unitId())).orElse(null);
            if (recalled != null && recalled.lineage().derivedFrom().contains(regressionCase.expectedUnitId())) {
                return true;
            }
        }
        return false;
    }
}