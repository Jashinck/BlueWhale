package org.skylark.bluewhale.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.skylark.bluewhale", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "jakarta..",
                            "com.fasterxml..", "lombok..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_adapters_or_protocols =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..infrastructure..", "..interfaces..");

    @ArchTest
    static final ArchRule interfaces_must_not_bypass_application =
            noClasses().that().resideInAPackage("..interfaces..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..infrastructure..");

    // 收敛终态：legacy 包不得回归，context 内核必须自洽（不反向依赖子域与端口）。
    @ArchTest
    static final ArchRule legacy_packages_must_not_return =
            noClasses().should().dependOnClassesThat()
                    .resideInAnyPackage("..domain.model..", "..domain.repository..");

    @ArchTest
    static final ArchRule context_kernel_must_be_self_contained =
            noClasses().that().resideInAPackage("..domain.context..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..domain.memory..", "..domain.graph..", "..domain.knowledge..",
                            "..domain.lifecycle..", "..domain.governance..", "..domain.feedback..",
                            "..domain.port..");
}
