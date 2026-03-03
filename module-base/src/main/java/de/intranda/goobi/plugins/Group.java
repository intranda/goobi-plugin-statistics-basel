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

}
