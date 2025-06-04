package com.example.rgr;

import java.util.concurrent.atomic.AtomicInteger;

class Car {
    private static final AtomicInteger idCounter = new AtomicInteger(1); // используем для потокобезопасности целочисленных типов
    private final int id; // id машины (пишется как #X)
    private final int requiredFuel; // сколько топлива потребуется автомобилю

    public Car() {
        this.id = idCounter.getAndIncrement(); // двигаем id на 1, getAndIncrement возвращает значение, увеличенное на 1
        this.requiredFuel = 10 + (int)(Math.random() * 41); // От 10 до 50 литров
    }

    public int getId() {
        return id;
    } // геттер id автомобиля (порядкового номера)

    public int getRequiredFuel() {
        return requiredFuel;
    } // геттер топлива, которое потратит автомобиль
}
