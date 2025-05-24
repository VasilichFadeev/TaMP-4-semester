package org.example.laba_6;

import javafx.application.Platform;

public class OilAI extends BaseAI<Oil> {
    private static final double AREA_WIDTH = 500;   // Ширина всей области
    private static final double AREA_HEIGHT = 679; // Высота всей области
    private static final double TARGET_AREA_WIDTH = AREA_WIDTH / 2;   // Ширина целевой области (правый нижний прямоугольник)
    private static final double TARGET_AREA_HEIGHT = AREA_HEIGHT / 2; // Высота целевой области

    public OilAI() {
    }

    @Override
    protected void updateAllAI() {
        if (!running) return;

        synchronized (lock) {
            for (Oil oil : objects) {
                // Если цель еще не установлена
                if (oil.targetPosX == 0 && oil.targetPosY == 0) {
                    double currentX = oil.getImageView().getX();
                    double currentY = oil.getImageView().getY();

                    // Проверяем, находится ли масло уже в целевой области (правый нижний прямоугольник)
                    if (currentX >= TARGET_AREA_WIDTH && currentY >= TARGET_AREA_HEIGHT) {
                        // Если уже в целевой области - остаемся на месте
                        oil.targetPosX = currentX;
                        oil.targetPosY = currentY;
                    } else {
                        // Иначе выбираем случайную точку в целевой области
                        oil.targetPosX = TARGET_AREA_WIDTH + Math.random() * TARGET_AREA_WIDTH;
                        oil.targetPosY = TARGET_AREA_HEIGHT + Math.random() * TARGET_AREA_HEIGHT;

                        // Убедимся, что не выходим за границы
                        oil.targetPosX = Math.min(oil.targetPosX, AREA_WIDTH - oil.getImageView().getFitWidth());
                        oil.targetPosY = Math.min(oil.targetPosY, AREA_HEIGHT - oil.getImageView().getFitHeight());
                    }
                }

                // Движение к цели
                double currentX = oil.getImageView().getX();
                double currentY = oil.getImageView().getY();
                double newX = currentX + (oil.targetPosX > currentX ? oil.speedX : -oil.speedX);
                double newY = currentY + (oil.targetPosY > currentY ? oil.speedY : -oil.speedY);

                Platform.runLater(() -> {
                    oil.getImageView().setX(newX);
                    oil.getImageView().setY(newY);
                });

                // Проверка достижения цели
                if (Math.abs(newX - oil.targetPosX) < 1 && Math.abs(newY - oil.targetPosY) < 1) {
                    removeObject(oil);
                }
            }
        }
    }
}