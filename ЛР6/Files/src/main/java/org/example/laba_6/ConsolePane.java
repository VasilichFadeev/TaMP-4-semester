package org.example.laba_6;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import java.io.*;

public class ConsolePane extends BorderPane {
    private final TextArea textArea;
    private final BufferedWriter writer;
    private final BufferedReader reader;

    public ConsolePane(InputStream in, OutputStream out) {
        // Настройка текстовой области
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setStyle("-fx-control-inner-background: #3F3F3F; " +
                "-fx-text-fill: #E0E0E0; " +
                "-fx-font-family: 'Consolas', monospace; " +
                "-fx-font-size: 14px;");
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS); // Всегда показывать вертикальный скролл
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);  // Горизонтальный скролл не нужен

        setCenter(scrollPane); // Добавляем ScrollPane вместо TextArea

        writer = new BufferedWriter(new OutputStreamWriter(out));
        reader = new BufferedReader(new InputStreamReader(in));

        // Приветственное сообщение
        showWelcomeMessage();

        // Обработка ввода
        textArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    String[] lines = textArea.getText().split("\n");
                    String lastLine = lines[lines.length - 1];
                    try {
                        textArea.appendText("\n");
                        writer.write(lastLine + "\n");
                        writer.flush();
                        textArea.setScrollTop(Double.MAX_VALUE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    event.consume();
                }
            }
        });

        // Поток для чтения ответов
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    javafx.application.Platform.runLater(() -> {
                        textArea.appendText("> " + finalLine + "\n");
                        textArea.setScrollTop(Double.MAX_VALUE);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void showWelcomeMessage() {
        String welcomeText =
                "Welcome to\n" +
                        "       ____            ____           ____      ____      ______      ______        ___          _____ \n" +
                        "__    ___/   __    ____   \\   __/       |  /     /__/   ____/ __/   ___    \\ __/   /     __/   ___  \\ \n" +
                        "_/   /       __/   /     /   /  __/    |   |/     / _/___   \\    __/   /    /   /__/   /    __/   _____/ \n" +
                        "/   /__   __/   /___/   /  __/     /|   |    /  ____/   /  __/   /___/   /__/   /___   /   /_____   \n" +
                        "\\____/      \\_______/     /___/  |____/  /______/       \\_______/    /______/   \\______/   \n\n" +
                        "Доступные команды:\n" +
                        "• Установить вероятность генерации машин <0.0-1.0 или 0%-100%>\n" +
                        "• Получить вероятность генерации машин\n" +
                        "───────────────────────────────────────────────────────\n";

        textArea.setText(welcomeText);
    }
}