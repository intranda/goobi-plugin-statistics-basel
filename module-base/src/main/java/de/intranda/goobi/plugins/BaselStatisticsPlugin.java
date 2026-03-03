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
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

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

    private List<String> stepnames;

    private Map<String, List<String>> collections;
    private Map<String, List<String>> columns;

    @Getter
    @Setter
    private String selectedStepName;

    @Getter
    @Setter
    private String selectedType;

    @Getter
    private String[] possibleTypes = { "Sammlungen", "Säulen" };

    @Getter
    private List<Group> resultList;

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
    }

    @Override
    public void calculate() {
        resultList = new ArrayList<>();
        if (StringUtils.isBlank(selectedStepName) && StringUtils.isBlank(selectedType)) {
            // abort, nothing selected
            // TODO show error message
            return;
        }

        if ("Sammlungen".equals(selectedType)) {
            for (String col : getCollections().keySet()) {
                List<String> projects = collections.get(col);

                Group group = getValuesFromDatabase(projects);
                group.setName(col);
                resultList.add(group);
            }
        } else if ("Säulen".equals(selectedType)) {
            for (String col : getColumns().keySet()) {
                List<String> projects = columns.get(col);
                Group group = getValuesFromDatabase(projects);
                group.setName(col);
                resultList.add(group);
            }
        } else {
            // TODO error, nothing selected
        }

        // TODO do something with data set
    }

    private Group getValuesFromDatabase(List<String> projects) {
        StringBuilder projectString = new StringBuilder();
        for (String projectName : projects) {
            if (!projectString.isEmpty()) {
                projectString.append(",");
            }
            projectString.append("'").append(projectName).append("'");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT SUM(sortHelperImages) as pages, COUNT(sortHelperImages) as processes, ");
        sql.append("CONCAT(YEAR(BearbeitungsEnde), '/', MONTH(BearbeitungsEnde)) AS finishDate ");
        sql.append("FROM schritte LEFT JOIN prozesse ON schritte.ProzesseID = prozesse.ProzesseID ");
        sql.append("LEFT JOIN projekte ON prozesse.ProjekteID = projekte.ProjekteID ");
        sql.append("WHERE schritte.titel = '").append(selectedStepName).append("' ");
        sql.append("AND BearbeitungsStatus = 3 ");
        if (startDateDate != null && endDateDate != null) {
            sql.append("AND  BearbeitungsEnde between '")
                    .append(dateFormat.format(startDateDate))
                    .append("' and '" + dateFormat.format(endDateDate))
                    .append("' ");
        } else if (startDateDate != null) {
            sql.append("BearbeitungsEnde > '").append(dateFormat.format(startDateDate)).append("' ");
        } else if (endDateDate != null) {
            sql.append("BearbeitungsEnde < '").append(dateFormat.format(endDateDate)).append("' ");
        }
        sql.append("AND projekte.titel IN ( ").append(projectString.toString()).append(") ");
        sql.append("GROUP BY finishDate ");

        @SuppressWarnings("unchecked")
        List<Object> results = ProcessManager.runSQL(sql.toString());

        Group group = new Group();

        for (Object rowObj : results) {
            Object[] row = (Object[]) rowObj;
            String pages = (String) row[0];
            String processes = (String) row[1];
            String date = (String) row[2];

            //  System.out.println(date + ": " + pages + " " + processes);

            Interval interval = new Interval(date, Integer.parseInt(pages), Integer.parseInt(processes));
            group.getValues().add(interval);
        }

        return group;
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
        selectedStepName = null;
        startDateDate = null;
        endDateDate = null;
        selectedType = null;
    }

    /**
     * public method to allow the export of the entire dataset as Excel file
     */
    public void generateExcelDownload() {

        Workbook wb = new XSSFWorkbook();
        //        Sheet sheet = wb.createSheet("results");
        //
        //        // create header
        //        Row headerRow = sheet.createRow(0);
        //        int columnCounter = 0;
        //        for (String headerName : myHeaders) {
        //            headerRow.createCell(columnCounter).setCellValue(Helper.getTranslation(headerName));
        //            columnCounter = columnCounter + 1;
        //        }
        //
        //        // add results
        //        int rowCounter = 1;
        //        for (Map<String, String> result : myResults) {
        //            Row resultRow = sheet.createRow(rowCounter);
        //            columnCounter = 0;
        //            for (String headerName : myHeaders) {
        //                String val = result.get(headerName);
        //                if (StringUtils.isNumeric(val)) {
        //                    resultRow.createCell(columnCounter, CellType.NUMERIC).setCellValue(Integer.parseInt(val));
        //                } else {
        //                    resultRow.createCell(columnCounter).setCellValue(val);
        //                }
        //                columnCounter++;
        //            }
        //            rowCounter++;
        //        }

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

}
