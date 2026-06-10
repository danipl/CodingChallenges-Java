package com.danipl.platform.datastructures.dependencyresolver;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyResolverTest {

    @Nested
    class LinearChains {

        @Test
        void linearChainAtoBtoC() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A", List.of("B"));
            resolver.add("B", List.of("C"));
            resolver.add("C", List.of());
            List<String> order = resolver.resolveBuildOrder();
            assertTrue(order.indexOf("C") < order.indexOf("B"));
            assertTrue(order.indexOf("B") < order.indexOf("A"));
        }

        @Test
        void noDepsReturnsInsertionOrder() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("X", List.of());
            resolver.add("Y", List.of());
            resolver.add("Z", List.of());
            List<String> order = resolver.resolveBuildOrder();
            assertEquals(List.of("X", "Y", "Z"), order);
        }
    }

    @Nested
    class DiamondDependency {

        @Test
        void diamondAdependsOnBAndCBothDependOnD() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A", List.of("B", "C"));
            resolver.add("B", List.of("D"));
            resolver.add("C", List.of("D"));
            resolver.add("D", List.of());
            List<String> order = resolver.resolveBuildOrder();
            assertTrue(order.indexOf("D") < order.indexOf("B"));
            assertTrue(order.indexOf("D") < order.indexOf("C"));
            assertTrue(order.indexOf("B") < order.indexOf("A"));
            assertTrue(order.indexOf("C") < order.indexOf("A"));
            assertEquals(4, order.size());
        }
    }

    @Nested
    class CircularDetection {

        @Test
        void directCircularAtoBtoa() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A", List.of("B"));
            resolver.add("B", List.of("A"));
            assertTrue(resolver.hasCircularDependency());
            assertThrows(CircularDependencyException.class,
                    () -> resolver.resolveBuildOrder());
        }

        @Test
        void indirectCircularAtoBtoCtoA() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A", List.of("B"));
            resolver.add("B", List.of("C"));
            resolver.add("C", List.of("A"));
            assertTrue(resolver.hasCircularDependency());
            assertThrows(CircularDependencyException.class,
                    () -> resolver.resolveBuildOrder());
        }

        @Test
        void selfDependency() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A", List.of("A"));
            assertTrue(resolver.hasCircularDependency());
        }
    }

    @Nested
    class ComplexDag {

        @Test
        void complexDirectedAcyclicGraph() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("web", List.of("api", "utils"));
            resolver.add("api", List.of("db", "auth"));
            resolver.add("db", List.of("core"));
            resolver.add("auth", List.of("core"));
            resolver.add("utils", List.of("core"));
            resolver.add("core", List.of());
            List<String> order = resolver.resolveBuildOrder();
            assertTrue(order.indexOf("core") < order.indexOf("db"));
            assertTrue(order.indexOf("core") < order.indexOf("auth"));
            assertTrue(order.indexOf("core") < order.indexOf("utils"));
            assertTrue(order.indexOf("db") < order.indexOf("api"));
            assertTrue(order.indexOf("auth") < order.indexOf("api"));
            assertTrue(order.indexOf("api") < order.indexOf("web"));
            assertTrue(order.indexOf("utils") < order.indexOf("web"));
            assertEquals(6, order.size());
        }

        @Test
        void multipleIndependentGraphs() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A1", List.of("A2"));
            resolver.add("A2", List.of());
            resolver.add("B1", List.of("B2"));
            resolver.add("B2", List.of());
            List<String> order = resolver.resolveBuildOrder();
            assertTrue(order.indexOf("A2") < order.indexOf("A1"));
            assertTrue(order.indexOf("B2") < order.indexOf("B1"));
            assertEquals(4, order.size());
        }

        @Test
        void missingImplicitDependenciesTreatedNoOp() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("A", List.of("B"));
            resolver.add("A", List.of("C"));
            // B and C never registered as libraries themselves
            List<String> order = resolver.resolveBuildOrder();
            assertTrue(order.contains("A"));
            assertTrue(order.indexOf("A") >= 0);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void emptyResolver() {
            DependencyResolver resolver = DependencyResolver.of();
            assertTrue(resolver.resolveBuildOrder().isEmpty());
            assertFalse(resolver.hasCircularDependency());
        }

        @Test
        void singleLibraryNoDeps() {
            DependencyResolver resolver = DependencyResolver.of();
            resolver.add("solo", List.of());
            assertEquals(List.of("solo"), resolver.resolveBuildOrder());
        }
    }
}
