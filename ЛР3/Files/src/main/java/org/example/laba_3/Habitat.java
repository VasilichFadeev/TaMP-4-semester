package org.example.laba_3;

import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;  // Правильный импорт для ImageView
import java.util.*;

public class Habitat {
    private static Habitat instance;
    private final Vector<GameObject> objects;
    private final HashSet<Integer> objectIds;
    private final TreeMap<Long, GameObject> birthTimeMap;
    private final Pane pane;
    private final int width;
    private final int height;
    private long simulationStartTime;

    // Конструктор для реализации Singleton
    private Habitat(Pane pane, int width, int height) {
        this.pane = pane;
        this.width = width;
        this.height = height;
        this.objects = new Vector<>();
        this.objectIds = new HashSet<>();
        this.birthTimeMap = new TreeMap<>();
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
        long currentTime = System.nanoTime() - simulationStartTime; // Текущее время симуляции
        Iterator<GameObject> iterator = objects.iterator(); // Итератор для безопасного удаления

        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (obj.isExpired(currentTime)) { // Проверка истечения времени жизни
                removeObject(obj); // Удаление объекта
                iterator.remove(); // Удаление из коллекции
            } else {
                obj.update(width, height); // Обновление позиции объекта
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

    public void setSimulationStartTime(long simulationStartTime) {
        this.simulationStartTime = simulationStartTime; // Установка времени начала симуляции
    }

    public int getObjectCount() {
        return objects.size(); // Общее количество объектов
    }

    public int getCarCount() {
        return (int) objects.stream().filter(obj -> obj instanceof Car).count(); // Количество машин
    }

    public int getOilCount() {
        return (int) objects.stream().filter(obj -> obj instanceof Oil).count(); // Количество машин
    }
}