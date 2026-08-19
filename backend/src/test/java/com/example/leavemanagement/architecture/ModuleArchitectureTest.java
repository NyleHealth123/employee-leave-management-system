package com.example.leavemanagement.architecture;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
@AnalyzeClasses(packages="com.example.leavemanagement",importOptions=ImportOption.DoNotIncludeTests.class)
class ModuleArchitectureTest {
 @ArchTest static final ArchRule controllers_do_not_call_repositories=noClasses().that().resideInAPackage("..api..").should().dependOnClassesThat().resideInAPackage("..persistence..");
 @ArchTest static final ArchRule domain_is_framework_independent=noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("org.springframework..","jakarta.persistence..");
}

