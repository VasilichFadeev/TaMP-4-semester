package org.example.laba_5;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.Serializable;

public class Oil extends GameObject implements Serializable {
    public OilAI oilAI;
    double targetPosX = 0;
    double targetPosY = 0;
    private static final long serialVersionUID = 1L;


    public Oil() {
        Image image = new Image(getClass().getResourceAsStream("/oil.png"));
        imageView = new ImageView(image);
        imageView.setFitWidth(70);
        imageView.setFitHeight(70);
        imageView.setPreserveRatio(true);

        speedX = 0.5 + Math.random();
        speedY = 0.5 + Math.random();

        Habitat.getInstance().getOilAI().addObject(this);
    }

    @Override
    public void update(int maxWidth, int maxHeight) {
        checkBoundaryCollision(maxWidth, maxHeight);
    }
}