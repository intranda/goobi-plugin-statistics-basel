package de.intranda.goobi.plugins;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IStatisticPlugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

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
    private final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
    private final String[] possibleTypes = { "Sammlungen", "Säulen" };

    @Getter
    private List<Group> resultList;

    @Getter
    private List<TableRow> tableRows;

    @Getter
    private List<String> tableColumns;

    @Getter
    private Map<String, String> columnHeaders;


    @Getter
    private final Set<String> dates = new HashSet<>();

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
        if (StringUtils.isBlank(selectedStepName) || StringUtils.isBlank(selectedType)) {
            // abort, nothing selected
            // TODO show error message
            String errorText = "Error: Please select one of the options: " + String.join(", ", possibleTypes) + ".";
            Helper.setFehlerMeldung(errorText);
            log.error(errorText, selectedType);
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
            String errorText = "Error: Please select one of the options: " + String.join(", ", possibleTypes) + ".";
            Helper.setFehlerMeldung(errorText);
            log.error(errorText, selectedType);
        }
        // check if data is found
        if (resultList.stream().mapToLong(group -> group.getValues().size()).sum() == 0) {
            String errorText = "Error: No data found.";
            Helper.setFehlerMeldung(errorText);
            log.error(errorText);
            resultList = null;
            return;
        }
        calculateStatistics();
        buildTableData();
    }

    private void calculateStatistics() {
        Map<Group, ListIterator<Interval>> listIteratorMap = new HashMap<>();
        boolean finished = false;

        for (Group group : resultList) {
            ListIterator<Interval> intervalListIterator = group.getValues().listIterator();
            listIteratorMap.putIfAbsent(group, intervalListIterator);
        }

        int finishedCounter = 0;
        while (!finished) {
            Interval currentSmallest = getNextSmallestDate(listIteratorMap);
            List<Interval> intervals = new LinkedList<>();
            int totalPages = 0;
            List<Interval> totalIntervals = new LinkedList<>();
            int pagesForTotalInterval = 0;
            for (Group key : listIteratorMap.keySet()) {
                Interval totalInterval = new Interval("total");
                totalInterval.setDate(currentSmallest.getDate());
                ListIterator<Interval> intervalListIterator = listIteratorMap.get(key);
                boolean foundAll = false;
                Interval interval;

                List<Interval> tempIntervals = new LinkedList<>();
                while (!foundAll) {
                    if (intervalListIterator.hasNext()) {
                        // erstelle 0 Objekte, wenn nötig
                        interval = intervalListIterator.next();
                        if (interval.getDate().compareTo(currentSmallest.getDate()) > 0) {
                            intervalListIterator.previous();
                            foundAll = true;
                        } else {
                            // addiere Objekte auf
                            tempIntervals.add(interval);
                            totalPages += interval.getPages();
                            totalInterval.setPages(totalInterval.getPages() + interval.getPages());
                            totalInterval.setProcesses(totalInterval.getProcesses() + interval.getProcesses());
                        }
                    } else {
                        finishedCounter++;
                        if (finishedCounter == listIteratorMap.size()) {
                            finished = true;
                        }
                        foundAll = true;
                    }
                }
                addEmptyObjectToMissingProjects(key, tempIntervals, intervalListIterator, currentSmallest);
                totalIntervals.add(totalInterval);
                pagesForTotalInterval += totalInterval.getPages();
                key.getTotalValues().add(totalInterval);
                intervals.addAll(tempIntervals);
                if (finished) {
                    break;
                }
            }

            for (Interval totalInterval : totalIntervals) {
                totalInterval.setPercent(totalInterval.getPages() / (float) pagesForTotalInterval);
            }
            for (Interval interval : intervals) {
                interval.setPercent(interval.getPages() / (float) totalPages);
            }
            // store dates for a more efficient excel export
            dates.add(currentSmallest.getDate());
            finishedCounter = 0;
        }
    }

    private void addEmptyObjectToMissingProjects(Group key, List<Interval> tempIntervals, ListIterator<Interval> intervalListIterator,
            Interval currentSmallest) {
        List<String> projectNames;
        if (Objects.equals(selectedType, possibleTypes[0])) {
            projectNames = getCollections().get(key.getName());
        } else {
            projectNames = getColumns().get(key.getName());
        }
        boolean foundName = false;
        for (String projectName : projectNames) {
            foundName = false;
            for (Interval tempInterval : tempIntervals) {
                if (tempInterval.getProjektTitle().equalsIgnoreCase(projectName)) {
                    foundName = true;
                    tempInterval.setProjektTitle(projectName);
                    break;
                }
            }
            if (!foundName) {
                intervalListIterator.add(new Interval(projectName, currentSmallest.getDate(), 0, 0, 0));
            }
        }
    }

    private Interval getNextSmallestDate(Map<Group, ListIterator<Interval>> listIteratorMap) {
        Interval currentInterval = null;
        Interval tempInterval = null;
        for (Group key : listIteratorMap.keySet()) {
            ListIterator<Interval> intervalListIterator = listIteratorMap.get(key);
            if (intervalListIterator.hasNext()) {
                tempInterval = intervalListIterator.next();
                intervalListIterator.previous();
            } else {
                continue;
            }
            if (currentInterval == null || currentInterval.getDate().compareTo(tempInterval.getDate()) > 0) {
                currentInterval = tempInterval;
            }
        }
        return currentInterval;
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
        sql.append("SELECT projekte.titel as titel, SUM(sortHelperImages) as pages, COUNT(sortHelperImages) as processes, ");
        sql.append("DATE_FORMAT(BearbeitungsEnde, '%Y/%m') AS finishDate ");
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
        sql.append("GROUP BY titel, finishDate ");
        sql.append("ORDER BY finishDate");

        @SuppressWarnings("unchecked")
        List<Object> results = ProcessManager.runSQL(sql.toString());

        Group group = new Group();

        for (Object rowObj : results) {
            Object[] row = (Object[]) rowObj;
            String projectTitle = (String) row[0];
            String pages = (String) row[1];
            String processes = (String) row[2];
            String date = (String) row[3];

            //  System.out.println(date + ": " + pages + " " + processes);

            Interval interval = new Interval(projectTitle, date, Integer.parseInt(pages), Integer.parseInt(processes), 0);
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

    private void buildTableData() {
        tableRows = new ArrayList<>();
        tableColumns = new ArrayList<>();
        columnHeaders = new LinkedHashMap<>();

        List<String> sortedDatesList = dates.stream().sorted().toList();

        // Build column keys and headers for each month
        for (String date : sortedDatesList) {
            String[] parts = date.split("/");
            String monthLabel = Month.of(Integer.parseInt(parts[1])).getDisplayName(TextStyle.FULL, Locale.GERMAN) + " " + parts[0];
            tableColumns.add(date + "_pages");
            columnHeaders.put(date + "_pages", monthLabel);
            tableColumns.add(date + "_pct");
            columnHeaders.put(date + "_pct", "%");
        }
        tableColumns.add("gesamt_pages");
        columnHeaders.put("gesamt_pages", "Gesamt");
        tableColumns.add("gesamt_pct");
        columnHeaders.put("gesamt_pct", "%");



        int grandTotal = resultList.stream()
                .flatMap(g -> g.getTotalValues().stream())
                .mapToInt(Interval::getPages)
                .sum();

        // Track column totals for bottom Gesamt row
        Map<String, Integer> dateColumnTotals = new LinkedHashMap<>();
        for (String date : sortedDatesList) {
            dateColumnTotals.put(date, 0);
        }

        boolean firstGroup = true;
        for (Group group : resultList) {
            if (!firstGroup) {
                TableRow separatorRow = new TableRow();
                separatorRow.setSeparator(true);
                tableRows.add(separatorRow);
            }
            firstGroup = false;

            // Group total row (bold)
            TableRow groupRow = new TableRow();
            groupRow.setGroupLabel(group.getName());
            groupRow.setBold(true);
            int groupTotal = 0;
            for (Interval totalInterval : group.getTotalValues()) {
                groupRow.getCells().put(totalInterval.getDate() + "_pages", formatNumber(totalInterval.getPages()));
                groupRow.getCells().put(totalInterval.getDate() + "_pct", formatPercent(totalInterval.getPercent()));
                groupTotal += totalInterval.getPages();
                dateColumnTotals.merge(totalInterval.getDate(), totalInterval.getPages(), Integer::sum);
            }
            groupRow.getCells().put("gesamt_pages", formatNumber(groupTotal));
            groupRow.getCells().put("gesamt_pct", formatPercent(grandTotal > 0 ? (float) groupTotal / grandTotal : 0));
            tableRows.add(groupRow);

            // Project sub-rows
            Map<String, List<Interval>> byProject = group.getValues().stream()
                    .collect(Collectors.groupingBy(Interval::getProjektTitle, TreeMap::new, Collectors.toList()));
            for (Map.Entry<String, List<Interval>> entry : byProject.entrySet()) {
                TableRow projectRow = new TableRow();
                projectRow.setProjectLabel(entry.getKey());
                int projectTotal = 0;
                for (Interval interval : entry.getValue()) {
                    projectRow.getCells().put(interval.getDate() + "_pages", formatNumber(interval.getPages()));
                    projectRow.getCells().put(interval.getDate() + "_pct", formatPercent(interval.getPercent()));
                    projectTotal += interval.getPages();
                }
                projectRow.getCells().put("gesamt_pages", formatNumber(projectTotal));
                projectRow.getCells().put("gesamt_pct", formatPercent(grandTotal > 0 ? (float) projectTotal / grandTotal : 0));
                tableRows.add(projectRow);
            }
        }

        // Bottom Gesamt row
        TableRow bottomSeparator = new TableRow();
        bottomSeparator.setSeparator(true);
        tableRows.add(bottomSeparator);
        TableRow gesamtRow = new TableRow();
        gesamtRow.setGroupLabel("Gesamt");
        gesamtRow.setBold(true);
        for (String date : sortedDatesList) {
            int colTotal = dateColumnTotals.getOrDefault(date, 0);
            gesamtRow.getCells().put(date + "_pages", formatNumber(colTotal));
            gesamtRow.getCells().put(date + "_pct", "100%");
        }
        gesamtRow.getCells().put("gesamt_pages", formatNumber(grandTotal));
        gesamtRow.getCells().put("gesamt_pct", "100%");
        tableRows.add(gesamtRow);
    }

    private String formatNumber(int value) {
        return String.format(Locale.GERMAN, "%,d", value);
    }

    private String formatPercent(float value) {
        return String.format(Locale.GERMAN, "%.1f%%", value * 100);
    }

    /**
     * public method to reset the calculated statistics again
     */
    public void resetStatistics() {
        selectedStepName = null;
        startDateDate = null;
        endDateDate = null;
        selectedType = null;
        tableRows = null;
        tableColumns = null;
        columnHeaders = null;
    }

    /**
     * public method to allow the export of the entire dataset as Excel file
     */
    public void generateExcelDownload() {
        new ExcelCreator(resultList, dates, selectedType).execute();
    }
}
