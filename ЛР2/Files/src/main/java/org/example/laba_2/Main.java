package org.example.laba_2;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.List;

public class Main extends Application {
    private MediaPlayer collisionSound;
    private MediaPlayer carCrashSound;
    private MediaPlayer oilSound;
    private Habitat habitat;
    private boolean isSimulationRunning = false;
    private long simulationStartTime = 0;
    private Text statsText;
    private boolean showTime = true;
    private long pauseTime = 0;
    private boolean showInfoDialog = true;

    private ToggleGroup timeToggle;
    private RadioButton showTimeRadio;
    private RadioButton hideTimeRadio;
    private Button startButton;
    private Button stopButton;
    private CheckBox showInfoCheckBox;
    private TextField carSpawnIntervalField;
    private TextField oilSpawnIntervalField;
    private ComboBox<String> carProbabilityCombo;
    private ListView<String> oilProbabilityList;
    private long lastCollisionSoundTime = 0;
    private static final long COLLISION_SOUND_COOLDOWN = 500_000_000L;

    private double CAR_SPAWN_PROBABILITY = 0.8;
    private double OIL_SPAWN_PROBABILITY = 0.4;
    private static long CAR_SPAWN_INTERVAL = 2_000_000_000L;
    private static long OIL_SPAWN_INTERVAL = 3_000_000_000L;
    private long lastCarSpawnTime = 0;
    private long lastOilSpawnTime = 0;
    private double time_counter = 0;

    @Override
    public void start(Stage stage) {
        int x_size = 870;
        int y_size = 738;
        int x_kutuzka_size = 570;
        int y_kutuzka_size = y_size;

        Image backgroundImage = new Image(getClass().getResourceAsStream("/background.png"), x_size, y_size, true, true);
        ImageView backgroundImageView = new ImageView(backgroundImage);

        BorderPane root = new BorderPane();
        Pane objectPane = new Pane();
        StackPane visualizationPane = new StackPane();
        StackPane.setAlignment(backgroundImageView, Pos.CENTER_LEFT);
        visualizationPane.getChildren().addAll(backgroundImageView, objectPane);
        root.setCenter(visualizationPane);

        // Главное меню (MenuBar)
        MenuBar menuBar = new MenuBar();

        // Меню "Файл"
        Menu fileMenu = new Menu("Файл");
        MenuItem startMenuItem = new MenuItem("Старт");
        MenuItem stopMenuItem = new MenuItem("Стоп");
        MenuItem exitMenuItem = new MenuItem("Выход");

        startMenuItem.setOnAction(e -> startSimulationFromMenu());
        stopMenuItem.setOnAction(e -> stopSimulationFromMenu());
        exitMenuItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(startMenuItem, stopMenuItem, new SeparatorMenuItem(), exitMenuItem);

        // Меню "Настройки"
        Menu settingsMenu = new Menu("Настройки");
        MenuItem spawnIntervalsMenuItem = new MenuItem("Периоды генерации...");
        MenuItem probabilitiesMenuItem = new MenuItem("Вероятности генерации...");

        spawnIntervalsMenuItem.setOnAction(e -> showSpawnIntervalsDialog());
        probabilitiesMenuItem.setOnAction(e -> showProbabilitiesDialog());

        settingsMenu.getItems().addAll(spawnIntervalsMenuItem, probabilitiesMenuItem);

        // Меню "Вид"
        Menu viewMenu = new Menu("Вид");
        CheckMenuItem showTimeMenuItem = new CheckMenuItem("Показывать время");
        showTimeMenuItem.setSelected(showTime);
        showTimeMenuItem.setOnAction(e -> toggleShowTime());

        CheckMenuItem showInfoMenuItem = new CheckMenuItem("Показывать информацию при остановке");
        showInfoMenuItem.setSelected(showInfoDialog);
        showInfoMenuItem.setOnAction(e -> toggleShowInfoDialog());

        viewMenu.getItems().addAll(showTimeMenuItem, showInfoMenuItem);

        menuBar.getMenus().addAll(fileMenu, settingsMenu, viewMenu);

        // Панель инструментов (ToolBar)
        ToolBar toolBar = new ToolBar();

        ToggleButton toolbarStartButton = new ToggleButton("▶");

        Image end_button = new Image(getClass().getResourceAsStream("/malevich_square.png"), 12, 12, true, true);
        Button toolbarEndButton = new Button();
        toolbarEndButton.setGraphic(new ImageView(end_button));

        ToggleButton toolbarShowTimeButton = new ToggleButton("⏱");
        ToggleButton toolbarShowInfoButton = new ToggleButton("ℹ");

        toolbarStartButton.setOnAction(e -> startSimulationFromMenu());
        toolbarEndButton.setOnAction(e -> stopSimulationFromMenu());
        toolbarShowTimeButton.setSelected(showTime);
        toolbarShowTimeButton.setOnAction(e -> toggleShowTime());
        toolbarShowInfoButton.setSelected(showInfoDialog);
        toolbarShowInfoButton.setOnAction(e -> toggleShowInfoDialog());

        toolBar.getItems().addAll(
                toolbarStartButton,
                toolbarEndButton,
                new Separator(),
                toolbarShowTimeButton,
                toolbarShowInfoButton
        );

        // Добавление MenuBar и ToolBar в верхнюю часть
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Правая панель управления
        VBox controlPanel = new VBox(10);
        statsText = new Text();
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");
        root.setRight(controlPanel);

        controlPanel.getChildren().add(statsText);

        habitat = Habitat.getInstance(objectPane, x_kutuzka_size, y_kutuzka_size);
        loadSounds();

        Label spawnSettingsLabel = new Label("Периоды генерации (сек):");
        carSpawnIntervalField = new TextField(String.valueOf(CAR_SPAWN_INTERVAL / 1_000_000_000L));
        oilSpawnIntervalField = new TextField(String.valueOf(OIL_SPAWN_INTERVAL / 1_000_000_000L));

        carSpawnIntervalField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                carSpawnIntervalField.setText(oldVal);
            }
        });

        oilSpawnIntervalField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                oilSpawnIntervalField.setText(oldVal);
            }
        });

        controlPanel.getChildren().addAll(
                new Separator(),
                spawnSettingsLabel,
                new HBox(5, new Label("Машины:"), carSpawnIntervalField),
                new HBox(5, new Label("Масло:"), oilSpawnIntervalField)
        );

        Label probLabel = new Label("Вероятности генерации:");
        carProbabilityCombo = new ComboBox<>(createProbabilityItems());
        oilProbabilityList = new ListView<>(createProbabilityItems());

        carProbabilityCombo.getSelectionModel().select("80%");
        carProbabilityCombo.setOnAction(e -> {
            String selected = carProbabilityCombo.getSelectionModel().getSelectedItem();
            CAR_SPAWN_PROBABILITY = Integer.parseInt(selected.replace("%", "")) / 100.0;
        });

        oilProbabilityList.getSelectionModel().select("40%");
        oilProbabilityList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            OIL_SPAWN_PROBABILITY = Integer.parseInt(newVal.replace("%", "")) / 100.0;
        });

        HBox carProbBox = new HBox(5, new Label("Машины:"), carProbabilityCombo);
        HBox oilProbBox = new HBox(5, new Label("Масло:"), oilProbabilityList);

        controlPanel.getChildren().addAll(
                new Separator(),
                probLabel,
                carProbBox,
                oilProbBox
        );

        timeToggle = new ToggleGroup();
        showTimeRadio = new RadioButton("Показывать время");
        showTimeRadio.setToggleGroup(timeToggle);
        showTimeRadio.setSelected(showTime);

        hideTimeRadio = new RadioButton("Скрывать время");
        hideTimeRadio.setToggleGroup(timeToggle);
        hideTimeRadio.setSelected(!showTime);

        timeToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            showTime = (newVal == showTimeRadio);
            statsText.setVisible(showTime);

            if (showTime && pauseTime > 0) {
                simulationStartTime = System.nanoTime() - (pauseTime - simulationStartTime);
                pauseTime = 0;
            } else if (!showTime) {
                pauseTime = System.nanoTime();
            }
        });

        controlPanel.getChildren().addAll(
                new Label("Отображение времени:"),
                showTimeRadio,
                hideTimeRadio
        );
        y_size = 800;
        Scene scene = new Scene(root, x_size, y_size);
        startButton = new Button("Старт");
        stopButton = new Button("Стоп");
        stopButton.setDisable(true);

        showInfoCheckBox = new CheckBox("Показывать информацию при остановке");
        showInfoCheckBox.setSelected(showInfoDialog);

        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case B -> {
                    if (!isSimulationRunning) {
                        startSimulation();
                        startButton.setDisable(true);
                        stopButton.setDisable(false);
                    }
                }
                case E -> {
                    if (isSimulationRunning) {
                        statsText.setVisible(true);
                        stopSimulation();
                        startButton.setDisable(false);
                        stopButton.setDisable(true);
                    }
                }
                case T -> {
                    showTime = !showTime;
                    statsText.setVisible(showTime);

                    if (showTime) {
                        if (pauseTime > 0) {
                            simulationStartTime = System.nanoTime() - (pauseTime - simulationStartTime);
                            pauseTime = 0;
                        }
                    } else {
                        pauseTime = System.nanoTime();
                    }
                    timeToggle.selectToggle(showTime ? showTimeRadio : hideTimeRadio);
                }
            }
        });

        startButton.setOnAction(e -> {
            startSimulation();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        });

        stopButton.setOnAction(e -> {
            statsText.setVisible(true);
            stopSimulation();
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });
        controlPanel.getChildren().addAll(startButton, stopButton);

        showInfoCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showInfoDialog = newVal;
        });

        controlPanel.getChildren().add(showInfoCheckBox);

        var timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isSimulationRunning) {
                    spawnCars(now);
                    spawnOil(now);
                    habitat.update();
                    checkCollisions(now);
                    updateUI(now);
                }
            }
        };
        timer.start();

        stage.setTitle("Bavarskaya pogonia za maslom");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void loadSounds() {
        try {
            Media collisionMedia = new Media(getClass().getResource("/sound.wav").toString());
            Media crashMedia = new Media(getClass().getResource("/crash.wav").toString());
            Media oilMedia = new Media(getClass().getResource("/oil_sound.wav").toString());

            collisionSound = new MediaPlayer(collisionMedia);
            carCrashSound = new MediaPlayer(crashMedia);
            oilSound = new MediaPlayer(oilMedia);
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    private void startSimulation() {
        isSimulationRunning = true;
        simulationStartTime = System.nanoTime();
        pauseTime = 0;
        time_counter = 0;
        statsText.setText("Time: 0.0 s");
        habitat.clear();
        resetStatsTextStyle();
        statsText.setVisible(showTime);
        lastCarSpawnTime = System.nanoTime();
        lastOilSpawnTime = System.nanoTime();
        if (!showTime) {
            pauseTime = simulationStartTime;
        }

        if (collisionSound != null) collisionSound.stop();
        if (carCrashSound != null) carCrashSound.stop();
        if (oilSound != null) oilSound.stop();

        for (GameObject obj : habitat.getObjects()) {
            if (obj instanceof Car) {
                ((Car) obj).initEngineSound();
            }
        }
    }

    private void stopSimulation() {
        isSimulationRunning = false;
        statsText.setVisible(false);
        if (showInfoDialog) {
            modalDialog();
        }
        if (collisionSound != null) collisionSound.stop();
        if (carCrashSound != null) carCrashSound.stop();
        if (oilSound != null) oilSound.stop();

        for (GameObject obj : habitat.getObjects()) {
            if (obj instanceof Car) {
                ((Car) obj).stopEngineSound();
            }
        }
    }

    private void modalDialog() {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Подтверждение остановки");
        dialog.setHeaderText("Статистика симуляции");

        Text statsText = new Text();
        statsText.setFill(Color.BLACK);
        statsText.setFont(Font.font("Arial", FontWeight.NORMAL, 16));

        long cars = habitat.getObjects().stream().filter(obj -> obj instanceof Car).count();
        long oils = habitat.getObjects().stream().filter(obj -> obj instanceof Oil).count();
        statsText.setText(String.format("Time: %.1f s\nCars: %d\nOils: %d",
                time_counter, cars, oils));

        VBox content = new VBox(statsText);
        content.setPadding(new Insets(10));

        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(ButtonType.OK, cancelButton);

        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (collisionSound != null) collisionSound.stop();
                if (carCrashSound != null) carCrashSound.stop();
                if (oilSound != null) oilSound.stop();

                for (GameObject obj : habitat.getObjects()) {
                    if (obj instanceof Car) {
                        ((Car) obj).stopEngineSound();
                    }
                }
            } else {
                isSimulationRunning = true;
                startButton.setDisable(true);
                stopButton.setDisable(false);
                continueSimulation();
            }
        });
    }

    private void continueSimulation() {
        isSimulationRunning = true;
        for (GameObject obj : habitat.getObjects()) {
            if (obj instanceof Car) {
                ((Car) obj).initEngineSound();
            }
        }
    }

    private void updateUI(long now) {
        if (showTime) {
            double elapsedSeconds;
            if (pauseTime > 0) {
                elapsedSeconds = (pauseTime - simulationStartTime) / 1e9;
            } else {
                elapsedSeconds = (now - simulationStartTime) / 1e9;
            }
            statsText.setText(String.format("Time: %.1f s", elapsedSeconds));
            time_counter = elapsedSeconds;
        }
    }

    private void resetStatsTextStyle() {
        statsText.setFill(Color.BLACK);
        statsText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
    }

    private void spawnCars(long now) {
        try {
            CAR_SPAWN_INTERVAL = (long)(Double.parseDouble(carSpawnIntervalField.getText()) * 1_000_000_000L);
        } catch (NumberFormatException e) {
            CAR_SPAWN_INTERVAL = 2_000_000_000L;
        }

        if (now - lastCarSpawnTime >= CAR_SPAWN_INTERVAL) {
            if (Math.random() < CAR_SPAWN_PROBABILITY) {
                habitat.spawnCar();
            }
            lastCarSpawnTime = now;
        }
    }

    private void spawnOil(long now) {
        try {
            OIL_SPAWN_INTERVAL = (long)(Double.parseDouble(oilSpawnIntervalField.getText()) * 1_000_000_000L);
        } catch (NumberFormatException e) {
            OIL_SPAWN_INTERVAL = 3_000_000_000L;
        }

        if (now - lastOilSpawnTime >= OIL_SPAWN_INTERVAL) {
            if (Math.random() < OIL_SPAWN_PROBABILITY) {
                habitat.spawnOil();
            }
            lastOilSpawnTime = now;
        }
    }

    private ObservableList<String> createProbabilityItems() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (int i = 0; i <= 10; i++) {
            items.add((i * 10) + "%");
        }
        return items;
    }

    private void checkCollisions(long now) {
        List<GameObject> objects = habitat.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                GameObject obj1 = objects.get(i);
                GameObject obj2 = objects.get(j);
                if (isColliding(obj1.getImageView(), obj2.getImageView(), 20)) {
                    if (obj1 instanceof Car && obj2 instanceof Car) {
                        playCollisionSound(carCrashSound, now);
                    } else if (obj1 instanceof Oil && obj2 instanceof Oil) {
                        playCollisionSound(oilSound, now);
                    } else {
                        playCollisionSound(collisionSound, now);
                    }
                }
            }
        }
    }

    private boolean isColliding(ImageView a, ImageView b, double distance) {
        double ax = a.getX() + a.getFitWidth() / 2;
        double ay = a.getY() + a.getFitHeight() / 2;
        double bx = b.getX() + b.getFitWidth() / 2;
        double by = b.getY() + b.getFitHeight() / 2;

        return Math.abs(ax - bx) < distance && Math.abs(ay - by) < distance;
    }

    private void playCollisionSound(MediaPlayer player, long now) {
        if (now - lastCollisionSoundTime >= COLLISION_SOUND_COOLDOWN) {
            if (player != null) {
                player.stop();
                player.seek(player.getStartTime());
                player.play();
            }
            lastCollisionSoundTime = now;
        }
    }

    // === Методы для работы с меню ===
    private void startSimulationFromMenu() {
        if (!isSimulationRunning) {
            startSimulation();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        }
    }

    private void stopSimulationFromMenu() {
        if (isSimulationRunning) {
            stopSimulation();
            startButton.setDisable(false);
            stopButton.setDisable(true);
        }
    }

    private void toggleShowTime() {
        showTime = !showTime;
        statsText.setVisible(showTime);
        if (showTime) {
            if (pauseTime > 0) {
                simulationStartTime = System.nanoTime() - (pauseTime - simulationStartTime);
                pauseTime = 0;
            }
        } else {
            pauseTime = System.nanoTime();
        }
        timeToggle.selectToggle(showTime ? showTimeRadio : hideTimeRadio);
    }

    private void toggleShowInfoDialog() {
        showInfoDialog = !showInfoDialog;
        showInfoCheckBox.setSelected(showInfoDialog);
    }

    private void showSpawnIntervalsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Настройка периодов генерации");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField carIntervalField = new TextField(String.valueOf(CAR_SPAWN_INTERVAL / 1_000_000_000L));
        TextField oilIntervalField = new TextField(String.valueOf(OIL_SPAWN_INTERVAL / 1_000_000_000L));

        grid.add(new Label("Период генерации машин (сек):"), 0, 0);
        grid.add(carIntervalField, 1, 0);
        grid.add(new Label("Период генерации масла (сек):"), 0, 1);
        grid.add(oilIntervalField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    CAR_SPAWN_INTERVAL = (long)(Double.parseDouble(carIntervalField.getText()) * 1_000_000_000L);
                    OIL_SPAWN_INTERVAL = (long)(Double.parseDouble(oilIntervalField.getText()) * 1_000_000_000L);
                    carSpawnIntervalField.setText(carIntervalField.getText());
                    oilSpawnIntervalField.setText(oilIntervalField.getText());
                } catch (NumberFormatException e) {
                    // Обработка ошибки ввода
                }
            }
        });
    }

    private void showProbabilitiesDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Настройка вероятностей");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> carProbCombo = new ComboBox<>(createProbabilityItems());
        carProbCombo.getSelectionModel().select((int)(CAR_SPAWN_PROBABILITY * 10) - 1);

        ListView<String> oilProbList = new ListView<>(createProbabilityItems());
        oilProbList.getSelectionModel().select((int)(OIL_SPAWN_PROBABILITY * 10) - 1);

        grid.add(new Label("Вероятность машин:"), 0, 0);
        grid.add(carProbCombo, 1, 0);
        grid.add(new Label("Вероятность масла:"), 0, 1);
        grid.add(oilProbList, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String carProb = carProbCombo.getSelectionModel().getSelectedItem();
                CAR_SPAWN_PROBABILITY = Integer.parseInt(carProb.replace("%", "")) / 100.0;
                carProbabilityCombo.getSelectionModel().select(carProb);

                String oilProb = oilProbList.getSelectionModel().getSelectedItem();
                OIL_SPAWN_PROBABILITY = Integer.parseInt(oilProb.replace("%", "")) / 100.0;
                oilProbabilityList.getSelectionModel().select(oilProb);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}