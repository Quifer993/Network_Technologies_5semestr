package ru.nsu.zolotorevskii.networks.lab3.other;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import ru.nsu.zolotorevskii.networks.lab3.holders.PlacesHolder;
import ru.nsu.zolotorevskii.networks.lab3.holders.WeatherHolder;

import java.util.concurrent.CompletableFuture;

public class ResultButton extends Button {
    private final double lat;
    private final double lng;
    private final Boxes boxes;

    public ResultButton(String text, Boxes boxes, double lat, double lng) {
        super(text);

        this.boxes = boxes;
        this.lat = lat;
        this.lng = lng;

        this.setPrefSize(300,60);
        this.setAlignment(Pos.BASELINE_LEFT);
        this.setOnAction(actionEvent -> {
            Platform.runLater(() -> {
                boxes.getDescriptionVBox().getChildren().clear();
                boxes.getWeatherLabel().setText("Searching....");
            });
            CompletableFuture.runAsync(new WeatherHolder(ResultButton.this.lat, ResultButton.this.lng, ResultButton.this.boxes));
            new PlacesHolder(ResultButton.this.lat, ResultButton.this.lng, this.boxes).find();
        });
    }
}
