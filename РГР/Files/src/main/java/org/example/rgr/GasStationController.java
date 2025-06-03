package org.example.rgr;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GasStationController {
    private TextArea statusArea;
    private HBox pumpsContainer;
    private ListView<String> queueList;
    private ListView<String> clientsList;
    private Button requestStateButton;
    private Button addRemoteCarButton;

    private final List<GasPump> pumps = new ArrayList<>();
    private final Queue<Car> carQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService carExecutor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService refillExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isRefilling = new AtomicBoolean(false);
    private ObservableList<String> connectedClients = FXCollections.observableArrayList();

    private static final int NUM_PUMPS = 3;
    private static final int PUMP_CAPACITY = 500; // Унифицировано с GasPump
    private static final int MIN_REFILL_AMOUNT = 50; // Пополнение, если меньше 50 литров
    private static final int REFILL_INTERVAL = 10;

    private Socket clientSocket;
    private PrintWriter socketOut;
    private ObjectInputStream socketObjectIn;
    private volatile boolean isRunning = true;
    private String serverIp;
    private int serverPort;

    // Сеттеры для UI-элементов
    public void setStatusArea(TextArea statusArea) {
        this.statusArea = statusArea;
    }

    public void setPumpsContainer(HBox pumpsContainer) {
        this.pumpsContainer = pumpsContainer;
    }

    public void setQueueList(ListView<String> queueList) {
        this.queueList = queueList;
    }

    public void setClientsList(ListView<String> clientsList) {
        this.clientsList = clientsList;
    }

    public void setRequestStateButton(Button requestStateButton) {
        this.requestStateButton = requestStateButton;
    }

    public void setAddRemoteCarButton(Button addRemoteCarButton) {
        this.addRemoteCarButton = addRemoteCarButton;
    }

    public void initialize() {
        // Инициализация колонок
        for (int i = 0; i < NUM_PUMPS; i++) {
            GasPump pump = new GasPump(i + 1, PUMP_CAPACITY);
            pumps.add(pump);
            updatePumpUI(pump);
        }

        // Инициализация списка клиентов
        clientsList.setItems(connectedClients);

        // Настройка кнопок
        requestStateButton.setOnAction(e -> requestState());
        addRemoteCarButton.setOnAction(e -> addRemoteCar());

        // Запуск обработки очереди
        startQueueProcessing();

        // Проверка на пополнение
        refillExecutor.scheduleAtFixedRate(this::checkForRefill,
                REFILL_INTERVAL, REFILL_INTERVAL, TimeUnit.SECONDS);

        log("Заправочная станция запущена с " + NUM_PUMPS + " колонками");
    }

    private void updatePumpUI(GasPump pump) {
        VBox pumpBox = new VBox(5);
        pumpBox.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 10;");

        Text pumpId = new Text("Колонка #" + pump.getId());
        Text fuelText = new Text();
        fuelText.textProperty().bind(pump.fuelLevelProperty().asString("Топливо: %d л"));
        Text statusText = new Text();
        statusText.textProperty().bind(pump.statusProperty());

        pumpBox.getChildren().addAll(pumpId, fuelText, statusText);
        Platform.runLater(() -> {
            // Проверяем, чтобы не дублировать UI-элементы
            if (!pumpsContainer.getChildren().stream().anyMatch(node -> {
                if (node instanceof VBox) {
                    VBox box = (VBox) node;
                    Text idText = (Text) box.getChildren().get(0);
                    return idText.getText().equals("Колонка #" + pump.getId());
                }
                return false;
            })) {
                pumpsContainer.getChildren().add(pumpBox);
            }
        });
    }

    public void setServerInfo(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        connectToServer();
    }

    private void connectToServer() {
        try {
            clientSocket = new Socket(serverIp, serverPort);
            socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
            socketObjectIn = new ObjectInputStream(clientSocket.getInputStream());
            log("Подключено к серверу: " + serverIp + ":" + serverPort);
            startServerListener();
        } catch (IOException e) {
            log("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    private void startServerListener() {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while (isRunning && (inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("CLIENT_LIST:")) {
                        String clientList = inputLine.substring(12).trim();
                        String[] clientIds = clientList.isEmpty() ? new String[0] : clientList.split(",");
                        Platform.runLater(() -> {
                            connectedClients.setAll(clientIds);
                        });
                    } else if (inputLine.startsWith("REQUEST_STATE:")) {
                        sendState();
                    } else if (inputLine.startsWith("ADD_CAR:")) {
                        Platform.runLater(this::addCar);
                    } else if (inputLine.startsWith("STATE:")) {
                        String[] parts = inputLine.split(":");
                        int dataLength = Integer.parseInt(parts[1]);
                        byte[] data = new byte[dataLength];
                        clientSocket.getInputStream().read(data);
                        applyState(data);
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    log("Ошибка связи с сервером: " + e.getMessage());
                }
            } finally {
                closeSocket();
            }
        }).start();
    }

    private void sendState() {
        try {
            GasStationState state = new GasStationState(pumps, carQueue);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(state);
            byte[] data = baos.toByteArray();
            socketOut.println("STATE:" + data.length);
            clientSocket.getOutputStream().write(data);
            clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            log("Отправка состояния");
        }
    }

    private void applyState(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            GasStationState state = (GasStationState) ois.readObject();
            Platform.runLater(() -> {
                pumps.clear();
                pumps.addAll(state.getPumps());
                carQueue.clear();
                carQueue.addAll(state.getCarQueue());

                // Обновляем UI для всех колонок
                pumpsContainer.getChildren().clear();
                for (GasPump pump : pumps) {
                    updatePumpUI(pump);
                }

                updateQueueUI();
                log("Применено состояние от другого клиента");
            });
        } catch (IOException | ClassNotFoundException e) {
            log("Ошибка применения состояния: " + e.getMessage());
        }
    }

    private void requestState() {
        String selectedClient = clientsList.getSelectionModel().getSelectedItem();
        if (selectedClient != null) {
            socketOut.println("REQUEST_STATE:" + selectedClient);
        } else {
            log("Выберите клиента для запроса состояния");
        }
    }

    private void addRemoteCar() {
        socketOut.println("ADD_CAR:");
        log("Отправлен запрос на добавление машины всем клиентам");
    }

    private void closeSocket() {
        try {
            isRunning = false;
            if (socketOut != null) {
                socketOut.close();
            }
            if (socketObjectIn != null) {
                socketObjectIn.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            log("Ошибка закрытия сокета: " + e.getMessage());
        }
    }

    public void shutdown() {
        isRunning = false;
        closeSocket();
        carExecutor.shutdownNow();
        refillExecutor.shutdownNow();
    }

    private void startQueueProcessing() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    Car car = carQueue.poll();
                    if (car != null) {
                        log("Обработка машины #" + car.getId() + " из очереди");
                        assignCarToPump(car);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log("Обработка очереди прервана");
                    break;
                }
            }
        }).start();
    }

    private void assignCarToPump(Car car) {
        Optional<GasPump> availablePump = pumps.stream()
                .filter(p -> p.isAvailable() && p.getFuelLevel() >= car.getRequiredFuel())
                .findFirst();

        if (availablePump.isPresent()) {
            GasPump pump = availablePump.get();
            pump.setAvailable(false);
            pump.setStatus("Заправка машины #" + car.getId());

            carExecutor.submit(() -> {
                try {
                    log("Машина #" + car.getId() + " заправляется на колонке #" + pump.getId() +
                            " (" + car.getRequiredFuel() + " л)");
                    Thread.sleep(car.getRequiredFuel() * 100L);

                    pump.consumeFuel(car.getRequiredFuel());
                    log("Машина #" + car.getId() + " завершила заправку на колонке #" + pump.getId());

                    pump.setStatus("Свободна");
                    pump.setAvailable(true);

                    Platform.runLater(this::updateQueueUI);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } else {
            Platform.runLater(() -> {
                carQueue.add(car);
                updateQueueUI();
            });
        }
    }

    private void checkForRefill() {
        Platform.runLater(() -> {
            boolean needsRefill = pumps.stream()
                    .allMatch(p -> p.getFuelLevel() < MIN_REFILL_AMOUNT);

            // Добавляем отладочный лог
            StringBuilder fuelLevels = new StringBuilder("Уровни топлива: ");
            pumps.forEach(p -> fuelLevels.append("Колонка #").append(p.getId())
                    .append(": ").append(p.getFuelLevel()).append(" л, "));
            log(fuelLevels.toString());

            if (needsRefill && isRefilling.compareAndSet(false, true)) {
                log("Все колонки имеют менее " + MIN_REFILL_AMOUNT + " л. Начинаем пополнение...");
                refillAll();
            } else if (!needsRefill) {
                log("Пополнение не требуется: не все колонки имеют менее " + MIN_REFILL_AMOUNT + " л");
            } else {
                log("Пополнение не начато: уже выполняется пополнение");
            }
        });
    }

    public void addCar() {
        Car car = new Car();
        carQueue.add(car);
        updateQueueUI();
        log("Добавлена машина #" + car.getId() + " (нужно " + car.getRequiredFuel() + " л)");
    }

    public void refillAll() {
        if (isRefilling.get()) {
            log("Уже идет процесс пополнения...");
            return;
        }

        isRefilling.set(true);
        log("Начато пополнение всех колонок");

        carExecutor.submit(() -> {
            try {
                Platform.runLater(() -> {
                    pumps.forEach(p -> {
                        p.setAvailable(false);
                        p.setStatus("Заправляется");
                        log("Колонка #" + p.getId() + " установлена в статус 'Заправляется'");
                    });
                });

                Thread.sleep(5000);

                Platform.runLater(() -> {
                    pumps.forEach(p -> {
                        p.refill();
                        p.setStatus("Свободна");
                        p.setAvailable(true);
                        log("Колонка #" + p.getId() + " пополнена до " + p.getFuelLevel() + " л");
                    });

                    log("Все колонки пополнены");
                    isRefilling.set(false);

                    if (!carQueue.isEmpty()) {
                        Car car = carQueue.poll();
                        if (car != null) {
                            assignCarToPump(car);
                            updateQueueUI();
                        }
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    isRefilling.set(false);
                    log("Пополнение прервано");
                });
            }
        });
    }

    private void updateQueueUI() {
        Platform.runLater(() -> {
            ObservableList<String> items = FXCollections.observableArrayList();
            carQueue.forEach(car -> items.add("Машина #" + car.getId() + " (" + car.getRequiredFuel() + " л)"));
            queueList.setItems(items);
            log("Обновление очереди: " + items.size() + " машин");
        });
    }

    private void log(String message) {
        Platform.runLater(() ->
                statusArea.appendText(message + "\n"));
    }
}