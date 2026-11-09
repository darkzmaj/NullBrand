package dev.korinetwork.nullbrand;
import org.junit.jupiter.api.Test;
import dev.korinetwork.nullbrand.render.MotdRenderer;
import static org.junit.jupiter.api.Assertions.*;
class RenderingRegressionTest {
    @Test void namedMiniMessageColorIsRenderedInPaperLegacyOutput() {
        assertEquals("§cHello", MotdRenderer.toLegacy("<red>Hello"));
    }
    @Test void legacyFormattingCanBeMixedWithMiniMessage() {
        String rendered=MotdRenderer.toLegacy("<red>&lHello");
        assertFalse(rendered.contains("&l"));assertTrue(rendered.contains("§l"));
    }
}
