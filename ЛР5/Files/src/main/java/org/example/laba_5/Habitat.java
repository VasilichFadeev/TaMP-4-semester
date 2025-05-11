package org.example.laba_5;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.io.*;
import java.util.*;

import static org.example.laba_5.Main.*;

public class    Habitat {
    private boolean paused;
    private boolean simulationPaused;
    private static Habitat instance;
    private final Vector<GameObject> objects;
    private final HashSet<Integer> objectIds;
    private final TreeMap<Long, GameObject> birthTimeMap;
    private final Pane pane;
    private final int width;
    private final int height;
    private long simulationStartTime;
    private final CarAI carAI = new CarAI();
    private final OilAI oilAI = new OilAI();

    // Конструктор для реализации Singleton
    private Habitat(Pane pane, int width, int height) {
        this.pane = pane;
        this.width = width;
        this.height = height;
        this.objects = new Vector<>();
        this.objectIds = new HashSet<>();
        this.birthTimeMap = new TreeMap<>();
    }

    public void setSimulationPaused(boolean paused) {
        this.simulationPaused = paused;
        if (paused) {
            carAI.pauseAI();
            oilAI.pauseAI();
        } else {
            carAI.resumeAI();
            oilAI.resumeAI();
        }
    }

    public CarAI getCarAI() {
        return carAI;
    }

    public OilAI getOilAI() {
        return oilAI;
    }

    // Получение экземпляра класса с инициализацией параметров
    public static Habitat getInstance(Pane pane, int width, int height) {
        if (instance == null) {
            instance = new Habitat(pane, width, height); // Создание нового экземпляра при первом вызове
        }
        return instance;
    }

    // Получение экземпляра без параметров (должен быть предварительно инициализирован)
    public static Habitat getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Habitat не инициализирован");
        }
        return instance;
    }

    // Проверка уникальности ID объекта
    public boolean isIdUnique(int id) {
        return !objectIds.contains(id);
    }

    // Создание и добавление машины в среду
    public void spawnCar(long lifetime) {
        Car car = new Car(); // Создание нового объекта Car
        car.birthTime = System.nanoTime() - simulationStartTime; // Установка времени рождения
        car.setLifetime(lifetime); // Установка времени жизни

        objects.add(car); // Добавление в Vector
        objectIds.add(car.getId()); // Добавление ID в HashSet
        birthTimeMap.put(car.birthTime, car); // Добавление в TreeMap

        pane.getChildren().add(car.getImageView()); // Добавление на панель
        positionObject(car.getImageView()); // Позиционирование объекта
    }

    // Создание и добавление масла в среду
    public void spawnOil(long lifetime) {
        Oil oil = new Oil(); // Создание нового объекта Oil
        oil.birthTime = System.nanoTime() - simulationStartTime; // Установка времени рождения
        oil.setLifetime(lifetime); // Установка времени жизни

        objects.add(oil); // Добавление в Vector
        objectIds.add(oil.getId()); // Добавление ID в HashSet
        birthTimeMap.put(oil.birthTime, oil); // Добавление в TreeMap

        pane.getChildren().add(oil.getImageView()); // Добавление на панель
        positionObject(oil.getImageView()); // Позиционирование объекта
    }

    // Случайное позиционирование объекта на панели
    private void positionObject(ImageView imageView) {
        imageView.setX(Math.random() * (width - imageView.getFitWidth()));
        imageView.setY(Math.random() * (height - imageView.getFitHeight()));
    }

    // Обновление состояния всех объектов
    public void update() {
        if (paused) return;

        long currentTime = System.nanoTime() - simulationStartTime;
        Iterator<GameObject> iterator = objects.iterator();

        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (obj.isExpired(currentTime)) {
                removeObject(obj);
                iterator.remove();
            } else {
                obj.update(width, height);
            }
        }
    }

    // Удаление объекта из всех коллекций и с панели
    private void removeObject(GameObject obj) {
        objectIds.remove(obj.getId()); // Удаление ID
        birthTimeMap.remove(obj.birthTime); // Удаление из TreeMap
        pane.getChildren().remove(obj.getImageView()); // Удаление с панели
    }

    // Очистка всех коллекций и панели
    public void clear() {
        pane.getChildren().removeIf(node -> node instanceof ImageView); // Удаление всех ImageView
        objects.clear(); // Очистка Vector
        objectIds.clear(); // Очистка HashSet
        birthTimeMap.clear(); // Очистка TreeMap
    }

    public Vector<GameObject> getObjects() {
        return new Vector<>(objects); // Возвращает копию коллекции
    }

    public TreeMap<Long, GameObject> getBirthTimeMap() {
        return new TreeMap<>(birthTimeMap); // Возвращает копию TreeMap
    }

    public void setCarAIPaused(boolean paused) {
        if (paused) carAI.pauseAI();
        else carAI.resumeAI();
    }

    public void setCarAIPriority(int priority) {
        if (carAI.thread != null) {
            carAI.thread.setPriority(priority);
        }
    }

    public void setOilAIPaused(boolean paused) {
        if (paused) oilAI.pauseAI();
        else oilAI.resumeAI();
    }

    public void setOilAIPriority(int priority) {
        if (oilAI.thread != null) {
            oilAI.thread.setPriority(priority);
        }
    }

    public void stopAllAI() {
        carAI.stopAI();
        oilAI.stopAI();
    }

    public void saveState(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Сохраняем настройки с точками в качестве разделителей
            writer.println("CAR_SPAWN_INTERVAL=" + String.format("%.2f", CAR_SPAWN_INTERVAL / 1e9).replace(',', '.'));
            writer.println("OIL_SPAWN_INTERVAL=" + String.format("%.2f", OIL_SPAWN_INTERVAL / 1e9).replace(',', '.'));
            writer.println("CAR_LIFETIME=" + String.format("%.2f", CAR_LIFETIME / 1e9).replace(',', '.'));
            writer.println("OIL_LIFETIME=" + String.format("%.2f", OIL_LIFETIME / 1e9).replace(',', '.'));
            writer.println("CAR_SPAWN_PROBABILITY=" + String.format("%.2f", CAR_SPAWN_PROBABILITY).replace(',', '.'));
            writer.println("OIL_SPAWN_PROBABILITY=" + String.format("%.2f", OIL_SPAWN_PROBABILITY).replace(',', '.'));
            writer.println("SIMULATION_TIME=" + String.format("%.9f", (System.nanoTime() - simulationStartTime) / 1e9).replace(',', '.'));

            // Сохраняем объекты
            for (GameObject obj : objects) {
                writer.println("--- " + obj.getClass().getSimpleName());
                writer.println(obj.serialize().replace(',', '.')); // Заменяем запятые на точки
            }
        }
    }

    public void loadState(String filename) throws IOException {
        clear(); // Очищаем текущее состояние

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            List<String> objLines = new ArrayList<>();
            long simTime = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("--- ")) {
                    if (!objLines.isEmpty()) {
                        try {
                            GameObject obj = GameObject.deserialize(objLines.toArray(new String[0]));
                            objects.add(obj);
                            objectIds.add(obj.getId());
                            birthTimeMap.put(obj.birthTime, obj);
                            pane.getChildren().add(obj.getImageView());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Ошибка загрузки объекта: " + e.getMessage());
                        }
                        objLines.clear();
                    }
                    objLines.add(line);
                } else if (line.startsWith("SIMULATION_TIME=")) {
                    try {
                        String timeStr = line.split("=")[1].trim().replace(',', '.');
                        simTime = (long)(Double.parseDouble(timeStr) * 1_000_000_000L);
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка формата времени симуляции: " + line);
                    }
                } else {
                    objLines.add(line);
                }
            }

            if (!objLines.isEmpty()) {
                try {
                    GameObject obj = GameObject.deserialize(objLines.toArray(new String[0]));
                    objects.add(obj);
                    objectIds.add(obj.getId());
                    birthTimeMap.put(obj.birthTime, obj);
                    pane.getChildren().add(obj.getImageView());
                } catch (IllegalArgumentException e) {
                    System.err.println("Ошибка загрузки объекта: " + e.getMessage());
                }
            }

            simulationStartTime = System.nanoTime() - simTime;
        }
    }
}