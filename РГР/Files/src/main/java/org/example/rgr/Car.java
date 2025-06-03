package org.example.rgr;

import java.util.concurrent.atomic.AtomicInteger;

class Car {
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final int id;
    private final int requiredFuel;

    public Car() {
        this.id = idCounter.getAndIncrement();
        this.requiredFuel = 10 + (int)(Math.random() * 41); // От 10 до 50 литров
    }

    public int getId() {
        return id;
    }

    public int getRequiredFuel() {
        return requiredFuel;
    }
}
