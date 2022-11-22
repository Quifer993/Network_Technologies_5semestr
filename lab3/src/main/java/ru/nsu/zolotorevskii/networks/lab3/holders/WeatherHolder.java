package ru.nsu.zolotorevskii.networks.lab3.holders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import javafx.application.Platform;
import ru.nsu.zolotorevskii.networks.lab3.other.Boxes;

import java.io.IOException;

public class WeatherHolder implements Runnable {
    private static final String API_KEY = "17ce44509be7522c2678be31f47f6ff2";

    private final double lat;

    private final double lng;

    private final Boxes boxes;

    public WeatherHolder(double lat, double lng, Boxes context) {
        this.lat = lat;
        this.lng = lng;
        this.boxes = context;
    }

    private void addResultsOnScreen(JsonObject jsonObject) {
        JsonObject weather = (JsonObject) jsonObject.getAsJsonArray("weather").get(0);
        JsonObject main = (JsonObject) jsonObject.get("main");
        JsonObject wind = (JsonObject) jsonObject.get("wind");

        String description = String.valueOf(weather.get("description")).replaceAll("\"","");
        String temp = String.valueOf(main.get("temp")).replaceAll("\"","");
        String feelsLike = String.valueOf(main.get("feels_like")).replaceAll("\"","");
        String speed = String.valueOf(wind.get("speed")).replaceAll("\"","");

        Platform.runLater(() -> {
            this.boxes.getWeatherLabel().setText(description +
                    "\nТемпература: " + temp + "`C" +
                    "\nОщущается: " + feelsLike + "`C" +
                    "\nСкорость ветра " + speed + " м/с");
        });
    }

    @Override
    public void run() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.openweathermap.org/data/2.5/weather" +
                        "?lat=" + lat +
                        "&lon=" + lng +
                        "&appid=" + API_KEY +
                        "&units=metric" +
                        "&lang=ru")
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            JsonObject jsonObject = new Gson().fromJson(response.body().string(), JsonObject.class);
            this.addResultsOnScreen(jsonObject);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
