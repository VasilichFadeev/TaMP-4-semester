package com.example.rgr;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Класс для хранения состояния заправочной станции.
 * Реализует интерфейс Serializable для возможности сериализации/десериализации.
 */
public class GasStationState implements Serializable {
    // Уникальный идентификатор версии класса для сериализации
    private static final long serialVersionUID = 1L;

    // Список топливных колонок
    private final List<GasPump> pumps;

    // Очередь машин на заправку
    private final Queue<Car> carQueue;

    /**
     * Конструктор класса GasStationState.
     * @param pumps список топливных колонок
     * @param carQueue очередь машин
     */
    public GasStationState(List<GasPump> pumps, Queue<Car> carQueue) {
        this.pumps = pumps;
        // Создаем новую потокобезопасную очередь на основе переданной
        this.carQueue = new ConcurrentLinkedQueue<>(carQueue);
    }

    /**
     * Возвращает список топливных колонок.
     * @return список колонок
     */
    public List<GasPump> getPumps() {
        return pumps;
    }

    /**
     * Возвращает очередь машин.
     * @return очередь машин
     */
    public Queue<Car> getCarQueue() {
        return carQueue;
    }
}