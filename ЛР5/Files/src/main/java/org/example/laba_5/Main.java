package org.example.laba_5;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
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
    private TextField carLifetimeField;
    private TextField oilLifetimeField;
    private ComboBox<String> carProbabilityCombo;
    private ListView<String> oilProbabilityList;
    private long lastCollisionSoundTime = 0;
    private static final long COLLISION_SOUND_COOLDOWN = 500_000_000L;

    private double CAR_SPAWN_PROBABILITY = 0.8;
    private double OIL_SPAWN_PROBABILITY = 0.4;
    private static long CAR_SPAWN_INTERVAL = 2_000_000_000L;
    private static long OIL_SPAWN_INTERVAL = 3_000_000_000L;
    private static final long CAR_LIFETIME = 10_000_000_000L;
    private static final long OIL_LIFETIME = 7_000_000_000L;
    private long lastCarSpawnTime = 0;
    private long lastOilSpawnTime = 0;
    private double time_counter = 0;

    private MenuItem startMenuItem;
    private MenuItem stopMenuItem;
    private CheckMenuItem showTimeMenuItem;
    private CheckMenuItem showInfoMenuItem;
    private ToggleButton toolbarStartButton;
    private ToggleButton toolbarConsoleButton;
    private ToggleButton toolbarShowTimeButton;
    private ToggleButton toolbarShowInfoButton;

    @Override
    public void start(Stage stage) throws IOException {
        int x_size = 1000;
        int y_size = 1000;
        int x_kutuzka_size = (500);
        int y_kutuzka_size = (679);

        // Инициализация полей для жизни (продолжительности и интервалов рождения)
        carLifetimeField = new TextField(String.valueOf(CAR_LIFETIME / 1_000_000_000L));
        oilLifetimeField = new TextField(String.valueOf(OIL_LIFETIME / 1_000_000_000L));
        carSpawnIntervalField = new TextField(String.valueOf(CAR_SPAWN_INTERVAL / 1_000_000_000L));
        oilSpawnIntervalField = new TextField(String.valueOf(OIL_SPAWN_INTERVAL / 1_000_000_000L));

        // Кнопки для панели управления рождением
        TextField headerCarLifeField = new TextField(String.valueOf(CAR_LIFETIME / 1_000_000_000L));
        TextField headerOilLifeField = new TextField(String.valueOf(OIL_LIFETIME / 1_000_000_000L));
        Button headerCurrentObjectsBtn = new Button("Текущие объекты");

        // Обработка исключений в полях ввода
        headerCarLifeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                headerCarLifeField.setText(oldVal);
            }
        });

        headerOilLifeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                headerOilLifeField.setText(oldVal);
            }
        });

        headerCurrentObjectsBtn.setOnAction(e -> {
            CurrentObjectsDialog dialog = new CurrentObjectsDialog(habitat.getBirthTimeMap());
            dialog.showAndWait();
        });

        Image backgroundImage = new Image(getClass().getResourceAsStream("/background.png"), (x_size * 0.92), (y_size * 0.92), true, true);
        ImageView backgroundImageView = new ImageView(backgroundImage);

        BorderPane root = new BorderPane();
        Pane objectPane = new Pane();
        StackPane visualizationPane = new StackPane();
        StackPane.setAlignment(backgroundImageView, Pos.CENTER_LEFT);
        visualizationPane.getChildren().addAll(backgroundImageView, objectPane);
        root.setCenter(visualizationPane);

        // Меню
        MenuBar menuBar = new MenuBar();

        // Шапка
        Menu fileMenu = new Menu("Файл");
        startMenuItem = new MenuItem("Старт");
        stopMenuItem = new MenuItem("Стоп");
        MenuItem exitMenuItem = new MenuItem("Выход");

        startMenuItem.setOnAction(e -> startSimulationFromMenu());
        stopMenuItem.setOnAction(e -> stopSimulationFromMenu());
        exitMenuItem.setOnAction(e -> {
            Habitat.getInstance().stopAllAI();
            Platform.exit();
            System.exit(0); // гарантированное завершение
        });

        fileMenu.getItems().addAll(startMenuItem, stopMenuItem, new SeparatorMenuItem(), exitMenuItem);

        // Настройки
        Menu settingsMenu = new Menu("Настройки");
        MenuItem spawnIntervalsMenuItem = new MenuItem("Периоды генерации...");
        MenuItem probabilitiesMenuItem = new MenuItem("Вероятности генерации...");

        spawnIntervalsMenuItem.setOnAction(e -> showSpawnIntervalsDialog());
        probabilitiesMenuItem.setOnAction(e -> showProbabilitiesDialog());

        settingsMenu.getItems().addAll(spawnIntervalsMenuItem, probabilitiesMenuItem);

        // Показать меню
        Menu viewMenu = new Menu("Вид");
        showTimeMenuItem = new CheckMenuItem("Показывать время");
        showTimeMenuItem.setSelected(showTime);
        showTimeMenuItem.setOnAction(e -> toggleShowTime());

        showInfoMenuItem = new CheckMenuItem("Показывать информацию при остановке");
        showInfoMenuItem.setSelected(showInfoDialog);
        showInfoMenuItem.setOnAction(e -> toggleShowInfoDialog());

        viewMenu.getItems().addAll(showTimeMenuItem, showInfoMenuItem);

        menuBar.getMenus().addAll(fileMenu, settingsMenu, viewMenu);

        PipedOutputStream commandOut = new PipedOutputStream();
        PipedInputStream commandIn = new PipedInputStream(commandOut);

        PipedOutputStream responseOut = new PipedOutputStream();
        PipedInputStream responseIn = new PipedInputStream(responseOut);

        // ToolBar
        ToolBar toolBar = new ToolBar();

            toolbarStartButton = new ToggleButton("▶");
            Image end_button = new Image(getClass().getResourceAsStream("/malevich_square.png"), 12, 12, true, true);
            Button toolbarEndButton = new Button();
        toolbarConsoleButton = new ToggleButton("Консоль");
        toolbarEndButton.setGraphic(new ImageView(end_button));

        toolbarShowTimeButton = new ToggleButton("⏱");
        toolbarShowInfoButton = new ToggleButton("ℹ");

        // Запуск потока обработки команд
        new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(commandIn));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(responseOut));

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String output;
                    if (line.startsWith("Установить вероятность генерации машин")) {
                        try {
                            String valueStr = line.substring(line.lastIndexOf(" ") + 1)
                                    .replace("%", "");
                            double probability = Double.parseDouble(valueStr);

                            if (line.contains("%") || probability > 1) {
                                probability /= 100;
                            }

                            if (probability >= 0 && probability <= 1) {
                                CAR_SPAWN_PROBABILITY = probability;
                                output = "Установлена вероятность: " + CAR_SPAWN_PROBABILITY + "\n";
                            } else {
                                output = "Ошибка: вероятность должна быть от 0 до 1 (или 0% до 100%)" + "\n";
                            }
                        } catch (NumberFormatException e) {
                            output = "Ошибка: неверный формат числа" + "\n";
                        }
                    }
                    else if (line.equals("Получить вероятность генерации машин")) {
                        output = "Текущая вероятность: " + CAR_SPAWN_PROBABILITY + "\n";
                    }
                    else {
                        output = "Неизвестная команда" + "\n";
                    }

                    writer.write(output + "\n");
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        toolbarStartButton.setOnAction(e -> startSimulationFromMenu());
        toolbarEndButton.setOnAction(e -> stopSimulationFromMenu());

        toolbarConsoleButton.setOnAction(e -> {
            if (toolbarConsoleButton.isSelected()) {
                ConsolePane console = new ConsolePane(responseIn, commandOut);
                root.setCenter(console);
            } else {
                root.setCenter(visualizationPane);
            }
        });

        toolbarShowTimeButton.setSelected(showTime);
        toolbarShowTimeButton.setOnAction(e -> toggleShowTime());
        toolbarShowInfoButton.setSelected(showInfoDialog);
        toolbarShowInfoButton.setOnAction(e -> toggleShowInfoDialog());

        toolBar.getItems().addAll(
                toolbarStartButton,
                toolbarEndButton,
                new Separator(),
                toolbarConsoleButton,
                new Separator(),
                toolbarShowTimeButton,
                toolbarShowInfoButton
        );

        // Добавление MenuBar, ToolBar наверх
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Правая панель
        VBox controlPanel = new VBox(10);
        statsText = new Text();
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");
        root.setRight(controlPanel);

        controlPanel.getChildren().add(statsText);

        habitat = Habitat.getInstance(objectPane, x_kutuzka_size, y_kutuzka_size);
        loadSounds();

        HBox aiControlPanel = new HBox(10);
        Button pauseCarAI = new Button("⏸ ИИ");
        Button resumeCarAI = new Button("▶ ИИ");
        Button pauseOilAI = new Button("⏸ ИИ");
        Button resumeOilAI = new Button("▶ ИИ");

        pauseCarAI.setOnAction(e -> Habitat.getInstance().setCarAIPaused(true));
        resumeCarAI.setOnAction(e -> Habitat.getInstance().setCarAIPaused(false));
        pauseOilAI.setOnAction(e -> Habitat.getInstance().setOilAIPaused(true));
        resumeOilAI.setOnAction(e -> Habitat.getInstance().setOilAIPaused(false));

        aiControlPanel.getChildren().addAll(
                new VBox(new Label("Машины:"), pauseCarAI, resumeCarAI),
                new VBox(new Label("Масло:"), pauseOilAI, resumeOilAI)
        );

        controlPanel.getChildren().addAll(new Separator(), new Label("Управление ИИ:"), aiControlPanel);

        ComboBox<String> carPriorityCombo;
        ComboBox<String> oilPriorityCombo;

        Label priorityLabel = new Label("Приоритет потоков:");
        carPriorityCombo = new ComboBox<>(FXCollections.observableArrayList(
                "MIN", "NORM", "MAX"
        ));
        oilPriorityCombo = new ComboBox<>(FXCollections.observableArrayList(
                "MIN", "NORM", "MAX"
        ));

        carPriorityCombo.getSelectionModel().select("NORM");
        oilPriorityCombo.getSelectionModel().select("NORM");

        carPriorityCombo.setOnAction(e -> {
            String selected = carPriorityCombo.getSelectionModel().getSelectedItem();
            int priority = Thread.MIN_PRIORITY;
            if ("NORM".equals(selected)) priority = Thread.NORM_PRIORITY;
            else if ("MAX".equals(selected)) priority = Thread.MAX_PRIORITY;
            Habitat.getInstance().setCarAIPriority(priority);
        });

        oilPriorityCombo.setOnAction(e -> {
            String selected = oilPriorityCombo.getSelectionModel().getSelectedItem();
            int priority = Thread.MIN_PRIORITY;
            if ("NORM".equals(selected)) priority = Thread.NORM_PRIORITY;
            else if ("MAX".equals(selected)) priority = Thread.MAX_PRIORITY;
            Habitat.getInstance().setOilAIPriority(priority);
        });

// Добавим в controlPanel
        controlPanel.getChildren().addAll(
                new Separator(),
                priorityLabel,
                new HBox(5, new Label("Машины:"), carPriorityCombo),
                new HBox(5, new Label("Масло:"), oilPriorityCombo)
        );

        Button currentObjectsButton = new Button("Текущие объекты");
        currentObjectsButton.setOnAction(e -> {
            CurrentObjectsDialog dialog = new CurrentObjectsDialog(habitat.getBirthTimeMap());
            dialog.showAndWait();
        });
        controlPanel.getChildren().add(currentObjectsButton);
        
        // Все поля
        controlPanel.getChildren().addAll(
                new Separator(),
                new Label("Время жизни объектов (сек):"),
                new HBox(5, new Label("Машины:"), carLifetimeField),
                new HBox(5, new Label("Масло:"), oilLifetimeField),
                new Separator(),
                new Label("Периоды генерации (сек):"),
                new HBox(5, new Label("Машины:"), carSpawnIntervalField),
                new HBox(5, new Label("Масло:"), oilSpawnIntervalField)
        );


        // Обработка исключений при вводе в полях правой панели
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

        carLifetimeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                carLifetimeField.setText(oldVal);
            }
        });

        oilLifetimeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                oilLifetimeField.setText(oldVal);
            }
        });

        // Настройки вероятностей
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

        controlPanel.getChildren().addAll(
                new Separator(),
                probLabel,
                new HBox(5, new Label("Машины:"), carProbabilityCombo),
                new HBox(5, new Label("Масло:"), oilProbabilityList)
        );

        // Показать время
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
            showTimeMenuItem.setSelected(showTime);
            toolbarShowTimeButton.setSelected(showTime);

            if (showTime && pauseTime > 0) {
                simulationStartTime = System.nanoTime() - (pauseTime - simulationStartTime);
                pauseTime = 0;
            } else if (!showTime) {
                pauseTime = System.nanoTime();
            }
        });

        controlPanel.getChildren().addAll(
                new Separator(),
                new Label("Отображение времени:"),
                showTimeRadio,
                hideTimeRadio
        );

        // Настройка сцены
        Scene scene = new Scene(root, x_size, y_size);
        startButton = new Button("Старт");
        stopButton = new Button("Стоп");

        showInfoCheckBox = new CheckBox("Показывать информацию при остановке");
        showInfoCheckBox.setSelected(showInfoDialog);

        // Кнопки
        startMenuItem.disableProperty().bind(startButton.disabledProperty());
        stopMenuItem.disableProperty().bind(stopButton.disabledProperty());
        toolbarStartButton.disableProperty().bind(startButton.disabledProperty());

        // Управление клавиатурой
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case B -> {
                    if (!isSimulationRunning) {
                        startSimulation();
                    }
                }
                case E -> {
                    if (isSimulationRunning) {
                        statsText.setVisible(true);
                        stopSimulation();
                    }
                }
                case T -> {
                    showTime = !showTime;
                    statsText.setVisible(showTime);
                    showTimeMenuItem.setSelected(showTime);
                    toolbarShowTimeButton.setSelected(showTime);

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

        startButton.setOnAction(e -> startSimulation());
        stopButton.setOnAction(e -> {
            statsText.setVisible(true);
            stopSimulation();
        });

        controlPanel.getChildren().addAll(startButton, stopButton);

        showInfoCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showInfoDialog = newVal;
            showInfoMenuItem.setSelected(newVal);
            toolbarShowInfoButton.setSelected(newVal);
        });

        controlPanel.getChildren().add(showInfoCheckBox);

        // Обновление состояний кнопок
        updateButtonStates();

        // Таймер
        var timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isSimulationRunning) {
                    // Удаление "умеревших" объектов
                    habitat.getObjects().removeIf(obj -> obj.isExpired(now - simulationStartTime));

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
        stage.setOnCloseRequest(event -> {
            Habitat.getInstance().stopAllAI();
            Platform.exit();
            System.exit(0);
        });
    }

    private void updateButtonStates() {
        startButton.setDisable(isSimulationRunning);
        stopButton.setDisable(!isSimulationRunning);
        toolbarStartButton.setSelected(isSimulationRunning);
    }

    private void loadSounds() {
        try {
            Media collisionMedia = new Media(getClass().getResource("/sound.wav").toString());
            Media crashMedia = new Media(getClass().getResource("/crash.wav").toString());
            Media oilMedia = new Media(getClass().getResource("/oil_sound.wav").toString());

            collisionSound = new MediaPlayer(collisionMedia);
//            carCrashSound = new MediaPlayer(crashMedia); // звуки убраны из-за стремления объектов к одной точке
//            oilSound = new MediaPlayer(oilMedia);
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    private void startSimulation() {
        isSimulationRunning = true;
        habitat.setSimulationPaused(false);
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
        updateButtonStates();
    }

    private void stopSimulation() {
        isSimulationRunning = false;
        habitat.setSimulationPaused(true);
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
        updateButtonStates();
        habitat.stopAllAI();
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
        updateButtonStates();
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
            long carLifetime = (long)(Double.parseDouble(carLifetimeField.getText()) * 1_000_000_000L);

            if (now - lastCarSpawnTime >= CAR_SPAWN_INTERVAL) {
                if (Math.random() < CAR_SPAWN_PROBABILITY) {
                    habitat.spawnCar(carLifetime);
                }
                lastCarSpawnTime = now;
            }
        } catch (NumberFormatException e) {
            System.err.println("Некорректное значение времени жизни машин");
        }
    }

    private void spawnOil(long now) {
        try {
            OIL_SPAWN_INTERVAL = (long)(Double.parseDouble(oilSpawnIntervalField.getText()) * 1_000_000_000L);
            long oilLifetime = (long)(Double.parseDouble(oilLifetimeField.getText()) * 1_000_000_000L);

            if (now - lastOilSpawnTime >= OIL_SPAWN_INTERVAL) {
                if (Math.random() < OIL_SPAWN_PROBABILITY) {
                    habitat.spawnOil(oilLifetime);
                }
                lastOilSpawnTime = now;
            }
        } catch (NumberFormatException e) {
            System.err.println("Некорректное значение времени жизни масла");
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

    // Методы для работы с меню
    private void startSimulationFromMenu() {
        if (!isSimulationRunning) {
            startSimulation();
        }
    }

    private void stopSimulationFromMenu() {
        if (isSimulationRunning) {
            stopSimulation();
        }
    }

    private void toggleShowTime() {
        showTime = !showTime;
        statsText.setVisible(showTime);
        showTimeMenuItem.setSelected(showTime);
        toolbarShowTimeButton.setSelected(showTime);
        timeToggle.selectToggle(showTime ? showTimeRadio : hideTimeRadio);

        if (showTime) {
            if (pauseTime > 0) {
                simulationStartTime = System.nanoTime() - (pauseTime - simulationStartTime);
                pauseTime = 0;
            }
        } else {
            pauseTime = System.nanoTime();
        }
    }

    private void toggleShowInfoDialog() {
        showInfoDialog = !showInfoDialog;
        showInfoCheckBox.setSelected(showInfoDialog);
        showInfoMenuItem.setSelected(showInfoDialog);
        toolbarShowInfoButton.setSelected(showInfoDialog);
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