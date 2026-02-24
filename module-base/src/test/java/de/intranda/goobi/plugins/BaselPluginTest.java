package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.sub.goobi.config.ConfigurationHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*" })
public class BaselPluginTest {

    private static String resourcesFolder;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse

        System.setProperty("log4j.configurationFile", log4jFile);
    }

    @Before
    public void setUp() throws Exception {

        PowerMock.mockStatic(ConfigurationHelper.class);
        ConfigurationHelper configurationHelper = EasyMock.createMock(ConfigurationHelper.class);
        EasyMock.expect(ConfigurationHelper.getInstance()).andReturn(configurationHelper).anyTimes();
        EasyMock.expect(configurationHelper.getConfigurationFolder()).andReturn(resourcesFolder).anyTimes();
        EasyMock.replay(configurationHelper);
        PowerMock.replay(ConfigurationHelper.class);
    }

    @Test
    public void testVersion() throws IOException {
        String s = "xyz";
        assertNotNull(s);
    }

    @Test
    public void testReadCollections() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();

        Map<String, List<String>> fixture = plugin.getCollections();
        assertNotNull(fixture);
        assertEquals(3, fixture.size());
        assertNull(fixture.get("not existing"));

        assertNotNull(fixture.get("Handschriften"));
        assertEquals(3, fixture.get("Handschriften").size());
        assertEquals("Kartause", fixture.get("Handschriften").get(0));
        assertEquals("Handschriften allgemein", fixture.get("Handschriften").get(1));
        assertEquals("E-Codices", fixture.get("Handschriften").get(2));
    }

    @Test
    public void testReadColumns() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();

        Map<String, List<String>> fixture = plugin.getColumns();
        assertNotNull(fixture);
        assertEquals(4, fixture.size());
        assertNull(fixture.get("not existing"));

        assertNotNull(fixture.get("Systematische Digitalisierung"));
        assertEquals(9, fixture.get("Systematische Digitalisierung").size());
        assertEquals("Handschriften allgemein", fixture.get("Systematische Digitalisierung").get(2));

    }
}
