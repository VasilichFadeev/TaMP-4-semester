package org.example.laba_4_1;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Oil extends GameObject {
    private OilAI oilAI;
    public Oil() {
        Image image = new Image(getClass().getResourceAsStream("/oil.png"));
        imageView = new ImageView(image);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        speedX = 1.5 + Math.random() * 1.5;
        speedY = 1.5 + Math.random() * 1.5;

        oilAI = new OilAI(this);
    }

    public double getSpeedX() {
        return speedX;
    }

    public double getSpeedY() {
        return speedY;
    }

    @Override
    public void update(int maxWidth, int maxHeight) {
        double speedX = this.getSpeedX(); // Используем this для доступа к нестатическим методам
        double speedY = this.getSpeedY();// Обновление состояния объектов
        oilAI.updateAI();
        checkBoundaryCollision(maxWidth, maxHeight);
    }
}