package de.intranda.goobi.plugins;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TableRow {

    private String groupLabel = "";
    private String projectLabel = "";
    private Map<String, String> cells = new LinkedHashMap<>();
    private Boolean bold = false;
    private Boolean separator = false;

}