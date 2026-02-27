package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.statistics.hibernate.FilterHelper;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ControllingManager;
import de.sub.goobi.persistence.managers.StepManager;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

/**
 * @author steffen
 *
 */
@Log4j2
@PluginImplementation
public class BaselStatisticsPlugin implements IStatisticPlugin {

    private static final long serialVersionUID = -4521526253463061214L;
    @Getter
    private String title = "intranda_statistics_basel";
    @Getter
    private PluginType type = PluginType.Statistics;

    @Getter
    @Setter
    private String filter;
    @Getter
    @Setter
    private Date startDate;
    @Getter
    @Setter
    private Date endDate;
    @Getter
    @Setter
    private Date startDateDate;
    @Getter
    @Setter
    private Date endDateDate;
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    @Getter
    private List<Map<String, String>> resultList;
    private List<String> headerList = new ArrayList<>();

    private List<String> stepnames;

    private Map<String, List<String>> collections;
    private Map<String, List<String>> columns;

    public List<String> getStepnames() {
        if (stepnames == null || stepnames.isEmpty()) {
            stepnames = StepManager.getDistinctStepTitles();
        }
        return stepnames;
    }

    public Map<String, List<String>> getCollections() {
        if (collections == null) {
            // first visit, load step names from database, load configuration
            loadConfiguration();
        }
        return collections;
    }

    public Map<String, List<String>> getColumns() {
        if (columns == null) {
            // first visit, load step names from database, load configuration
            loadConfiguration();
        }
        return columns;
    }

    private void loadConfiguration() {

        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());

        collections = new LinkedHashMap<>();

        List<HierarchicalConfiguration> configuredCollections = config.configurationsAt("//category[@name='Sammlungen']/group");
        for (HierarchicalConfiguration group : configuredCollections) {
            String groupName = group.getString("@name");
            List<String> projects = Arrays.asList(group.getStringArray("/project"));
            collections.put(groupName, projects);
        }

        columns = new LinkedHashMap<>();
        List<HierarchicalConfiguration> configuredColumns = config.configurationsAt("//category[@name='Säulen']/group");
        for (HierarchicalConfiguration group : configuredColumns) {
            String groupName = group.getString("@name");
            List<String> projects = Arrays.asList(group.getStringArray("/project"));
            columns.put(groupName, projects);
        }
    }

    @Override
    public String getGui() {
        return "/uii/plugin_statistics_basel.xhtml";
    }

    /**
     * Generate a list of headers for easier request of specific columns
     */
    public BaselStatisticsPlugin() {
        headerList.add("PROZESSEID");
        headerList.add("TITEL");
        headerList.add("SORTHELPERIMAGES");
        headerList.add("SORTHELPERDOCSTRUCTS");
        headerList.add("SORTHELPERMETADATA");
    }

    @Override
    public void calculate() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT PROZESSEID, TITEL,SORTHELPERIMAGES, SORTHELPERDOCSTRUCTS, SORTHELPERMETADATA FROM prozesse ");

        String subquery = FilterHelper.criteriaBuilder(filter, false, null, null, null, true, false);
        if (StringUtils.isNotBlank(subquery)) {
            sql.append(subquery);
        }

        if (startDateDate != null && endDateDate != null) {
            if (sql.toString().endsWith("FROM PROZESSE ")) {
                sql.append("WHERE ");
            } else {
                sql.append("AND ");
            }
            sql.append("erstellungsdatum between '" + dateFormat.format(startDateDate) + "' and '" + dateFormat.format(endDateDate) + "' ");
        } else if (startDateDate != null) {
            if (sql.toString().endsWith("FROM PROZESSE ")) {
                sql.append("WHERE ");
            } else {
                sql.append("AND ");
            }
            sql.append("ERSTELLUNGSDATUM > '" + dateFormat.format(startDateDate) + "' ");
        } else if (endDateDate != null) {
            if (sql.toString().endsWith("FROM PROZESSE ")) {
                sql.append("WHERE ");
            } else {
                sql.append("AND ");
            }
            sql.append("ERSTELLUNGSDATUM < '" + dateFormat.format(endDateDate) + "' ");
        }
        sql.append(";");
        resultList = ControllingManager.getResultsAsMaps(sql.toString());
    }

    @Override
    public boolean getPermissions() {
        return true;
    }

    @Override
    public String getData() {
        return null;
    }

    /**
     * public method to reset the calculated statistics again
     */
    public void resetStatistics() {
        resultList = null;
        startDateDate = null;
        endDateDate = null;
    }

    /**
     * public method to allow the export of the entire dataset as Excel file
     */
    public void generateExcelDownload() {
        List<Map<String, String>> myResults = null;
        List<String> myHeaders = null;
        if (resultList != null && !resultList.isEmpty()) {
            myResults = resultList;
            myHeaders = headerList;
        } else {
            Helper.setMeldung("No results to export.");
            return;
        }
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("results");

        // create header
        Row headerRow = sheet.createRow(0);
        int columnCounter = 0;
        for (String headerName : myHeaders) {
            headerRow.createCell(columnCounter).setCellValue(Helper.getTranslation(headerName));
            columnCounter = columnCounter + 1;
        }

        // add results
        int rowCounter = 1;
        for (Map<String, String> result : myResults) {
            Row resultRow = sheet.createRow(rowCounter);
            columnCounter = 0;
            for (String headerName : myHeaders) {
                String val = result.get(headerName);
                if (StringUtils.isNumeric(val)) {
                    resultRow.createCell(columnCounter, CellType.NUMERIC).setCellValue(Integer.parseInt(val));
                } else {
                    resultRow.createCell(columnCounter).setCellValue(val);
                }
                columnCounter++;
            }
            rowCounter++;
        }

        // write result into output stream
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        OutputStream out;
        try {
            out = response.getOutputStream();
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename=\"report.xlsx\"");
            wb.write(out);
            out.flush();
            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
        try {
            wb.close();
        } catch (IOException e) {
            log.error(e);
        }
    }

    /**
     * private method to retrieve specific information from internal result list for chart generation
     *
     * @param field String of a column insults of resultlist map
     * @return JSON String of data for ChartJs-Barchart-Diagramm
     */
    private String getChartInfo(String field) {
        String result = "";
        if (resultList != null && !resultList.isEmpty()) {
            for (Map<String, String> t : resultList) {
                result += "\"" + t.get(field) + "\", ";
            }
            if (result.endsWith(", ")) {
                result = result.substring(0, result.length() - 2);
            }
        } else {
            Helper.setMeldung("No results to export.");
            return "";
        }
        return result;
    }

    /**
     * Public method to retrieve the labels for legend
     *
     * @return get labels for legend generation
     */
    public String getChartLabels() {
        return getChartInfo("TITEL");
    }

    /**
     * Public method to retrieve the number of images per process
     *
     * @return get number of images per process
     */
    public String getChartValuesImages() {
        return getChartInfo("SORTHELPERIMAGES");
    }

    /**
     * Public method to retrieve the number of docstructs per process
     *
     * @return get number of docstructs per process
     */
    public String getChartValuesDocstructs() {
        return getChartInfo("SORTHELPERDOCSTRUCTS");
    }

    /**
     * Public method to retrieve the number of metadata per process
     *
     * @return get number of metadata per process
     */
    public String getChartValuesMetadata() {
        return getChartInfo("SORTHELPERMETADATA");
    }

}
