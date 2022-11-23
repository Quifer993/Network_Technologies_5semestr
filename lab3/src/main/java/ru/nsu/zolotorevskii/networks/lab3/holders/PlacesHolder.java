package ru.nsu.zolotorevskii.networks.lab3.holders;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import ru.nsu.zolotorevskii.networks.lab3.other.Boxes;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class PlacesHolder {
    private static final String API_KEY = "5ae2e3f221c38a28845f05b675403f9a50b7edf463e6f89613aaf81e";
    private static final int RADIUS = 500;
    private final double lat;
    private final double lng;
    Boxes boxes;

    public PlacesHolder(double lat, double lng, Boxes boxes) {
        this.lat = lat;
        this.lng = lng;
        this.boxes = boxes;
    }

    public void find() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.opentripmap.com/0.1/ru/places/radius?" +
                        "lang=ru" +
                        "&lon=" + lng +
                        "&lat=" + lat +
                        "&radius=" + RADIUS +
                        "&apikey=" + API_KEY)
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            JsonObject jsonObject = new Gson().fromJson(response.body().string(), JsonObject.class);
            JsonArray features = jsonObject.getAsJsonArray("features");

            if(features != null){
                for (JsonElement element : features) {
                    JsonObject object = (JsonObject)element;
                    JsonObject properties = (JsonObject) object.get("properties");
                    String xid = String.valueOf(properties.get("xid")).replaceAll("\"","");
                    CompletableFuture.runAsync(() -> new DescriptionHolder(this.boxes).findDescription(xid));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
