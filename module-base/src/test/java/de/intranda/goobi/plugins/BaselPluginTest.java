package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import de.sub.goobi.helper.Helper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigurationHelper.class, Helper.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*", "jdk.internal.reflect.*", "java.text.*" })
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
    @Test
    public void testCalculate() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();

        Map<String, List<String>> collections = plugin.getCollections();

        plugin.calculate();
        List<Group> resultList = plugin.getResultList();
        System.out.println(resultList);
    }

    @Test
    public void testExcelHeaderAlignsWithGesamtColumn() throws IOException {
        PowerMock.mockStatic(Helper.class);
        EasyMock.expect(Helper.getTranslation(EasyMock.anyString()))
                .andAnswer(() -> (String) EasyMock.getCurrentArguments()[0])
                .anyTimes();
        PowerMock.replay(Helper.class);

        Group group = new Group();
        group.setName("TestGruppe");
        group.getValues().add(new Interval("ProjektA", "2024/01", 100, 5, 0.5f));
        group.getValues().add(new Interval("ProjektA", "2024/02", 200, 10, 0.5f));
        group.getTotalValues().add(new Interval("total", "2024/01", 100, 5, 1.0f));
        group.getTotalValues().add(new Interval("total", "2024/02", 200, 10, 1.0f));

        Set<String> dates = new LinkedHashSet<>();
        dates.add("2024/01");
        dates.add("2024/02");

        List<Group> groups = new ArrayList<>();
        groups.add(group);

        ExcelCreator creator = new ExcelCreator(groups, dates, "Sammlungen");
        Workbook wb = creator.buildWorkbook();
        try {
            Sheet sheet = wb.getSheet("results");
            assertNotNull("sheet 'results' must exist", sheet);

            Row header = sheet.getRow(0);
            int gesamtHeaderCol = -1;
            for (int i = 0; i < header.getLastCellNum(); i++) {
                Cell c = header.getCell(i);
                if (c != null && c.getCellType() == CellType.STRING && "Gesamt".equals(c.getStringCellValue())) {
                    gesamtHeaderCol = i;
                    break;
                }
            }
            assertTrue("'Gesamt' header must be present", gesamtHeaderCol >= 0);

            // The bottom total row is the last row of the sheet; its 'Gesamt' sum cell
            // must sit directly below the 'Gesamt' header column.
            Row bottom = sheet.getRow(sheet.getLastRowNum());
            Cell gesamtDataCell = bottom.getCell(gesamtHeaderCol);
            assertNotNull("data cell below 'Gesamt' header (column " + gesamtHeaderCol + ") must exist", gesamtDataCell);
            assertEquals("cell below 'Gesamt' header must carry the SUM formula",
                    CellType.FORMULA, gesamtDataCell.getCellType());
            assertTrue("expected SUM formula but got: " + gesamtDataCell.getCellFormula(),
                    gesamtDataCell.getCellFormula().startsWith("SUM("));
        } finally {
            wb.close();
        }
    }

    @Test
    public void testGroupNameCellIsStringType() throws IOException {
        PowerMock.mockStatic(Helper.class);
        EasyMock.expect(Helper.getTranslation(EasyMock.anyString()))
                .andAnswer(() -> (String) EasyMock.getCurrentArguments()[0])
                .anyTimes();
        PowerMock.replay(Helper.class);

        Group group = new Group();
        group.setName("GruppeMitName");
        group.getValues().add(new Interval("ProjektA", "2024/01", 100, 5, 1.0f));
        group.getTotalValues().add(new Interval("total", "2024/01", 100, 5, 1.0f));

        Set<String> dates = new LinkedHashSet<>();
        dates.add("2024/01");

        ExcelCreator creator = new ExcelCreator(Collections.singletonList(group), dates, "Sammlungen");
        Workbook wb = creator.buildWorkbook();
        try {
            Sheet sheet = wb.getSheet("results");
            // search for a cell in column 0 holding the group name
            Cell groupNameCell = null;
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Cell c = row.getCell(0);
                if (c != null && c.getCellType() == CellType.STRING && "GruppeMitName".equals(c.getStringCellValue())) {
                    groupNameCell = c;
                    break;
                }
            }
            assertNotNull("group name cell must exist as STRING with value 'GruppeMitName'", groupNameCell);
            assertEquals("group name cell must be of STRING type", CellType.STRING, groupNameCell.getCellType());
        } finally {
            wb.close();
        }
    }

    @Test
    public void testResetStatisticsKeepsSelectedTypeValid() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();
        plugin.setSelectedType("Säulen");
        plugin.setSelectedStepName("Scannen");

        plugin.resetStatistics();

        assertEquals("resetStatistics must reset to the default type, not null",
                "Sammlungen", plugin.getSelectedType());
        assertNull(plugin.getSelectedStepName());
        assertNull(plugin.getStartDateDate());
        assertNull(plugin.getEndDateDate());
    }

    @Test
    public void testBuildSqlQueryUsesAndWhenOnlyStartDateSet() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();
        plugin.setSelectedStepName("Scannen");
        plugin.setStartDateDate(Date.valueOf("2024-01-01"));
        // endDateDate stays null

        String sql = plugin.buildSqlQuery(Collections.singletonList("ProjektA"));

        assertTrue("only-startDate SQL must chain with 'AND BearbeitungsEnde >': " + sql,
                sql.contains("AND BearbeitungsEnde > '2024-01-01'"));
        assertFalse("SQL must not contain dangling 'BearbeitungsStatus = 3 BearbeitungsEnde' without AND: " + sql,
                sql.matches("(?s).*BearbeitungsStatus = 3\\s+BearbeitungsEnde.*"));
    }

    @Test
    public void testBuildSqlQueryUsesAndWhenOnlyEndDateSet() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();
        plugin.setSelectedStepName("Scannen");
        plugin.setEndDateDate(Date.valueOf("2024-06-30"));
        // startDateDate stays null

        String sql = plugin.buildSqlQuery(Collections.singletonList("ProjektA"));

        assertTrue("only-endDate SQL must chain with 'AND BearbeitungsEnde <': " + sql,
                sql.contains("AND BearbeitungsEnde < '2024-06-30'"));
        assertFalse("SQL must not contain dangling 'BearbeitungsStatus = 3 BearbeitungsEnde' without AND: " + sql,
                sql.matches("(?s).*BearbeitungsStatus = 3\\s+BearbeitungsEnde.*"));
    }

    @Test
    public void testBuildSqlQueryUsesBetweenWhenBothDatesSet() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();
        plugin.setSelectedStepName("Scannen");
        plugin.setStartDateDate(Date.valueOf("2024-01-01"));
        plugin.setEndDateDate(Date.valueOf("2024-06-30"));

        String sql = plugin.buildSqlQuery(Collections.singletonList("ProjektA"));

        assertTrue("both-dates SQL must contain BETWEEN clause: " + sql,
                sql.contains("AND BearbeitungsEnde between '2024-01-01' and '2024-06-30'"));
    }

    @Test
    public void testBuildSqlQueryOmitsDateClauseWhenNoDatesSet() {
        BaselStatisticsPlugin plugin = new BaselStatisticsPlugin();
        plugin.setSelectedStepName("Scannen");

        String sql = plugin.buildSqlQuery(Collections.singletonList("ProjektA"));

        // BearbeitungsEnde must only appear inside DATE_FORMAT(...) of the SELECT,
        // never as a WHERE filter (>, <, between) when neither date bound is set.
        assertFalse("SQL must not filter by BearbeitungsEnde > when no dates are set: " + sql,
                sql.contains("BearbeitungsEnde >"));
        assertFalse("SQL must not filter by BearbeitungsEnde < when no dates are set: " + sql,
                sql.contains("BearbeitungsEnde <"));
        assertFalse("SQL must not use BETWEEN when no dates are set: " + sql,
                sql.contains("BearbeitungsEnde between"));
    }
}
