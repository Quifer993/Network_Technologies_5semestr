module ru.nsu.zolotorevskii.networks.lab3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires okhttp;
    requires com.google.gson;


    opens ru.nsu.zolotorevskii.networks.lab3 to javafx.fxml;
    exports ru.nsu.zolotorevskii.networks.lab3;
}