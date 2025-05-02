package org.example.laba_4;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

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
            stopAllAI();
        } else {
            resumeAllAI();
        }
    }

    public boolean isSimulationPaused() {
        return simulationPaused;
    }

    public void resumeAllAI() {
        for (GameObject obj : objects) {
            if (obj instanceof Car) {
                ((Car) obj).carAI.resumeAI();
            } else if (obj instanceof Oil) {
                ((Oil) obj).oilAI.resumeAI();
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
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

    public void setCarAIPaused(boolean paused) {
        for (GameObject obj : objects) {
            if (obj instanceof Car) {
                CarAI ai = ((Car) obj).carAI;
                if (paused) ai.pauseAI();
                else ai.resumeAI();
            }
        }
    }

    public void setCarAIPriority(int priority) {
        for (GameObject obj : objects) {
            if (obj instanceof Car && ((Car) obj).carAI.thread != null) {
                ((Car) obj).carAI.thread.setPriority(priority);
            }
        }
    }

    public void setOilAIPaused(boolean paused) {
        for (GameObject obj : objects) {
            if (obj instanceof Oil) {
                OilAI ai = ((Oil) obj).oilAI;
                if (paused) ai.pauseAI();
                else ai.resumeAI();
            }
        }
    }

    public void setOilAIPriority(int priority) {
        for (GameObject obj : objects) {
            if (obj instanceof Oil && ((Oil) obj).oilAI.thread != null) {
                ((Oil) obj).oilAI.thread.setPriority(priority);
            }
        }
    }

    public void stopAllAI() {
        for (GameObject obj : objects) {
            if (obj instanceof Car) {
                ((Car) obj).carAI.stopAI();
            } else if (obj instanceof Oil) {
                ((Oil) obj).oilAI.stopAI();
            }
        }
    }
}