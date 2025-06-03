module org.example.rgr {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.rgr to javafx.fxml;
    exports org.example.rgr;
}