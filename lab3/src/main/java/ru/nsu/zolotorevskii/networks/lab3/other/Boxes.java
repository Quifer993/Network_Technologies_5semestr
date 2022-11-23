package ru.nsu.zolotorevskii.networks.lab3.other;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class Boxes {
    private final VBox descriptionVBox;
    private final VBox resultsVBox;
    private final Label weatherLabel;

    public Boxes(VBox descriptionVBox, VBox resultsVBox, Label weatherLabel) {
        this.descriptionVBox = descriptionVBox;
        this.resultsVBox = resultsVBox;
        this.weatherLabel = weatherLabel;
    }

    public VBox getDescriptionVBox() {
        return descriptionVBox;
    }

    public VBox getResultsVBox() {
        return resultsVBox;
    }

    public Label getWeatherLabel() {
        return weatherLabel;
    }
}
