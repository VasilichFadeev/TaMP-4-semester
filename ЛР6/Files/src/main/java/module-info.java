module org.example.laba_6 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires java.desktop;
    opens org.example.laba_6 to javafx.fxml;
    exports org.example.laba_6;
}