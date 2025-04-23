package org.example.laba_4_1;

import java.util.Random;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CarAI extends BaseAI {
    private Car car;
    private Random rand = new Random();
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
        if (car.getImageView().getX() < targetPosX) {
            car.getImageView().setX(car.getImageView().getX() + car.speedX);
        } else if (car.getImageView().getX() > targetPosX) {
            car.getImageView().setX(car.getImageView().getX() - car.speedX);
        }

        if (car.getImageView().getY() < targetPosY) {
            car.getImageView().setY(car.getImageView().getY() + car.speedY);
        } else if (car.getImageView().getY() > targetPosY) {
            car.getImageView().setY(car.getImageView().getY() - car.speedY);
        }

        if (Math.abs(car.getImageView().getX() - targetPosX) < 1 &&
                Math.abs(car.getImageView().getY() - targetPosY) < 1) {
            stopAI();
        }
    }
}
