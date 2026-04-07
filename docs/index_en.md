---
title: Basel Statistics
identifier: intranda_statistics_basel
description: Statistics plugin for cross-project evaluation of digitisation output grouped by collections or thematic columns
published: false
keywords:
    - Goobi workflow
    - Plugin
    - Statistics Plugin
---

## Introduction
This statistics plugin enables the evaluation of completed workflow steps across multiple Goobi projects. Results are grouped according to configurable collections or thematic columns and displayed as a table showing the number of digitised pages and the percentage share for each month. The results can also be downloaded as an Excel file.


## Installation
To use the plugin, the following files must be installed:

```bash
/opt/digiverso/goobi/plugins/statistics/plugin-statistics-basel-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-statistics-basel-gui.jar
/opt/digiverso/goobi/config/plugin_intranda_statistics_basel.xml
```

To use this plugin, the user must have the correct role permission. The following table provides an overview of the required permissions:

| Permission | Description |
|---|---|
| `Plugin_statistics_basel` | Grants access to the Basel statistics plugin and enables cross-project evaluation of digitisation output. |

To assign the role to a user group, open the Goobi administration interface and navigate to `Administration` > `User groups`. Select the desired user group or create a new one and add the role `Plugin_statistics_basel` to the roles field of the group.

<!-- SCREENSHOT 2 (screen2_en.png): Goobi interface in the user groups section, showing the role "Plugin_statistics_basel" assigned to a group. Visible is the edit mask of a user group with the role entered in the roles list. -->
![Correctly assigned role for users](screen2_en.png)

## Overview and Functionality
Once the plugin has been correctly installed and configured, it can be found under the `Statistics` menu item.

The plugin allows any workflow step to be evaluated across multiple projects. The projects are grouped together using the configuration file, either as **Collections** (thematically related holdings) or as **Columns** (organisationally or thematically defined digitisation programmes).

After calculation, the plugin displays a table whose columns represent the individual months of the evaluated period. Each group is shown as a bold row, with the individual projects of the group listed below it. For each month and each group or project, the number of pages and the percentage share of the total volume are shown. At the end of the table there is a bold total row with the summed values of all groups.

<!-- SCREENSHOT 3 (screen3_en.png): Plugin interface showing the filter section at the top and a calculated results table below. The filter section shows the fields "Step", "Period from", "Period to", and the radio button selection between "Sammlungen" and "Säulen". The table below shows groups (bold) with their sub-projects and monthly page counts and percentage values in the columns. At the end of the table a bold "Gesamt" (Total) row is visible. Below the table there is a "Download Excel" button. -->
![User interface of the plugin](screen3_en.png)

## Configuration
The plugin is configured in the file `plugin_intranda_statistics_basel.xml` as shown here:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config_plugin>
    <category name="Sammlungen">
        <group name="Group 1">
            <project>Project A</project>
            <project>Project B</project>
            <project>Project C</project>
        </group>
        <group name="Group 2">
            <project>Project D</project>
            <project>Project E</project>
            <project>Project F</project>
        </group>
        <group name="Group 3">
            <project>Project G</project>
            <project>Project H</project>
        </group>
    </category>
    <category name="Säulen">
        <group name="Column 1">
            <project>Project A</project>
            <project>Project B</project>
            <project>Project C</project>
            <project>Project D</project>
            <project>Project E</project>
        </group>
        <group name="Column 2">
            <project>Project F</project>
            <project>Project G</project>
        </group>
        <group name="Column 3">
            <project>Project H</project>
        </group>
    </category>
</config_plugin>
```

The following table provides an overview of the parameters and their descriptions:

Parameter               | Description
------------------------|------------------------------------
`category`              | Defines a display variant. The value of the `name` attribute must be either `Sammlungen` or `Säulen`, as these are fixed in the plugin.
`group`                 | Groups several projects under a named heading. The value of the `name` attribute is displayed as the group label in the results table.
`project`               | Specifies the exact name of a Goobi project belonging to the parent group. The name must match the project name in Goobi exactly.
