package org.example.laba_6;

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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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
    private long CAR_SPAWN_INTERVAL = 2_000_000_000L;
    private long OIL_SPAWN_INTERVAL = 3_000_000_000L;
    private long CAR_LIFETIME = 10_000_000_000L;
    private long OIL_LIFETIME = 7_000_000_000L;
    private long lastCarSpawnTime = 0;
    private long lastOilSpawnTime = 0;
    private double time_counter = 0;

    private MenuItem startMenuItem;
    private MenuItem stopMenuItem;
    private CheckMenuItem showTimeMenuItem;
    private CheckMenuItem showInfoMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem loadMenuItem;
    private ToggleButton toolbarStartButton;
    private ToggleButton toolbarConsoleButton;
    private ToggleButton toolbarShowTimeButton;
    private ToggleButton toolbarShowInfoButton;
    private Pane objectPane;
    private boolean isLoadedSimulation;

    private static final String CONFIG_FILE = "config.txt";
    private static final DecimalFormat DECIMAL_FORMAT;

    private Socket clientSocket;
    private PrintWriter socketOut;
    private ObjectInputStream socketObjectIn;
    private ListView<String> connectedClientsListView;
    private ObservableList<String> connectedClients = FXCollections.observableArrayList();
    private Button copySimulationButton;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMAT = new DecimalFormat("#.##", symbols);
        DECIMAL_FORMAT.setParseBigDecimal(true);
    }

    @Override
    public void start(Stage stage) {
        try {
            System.out.println("Начало метода start");

            int x_size = 1040;
            int y_size = 1000;
            int x_kutuzka_size = 500;
            int y_kutuzka_size = 679;

            // Загрузка конфигурации
            System.out.println("Загрузка конфигурации");
            loadConfig();

            // Инициализация полей ввода
            System.out.println("Инициализация полей ввода");
            carLifetimeField = new TextField(String.valueOf(CAR_LIFETIME / 1_000_000_000L));
            oilLifetimeField = new TextField(String.valueOf(OIL_LIFETIME / 1_000_000_000L));
            carSpawnIntervalField = new TextField(String.valueOf(CAR_SPAWN_INTERVAL / 1_000_000_000L));
            oilSpawnIntervalField = new TextField(String.valueOf(OIL_SPAWN_INTERVAL / 1_000_000_000L));

            carSpawnIntervalField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.matches("\\d*")) {
                    try {
                        CAR_SPAWN_INTERVAL = (long)(Double.parseDouble(newVal) * 1_000_000_000L);
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            oilSpawnIntervalField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.matches("\\d*")) {
                    try {
                        OIL_SPAWN_INTERVAL = (long)(Double.parseDouble(newVal) * 1_000_000_000L);
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            carLifetimeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.matches("\\d*")) {
                    try {
                        CAR_LIFETIME = (long)(Double.parseDouble(newVal) * 1_000_000_000L);
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            oilLifetimeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.matches("\\d*")) {
                    try {
                        OIL_LIFETIME = (long)(Double.parseDouble(newVal) * 1_000_000_000L);
                    } catch (NumberFormatException ignored) {
                    }
                }
            });

            TextField headerCarLifeField = new TextField(String.valueOf(CAR_LIFETIME / 1_000_000_000L));
            TextField headerOilLifeField = new TextField(String.valueOf(OIL_LIFETIME / 1_000_000_000L));
            Button headerCurrentObjectsBtn = new Button("Текущие объекты");

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

            // Загрузка фонового изображения
            System.out.println("Загрузка фонового изображения");
            Image backgroundImage;
            try {
                backgroundImage = new Image(getClass().getResourceAsStream("/background.png"), (x_size * 0.92), (y_size * 0.92), true, true);
            } catch (Exception e) {
                System.err.println("Ошибка загрузки background.png: " + e.getMessage());
                backgroundImage = new Image(new ByteArrayInputStream(new byte[0]));
            }
            ImageView backgroundImageView = new ImageView(backgroundImage);

            // Создание основной панели
            System.out.println("Создание основной панели");
            BorderPane root = new BorderPane();
            objectPane = new Pane();
            StackPane visualizationPane = new StackPane();
            StackPane.setAlignment(backgroundImageView, Pos.CENTER_LEFT);
            visualizationPane.getChildren().addAll(backgroundImageView, objectPane);
            root.setCenter(visualizationPane);

            // Создание меню
            System.out.println("Создание меню");
            MenuBar menuBar = new MenuBar();
            Menu fileMenu = new Menu("Файл");
            startMenuItem = new MenuItem("Старт");
            stopMenuItem = new MenuItem("Стоп");
            saveMenuItem = new MenuItem("Сохранить");
            loadMenuItem = new MenuItem("Загрузить");
            MenuItem exitMenuItem = new MenuItem("Выход");

            startMenuItem.setOnAction(e -> startSimulationFromMenu());
            stopMenuItem.setOnAction(e -> stopSimulationFromMenu());
            saveMenuItem.setOnAction(e -> saveSimulation());
            loadMenuItem.setOnAction(e -> loadSimulation());
            exitMenuItem.setOnAction(e -> {
                saveConfig();
                Habitat.getInstance().stopAllAI();
                closeSocket();
                Platform.exit();
                System.exit(0);
            });

            fileMenu.getItems().addAll(startMenuItem, stopMenuItem, new SeparatorMenuItem(), saveMenuItem, loadMenuItem, new SeparatorMenuItem(), exitMenuItem);

            Menu settingsMenu = new Menu("Настройки");
            MenuItem spawnIntervalsMenuItem = new MenuItem("Периоды генерации...");
            MenuItem probabilitiesMenuItem = new MenuItem("Вероятности генерации...");

            spawnIntervalsMenuItem.setOnAction(e -> showSpawnIntervalsDialog());
            probabilitiesMenuItem.setOnAction(e -> showProbabilitiesDialog());

            settingsMenu.getItems().addAll(spawnIntervalsMenuItem, probabilitiesMenuItem);

            Menu viewMenu = new Menu("Вид");
            showTimeMenuItem = new CheckMenuItem("Показывать время");
            showTimeMenuItem.setSelected(showTime);
            showTimeMenuItem.setOnAction(e -> toggleShowTime());

            showInfoMenuItem = new CheckMenuItem("Показывать информацию при остановке");
            showInfoMenuItem.setSelected(showInfoDialog);
            showInfoMenuItem.setOnAction(e -> toggleShowInfoDialog());

            viewMenu.getItems().addAll(showTimeMenuItem, showInfoMenuItem);
            menuBar.getMenus().addAll(fileMenu, settingsMenu, viewMenu);

            // Создание консоли
            System.out.println("Создание консоли");
            PipedOutputStream commandOut = new PipedOutputStream();
            PipedInputStream commandIn = new PipedInputStream(commandOut);
            PipedOutputStream responseOut = new PipedOutputStream();
            PipedInputStream responseIn = new PipedInputStream(responseOut);

            // Панель инструментов
            System.out.println("Создание панели инструментов");
            ToolBar toolBar = new ToolBar();
            toolbarStartButton = new ToggleButton("▶");
            Image end_button;
            try {
                end_button = new Image(getClass().getResourceAsStream("/malevich_square.png"), 12, 12, true, true);
            } catch (Exception e) {
                System.err.println("Ошибка загрузки malevich_square.png: " + e.getMessage());
                end_button = new Image(new ByteArrayInputStream(new byte[0]));
            }
            Button toolbarEndButton = new Button();
            toolbarConsoleButton = new ToggleButton("Консоль");
            toolbarEndButton.setGraphic(new ImageView(end_button));
            toolbarShowTimeButton = new ToggleButton("⏱");
            toolbarShowInfoButton = new ToggleButton("ℹ");

            new Thread(() -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(commandIn));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(responseOut));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String output;
                        if (line.startsWith("Установить вероятность генерации машин")) {
                            try {
                                String valueStr = line.substring(line.lastIndexOf(" ") + 1).replace("%", "");
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
                        } else if (line.equals("Получить вероятность генерации машин")) {
                            output = "Текущая вероятность: " + CAR_SPAWN_PROBABILITY + "\n";
                        } else {
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

            VBox topContainer = new VBox(menuBar, toolBar);
            root.setTop(topContainer);

            // Правая панель
            System.out.println("Создание правой панели");
            VBox controlPanel = new VBox(10);
            statsText = new Text();
            controlPanel.setPadding(new Insets(10));
            controlPanel.setStyle("-fx-background-color: #f0f0f0;");

            ScrollPane scrollPane = new ScrollPane(controlPanel);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(false);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setPrefWidth(350); // Увеличена ширина панели
            scrollPane.setStyle("-fx-background-color: #f0f0f0;");
            root.setRight(scrollPane);

            controlPanel.getChildren().add(statsText);

            // Инициализация Habitat
            System.out.println("Инициализация Habitat");
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
            carPriorityCombo = new ComboBox<>(FXCollections.observableArrayList("MIN", "NORM", "MAX"));
            oilPriorityCombo = new ComboBox<>(FXCollections.observableArrayList("MIN", "NORM", "MAX"));

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

            Label probLabel = new Label("Вероятности генерации:");
            carProbabilityCombo = new ComboBox<>(createProbabilityItems());
            oilProbabilityList = new ListView<>(createProbabilityItems());
            oilProbabilityList.setPrefHeight(100);
            oilProbabilityList.setMaxHeight(100);

            carProbabilityCombo.getSelectionModel().select((int)(CAR_SPAWN_PROBABILITY * 100) + "%");
            carProbabilityCombo.setOnAction(e -> {
                String selected = carProbabilityCombo.getSelectionModel().getSelectedItem();
                CAR_SPAWN_PROBABILITY = Integer.parseInt(selected.replace("%", "")) / 100.0;
            });

            oilProbabilityList.getSelectionModel().select((int)(OIL_SPAWN_PROBABILITY * 100) + "%");
            oilProbabilityList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                OIL_SPAWN_PROBABILITY = Integer.parseInt(newVal.replace("%", "")) / 100.0;
            });

            controlPanel.getChildren().addAll(
                    new Separator(),
                    probLabel,
                    new HBox(5, new Label("Машины:"), carProbabilityCombo),
                    new HBox(5, new Label("Масло:"), oilProbabilityList)
            );

            connectedClientsListView = new ListView<>();
            connectedClientsListView.setItems(connectedClients);
            connectedClientsListView.setPrefHeight(100);
            copySimulationButton = new Button("Копировать симуляцию");
            copySimulationButton.setOnAction(e -> copySimulationFromClient());

            controlPanel.getChildren().addAll(
                    new Separator(),
                    new Label("Подключенные клиенты:"),
                    connectedClientsListView,
                    copySimulationButton
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

            // Создание сцены
            System.out.println("Создание сцены");
            Scene scene = new Scene(root, x_size, y_size);
            startButton = new Button("Старт");
            stopButton = new Button("Стоп");

            showInfoCheckBox = new CheckBox("Показывать информацию при остановке");
            showInfoCheckBox.setSelected(showInfoDialog);

            startMenuItem.disableProperty().bind(startButton.disabledProperty());
            stopMenuItem.disableProperty().bind(stopButton.disabledProperty());
            toolbarStartButton.disableProperty().bind(startButton.disabledProperty());

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

            updateButtonStates();

            // Анимация
            System.out.println("Запуск анимации");
            var timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (isSimulationRunning) {
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

            // Настройка сцены
            System.out.println("Настройка сцены");
            stage.setTitle("Bavarskaya pogonia za maslom");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.setOnCloseRequest(event -> {
                saveConfig();
                Habitat.getInstance().stopAllAI();
                closeSocket();
                Platform.exit();
                System.exit(0);
            });

            // Подключение к серверу асинхронно
            System.out.println("Запуск асинхронного подключения к серверу");
            new Thread(this::connectToServer).start();

            // Показ окна
            System.out.println("Отображение окна");
            stage.show();
            System.out.println("Окно отображено");
        } catch (Exception e) {
            System.err.println("Ошибка в методе start: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Ошибка инициализации", "Не удалось запустить приложение: " + e.getMessage()));
        }
    }

    private void connectToServer() {
        try {
            System.out.println("Попытка подключения к серверу");
            clientSocket = new Socket("localhost", 12345);
            socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
            socketObjectIn = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("Подключено к серверу, запуск слушателя");
            startServerListener();
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Ошибка подключения", "Не удалось подключиться к серверу"));
        }
    }

    private void closeSocket() {
        try {
            if (socketOut != null) socketOut.close();
            if (socketObjectIn != null) socketObjectIn.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServerListener() {
        new Thread(() -> {
            try {
                System.out.println("Запуск слушателя сервера");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Получено сообщение от сервера: '" + inputLine + "'");
                    if (inputLine.startsWith("CLIENT_LIST:")) {
                        System.out.println("Обработка сообщения CLIENT_LIST");
                        String clientList = inputLine.substring(12).trim();
                        System.out.println("Сырой список клиентов: '" + clientList + "'");
                        String[] clientIds = clientList.isEmpty() ? new String[0] : clientList.split(",");
                        System.out.println("Разделенный список клиентов: " + Arrays.toString(clientIds));
                        Platform.runLater(() -> {
                            connectedClients.clear();
                            connectedClients.addAll(clientIds);
                            System.out.println("Обновлен список клиентов в UI: " + connectedClients);
                            // Дополнительная проверка содержимого ListView
                            System.out.println("Элементы в ListView: " + connectedClientsListView.getItems());
                        });
                    } else if (inputLine.startsWith("REQUEST_SIMULATION:")) {
                        System.out.println("Получен запрос симуляции: " + inputLine);
                        sendSimulationState();
                    } else if (inputLine.startsWith("SIMULATION_STATE:")) {
                        System.out.println("Получено состояние симуляции: " + inputLine);
                        String[] parts = inputLine.split(":");
                        int dataLength = Integer.parseInt(parts[2]);
                        byte[] data = new byte[dataLength];
                        clientSocket.getInputStream().read(data);
                        applySimulationState(data);
                    } else {
                        System.out.println("Неизвестное сообщение: " + inputLine);
                    }
                }
                System.out.println("Слушатель сервера завершил работу (соединение закрыто)");
            } catch (IOException e) {
                System.err.println("Ошибка в слушателе сервера: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Ошибка", "Потеряно соединение с сервером"));
            }
        }).start();
    }

    private void sendSimulationState() {
        List<Car> cars = habitat.getObjects().stream()
                .filter(obj -> obj instanceof Car)
                .map(obj -> (Car)obj)
                .collect(Collectors.toList());
        List<Oil> oils = habitat.getObjects().stream()
                .filter(obj -> obj instanceof Oil)
                .map(obj -> (Oil)obj)
                .collect(Collectors.toList());

        SimulationState state = new SimulationState(
                cars, oils,
                isSimulationRunning ? System.nanoTime() - simulationStartTime : pauseTime - simulationStartTime
        );

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(state);
            oos.flush();
            byte[] data = baos.toByteArray();
            socketOut.println("SIMULATION_STATE:0:" + data.length);
            clientSocket.getOutputStream().write(data);
            clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applySimulationState(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            SimulationState state = (SimulationState) ois.readObject();
            Platform.runLater(() -> {
                habitat.clear();
                objectPane.getChildren().clear();
                for (CarState carState : state.getCars()) {
                    Car car = carState.createCar();
                    habitat.getObjects().add(car);
                    habitat.getCarAI().addObject(car);
                    objectPane.getChildren().add(car.getImageView());
                }
                for (OilState oilState : state.getOils()) {
                    Oil oil = oilState.createOil();
                    habitat.getObjects().add(oil);
                    habitat.getOilAI().addObject(oil);
                    objectPane.getChildren().add(oil.getImageView());
                }
                simulationStartTime = System.nanoTime() - state.getSimulationTime();
                if (!isSimulationRunning) {
                    pauseTime = System.nanoTime();
                } else {
                    habitat.setSimulationPaused(false);
                }
            });
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось применить состояние симуляции");
        }
    }

    private void copySimulationFromClient() {
        String selectedClient = connectedClientsListView.getSelectionModel().getSelectedItem();
        if (selectedClient != null) {
            socketOut.println("REQUEST_SIMULATION:" + selectedClient);
        } else {
            showAlert("Ошибка", "Выберите клиента для копирования симуляции");
        }
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            props.load(reader);
            CAR_SPAWN_INTERVAL = Long.parseLong(props.getProperty("car_spawn_interval", String.valueOf(CAR_SPAWN_INTERVAL)));
            OIL_SPAWN_INTERVAL = Long.parseLong(props.getProperty("oil_spawn_interval", String.valueOf(OIL_SPAWN_INTERVAL)));
            CAR_LIFETIME = Long.parseLong(props.getProperty("car_lifetime", String.valueOf(CAR_LIFETIME)));
            OIL_LIFETIME = Long.parseLong(props.getProperty("oil_lifetime", String.valueOf(OIL_LIFETIME)));
            CAR_SPAWN_PROBABILITY = Double.parseDouble(props.getProperty("car_spawn_probability", String.valueOf(CAR_SPAWN_PROBABILITY)));
            OIL_SPAWN_PROBABILITY = Double.parseDouble(props.getProperty("oil_spawn_probability", String.valueOf(OIL_SPAWN_PROBABILITY)));
            showTime = Boolean.parseBoolean(props.getProperty("show_time", String.valueOf(showTime)));
            showInfoDialog = Boolean.parseBoolean(props.getProperty("show_info_dialog", String.valueOf(showInfoDialog)));
            time_counter = Double.parseDouble(props.getProperty("global_time", String.valueOf(time_counter)));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading config: " + e.getMessage());
        }
    }

    private void saveConfig() {
        Properties props = new Properties();
        props.setProperty("car_spawn_interval", String.valueOf(CAR_SPAWN_INTERVAL));
        props.setProperty("oil_spawn_interval", String.valueOf(OIL_SPAWN_INTERVAL));
        props.setProperty("car_lifetime", String.valueOf(CAR_LIFETIME));
        props.setProperty("oil_lifetime", String.valueOf(OIL_LIFETIME));
        props.setProperty("car_spawn_probability", DECIMAL_FORMAT.format(CAR_SPAWN_PROBABILITY));
        props.setProperty("oil_spawn_probability", DECIMAL_FORMAT.format(OIL_SPAWN_PROBABILITY));
        props.setProperty("show_time", String.valueOf(showTime));
        props.setProperty("show_info_dialog", String.valueOf(showInfoDialog));
        props.setProperty("global_time", DECIMAL_FORMAT.format(time_counter));

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            props.store(writer, "Simulation Configuration");
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    private void updateButtonStates() {
        startButton.setDisable(isSimulationRunning);
        stopButton.setDisable(!isSimulationRunning);
        toolbarStartButton.setSelected(isSimulationRunning);
    }

    private void loadSounds() {
        try {
            Media collisionMedia = new Media(getClass().getResource("/sound.wav").toString());
            collisionSound = new MediaPlayer(collisionMedia);
            Media crashMedia = new Media(getClass().getResource("/crash.wav").toString());
            carCrashSound = new MediaPlayer(crashMedia);
            Media oilMedia = new Media(getClass().getResource("/oil_sound.wav").toString());
            oilSound = new MediaPlayer(oilMedia);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки звуков: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startSimulation() {
        isSimulationRunning = true;
        habitat.setSimulationPaused(false);
        if (!isLoadedSimulation) {
            time_counter = 0;
            simulationStartTime = System.nanoTime();
        } else {
            simulationStartTime = System.nanoTime() - (long)(time_counter * 1_000_000_000L);
            isLoadedSimulation = false;
        }
        pauseTime = 0;
        statsText.setText(String.format("Time: %.1f s", time_counter));
        statsText.setVisible(showTime);
        habitat.clear();
        resetStatsTextStyle();
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
        statsText.setText(String.format("Time: %.1f s\nCars: %d\nOils: %d", time_counter, cars, oils));
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
            long carLifetime = CAR_LIFETIME;
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
            long oilLifetime = OIL_LIFETIME;
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

    private void saveSimulation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить симуляцию");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("NFS", "*.car"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                List<Car> cars = habitat.getObjects().stream()
                        .filter(obj -> obj instanceof Car)
                        .map(obj -> (Car)obj)
                        .collect(Collectors.toList());
                List<Oil> oils = habitat.getObjects().stream()
                        .filter(obj -> obj instanceof Oil)
                        .map(obj -> (Oil)obj)
                        .collect(Collectors.toList());
                SimulationState state = new SimulationState(
                        cars, oils,
                        isSimulationRunning ? System.nanoTime() - simulationStartTime : pauseTime - simulationStartTime
                );
                oos.writeObject(state);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Ошибка сохранения", "Не удалось сохранить симуляцию");
            }
        }
    }

    private void loadSimulation() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Загрузить симуляцию");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("NFS", "*.car"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                SimulationState state = (SimulationState) ois.readObject();
                habitat.clear();
                objectPane.getChildren().clear();
                for (CarState carState : state.getCars()) {
                    Car car = carState.createCar();
                    habitat.getObjects().add(car);
                    habitat.getCarAI().addObject(car);
                    objectPane.getChildren().add(car.getImageView());
                }
                for (OilState oilState : state.getOils()) {
                    Oil oil = oilState.createOil();
                    habitat.getObjects().add(oil);
                    habitat.getOilAI().addObject(oil);
                    objectPane.getChildren().add(oil.getImageView());
                }
                simulationStartTime = System.nanoTime() - state.getSimulationTime();
                if (!habitat.getCarAI().running) {
                    habitat.getCarAI().resumeAI();
                }
                if (!habitat.getOilAI().running) {
                    habitat.getOilAI().resumeAI();
                }
                if (!isSimulationRunning) {
                    pauseTime = System.nanoTime();
                } else {
                    habitat.setSimulationPaused(false);
                }
                updateUI(System.nanoTime());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                showAlert("Ошибка загрузки", "Не удалось загрузить симуляцию");
            }
            habitat.setCarAIPaused(false);
            habitat.setOilAIPaused(false);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showProbabilitiesDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Настройка вероятностей");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        ComboBox<String> carProbCombo = new ComboBox<>(createProbabilityItems());
        carProbCombo.getSelectionModel().select((int)(CAR_SPAWN_PROBABILITY * 100) + "%");
        ListView<String> oilProbList = new ListView<>(createProbabilityItems());
        oilProbList.getSelectionModel().select((int)(OIL_SPAWN_PROBABILITY * 100) + "%");
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