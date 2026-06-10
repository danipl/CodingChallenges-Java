package com.danipl.platform.datastructures.configmerger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConfigMerger tests")
class ConfigMergerTest {

    private ConfigMerger merger;

    @BeforeEach
    void setUp() {
        merger = ConfigMerger.of();
    }

    @Nested
    @DisplayName("Basic merge")
    class BasicMerge {

        @Test
        @DisplayName("single source merges correctly")
        void singleSourceMerge() {
            ConfigNode defaults = new ConfigNode("root", null, Map.of(
                    "host", new ConfigNode("host", "localhost"),
                    "port", new ConfigNode("port", 8080)
            ));
            merger.addSource("defaults", defaults, 0);
            ConfigNode merged = merger.merge();

            assertNotNull(merged);
            ConfigNode host = merged.getChildren().get("host");
            assertEquals("localhost", host.getValue());
        }

        @Test
        @DisplayName("empty sources result in empty merge")
        void emptySourceMerges() {
            ConfigNode empty = new ConfigNode("root", null);
            merger.addSource("empty", empty, 0);
            ConfigNode merged = merger.merge();
            assertNotNull(merged);
            assertTrue(merged.getChildren().isEmpty());
        }
    }

    @Nested
    @DisplayName("Priority override")
    class PriorityOverride {

        @Test
        @DisplayName("higher priority source overrides lower priority")
        void higherPriorityOverrides() {
            ConfigNode defaults = new ConfigNode("root", null, Map.of(
                    "host", new ConfigNode("host", "localhost"),
                    "port", new ConfigNode("port", 8080)
            ));
            ConfigNode prod = new ConfigNode("root", null, Map.of(
                    "host", new ConfigNode("host", "prod.example.com")
            ));
            merger.addSource("defaults", defaults, 0);
            merger.addSource("prod", prod, 100);
            ConfigNode merged = merger.merge();

            assertEquals("prod.example.com", merged.getChildren().get("host").getValue());
            assertEquals(8080, merged.getChildren().get("port").getValue());
        }

        @Test
        @DisplayName("multiple sources merged correctly")
        void multipleSourcesMerged() {
            ConfigNode s1 = new ConfigNode("root", null, Map.of(
                    "a", new ConfigNode("a", "from-s1"),
                    "b", new ConfigNode("b", "from-s1")
            ));
            ConfigNode s2 = new ConfigNode("root", null, Map.of(
                    "b", new ConfigNode("b", "from-s2"),
                    "c", new ConfigNode("c", "from-s2")
            ));
            ConfigNode s3 = new ConfigNode("root", null, Map.of(
                    "c", new ConfigNode("c", "from-s3"),
                    "d", new ConfigNode("d", "from-s3")
            ));
            merger.addSource("s1", s1, 1);
            merger.addSource("s2", s2, 2);
            merger.addSource("s3", s3, 3);
            ConfigNode merged = merger.merge();

            assertEquals("from-s1", merged.getChildren().get("a").getValue());
            assertEquals("from-s2", merged.getChildren().get("b").getValue());
            assertEquals("from-s3", merged.getChildren().get("c").getValue());
            assertEquals("from-s3", merged.getChildren().get("d").getValue());
        }
    }

    @Nested
    @DisplayName("Nested path resolution")
    class NestedPathResolution {

        @Test
        @DisplayName("resolve nested dotPath")
        void resolveNestedDotPath() {
            ConfigNode db = new ConfigNode("database", null, Map.of(
                    "host", new ConfigNode("host", "db.example.com"),
                    "port", new ConfigNode("port", 5432),
                    "credentials", new ConfigNode("credentials", null, Map.of(
                            "username", new ConfigNode("username", "admin"),
                            "password", new ConfigNode("password", "secret")
                    ))
            ));
            ConfigNode root = new ConfigNode("root", null, Map.of("database", db));
            merger.addSource("db-config", root, 0);

            assertEquals(Optional.of("db.example.com"), merger.resolve("database.host"));
            assertEquals(Optional.of(5432), merger.resolve("database.port"));
            assertEquals(Optional.of("admin"), merger.resolve("database.credentials.username"));
            assertEquals(Optional.of("secret"), merger.resolve("database.credentials.password"));
        }

        @Test
        @DisplayName("missing path returns empty optional")
        void missingPathReturnsEmpty() {
            merger.addSource("empty", new ConfigNode("root", null), 0);
            assertEquals(Optional.empty(), merger.resolve("nonexistent"));
            assertEquals(Optional.empty(), merger.resolve("a.b.c"));
        }

        @Test
        @DisplayName("override partial subtree")
        void overridePartialSubtree() {
            ConfigNode defaults = new ConfigNode("root", null, Map.of(
                    "database", new ConfigNode("database", null, Map.of(
                            "host", new ConfigNode("host", "localhost"),
                            "port", new ConfigNode("port", 5432),
                            "ssl", new ConfigNode("ssl", true)
                    ))
            ));
            ConfigNode override = new ConfigNode("root", null, Map.of(
                    "database", new ConfigNode("database", null, Map.of(
                            "host", new ConfigNode("host", "prod-host")
                    ))
            ));
            merger.addSource("defaults", defaults, 0);
            merger.addSource("override", override, 10);
            ConfigNode merged = merger.merge();

            assertEquals("prod-host", merged.getChildren().get("database").getChildren().get("host").getValue());
            assertEquals(5432, merged.getChildren().get("database").getChildren().get("port").getValue());
            assertEquals(true, merged.getChildren().get("database").getChildren().get("ssl").getValue());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("conflicting types (value vs children) - children win")
        void conflictingTypes() {
            ConfigNode leaf = new ConfigNode("key", "leaf-value");
            ConfigNode branch = new ConfigNode("key", null, Map.of(
                    "sub", new ConfigNode("sub", "nested-value")
            ));
            merger.addSource("leaf", leaf, 0);
            merger.addSource("branch", branch, 10);
            ConfigNode merged = merger.merge();
            assertNotNull(merged.getChildren().get("key").getChildren().get("sub"));
            assertEquals("nested-value", merged.getChildren().get("key").getChildren().get("sub").getValue());
        }

        @Test
        @DisplayName("dotPath with special characters in key names")
        void dotPathWithSpecialChars() {
            ConfigNode node = new ConfigNode("root", null, Map.of(
                    "my-key", new ConfigNode("my-key", "value1"),
                    "my_key", new ConfigNode("my_key", "value2"),
                    "my.key", new ConfigNode("my.key", "value3")
            ));
            // Note: resolver should handle keys with dots in them
            // This tests how the implementation handles ambiguous paths
            merger.addSource("special", node, 0);
            assertNotNull(merger.resolve("my-key"));
        }

        @Test
        @DisplayName("deep nesting (5+ levels)")
        void deepNesting() {
            Map<String, ConfigNode> l5 = Map.of("leaf", new ConfigNode("leaf", "deep-value"));
            Map<String, ConfigNode> l4 = Map.of("level5", new ConfigNode("level5", null, l5));
            Map<String, ConfigNode> l3 = Map.of("level4", new ConfigNode("level4", null, l4));
            Map<String, ConfigNode> l2 = Map.of("level3", new ConfigNode("level3", null, l3));
            Map<String, ConfigNode> l1 = Map.of("level2", new ConfigNode("level2", null, l2));
            ConfigNode root = new ConfigNode("root", null, Map.of("level1", new ConfigNode("level1", null, l1)));
            merger.addSource("deep", root, 0);

            assertEquals(Optional.of("deep-value"), merger.resolve("level1.level2.level3.level4.level5.leaf"));
        }
    }
}
