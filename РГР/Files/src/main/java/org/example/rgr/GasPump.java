package org.example.rgr;

import javafx.beans.property.*;

class GasPump {
    private final int id;
    private final IntegerProperty fuelLevel;
    private final StringProperty status;
    private boolean isAvailable;
    private static final int PUMP_CAPACITY = 500;

    public GasPump(int id, int initialFuel) {
        this.id = id;
        this.fuelLevel = new SimpleIntegerProperty(initialFuel);
        this.status = new SimpleStringProperty("Свободна");
        this.isAvailable = true;
    }

    public int getId() {
        return id;
    }

    public int getFuelLevel() {
        return fuelLevel.get();
    }

    public IntegerProperty fuelLevelProperty() {
        return fuelLevel;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public void consumeFuel(int amount) {
        fuelLevel.set(fuelLevel.get() - amount);
    }

    public void refill() {
        fuelLevel.set(PUMP_CAPACITY);
    }

    public static int getPumpCapacity() {
        return PUMP_CAPACITY;
    }
}