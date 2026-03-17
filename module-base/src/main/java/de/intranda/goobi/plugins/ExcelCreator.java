package de.intranda.goobi.plugins;

import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
public class ExcelCreator {

    private final List<Group> resultList;
    private final Set<String> dates;
    private final String selectedType;

    private final Workbook wb = new XSSFWorkbook();

    private Sheet sheet;

    private final java.awt.Color colorBorder = java.awt.Color.decode("#D9D9D9");
    private final java.awt.Color colorBackgroundLight = java.awt.Color.decode("#f2f2f2");
    private final java.awt.Color colorBackgroundMedium = java.awt.Color.decode("#e8e8e8");
    private final java.awt.Color colorBackgroundDark = java.awt.Color.decode("#a6a6a6");

    private Font boldFont;

    private short percentFormat;

    private final Map<String, CellStyle> cellStyleMap = new HashMap<>();

    private boolean backgroundColorToggle = true;


    private void initWorkbook() {
        boldFont = wb.createFont();
        boldFont.setBold(true);
        percentFormat = wb.createDataFormat().getFormat("0.00%");
    }

    public String convertSheetToHtml() {
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();

        StringBuilder html = new StringBuilder();
        html.append("<table border='1'>");

        for (Row row : sheet) {
            html.append("<tr>");

            for (Cell cell : row) {
                html.append("<td>");

                CellType type = cell.getCellType();

                if (type == CellType.FORMULA) {
                    CellValue value = evaluator.evaluate(cell);

                    switch (value.getCellType()) {
                        case STRING:
                            html.append(value.getStringValue());
                            break;
                        case NUMERIC:
                            html.append(value.getNumberValue());
                            break;
                        case BOOLEAN:
                            html.append(value.getBooleanValue());
                            break;
                        default:
                            html.append("");
                    }
                } else {
                    html.append(formatter.formatCellValue(cell));
                }

                html.append("</td>");
            }

            html.append("</tr>");
        }

        html.append("</table>");

        return html.toString();
    }

    public void execute() {
        // Init
        initWorkbook();
        List<CellAddress> cellsPages = new LinkedList<>();
        List<CellAddress> cellsPercent = new LinkedList<>();

        List<CellAddress> cellsPagesSums = new LinkedList<>();
        List<CellAddress> cellsPagesShow = new LinkedList<>();

        sheet = wb.createSheet("results");

        String[] myHeaders = { selectedType, "Projekte" };
        myHeaders = fillHeaders(myHeaders);

        Row headerRow = sheet.createRow(0);
        int columnCounter = 0;

        Cell currentCell;

        // Set Header Row
        for (String headerName : myHeaders) {
            if (headerName.isEmpty()) {
                continue;
            }
            currentCell = headerRow.createCell(columnCounter);
            currentCell.setCellValue(Helper.getTranslation(headerName));
            if (currentCell.getColumnIndex() < 2) {
                currentCell.setCellStyle(buildCellStyle(null, true, false, false));
            } else {
                currentCell.setCellStyle(buildCellStyle(getBackgroundColor(), true, false, true));
                if (headerName.contains("%")) {
                    toggleColor();
                }
            }
            if (headerName.length() > 8) {
                sheet.autoSizeColumn(currentCell.getColumnIndex());
            }
            columnCounter++;
        }

        // add results
        int rowCounter = 1;
        columnCounter = 0;

        Map<String, List<CellAddress>> cellsTotalPages = new HashMap<>();
        resetColorToggle();
        rowCounter = createEmptyRow(sheet, rowCounter);
        for (Group group : resultList) {
            Row resultRow = sheet.createRow(rowCounter++);
            createTotalRow(group, resultRow, cellsPages, cellsPercent, cellsTotalPages, cellsPagesSums);

            Map<String, List<Interval>> intervalMap =
                    group.getValues().stream().sorted(Comparator.comparing(Interval::getProjektTitle)).collect(Collectors.groupingBy(
                            Interval::getProjektTitle,
                            TreeMap::new,
                            Collectors.toList()
                    ));

            rowCounter = createSubRows(intervalMap, sheet, rowCounter, cellsPages, cellsPercent, cellsPagesShow);

            rowCounter = createEmptyRow(sheet, rowCounter);
        }

        Row totalRow = sheet.createRow(rowCounter);
        List<CellAddress> totalAddresses = new ArrayList<>();
        columnCounter = createTotalRowBottom(totalRow, columnCounter, cellsTotalPages, totalAddresses);

        cellsPagesShow.addAll(cellsPagesSums);

        // add Formular for total percentages
        createTotalPercentagesPerRow(cellsPagesShow, sheet, columnCounter, cellsPagesSums);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

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

    private void resetColorToggle() {
        backgroundColorToggle = true;
    }

    private java.awt.Color getBackgroundColor() {
        return backgroundColorToggle ? colorBackgroundLight : colorBackgroundMedium;
    }

    private void toggleColor() {
        backgroundColorToggle = !backgroundColorToggle;
    }

    private CellStyle buildCellStyle(java.awt.Color color, boolean bold, boolean percent, boolean rightAlign) {
        String key = (color != null ? color.toString() : "NoColor") + (bold ? "bold" : "noBold") + (percent ? "percent" : "noPercent") + (rightAlign ? "rightAlign" : "leftAlign");
        if (!cellStyleMap.containsKey(key)) {
            CellStyle style = wb.createCellStyle();
            if (color != null) {
                style.setFillForegroundColor(new XSSFColor(color, null));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (bold) {
                style.setFont(boldFont);
            }
            if (percent) {
                style.setDataFormat(percentFormat);
            }
            if (rightAlign) {
                style.setAlignment(HorizontalAlignment.RIGHT);
            }
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cellStyleMap.put(key, style);
        }

        return cellStyleMap.get(key);
    }

    private void createTotalPercentagesPerRow(List<CellAddress> cellsPagesShow, Sheet sheet, int columnCounter,
            List<CellAddress> cellsPagesSums) {
        Cell lastCell = null;
        Cell currentCell;
        for (CellAddress cellsPagesSum : cellsPagesShow) {
            // if no cell above -> this one has to be bold
            Row row = sheet.getRow(cellsPagesSum.getRow());
            currentCell = row.createCell(columnCounter, CellType.FORMULA);
            currentCell.setCellFormula(
                    row.getCell(currentCell.getAddress().getColumn() - 1).getAddress().toString() + "/SUM(" + cellsPagesSums.stream()
                            .map(CellAddress::toString)
                            .collect(Collectors.joining(",")) + ")");

            if (lastCell == null || (currentCell.getAddress().getRow() - lastCell.getAddress().getRow()) != 1) {
                currentCell.setCellStyle(buildCellStyle(colorBackgroundMedium, true, true, false));
            } else {
                currentCell.setCellStyle(buildCellStyle(colorBackgroundMedium, false, true, false));
            }
            lastCell = currentCell;
        }
    }

    private int createTotalRowBottom(Row totalRow, int columnCounter, Map<String, List<CellAddress>> cellsTotalPages,
            List<CellAddress> totalAddresses) {
        Cell currentCell;
        currentCell = totalRow.createCell(columnCounter++, CellType.STRING);
        currentCell.setCellValue("Gesamt");
        currentCell.setCellStyle(buildCellStyle(colorBackgroundDark, true, false, false));

        currentCell = totalRow.createCell(columnCounter++, CellType.STRING);
        currentCell.setCellStyle(buildCellStyle(colorBackgroundDark, false, false, false));

        for (String key : cellsTotalPages.keySet().stream().sorted().toList()) {
            totalAddresses.add(fillCellFormulaSum(totalRow, columnCounter++, cellsTotalPages.get(key),
                    buildCellStyle(colorBackgroundDark, true, false, false)).getAddress());

            currentCell = totalRow.createCell(columnCounter++, CellType.STRING);
            currentCell.setCellStyle(buildCellStyle(colorBackgroundDark, false, false, false));
            //            currentCell = fillCellFormulaSum(totalRow, columnCounter++, cellsTotalPercent.get(key));
            //            currentCell.setCellStyle(boldPercentStyle);
        }
        columnCounter++;
        fillCellFormulaSum(totalRow, columnCounter++, totalAddresses, buildCellStyle(colorBackgroundDark, true, false, false));
        totalRow.createCell(columnCounter, CellType.FORMULA).setCellStyle(buildCellStyle(colorBackgroundDark, false, false, false));
        return columnCounter;
    }

    private int createSubRows(Map<String, List<Interval>> intervalMap, Sheet sheet, int rowCounter, List<CellAddress> cellsPages,
            List<CellAddress> cellsPercent, List<CellAddress> cellsPagesShow) {
        Cell currentCell;
        Row resultRow;
        resetColorToggle();
        for (String key : intervalMap.keySet()) {
            int columnCounter = 1;
            List<Interval> intervals = intervalMap.get(key);
            resultRow = sheet.createRow(rowCounter++);
            resultRow.createCell(columnCounter++, CellType.STRING).setCellValue(key);
            cellsPages.clear();
            cellsPercent.clear();
            for (Interval interval : intervals) {
                columnCounter =
                        createNextCell(resultRow, columnCounter, interval.getPages(), cellsPages, buildCellStyle(getBackgroundColor(), false, false, false));
                columnCounter = createNextCell(resultRow, columnCounter, interval.getPercent(), cellsPercent,
                        buildCellStyle(getBackgroundColor(), false, true, false));
                toggleColor();
            }
            columnCounter++;
            cellsPagesShow.add(
                    fillCellFormulaSum(resultRow, columnCounter, cellsPages, buildCellStyle(getBackgroundColor(), false, false, false)).getAddress());
        }
        return rowCounter;
    }

    private int createEmptyRow(Sheet sheet, int rowCounter) {

        Row resultRow;
        resetColorToggle();
        int columnCounter = 2;
        resultRow = sheet.createRow(rowCounter++);
        for (int i = 0; i < dates.size(); i++) {
            resultRow.createCell(columnCounter++, CellType.BLANK).setCellStyle(buildCellStyle(getBackgroundColor(), false, false, false));
            resultRow.createCell(columnCounter++, CellType.BLANK).setCellStyle(buildCellStyle(getBackgroundColor(), false, false, false));
            toggleColor();
        }
        columnCounter++;
        resultRow.createCell(columnCounter++, CellType.BLANK).setCellStyle(buildCellStyle(getBackgroundColor(), false, false, false));
        resultRow.createCell(columnCounter++, CellType.BLANK).setCellStyle(buildCellStyle(getBackgroundColor(), false, false, false));

        return rowCounter;
    }

    private void createTotalRow(Group group, Row resultRow, List<CellAddress> cellsPages, List<CellAddress> cellsPercent,
            Map<String, List<CellAddress>> cellsTotalPages, List<CellAddress> cellsPagesSums) {
        int columnCounter = 0;
        Cell currentCell;
        currentCell = resultRow.createCell(columnCounter++, CellType.NUMERIC);
        currentCell.setCellValue(group.getName());
        currentCell.setCellStyle(buildCellStyle(null, true, false, false));
        cellsPages.clear();
        cellsPercent.clear();
        columnCounter++;
        for (Interval totalValue : group.getTotalValues()) {
            cellsTotalPages.putIfAbsent(totalValue.getDate(), new ArrayList<>());

            currentCell = resultRow.createCell(columnCounter++, CellType.NUMERIC);
            currentCell.setCellValue((float) totalValue.getPages());
            currentCell.setCellStyle(buildCellStyle(getBackgroundColor(), true, false, false));
            cellsPages.add(currentCell.getAddress());
            cellsTotalPages.get(totalValue.getDate()).add(currentCell.getAddress());

            currentCell = resultRow.createCell(columnCounter++, CellType.NUMERIC);
            currentCell.setCellValue(totalValue.getPercent());
            currentCell.setCellStyle(buildCellStyle(getBackgroundColor(), true, true, false));
            cellsPercent.add(currentCell.getAddress());
            //                cellsTotalPercent.get(totalValue.getDate()).add(currentCell.getAddress());
            toggleColor();
        }

        columnCounter++;
        cellsPagesSums.add(
                fillCellFormulaSum(resultRow, columnCounter++, cellsPages, buildCellStyle(getBackgroundColor(), true, false, false)).getAddress());
    }

    private static int createNextCell(Row resultRow, int columnCounter, float cellValue, List<CellAddress> cellAddressList, CellStyle style) {
        Cell currentCell;
        currentCell = resultRow.createCell(columnCounter++, CellType.NUMERIC);
        currentCell.setCellValue(cellValue);
        if (style != null) {
            currentCell.setCellStyle(style);
        }
        cellAddressList.add(currentCell.getAddress());
        return columnCounter;
    }

    private static Cell fillCellFormulaSum(Row resultRow, int columnCounter, List<CellAddress> cellList, CellStyle style) {
        Cell currentCell;
        currentCell = resultRow.createCell(columnCounter, CellType.FORMULA);
        currentCell.setCellFormula("SUM(" + cellList.stream().map(CellAddress::toString).collect(Collectors.joining(",")) + ")");
        if (style != null) {
            currentCell.setCellStyle(style);
        }
        return currentCell;
    }

    private String[] fillHeaders(String[] myHeaders) {
        String[] tempHeaders = new String[myHeaders.length + dates.size() * 2 + 3];
        System.arraycopy(myHeaders, 0, tempHeaders, 0, myHeaders.length);
        int pos = myHeaders.length;

        for (String date : dates.stream().sorted().toList()) {
            String[] monthData = date.split("/");
            Month month = Month.of(Integer.parseInt(monthData[1]));
            String year = monthData[0];
            tempHeaders[pos++] = month.getDisplayName(TextStyle.FULL, Locale.GERMAN) + " " + year;
            tempHeaders[pos++] = "%";
        }

        tempHeaders[pos++] = "";
        tempHeaders[pos++] = "Gesamt";
        tempHeaders[pos] = "%";

        return tempHeaders;
    }

    public void createPivotTable() {

    }
}
