package com.example.rgr;

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
    // элементы интерфейса
    private TextArea statusArea; // текстовая область для вывода сообщений
    private HBox pumpsContainer; // горизонтальный контейнер для отображения колонок
    private ListView<String> queueList; // список для отображения очереди машин
    private ListView<String> clientsList; // список подключенных клиентов
    private Button requestStateButton; // кнопка запроса состояния у других клиентов
    private Button addRemoteCarButton; // кнопка добавления машины во все клиенты

    // данные и управление заправкой
    private final List<GasPump> pumps = new ArrayList<>(); // список всех топливных колонок
    private final Queue<Car> carQueue = new ConcurrentLinkedQueue<>(); // очередь машин, ConcurrentLinkedQueue для потокобезопасности
    private final ExecutorService carExecutor = Executors.newCachedThreadPool(); // пул потоков обработки мащин
    private final ScheduledExecutorService refillExecutor = Executors.newSingleThreadScheduledExecutor(); // пул периодической дозаправки колонок
    private final AtomicBoolean isRefilling = new AtomicBoolean(false); // флаг указания на процесс дозаправки
    private ObservableList<String> connectedClients = FXCollections.observableArrayList(); // список подключенных клиентов

    // константы конфигурации
    private static final int NUM_PUMPS = 3; // количество колонок
    private static final int PUMP_CAPACITY = 500; // максимальный объём колонки
    private static final int MIN_REFILL_AMOUNT = 50; // уровень топлива, ниже которого происходит автоматическая дозаправка
    private static final int REFILL_INTERVAL = 10; // интервал проверки необходимости пополнения топлива на колонках

    // сети
    private Socket clientSocket; // сокет для подключения к серверу
    private PrintWriter socketOut; // поток вывода для отправки сообщений серверу
    private ObjectInputStream socketObjectIn; // поток ввода для получения объектов от сервера
    private volatile boolean isRunning = true; // флаг работы сетевого подключения
    private String serverIp; // IP-адрес сервера
    private int serverPort; // порт сервера

    // сеттеры интерфейса
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
        // инициализация колонок
        for (int i = 0; i < NUM_PUMPS; i++) {
            GasPump pump = new GasPump(i + 1, PUMP_CAPACITY);
            pumps.add(pump);
            updatePumpUI(pump);
        }

        // инициализация списка клиентов
        clientsList.setItems(connectedClients);

        // лямбда-выражения для кнопок запроса состояния и добавления машины во все клиенты
        requestStateButton.setOnAction(e -> requestState());
        addRemoteCarButton.setOnAction(e -> addRemoteCar());

        // старт очереди
        startQueueProcessing();

        // периодическое пополнение колонок топливом
        refillExecutor.scheduleAtFixedRate(this::checkForRefill,
                REFILL_INTERVAL, REFILL_INTERVAL, TimeUnit.SECONDS);

        log("Заправочная станция запущена с " + NUM_PUMPS + " колонками");
    }

    // обновление интерфейса для отображения статуса колонки
    private void updatePumpUI(GasPump pump) {
        // контейнер для отображения конфигурации о колонке
        VBox pumpBox = new VBox(5);
        pumpBox.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 10;");

        // заголовок с номером колонки
        Text pumpId = new Text("Колонка #" + pump.getId());

        // текст для отображения уровня топлива
        Text fuelText = new Text();
        fuelText.textProperty().bind(pump.fuelLevelProperty().asString("Топливо: %d л"));

        // текст для отображения статуса
        Text statusText = new Text();
        statusText.textProperty().bind(pump.statusProperty());

        // добавление всех элементов в контейнер
        pumpBox.getChildren().addAll(pumpId, fuelText, statusText);

        // обновление интерфейса в потоке JavaFX
        Platform.runLater(() -> {
            // проверка, не добавлена ли уже колонка
            if (!pumpsContainer.getChildren().stream().anyMatch(node -> {
                if (node instanceof VBox) {
                    VBox box = (VBox) node;
                    Text idText = (Text) box.getChildren().get(0);
                    return idText.getText().equals("Колонка #" + pump.getId());
                }
                return false;
            })) {
                pumpsContainer.getChildren().add(pumpBox); // добавление колонки в интерфейс
            }
        });
    }

    // установка информации о сервере
    public void setServerInfo(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
        connectToServer();
    }

    // установка соединения с сервером
    private void connectToServer() {
        try {
            // создание соединения
            clientSocket = new Socket(serverIp, serverPort);
            // инициализация потоков для обмена данными
            socketOut = new PrintWriter(clientSocket.getOutputStream(), true);
            socketObjectIn = new ObjectInputStream(clientSocket.getInputStream());
            log("Подключено к серверу: " + serverIp + ":" + serverPort);
            startServerListener(); // запуск потока для прослушивания сервера
        } catch (IOException e) {
            log("Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    // запуск потока для прослушивания сообщений от сервера
    private void startServerListener() {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                // основной цикл прослушивания сервера
                while (isRunning && (inputLine = in.readLine()) != null) {
                    // обработка списка подключенных клиентов
                    if (inputLine.startsWith("CLIENT_LIST:")) {
                        String clientList = inputLine.substring(12).trim();
                        String[] clientIds = clientList.isEmpty() ? new String[0] : clientList.split(",");
                        Platform.runLater(() -> {
                            connectedClients.setAll(clientIds); // обновление списка клиентов в интерфейсе
                        });
                    }
                    // обработка запроса состояния
                    else if (inputLine.startsWith("REQUEST_STATE:")) {
                        sendState(); // отправка текущего состояния
                    }
                    // обработка команды добавления машины
                    else if (inputLine.startsWith("ADD_CAR:")) {
                        Platform.runLater(this::addCar);
                    }
                    // обработка получения состояния от другого клиента
                    else if (inputLine.startsWith("STATE:")) {
                        String[] parts = inputLine.split(":");
                        int dataLength = Integer.parseInt(parts[1]);
                        byte[] data = new byte[dataLength];
                        clientSocket.getInputStream().read(data);
                        applyState(data); // применение полученного состояния
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    log("Ошибка связи с сервером: " + e.getMessage());
                }
            } finally {
                closeSocket(); // закрываем соединение при ошибке
            }
        }).start();
    }

    // отправляет текущее состояние станции на сервер
    private void sendState() {
        try {
            // создаём объект состояния
            GasStationState state = new GasStationState(pumps, carQueue);
            // сериализация состояния
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(state);
            byte[] data = baos.toByteArray();
            // отправка данных
            socketOut.println("STATE:" + data.length);
            clientSocket.getOutputStream().write(data);
            clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            log("Ошибка отправки состояния: " + e.getMessage());
        }
    }

    // применяет полученное состояние станции
    private void applyState(byte[] data) {
        try {
            // десериализация данных
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            GasStationState state = (GasStationState) ois.readObject();
            // обновление состояния
            Platform.runLater(() -> {
                pumps.clear();
                pumps.addAll(state.getPumps()); // обновление колонок
                carQueue.clear();
                carQueue.addAll(state.getCarQueue()); // обновление очереди

                // обновление интерфейса всех колонок
                pumpsContainer.getChildren().clear();
                for (GasPump pump : pumps) {
                    updatePumpUI(pump);
                }

                updateQueueUI(); // обновление очереди
                log("Применено состояние от другого клиента");
            });
        } catch (IOException | ClassNotFoundException e) {
            log("Ошибка применения состояния: " + e.getMessage());
        }
    }

    // запрос состояния у выбранного клиента
    private void requestState() {
        String selectedClient = clientsList.getSelectionModel().getSelectedItem();
        if (selectedClient != null) {
            socketOut.println("REQUEST_STATE:" + selectedClient);
        } else {
            log("Выберите клиента для запроса состояния");
        }
    }

    // отправка запроса на добавление машины всем клиентам
    private void addRemoteCar() {
        socketOut.println("ADD_CAR:");
        log("Отправлен запрос на добавление машины всем клиентам");
    }

    // закрытие соединения
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

    // завершение работы приложения
    public void shutdown() {
        isRunning = false;
        closeSocket();
        carExecutor.shutdownNow(); // остановка пула потоков для машин
        refillExecutor.shutdownNow(); // остановка пула потоков для дозаправки
    }

    // запуск обработки очереди машин
    private void startQueueProcessing() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    Car car = carQueue.poll(); // берём машину из очереди
                    if (car != null) {
                        log("Обработка машины #" + car.getId() + " из очереди");
                        assignCarToPump(car); // назначение на свободную колонку
                    }
                    Thread.sleep(1000); // пауза между проверками
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log("Обработка очереди прервана");
                    break;
                }
            }
        }).start();
    }

    // назначает машину на свободную колонку
    private void assignCarToPump(Car car) {
        // поиск подходящей колонки
        Optional<GasPump> availablePump = pumps.stream()
                .filter(p -> p.isAvailable() && p.getFuelLevel() >= car.getRequiredFuel())
                .findFirst();

        if (availablePump.isPresent()) {
            GasPump pump = availablePump.get();
            pump.setAvailable(false);
            pump.setStatus("Заправка машины #" + car.getId());

            // запуск процесса заправки в отдельном потоке
            carExecutor.submit(() -> {
                try {
                    log("Машина #" + car.getId() + " заправляется на колонке #" + pump.getId() +
                            " (" + car.getRequiredFuel() + " л)");
                    Thread.sleep(car.getRequiredFuel() * 100L); // имитация времени заправки

                    pump.consumeFuel(car.getRequiredFuel());
                    log("Машина #" + car.getId() + " завершила заправку на колонке #" + pump.getId());

                    pump.setStatus("Свободна");
                    pump.setAvailable(true);

                    Platform.runLater(this::updateQueueUI); // обновление очереди
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } else {
            // если нет свободных колонок, то возвращаем машину в очередь
            Platform.runLater(() -> {
                carQueue.add(car);
                updateQueueUI();
            });
        }
    }

    // проверка необходимости дозаправки колонок
    private void checkForRefill() {
        Platform.runLater(() -> {
            // проверяем, нужно ли пополнение и можем ли мы начать
            if (isRefilling.compareAndSet(false, true)) {
                boolean needsRefill = pumps.stream()
                        .allMatch(p -> p.getFuelLevel() <= MIN_REFILL_AMOUNT);

                // логи текущих уровней топлива
                StringBuilder fuelLevels = new StringBuilder("Уровни топлива: ");
                pumps.forEach(p -> fuelLevels.append("Колонка #").append(p.getId())
                        .append(": ").append(p.getFuelLevel()).append(" л, "));
                log(fuelLevels.toString());

                if (needsRefill) {
                    log("Все колонки имеют менее или равно " + MIN_REFILL_AMOUNT + " л. Начинаем пополнение...");
                    refillAll(); // запуск дозаправки
                } else {
                    isRefilling.set(false); // сбрасываем флаг, если пополнение не требуется
                    log("Пополнение не требуется: не все колонки имеют менее или равно " + MIN_REFILL_AMOUNT + " л");
                }
            } else {
                // логируем, что пополнение уже выполняется
                log("Пополнение не начато: уже выполняется пополнение");
            }
        });
    }

    // выполняет дозаправку всех колонок
    public void refillAll() {
        carExecutor.submit(() -> {
            try {
                // устанавливаем статус заправки
                Platform.runLater(() -> {
                    pumps.forEach(p -> {
                        p.setAvailable(false);
                        p.setStatus("Заправляется");
                        log("Колонка #" + p.getId() + " установлена в статус 'Заправляется'");
                    });
                });

                // имитация времени дозаправки
                Thread.sleep(5000);

                // дозаправляем колонки
                Platform.runLater(() -> {
                    pumps.forEach(p -> {
                        p.refill();
                        p.setStatus("Свободна");
                        p.setAvailable(true);
                        updatePumpUI(p);
                        log("Колонка #" + p.getId() + " пополнена до " + p.getFuelLevel() + " л");
                    });

                    log("Все колонки пополнены");
                    isRefilling.set(false); // Сбрасываем флаг после завершения

                    // обрабатываем следующую машину, если есть
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
                log("Пополнение прервано: " + e.getMessage());
                Platform.runLater(() -> isRefilling.set(false));
            }
        });
    }

    // обновление очереди машин
    private void updateQueueUI() {
        Platform.runLater(() -> {
            ObservableList<String> items = FXCollections.observableArrayList();
            carQueue.forEach(car -> items.add("Машина #" + car.getId() + " (" + car.getRequiredFuel() + " л)"));
            queueList.setItems(items);
            log("Обновление очереди: " + items.size() + " машин");
        });
    }

    // добавление сообщения в лог
    private void log(String message) {
        Platform.runLater(() ->
                statusArea.appendText(message + "\n"));
    }

    // добавление новой машины в очередь
    public void addCar() {
        Car car = new Car();
        carQueue.add(car);
        updateQueueUI();
        log("Добавлена машина #" + car.getId() + " (нужно " + car.getRequiredFuel() + " л)");
    }
}