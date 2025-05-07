package org.example.laba_5;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Car extends GameObject {
    private MediaPlayer mediaPlayer;
    double targetPosX = 0;
    double targetPosY = 0;

    public Car() {
        Image image = new Image(getClass().getResourceAsStream("/bmw.png"));
        imageView = new ImageView(image);
        imageView.setFitWidth(120);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        speedX = 1 + Math.random() * 2;
        speedY = 1 + Math.random() * 2;

        Habitat.getInstance().getCarAI().addObject(this);

        initEngineSound();
    }

    public void initEngineSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        String mediaPath = getClass().getResource("/engine.wav").toString();
        Media sound = new Media(mediaPath);
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setVolume(0.5);
        mediaPlayer.play();
    }

    @Override
    public void update(int maxWidth, int maxHeight) {
        checkBoundaryCollision(maxWidth, maxHeight);
    }

    public void stopEngineSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}