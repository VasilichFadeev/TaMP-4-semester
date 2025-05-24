package org.example.laba_6;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SimulationState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<CarState> cars;
    private final List<OilState> oils;
    private final long simulationTime;

    public SimulationState(List<Car> cars, List<Oil> oils, long simulationTime) {
        this.cars = new ArrayList<>();
        for (Car car : cars) {
            this.cars.add(new CarState(car));
        }
        this.oils = new ArrayList<>();
        for (Oil oil : oils) {
            this.oils.add(new OilState(oil));
        }
        this.simulationTime = simulationTime;
    }

    public List<CarState> getCars() {
        return cars;
    }

    public List<OilState> getOils() {
        return oils;
    }

    public long getSimulationTime() {
        return simulationTime;
    }
}

class CarState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double x;
    private final double y;
    private final double speedX;
    private final double speedY;
    private final long birthTime;
    private final long lifetime;
    private final double targetPosX;
    private final double targetPosY;

    public CarState(Car car) {
        this.x = car.getX();
        this.y = car.getY();
        this.speedX = car.speedX;
        this.speedY = car.speedY;
        this.birthTime = car.birthTime;
        this.lifetime = car.lifetime;
        this.targetPosX = car.targetPosX;
        this.targetPosY = car.targetPosY;
    }

    public Car createCar() {
        Car car = new Car();
        car.getImageView().setX(x);
        car.getImageView().setY(y);
        car.speedX = speedX;
        car.speedY = speedY;
        car.birthTime = birthTime;
        car.lifetime = lifetime;
        car.targetPosX = targetPosX;
        car.targetPosY = targetPosY;
        return car;
    }
}

class OilState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double x;
    private final double y;
    private final double speedX;
    private final double speedY;
    private final long birthTime;
    private final long lifetime;
    private final double targetPosX;
    private final double targetPosY;

    public OilState(Oil oil) {
        this.x = oil.getX();
        this.y = oil.getY();
        this.speedX = oil.speedX;
        this.speedY = oil.speedY;
        this.birthTime = oil.birthTime;
        this.lifetime = oil.lifetime;
        this.targetPosX = oil.targetPosX;
        this.targetPosY = oil.targetPosY;
    }

    public Oil createOil() {
        Oil oil = new Oil();
        oil.getImageView().setX(x);
        oil.getImageView().setY(y);
        oil.speedX = speedX;
        oil.speedY = speedY;
        oil.birthTime = birthTime;
        oil.lifetime = lifetime;
        oil.targetPosX = targetPosX;
        oil.targetPosY = targetPosY;
        return oil;
    }
}