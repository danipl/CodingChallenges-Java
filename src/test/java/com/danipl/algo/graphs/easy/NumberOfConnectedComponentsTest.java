package com.danipl.algo.graphs.easy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class NumberOfConnectedComponentsTest {

    private NumberOfConnectedComponents solution;

    @BeforeEach
    void setUp() {
        solution = new NumberOfConnectedComponents();
    }

    @Test
    void testDisconnectedGraph_threeComponents() {
        // Given: 6 nodes, edges form 3 separate components: {0,1,2}, {3,4}, {5}
        int n = 6;
        int[][] edges = {{0, 1}, {1, 2}, {3, 4}};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(3, result);
    }

    @Test
    void testFullyConnected_singleComponent() {
        // Given: 5 nodes all connected in a line
        int n = 5;
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 4}};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(1, result);
    }

    @Test
    void testNoEdges_eachNodeIsOwnComponent() {
        // Given: 4 nodes, no edges
        int n = 4;
        int[][] edges = {};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(4, result);
    }

    @Test
    void testSingleNode_singleComponent() {
        // Given: 1 node, no edges
        int n = 1;
        int[][] edges = {};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(1, result);
    }

    @Test
    void testZeroNodes_zeroComponents() {
        // Given: 0 nodes
        int n = 0;
        int[][] edges = {};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCycleInComponent_stillOneComponent() {
        // Given: 4 nodes forming a cycle {0,1,2,3} plus isolated node {4}
        int n = 5;
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(2, result);
    }

    @Test
    void testStarGraph_singleComponent() {
        // Given: star graph with center 0 and leaves 1,2,3,4
        int n = 5;
        int[][] edges = {{0, 1}, {0, 2}, {0, 3}, {0, 4}};

        // When
        int result = solution.countComponents(n, edges);

        // Then
        assertEquals(1, result);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testMultipleCases(int n, int[][] edges, int expected) {
        assertEquals(expected, solution.countComponents(n, edges));
    }

    static Stream<Arguments> provideTestCases() {
        return Stream.of(
            Arguments.of(6, new int[][]{{0, 1}, {1, 2}, {3, 4}}, 3),
            Arguments.of(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}}, 1),
            Arguments.of(4, new int[][]{}, 4),
            Arguments.of(1, new int[][]{}, 1),
            Arguments.of(0, new int[][]{}, 0),
            Arguments.of(5, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}}, 2),
            Arguments.of(3, new int[][]{{0, 1}, {2, 1}}, 1),
            Arguments.of(7, new int[][]{{0, 1}, {2, 3}, {4, 5}, {5, 6}}, 3)
        );
    }
}
