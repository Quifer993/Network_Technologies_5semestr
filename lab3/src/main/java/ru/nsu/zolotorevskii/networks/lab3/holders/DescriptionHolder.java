package ru.nsu.zolotorevskii.networks.lab3.holders;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import javafx.application.Platform;
import javafx.scene.control.Label;
import ru.nsu.zolotorevskii.networks.lab3.other.Boxes;

import java.io.IOException;

public class DescriptionHolder{
    private static final String API_KEY = "5ae2e3f221c38a28845f05b675403f9a50b7edf463e6f89613aaf81e";

    private final Boxes boxes;

    public DescriptionHolder(Boxes context) {
        this.boxes = context;
    }

    public void addResultsOnScreen(String name, String rate, String kinds) {
        Platform.runLater(() -> {
            this.boxes.getDescriptionVBox().getChildren().add(new Label(
                    "Имя: " + name +
                    ", уровень рейтинга : " + rate +
                    "\nтип: \n" + kinds));
        });
    }

    public void findDescription(String xid) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.opentripmap.com/0.1/ru/places/xid/" + xid + "?apikey=" + API_KEY)
                .get()
                .build();
        String name;
        String rate;
        String kinds;

        try {
            Response response = client.newCall(request).execute();
            JsonObject jsonObject = new Gson().fromJson(response.body().string(), JsonObject.class);
            name = String.valueOf(jsonObject.get("name")).replaceAll("\"","");
            rate = String.valueOf(jsonObject.get("rate")).replaceAll("\"","");
            kinds = String.valueOf(jsonObject.get("kinds")).replaceAll("\"","");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        addResultsOnScreen(name, rate, kinds);
    }
}
