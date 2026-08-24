package com.example.leavemanagement.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AnalyzeClasses(
        packages = ArchitectureLayerBoundaryTest.BASE_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureLayerBoundaryTest {
    static final String BASE_PACKAGE = "com.example.leavemanagement";
    private static final Set<String> MODULES = Set.of(
            "audit", "auth", "balance", "calendar", "people", "policy", "reporting", "request", "shared");

    @ArchTest
    static final ArchRule api_must_not_access_persistence_directly = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_api_transport = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_persistence_implementation = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..");

    @ArchTest
    static final ArchRule persistence_must_not_depend_on_api_transport = noClasses()
            .that().resideInAPackage("..persistence..")
            .should().dependOnClassesThat().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule controllers_must_delegate_without_database_framework_access = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.data..", "org.springframework.jdbc..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule infrastructure_and_security_must_not_call_feature_apis = noClasses()
            .that().resideInAnyPackage("..config..", "..security..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE_PACKAGE + ".audit.api..",
                    BASE_PACKAGE + ".auth.api..",
                    BASE_PACKAGE + ".balance.api..",
                    BASE_PACKAGE + ".calendar.api..",
                    BASE_PACKAGE + ".people.api..",
                    BASE_PACKAGE + ".policy.api..",
                    BASE_PACKAGE + ".reporting.api..",
                    BASE_PACKAGE + ".request.api..");

    @ArchTest
    static final ArchRule security_persistence_access_is_limited_to_current_actor_resolution = classes()
            .that().resideInAPackage("..security..")
            .and().doNotHaveSimpleName("CurrentActorProvider")
            .should().onlyDependOnClassesThat().resideOutsideOfPackage("..persistence..");

    @ArchTest
    static final ArchRule infrastructure_must_not_bypass_application_through_feature_persistence = noClasses()
            .that().resideInAPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..");

    @ArchTest
    static void six_layer_graph_has_no_prohibited_cycles_and_all_classes_are_classified(JavaClasses classes) {
        var graph = new EnumMap<Layer, Set<Layer>>(Layer.class);
        for (var layer : Layer.values()) graph.put(layer, EnumSet.noneOf(Layer.class));

        for (var source : classes) {
            if (!source.getPackageName().startsWith(BASE_PACKAGE)) continue;
            if (!source.getPackageName().equals(BASE_PACKAGE)) {
                var module = source.getPackageName().substring(BASE_PACKAGE.length() + 1).split("\\.")[0];
                assertTrue(MODULES.contains(module),
                        () -> "Class is outside the approved modular-monolith modules: " + source.getName());
            }
            var sourceLayer = layerOf(source);
            assertTrue(sourceLayer != null, () -> "Unclassified modular-monolith package: " + source.getName());
            for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                var target = dependency.getTargetClass();
                if (!target.getPackageName().startsWith(BASE_PACKAGE)) continue;
                if (source.getSimpleName().equals("CurrentActorProvider")
                        && target.getPackageName().contains(".persistence")) {
                    assertTrue(target.getPackageName().equals(BASE_PACKAGE + ".people.persistence"),
                            () -> "Current-actor resolution bypasses its approved people persistence boundary: "
                                    + dependency.getDescription());
                }
                var targetLayer = layerOf(target);
                assertTrue(targetLayer != null, () -> "Unclassified modular-monolith package: " + target.getName());
                if (sourceLayer != targetLayer) graph.get(sourceLayer).add(targetLayer);
            }
        }

        assertTrue(findCycle(graph).isEmpty(), () -> "Prohibited architectural layer cycle: " + findCycle(graph));
    }

    private static Layer layerOf(JavaClass type) {
        var packageName = type.getPackageName();
        if (packageName.equals(BASE_PACKAGE)) return Layer.INFRASTRUCTURE;
        if (packageName.equals(BASE_PACKAGE + ".shared.api")) {
            return Set.of("GlobalExceptionHandler", "LocalDemoResetController").contains(type.getSimpleName())
                    ? Layer.API_PRESENTATION : Layer.SECURITY_CROSS_CUTTING;
        }
        if (packageName.contains(".api")) return Layer.API_PRESENTATION;
        if (packageName.contains(".application")) return Layer.APPLICATION;
        if (packageName.contains(".domain")) return Layer.DOMAIN;
        if (packageName.contains(".persistence")) return Layer.PERSISTENCE;
        if (packageName.contains(".config")) return Layer.INFRASTRUCTURE;
        if (packageName.contains(".security")) return Layer.SECURITY_CROSS_CUTTING;
        return null;
    }

    private static String findCycle(Map<Layer, Set<Layer>> graph) {
        for (var start : Layer.values()) {
            var path = new ArrayDeque<Layer>();
            if (visit(start, start, graph, path, EnumSet.noneOf(Layer.class))) return path.toString();
        }
        return "";
    }

    private static boolean visit(Layer start, Layer current, Map<Layer, Set<Layer>> graph,
                                 ArrayDeque<Layer> path, Set<Layer> visited) {
        path.addLast(current);
        visited.add(current);
        for (var next : graph.get(current)) {
            if (next == start) {
                path.addLast(start);
                return true;
            }
            if (!visited.contains(next) && visit(start, next, graph, path, visited)) return true;
        }
        path.removeLast();
        return false;
    }

    private enum Layer {
        API_PRESENTATION,
        APPLICATION,
        DOMAIN,
        PERSISTENCE,
        INFRASTRUCTURE,
        SECURITY_CROSS_CUTTING
    }
}
