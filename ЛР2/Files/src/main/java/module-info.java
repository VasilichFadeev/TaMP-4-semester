module org.example.laba_2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires java.desktop;
    opens org.example.laba_2 to javafx.fxml;
    exports org.example.laba_2;
}