package org.example.laba_2;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Oil extends GameObject {
    public Oil() {
        Image image = new Image(getClass().getResourceAsStream("/oil.png"));
        imageView = new ImageView(image);
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        speedX = 1.5 + Math.random() * 1.5;
        speedY = 1.5 + Math.random() * 1.5;
    }

    @Override
    public void update(int maxWidth, int maxHeight) {
        imageView.setX(imageView.getX() + speedX);
        imageView.setY(imageView.getY() + speedY);
        checkBoundaryCollision(maxWidth, maxHeight);
    }
}