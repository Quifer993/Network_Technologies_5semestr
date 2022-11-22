package ru.nsu.zolotorevskii.networks.lab3;

import javafx.fxml.FXML;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import ru.nsu.zolotorevskii.networks.lab3.holders.LocationHolder;
import ru.nsu.zolotorevskii.networks.lab3.other.Boxes;

public class MainController {
    private Boxes boxes;

    @FXML
    private VBox descriptionVBox;

    @FXML
    private VBox resultsVBox;

    @FXML
    private Label weatherLabel;

    @FXML
    private TextField inputString;


    @FXML
    void searchRequest(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER) && !inputString.getText().isEmpty()) {
            this.resultsVBox.getChildren().clear();
            this.weatherLabel.setText("");
            this.descriptionVBox.getChildren().clear();
            this.resultsVBox.getChildren().add(new Label("Searching..."));
            new LocationHolder(this.boxes, this.inputString.getText()).startWork();
        }
    }

    @FXML
    void initialize() {
        assert this.descriptionVBox != null : "fx:id=\"descriptionVBox\" was not injected: check your FXML file 'main-view.fxml'.";
        assert this.resultsVBox != null : "fx:id=\"resultsVBox\" was not injected: check your FXML file 'main-view.fxml'.";
        assert this.inputString != null : "fx:id=\"userInput\" was not injected: check your FXML file 'main-view.fxml'.";
        assert this.weatherLabel != null : "fx:id=\"weatherLabel\" was not injected: check your FXML file 'main-view.fxml'.";

        this.boxes = new Boxes(this.descriptionVBox, this.resultsVBox, this.weatherLabel);
    }
}
