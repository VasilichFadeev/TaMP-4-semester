package org.example.laba_4_1;

import javafx.scene.image.Image;

public abstract class BaseAI {
    private boolean ai_on = true;
    protected Thread thread;
    protected double targetPosX;
    protected double targetPosY;
    public abstract void generateFinishPos(double start_pos_x, double end_pos_x, double start_pos_y, double end_pos_y);
    public abstract void updateAI();
    public void stopAI() {
        ai_on = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
