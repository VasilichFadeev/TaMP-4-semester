module org.example.laba_7 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires java.desktop;
    requires java.sql;
    opens org.example.laba_7 to javafx.fxml;
    exports org.example.laba_7;
}