package org.example.laba_4_1;

import java.util.Random;

public class OilAI extends BaseAI {
    private Oil oil;
    private double targetPosX, targetPosY;
    private Random rand = new Random();

    public OilAI(Oil oil) {
        this.oil = oil;
        generateFinishPos(250, 500, 340, 679);
        System.out.println(targetPosX);
        System.out.println(targetPosY);
    }

    @Override
    public void generateFinishPos(double start_pos_x, double end_pos_x, double start_pos_y, double end_pos_y) {
        targetPosX = start_pos_x + (Math.random() * (end_pos_x - start_pos_x));
        targetPosY = start_pos_y + (Math.random() * (end_pos_y - start_pos_y));
    }

    @Override
    public void updateAI() {
        if (oil.getImageView().getX() < targetPosX) {
            oil.getImageView().setX(oil.getImageView().getX() + oil.speedX);
        } else if (oil.getImageView().getX() > targetPosX) {
            oil.getImageView().setX(oil.getImageView().getX() - oil.speedX);
        }

        if (oil.getImageView().getY() < targetPosY) {
            oil.getImageView().setY(oil.getImageView().getY() + oil.speedY);
        } else if (oil.getImageView().getY() > targetPosY) {
            oil.getImageView().setY(oil.getImageView().getY() - oil.speedY);
        }

        if (Math.abs(oil.getImageView().getX() - targetPosX) < 1 &&
                Math.abs(oil.getImageView().getY() - targetPosY) < 1) {
            stopAI();
        }
    }
}
