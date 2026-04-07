---
title: Basel Statistiken
identifier: intranda_statistics_basel
description: Statistik-Plugin zur projektübergreifenden Auswertung von Digitalisierungsleistungen nach Sammlungen oder thematischen Säulen
published: false
keywords:
    - Goobi workflow
    - Plugin
    - Statistics Plugin
---

## Einführung
Dieses Statistik-Plugin ermöglicht die Auswertung von abgeschlossenen Arbeitsschritten über mehrere Goobi-Projekte hinweg. Die Ergebnisse werden gruppiert nach konfigurierbaren Sammlungen oder thematischen Säulen als Tabelle dargestellt, die für jeden Monat die Anzahl der digitalisierten Seiten und den prozentualen Anteil ausweist. Die Ergebnisse können zusätzlich als Excel-Datei heruntergeladen werden.


## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
/opt/digiverso/goobi/plugins/statistics/plugin-statistics-basel-base.jar
/opt/digiverso/goobi/plugins/GUI/plugin-statistics-basel-gui.jar
/opt/digiverso/goobi/config/plugin_intranda_statistics_basel.xml
```

Für eine Nutzung dieses Plugins muss der Nutzer über die korrekte Rollenberechtigung verfügen. Die folgende Tabelle gibt einen Überblick über die benötigten Berechtigungen:

| Berechtigung | Beschreibung |
|---|---|
| `Plugin_statistics_basel` | Ermöglicht den Zugriff auf das Basel-Statistik-Plugin und die Auswertung projektübergreifender Digitalisierungsleistungen. |

Um die Rolle einer Nutzergruppe zuzuweisen, öffnen Sie die Goobi-Verwaltungsoberfläche und navigieren Sie zu `Administration` > `Nutzergruppen`. Wählen Sie die gewünschte Nutzergruppe aus oder legen Sie eine neue an und fügen Sie die Rolle `Plugin_statistics_basel` in das Rollenfeld der Gruppe ein.

<!-- SCREENSHOT 2 (screen2_de.png): Goobi-Oberfläche im Bereich Nutzergruppen, in der die Rolle "Plugin_statistics_basel" einer Gruppe zugewiesen ist. Zu sehen ist die Bearbeitungsmaske einer Nutzergruppe mit der eingetragenen Rolle in der Rollenliste. -->
![Korrekt zugewiesene Rolle für die Nutzer](screen2_de.png)

## Überblick und Funktionsweise
Wenn das Plugin korrekt installiert und konfiguriert wurde, ist es innerhalb des Menüpunkts `Statistiken` zu finden.

Das Plugin ermöglicht die Auswertung eines beliebigen Arbeitsschritts über mehrere Projekte hinweg. Dabei werden die Projekte anhand der Konfigurationsdatei zu Gruppen zusammengefasst, die entweder als **Sammlungen** oder als **Säulen** dargestellt werden.

Nach der Berechnung zeigt das Plugin eine Tabelle an, deren Spalten die einzelnen Monate des ausgewerteten Zeitraums darstellen. Jede Gruppe wird als fett gedruckte Zeile angezeigt, darunter sind die einzelnen Projekte der Gruppe aufgeführt. Für jeden Monat und jede Gruppe bzw. jedes Projekt werden die Seitenanzahl sowie der prozentuale Anteil am Gesamtvolumen ausgewiesen. Am Ende der Tabelle befindet sich eine fettgedruckte Gesamtzeile mit den summierten Werten aller Gruppen.

<!-- SCREENSHOT 3 (screen3_de.png): Plugin-Oberfläche mit dem Auswahlbereich oben und einer berechneten Ergebnistabelle darunter. Im Auswahlbereich sind die Felder "Schritt", "Zeitraum von", "Zeitraum bis" sowie die Auswahlmöglichkeit zwischen "Sammlungen" und "Säulen" zu sehen. Die Tabelle darunter zeigt Gruppen (fett) mit ihren Unterprojekten sowie monatliche Seitenzahlen und Prozentwerte in den Spalten. Am Ende der Tabelle ist eine fett gedruckte "Gesamt"-Zeile sichtbar. Unterhalb der Tabelle befindet sich ein Button "Excel herunterladen". -->
![Nutzeroberfläche des Plugins](screen3_de.png)

## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_statistics_basel.xml` wie hier aufgezeigt:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config_plugin>
    <category name="Sammlungen">
        <group name="Gruppe 1">
            <project>Projekt A</project>
            <project>Projekt B</project>
            <project>Projekt C</project>
        </group>
        <group name="Gruppe 2">
            <project>Projekt D</project>
            <project>Projekt E</project>
            <project>Projekt F</project>
        </group>
        <group name="Gruppe 3">
            <project>Projekt G</project>
            <project>Projekt H</project>
        </group>
    </category>
    <category name="Säulen">
        <group name="Säule 1">
            <project>Projekt A</project>
            <project>Projekt B</project>
            <project>Projekt C</project>
            <project>Projekt D</project>
            <project>Projekt E</project>
        </group>
        <group name="Säule 2">
            <project>Projekt F</project>
            <project>Projekt G</project>
        </group>
        <group name="Säule 3">
            <project>Projekt H</project>
        </group>
    </category>
</config_plugin>
```

Die folgende Tabelle enthält eine Zusammenstellung der Parameter und ihrer Beschreibungen:

Parameter               | Erläuterung
------------------------|------------------------------------
`category`              | Definiert eine Anzeigevariante. Der Wert des Attributs `name` muss entweder `Sammlungen` oder `Säulen` lauten, da diese fest im Plugin verankert sind.
`group`                 | Fasst mehrere Projekte zu einer benannten Gruppe zusammen. Der Wert des Attributs `name` wird in der Tabelle als Gruppenbezeichnung angezeigt.
`project`               | Gibt den genauen Namen eines Goobi-Projekts an, das zur übergeordneten Gruppe gehört. Der Name muss exakt mit dem Projektnamen in Goobi übereinstimmen.
