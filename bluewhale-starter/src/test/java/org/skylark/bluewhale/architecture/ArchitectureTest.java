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
}
