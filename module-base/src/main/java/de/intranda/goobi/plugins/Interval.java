package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Interval {

    private String projektTitle;

    private String date;

    private int pages;

    private int processes;

    private float percent;

    public Interval(String projektTitle) {
        this.projektTitle = projektTitle;
    }
}
