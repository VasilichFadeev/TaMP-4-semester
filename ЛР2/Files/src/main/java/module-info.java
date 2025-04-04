module org.example.laba_1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires java.desktop;
    opens org.example.laba_1 to javafx.fxml;
    exports org.example.laba_1;
}