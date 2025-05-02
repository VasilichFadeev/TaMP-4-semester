package org.example.laba_4;

import javafx.application.Platform;

public class CarAI extends BaseAI {
    private final Car car;
    private double targetPosX, targetPosY;

    public CarAI(Car car) {
        this.car = car;
        generateFinishPos(0, 250, 0, 340);
    }

    @Override
    public void generateFinishPos(double start_pos_x, double end_pos_x, double start_pos_y, double end_pos_y) {
        targetPosX = start_pos_x + (Math.random() * (end_pos_x - start_pos_x));
        targetPosY = start_pos_y + (Math.random() * (end_pos_y - start_pos_y));
    }

    @Override
    public void updateAI() {
        if (!running) return;

        double currentX = car.getImageView().getX();
        double currentY = car.getImageView().getY();
        double newX = currentX + (targetPosX > currentX ? car.speedX : -car.speedX);
        double newY = currentY + (targetPosY > currentY ? car.speedY : -car.speedY);

        Platform.runLater(() -> {
            car.getImageView().setX(newX);
            car.getImageView().setY(newY);
        });

        if (Math.abs(newX - targetPosX) < 1 && Math.abs(newY - targetPosY) < 1) {
            stopAI();
        }
    }
}