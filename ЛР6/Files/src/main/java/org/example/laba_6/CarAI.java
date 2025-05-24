package org.example.laba_6;

import javafx.application.Platform;

public class CarAI extends BaseAI<Car> {
    private static final double AREA_WIDTH = 500;   // Ширина всей области
    private static final double AREA_HEIGHT = 679; // Высота всей области
    private static final double TARGET_AREA_WIDTH = AREA_WIDTH / 2;   // Ширина целевой области (левый верхний прямоугольник)
    private static final double TARGET_AREA_HEIGHT = AREA_HEIGHT / 2; // Высота целевой области
    private long startTime;

    public CarAI() {
    }

    @Override
    protected void updateAllAI() {
        if (!running) return;

        synchronized (lock) {
            for (Car car : objects) {
                // Если цель еще не установлена
                if (car.targetPosX == 0 && car.targetPosY == 0) {
                    double currentX = car.getImageView().getX();
                    double currentY = car.getImageView().getY();

                    // Проверяем, находится ли машина уже в целевой области (левый верхний прямоугольник)
                    if (currentX < TARGET_AREA_WIDTH && currentY < TARGET_AREA_HEIGHT) {
                        // Если уже в целевой области - остаемся на месте
                        car.targetPosX = currentX;
                        car.targetPosY = currentY;
                    } else {
                        // Иначе выбираем случайную точку в целевой области
                        car.targetPosX = Math.random() * TARGET_AREA_WIDTH;
                        car.targetPosY = Math.random() * TARGET_AREA_HEIGHT;
                    }
                }

                // Движение к цели
                double currentX = car.getImageView().getX();
                double currentY = car.getImageView().getY();
                double newX = currentX + (car.targetPosX > currentX ? car.speedX : -car.speedX);
                double newY = currentY + (car.targetPosY > currentY ? car.speedY : -car.speedY);

                Platform.runLater(() -> {
                    car.getImageView().setX(newX);
                    car.getImageView().setY(newY);
                });

                // Проверка достижения цели
                if (Math.abs(newX - car.targetPosX) < 1 && Math.abs(newY - car.targetPosY) < 1) {
                    removeObject(car);
                }
            }
        }
    }
}