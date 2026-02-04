package com.paragon.messaging.batching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BackpressureStrategy}.
 */
@DisplayName("BackpressureStrategy")
class BackpressureStrategyTest {

    @Nested
    @DisplayName("DROP_NEW Strategy")
    class DropNewTests {

        @Test
        @DisplayName("canLoseMessages() returns true")
        void canLoseMessages_returnsTrue() {
            assertTrue(BackpressureStrategy.DROP_NEW.canLoseMessages());
        }

        @Test
        @DisplayName("canBlock() returns false")
        void canBlock_returnsFalse() {
            assertFalse(BackpressureStrategy.DROP_NEW.canBlock());
        }

        @Test
        @DisplayName("description() returns accurate description")
        void description_returnsAccurate() {
            String desc = BackpressureStrategy.DROP_NEW.description();

            assertNotNull(desc);
            assertTrue(desc.toLowerCase().contains("descarta"));
            assertTrue(desc.toLowerCase().contains("novas"));
        }
    }

    @Nested
    @DisplayName("DROP_OLDEST Strategy")
    class DropOldestTests {

        @Test
        @DisplayName("canLoseMessages() returns true")
        void canLoseMessages_returnsTrue() {
            assertTrue(BackpressureStrategy.DROP_OLDEST.canLoseMessages());
        }

        @Test
        @DisplayName("canBlock() returns false")
        void canBlock_returnsFalse() {
            assertFalse(BackpressureStrategy.DROP_OLDEST.canBlock());
        }

        @Test
        @DisplayName("description() returns accurate description")
        void description_returnsAccurate() {
            String desc = BackpressureStrategy.DROP_OLDEST.description();

            assertNotNull(desc);
            assertTrue(desc.toLowerCase().contains("descarta"));
            assertTrue(desc.toLowerCase().contains("antigas"));
        }
    }

    @Nested
    @DisplayName("REJECT_WITH_NOTIFICATION Strategy")
    class RejectWithNotificationTests {

        @Test
        @DisplayName("canLoseMessages() returns true")
        void canLoseMessages_returnsTrue() {
            assertTrue(BackpressureStrategy.REJECT_WITH_NOTIFICATION.canLoseMessages());
        }

        @Test
        @DisplayName("canBlock() returns false")
        void canBlock_returnsFalse() {
            assertFalse(BackpressureStrategy.REJECT_WITH_NOTIFICATION.canBlock());
        }

        @Test
        @DisplayName("description() returns accurate description")
        void description_returnsAccurate() {
            String desc = BackpressureStrategy.REJECT_WITH_NOTIFICATION.description();

            assertNotNull(desc);
            assertTrue(desc.toLowerCase().contains("rejeita"));
            assertTrue(desc.toLowerCase().contains("notifica"));
        }
    }

    @Nested
    @DisplayName("BLOCK_UNTIL_SPACE Strategy")
    class BlockUntilSpaceTests {

        @Test
        @DisplayName("canLoseMessages() returns false")
        void canLoseMessages_returnsFalse() {
            assertFalse(BackpressureStrategy.BLOCK_UNTIL_SPACE.canLoseMessages());
        }

        @Test
        @DisplayName("canBlock() returns true")
        void canBlock_returnsTrue() {
            assertTrue(BackpressureStrategy.BLOCK_UNTIL_SPACE.canBlock());
        }

        @Test
        @DisplayName("description() returns accurate description")
        void description_returnsAccurate() {
            String desc = BackpressureStrategy.BLOCK_UNTIL_SPACE.description();

            assertNotNull(desc);
            assertTrue(desc.toLowerCase().contains("bloqueia"));
        }
    }

    @Nested
    @DisplayName("FLUSH_AND_ACCEPT Strategy")
    class FlushAndAcceptTests {

        @Test
        @DisplayName("canLoseMessages() returns false")
        void canLoseMessages_returnsFalse() {
            assertFalse(BackpressureStrategy.FLUSH_AND_ACCEPT.canLoseMessages());
        }

        @Test
        @DisplayName("canBlock() returns false")
        void canBlock_returnsFalse() {
            assertFalse(BackpressureStrategy.FLUSH_AND_ACCEPT.canBlock());
        }

        @Test
        @DisplayName("description() returns accurate description")
        void description_returnsAccurate() {
            String desc = BackpressureStrategy.FLUSH_AND_ACCEPT.description();

            assertNotNull(desc);
            assertTrue(desc.toLowerCase().contains("processa"));
            assertTrue(desc.toLowerCase().contains("aceita"));
        }
    }

    @Nested
    @DisplayName("Strategy Comparison")
    class StrategyComparisonTests {

        @Test
        @DisplayName("only BLOCK_UNTIL_SPACE can block")
        void onlyBlockUntilSpace_canBlock() {
            for (BackpressureStrategy strategy : BackpressureStrategy.values()) {
                if (strategy == BackpressureStrategy.BLOCK_UNTIL_SPACE) {
                    assertTrue(strategy.canBlock(), strategy + " should be able to block");
                } else {
                    assertFalse(strategy.canBlock(), strategy + " should not block");
                }
            }
        }

        @Test
        @DisplayName("lossy strategies are DROP_NEW, DROP_OLDEST, and REJECT_WITH_NOTIFICATION")
        void lossyStrategies_identified() {
            assertTrue(BackpressureStrategy.DROP_NEW.canLoseMessages());
            assertTrue(BackpressureStrategy.DROP_OLDEST.canLoseMessages());
            assertTrue(BackpressureStrategy.REJECT_WITH_NOTIFICATION.canLoseMessages());
            
            assertFalse(BackpressureStrategy.BLOCK_UNTIL_SPACE.canLoseMessages());
            assertFalse(BackpressureStrategy.FLUSH_AND_ACCEPT.canLoseMessages());
        }

        @Test
        @DisplayName("lossless strategies are BLOCK_UNTIL_SPACE and FLUSH_AND_ACCEPT")
        void losslessStrategies_identified() {
            assertFalse(BackpressureStrategy.BLOCK_UNTIL_SPACE.canLoseMessages());
            assertFalse(BackpressureStrategy.FLUSH_AND_ACCEPT.canLoseMessages());
        }

        @Test
        @DisplayName("all strategies have non-empty descriptions")
        void allStrategies_haveDescriptions() {
            for (BackpressureStrategy strategy : BackpressureStrategy.values()) {
                String desc = strategy.description();
                assertNotNull(desc, strategy + " should have a description");
                assertFalse(desc.isBlank(), strategy + " description should not be blank");
            }
        }

        @Test
        @DisplayName("all strategies have unique descriptions")
        void allStrategies_haveUniqueDescriptions() {
            BackpressureStrategy[] strategies = BackpressureStrategy.values();
            
            for (int i = 0; i < strategies.length; i++) {
                for (int j = i + 1; j < strategies.length; j++) {
                    assertNotEquals(
                            strategies[i].description(),
                            strategies[j].description(),
                            "Descriptions should be unique"
                    );
                }
            }
        }
    }

    @Nested
    @DisplayName("Enum Properties")
    class EnumPropertiesTests {

        @Test
        @DisplayName("valueOf() returns correct strategy")
        void valueOf_returnsCorrectStrategy() {
            assertEquals(BackpressureStrategy.DROP_NEW, 
                    BackpressureStrategy.valueOf("DROP_NEW"));
            assertEquals(BackpressureStrategy.DROP_OLDEST, 
                    BackpressureStrategy.valueOf("DROP_OLDEST"));
            assertEquals(BackpressureStrategy.REJECT_WITH_NOTIFICATION, 
                    BackpressureStrategy.valueOf("REJECT_WITH_NOTIFICATION"));
            assertEquals(BackpressureStrategy.BLOCK_UNTIL_SPACE, 
                    BackpressureStrategy.valueOf("BLOCK_UNTIL_SPACE"));
            assertEquals(BackpressureStrategy.FLUSH_AND_ACCEPT, 
                    BackpressureStrategy.valueOf("FLUSH_AND_ACCEPT"));
        }

        @Test
        @DisplayName("values() returns all strategies")
        void values_returnsAllStrategies() {
            BackpressureStrategy[] strategies = BackpressureStrategy.values();

            assertEquals(5, strategies.length, "Should have exactly 5 strategies");
        }

        @Test
        @DisplayName("name() returns correct string")
        void name_returnsCorrectString() {
            assertEquals("DROP_NEW", BackpressureStrategy.DROP_NEW.name());
            assertEquals("DROP_OLDEST", BackpressureStrategy.DROP_OLDEST.name());
            assertEquals("REJECT_WITH_NOTIFICATION", 
                    BackpressureStrategy.REJECT_WITH_NOTIFICATION.name());
            assertEquals("BLOCK_UNTIL_SPACE", BackpressureStrategy.BLOCK_UNTIL_SPACE.name());
            assertEquals("FLUSH_AND_ACCEPT", BackpressureStrategy.FLUSH_AND_ACCEPT.name());
        }
    }

    @Nested
    @DisplayName("Use Case Recommendations")
    class UseCaseTests {

        @Test
        @DisplayName("DROP_OLDEST is best for recent message priority")
        void dropOldest_forRecentPriority() {
            // This strategy prioritizes recent messages
            assertTrue(BackpressureStrategy.DROP_OLDEST.canLoseMessages());
            assertFalse(BackpressureStrategy.DROP_OLDEST.canBlock());
        }

        @Test
        @DisplayName("FLUSH_AND_ACCEPT is best for no message loss without blocking")
        void flushAndAccept_forNoLossNoBlock() {
            // This strategy guarantees no message loss and doesn't block
            assertFalse(BackpressureStrategy.FLUSH_AND_ACCEPT.canLoseMessages());
            assertFalse(BackpressureStrategy.FLUSH_AND_ACCEPT.canBlock());
        }

        @Test
        @DisplayName("BLOCK_UNTIL_SPACE is best when message loss is unacceptable")
        void blockUntilSpace_forCriticalMessages() {
            // This strategy guarantees delivery but may block
            assertFalse(BackpressureStrategy.BLOCK_UNTIL_SPACE.canLoseMessages());
            assertTrue(BackpressureStrategy.BLOCK_UNTIL_SPACE.canBlock());
        }
    }
}
