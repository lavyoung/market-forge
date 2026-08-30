package com.lavyoung.marketforge;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.lavyoung.marketforge",
        importOptions = ImportOption.DoNotIncludeTests.class
)
/**
 * 校验 DDD 分层依赖边界的架构测试。
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
 * @version 1.0.0-SNAPSHOT
 */
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domainShouldNotDependOnTrigger =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..trigger..");

    @ArchTest
    static final ArchRule domainShouldNotDependOnApplication =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..application..");

    @ArchTest
    static final ArchRule applicationShouldNotDependOnTrigger =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..trigger..");

    @ArchTest
    static final ArchRule infrastructureShouldNotDependOnTrigger =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..trigger..");

}
