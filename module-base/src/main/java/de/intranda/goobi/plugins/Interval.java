package de.intranda.goobi.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Interval {

    private final String projektTitle;

    private String date;

    private int pages;

    private int processes;

    private float percent;

}
