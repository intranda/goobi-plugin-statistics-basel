package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Group {

    private String name;

    private List<Interval> values = new ArrayList<>();

    private List<Interval> totalValues = new ArrayList<>();

    public int getTotalPages() {
        return values.stream().mapToInt(Interval::getPages).sum();
    }

    public int getTotalProcesses() {
        return values.stream().mapToInt(Interval::getProcesses).sum();
    }
}
