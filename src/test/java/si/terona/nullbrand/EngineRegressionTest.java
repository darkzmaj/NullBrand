package si.terona.nullbrand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.terona.nullbrand.engine.BrandEngine;
import si.terona.nullbrand.favicon.FaviconStore;
import si.terona.nullbrand.model.*;
import si.terona.nullbrand.playercount.FakePlayerEngine;
import si.terona.nullbrand.util.*;
import java.nio.file.*;
import java.util.logging.Logger;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class EngineRegressionTest {
    @TempDir Path dir;
    private BrandEngine engine(String config) throws Exception {
        Files.writeString(dir.resolve("config.yml"), config);
        BrandEngine e = new BrandEngine(new LogUtil(Logger.getAnonymousLogger(), false),"test");
        assertTrue(e.load(dir,getClass().getClassLoader()));return e;
    }
    @Test void runtimeMaintenanceControlsDisplayedProfile() throws Exception {
        BrandEngine e=engine("maintenance: {enabled: true, profile: maintenance}\nprofiles:\n  default: {lines: [normal]}\n  maintenance: {lines: [outage]}\nmotd: {profile: default}\n");
        try {
            assertEquals("maintenance",e.state().resolveProfile().getName());
            e.state().maintenanceManager().setActive(false);
            assertEquals("default",e.state().resolveProfile().getName());
            e.state().maintenanceManager().setActive(true);
            assertEquals("maintenance",e.state().resolveProfile().getName());
        } finally {e.shutdown();}
    }
    @Test void invalidYamlPreservesPreviousStateAndRejectsObjectTags() throws Exception {
        BrandEngine e=engine("profiles: {default: {lines: [working]}}\n");
        try {
            Object state=e.state();
            for(String bad:new String[]{"profiles: [", "!!java.net.URL [https://example.com]", "motd: {enabled: true, enabled: false}"}) {
                Files.writeString(dir.resolve("config.yml"),bad);
                assertFalse(e.load(dir,getClass().getClassLoader())); assertSame(state,e.state());
            }
        } finally {e.shutdown();}
    }
    @Test void profileSpecificFakePlayersAndExplicitDisableWork() throws Exception {
        BrandEngine e=engine("fakeplayers: {enabled: true, mode: STATIC, amount: 50}\nprofiles:\n  default:\n    lines: [normal]\n    fakeplayers: {enabled: true, mode: STATIC, amount: 3}\n  quiet:\n    lines: [quiet]\n    fakeplayers: {enabled: false}\n");
        try {
            assertEquals(8,e.state().fakePlayersFor(e.state().config().getProfile("default")).computeDisplayed(5));
            assertEquals(5,e.state().fakePlayersFor(e.state().config().getProfile("quiet")).computeDisplayed(5));
        } finally {e.shutdown();}
    }
    @Test void staticOverrideAndOverflowAreHandled() {
        FakePlayerEngine e=new FakePlayerEngine(new FakePlayerSettings(true,FakePlayerMode.STATIC,5,0,Integer.MAX_VALUE,1,0,20));
        e.setStaticOverride(10);assertEquals(15,e.computeDisplayed(5));
        assertEquals(Integer.MAX_VALUE,e.computeDisplayed(Integer.MAX_VALUE));
        FakePlayerEngine r=new FakePlayerEngine(new FakePlayerSettings(true,FakePlayerMode.RANDOM,0,0,Integer.MAX_VALUE,1,0,20));
        assertTrue(r.computeDisplayed(0)>=0);
    }
    @Test void truncatedHexHoverInputNeverThrows() {
        for(String s:new String[]{"&#12345","<#123456","&#","<#"}) assertDoesNotThrow(()->ColorUtil.translateHover(s));
    }
    @Test void faviconsRejectTraversalInvalidDimensionsAndMalformedFiles() throws Exception {
        Path icons=Files.createDirectory(dir.resolve("favicons"));
        ImageIO.write(new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB),"png",dir.resolve("outside.png").toFile());
        assertFalse(FaviconStore.load(icons,"../outside.png").hasFavicon());
        ImageIO.write(new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB),"png",icons.resolve("small.png").toFile());
        assertFalse(FaviconStore.load(icons,"small.png").hasFavicon());
        Files.writeString(icons.resolve("invalid.png"),"invalid");assertFalse(FaviconStore.load(icons,"invalid.png").hasFavicon());
        Files.copy(dir.resolve("outside.png"),icons.resolve("valid.png"));assertTrue(FaviconStore.load(icons,"valid.png").hasFavicon());
    }
}
