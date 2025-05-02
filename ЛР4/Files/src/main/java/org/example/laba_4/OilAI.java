package org.example.laba_4;

import javafx.application.Platform;

public class OilAI extends BaseAI {
    private final Oil oil;
    private double targetPosX, targetPosY;

    public OilAI(Oil oil) {
        this.oil = oil;
        generateFinishPos(250, 500, 340, 679);
    }

    @Override
    public void generateFinishPos(double start_pos_x, double end_pos_x, double start_pos_y, double end_pos_y) {
        double maxWidth = 500;
        double maxHeight = 679;
        targetPosX = Math.min(start_pos_x + (Math.random() * (end_pos_x - start_pos_x)), maxWidth - oil.getImageView().getFitWidth());
        targetPosY = Math.min(start_pos_y + (Math.random() * (end_pos_y - start_pos_y)), maxHeight - oil.getImageView().getFitHeight());
    }

    @Override
    public void updateAI() {
        if (!running) return;

        double currentX = oil.getImageView().getX();
        double currentY = oil.getImageView().getY();
        double newX = currentX + (targetPosX > currentX ? oil.speedX : -oil.speedX);
        double newY = currentY + (targetPosY > currentY ? oil.speedY : -oil.speedY);

        Platform.runLater(() -> {
            oil.getImageView().setX(newX);
            oil.getImageView().setY(newY);
        });

        if (Math.abs(newX - targetPosX) < 1 && Math.abs(newY - targetPosY) < 1) {
            stopAI();
        }
    }
}