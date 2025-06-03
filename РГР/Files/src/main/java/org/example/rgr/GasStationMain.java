package org.example.rgr;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class GasStationMain extends Application {

    private GasStationController controller;

    @Override
    public void start(Stage primaryStage) {
        // Инициализируем контроллер
        controller = new GasStationController();

        // Создаём UI вручную
        VBox root = createUI();

        // Создаём сцену
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Заправочная станция");
        primaryStage.setScene(scene);

        // Показываем диалог для ввода IP и порта
        ServerInfo serverInfo = showServerSelectionDialog(primaryStage);
        if (serverInfo == null) {
            Platform.exit();
            return;
        }

        // Устанавливаем информацию о сервере и инициализируем контроллер
        controller.setServerInfo(serverInfo.ip, serverInfo.port);
        controller.initialize();

        // Обработчик закрытия окна
        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    private VBox createUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Заголовок
        Text title = new Text("Заправочная станция");
        title.setFont(Font.font("System", 20));

        // Панель кнопок
        HBox buttonBox = new HBox(10);
        Button addCarButton = new Button("Добавить машину");
        Button refillAllButton = new Button("Пополнить все колонки");
        Button requestStateButton = new Button("Запросить состояние");
        Button addRemoteCarButton = new Button("Добавить машину удалённо");
        buttonBox.getChildren().addAll(addCarButton, refillAllButton, requestStateButton, addRemoteCarButton);

        // Область статуса
        Text statusLabel = new Text("Статус:");
        statusLabel.setFont(Font.font("System", 14));
        TextArea statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        statusArea.setPrefHeight(200);

        // Панель колонок
        Text pumpsLabel = new Text("Колонки:");
        pumpsLabel.setFont(Font.font("System", 14));
        HBox pumpsContainer = new HBox(20);
        pumpsContainer.setAlignment(Pos.CENTER);

        // Очередь машин
        Text queueLabel = new Text("Очередь машин:");
        queueLabel.setFont(Font.font("System", 14));
        ListView<String> queueList = new ListView<>();
        queueList.setPrefHeight(100);

        // Список подключённых клиентов
        Text clientsLabel = new Text("Подключенные клиенты:");
        clientsLabel.setFont(Font.font("System", 14));
        ListView<String> clientsList = new ListView<>();
        clientsList.setPrefHeight(100);

        // Добавляем элементы в корневой контейнер
        root.getChildren().addAll(
                title,
                buttonBox,
                new Separator(),
                statusLabel,
                statusArea,
                new Separator(),
                pumpsLabel,
                pumpsContainer,
                new Separator(),
                queueLabel,
                queueList,
                new Separator(),
                clientsLabel,
                clientsList
        );

        // Привязываем элементы к контроллеру (имитация FXML-инъекции)
        controller.setStatusArea(statusArea);
        controller.setPumpsContainer(pumpsContainer);
        controller.setQueueList(queueList);
        controller.setClientsList(clientsList);
        controller.setRequestStateButton(requestStateButton);
        controller.setAddRemoteCarButton(addRemoteCarButton);

        // Привязываем действия кнопок к методам контроллера
        addCarButton.setOnAction(event -> controller.addCar());
        refillAllButton.setOnAction(event -> controller.refillAll());

        return root;
    }

    private static class ServerInfo {
        String ip;
        int port;

        ServerInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    private ServerInfo showServerSelectionDialog(Stage owner) {
        Dialog<ServerInfo> dialog = new Dialog<>();
        dialog.setTitle("Подключение к серверу");
        dialog.setHeaderText("Введите IP-адрес и порт сервера");

        // Устанавливаем владельца после создания сцены
        if (owner.getScene() != null) {
            dialog.initOwner(owner);
        }

        ButtonType connectButtonType = new ButtonType("Подключиться", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipField = new TextField("127.0.0.1");
        TextField portField = new TextField("12345");
        Label statusLabel = new Label("Введите данные для проверки");

        grid.add(new Label("IP-адрес:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Порт:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(statusLabel, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Button connectButton = (Button) dialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateServerInput(ipField, portField, connectButton, statusLabel);
        });
        ipField.textProperty().addListener((obs, oldVal, newVal) -> {
            validateServerInput(ipField, portField, connectButton, statusLabel);
        });

        dialog.setResultConverter(button -> {
            if (button == connectButtonType) {
                try {
                    return new ServerInfo(ipField.getText().trim(), Integer.parseInt(portField.getText().trim()));
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Некорректный формат порта");
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private void validateServerInput(TextField ipField, TextField portField, Button connectButton, Label statusLabel) {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            connectButton.setDisable(true);
            statusLabel.setText("IP и порт обязательны");
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            if (port < 0 || port > 65535) {
                connectButton.setDisable(true);
                statusLabel.setText("Порт должен быть в диапазоне 0-65535");
                return;
            }

            new Thread(() -> {
                try (Socket testSocket = new Socket()) {
                    testSocket.connect(new InetSocketAddress(ip, port), 1000);
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        statusLabel.setText("Сервер доступен");
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        connectButton.setDisable(true);
                        statusLabel.setText("Сервер недоступен: " + e.getMessage());
                    });
                }
            }).start();
        } catch (NumberFormatException e) {
            connectButton.setDisable(true);
            statusLabel.setText("Некорректный формат порта");
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}