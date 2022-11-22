package ru.nsu.zolotorevskii.networks.lab3.holders;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import javafx.application.Platform;
import ru.nsu.zolotorevskii.networks.lab3.other.Boxes;
import ru.nsu.zolotorevskii.networks.lab3.other.ResultButton;

import java.io.IOException;

public class LocationHolder {
    private static final String API_KEY = "3ed912c2-6fa8-4b04-8efb-91d844a107ab";

    private final Boxes boxes;
    private final String location;

    public LocationHolder(Boxes boxes, String location) {
        this.boxes = boxes;
        this.location = location;
    }

    private void addResultsOnScreen(JsonArray hits) {
        Platform.runLater(() -> {
            this.boxes.getResultsVBox().getChildren().clear();
        });

        for (JsonElement jsonElement : hits) {
            JsonObject jsonLocation = (JsonObject)jsonElement;

            double lat = Double.parseDouble(jsonLocation.getAsJsonObject("point").get("lat").toString().replaceAll("\"", ""));
            double lng = Double.parseDouble(jsonLocation.getAsJsonObject("point").get("lng").toString().replaceAll("\"", ""));

            String name = String.valueOf(jsonLocation.get("name")).replaceAll("\"", "");
            String osm_value = String.valueOf(jsonLocation.get("osm_value")).replaceAll("\"", "");
            String country = String.valueOf(jsonLocation.get("country")).replaceAll("\"", "");
            String city = String.valueOf(jsonLocation.get("city")).replaceAll("\"", "");
            String buttonName;
            if(city == "null"){
                buttonName = name + " : " + osm_value + "\nстрана: " + country;
            }
            else{
                buttonName = name + " : " + osm_value + "\nстрана: " + country + ", город: " + city;
            }
            Platform.runLater(() -> {
                this.boxes.getResultsVBox().getChildren().add(new ResultButton(buttonName, this.boxes, lat, lng));
            });
        }
    }

    public void startWork() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://graphhopper.com/api/1/geocode?q=" + location + "&limit=" + 6 + "&key=" + API_KEY )
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            JsonObject jsonObject = new Gson().fromJson(response.body().string(), JsonObject.class);
            JsonArray hits = jsonObject.getAsJsonArray("hits");
            addResultsOnScreen(hits);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
