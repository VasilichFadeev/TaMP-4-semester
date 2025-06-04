package com.example.rgr;

import javafx.beans.property.*;

class GasPump {
    private final int id;
    private final IntegerProperty fuelLevel;
    private final StringProperty status;
    private boolean isAvailable;
    private static final int PUMP_CAPACITY = 500;

    // конструктор класса GasPump
    public GasPump(int id, int initialFuel) {
        this.id = id;
        this.fuelLevel = new SimpleIntegerProperty(initialFuel);
        this.status = new SimpleStringProperty("Свободна");
        this.isAvailable = true;
    }

    public int getId() {
        return id;
    } // порядковый номер колонки

    public int getFuelLevel() {
        return fuelLevel.get();
    } // уровень топлива в колонке

    public IntegerProperty fuelLevelProperty() {
        return fuelLevel;
    } // IntegerProperty класс из JavaFX, используем его для автоматического обновления числа уровня топлива на колонке в интерфейсе

    public StringProperty statusProperty() {
        return status;
    } // аналогично IntegerProperty, но для строковых типов данных

    public void setStatus(String status) {
        this.status.set(status);
    } //  возвращает статус колонки, например "свободна" или "занята"

    public boolean isAvailable() {
        return isAvailable;
    } // проверяет, доступна ли колонка

    public void setAvailable(boolean available) {
        isAvailable = available;
    } // устанавливает, свободна колонка или нет

    public void consumeFuel(int amount) {
        fuelLevel.set(fuelLevel.get() - amount);
    } // уменьшает уровень топлива на указанный

    public void refill() {
        fuelLevel.set(PUMP_CAPACITY);
    } // возвращает максимальную вместимость колонки

}