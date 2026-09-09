package com.lavyoung.marketforge;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 校验 DDD 分层依赖边界的架构测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@AnalyzeClasses(
        packages = "com.lavyoung.marketforge",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    /** 领域层不得依赖基础设施层。 */
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");

    /** 领域层不得依赖触发器层。 */
    @ArchTest
    static final ArchRule domainShouldNotDependOnTrigger =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..trigger..");

    /** 领域层不得依赖应用层。 */
    @ArchTest
    static final ArchRule domainShouldNotDependOnApplication =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..app..");

    /** 基础设施层不得依赖触发器层。 */
    @ArchTest
    static final ArchRule infrastructureShouldNotDependOnTrigger =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..trigger..");

    /**
     * 入站适配器不得绕过应用层直接依赖领域层。
     */
    @ArchTest
    static final ArchRule triggerShouldNotDependOnDomain =
            noClasses()
                    .that()
                    .resideInAPackage("..trigger..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..domain..");

    /**
     * 应用层不得依赖入站适配器或基础设施实现。
     */
    @ArchTest
    static final ArchRule applicationShouldNotDependOnOuterAdapters =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..trigger..", "..infrastructure..", "..app..");

    /**
     * API 契约不得反向依赖领域或技术实现模块。
     */
    @ArchTest
    static final ArchRule apiShouldNotDependOnImplementationModules =
            noClasses()
                    .that()
                    .resideInAPackage("..api..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..domain..", "..infrastructure..", "..trigger..", "..app..");

}
