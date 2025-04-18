package org.example.laba_3;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Car extends GameObject {
    private MediaPlayer mediaPlayer;

    public Car() {
        // Загрузка изображения машины
        Image image = new Image(getClass().getResourceAsStream("/bmw.png"));
        imageView = new ImageView(image);
        imageView.setFitWidth(120);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        // Установка случайной скорости
        speedX = 1 + Math.random() * 2;
        speedY = 1 + Math.random() * 2;

        // Инициализация звука двигателя
        initEngineSound();
    }

    public void initEngineSound() {
        // Если MediaPlayer уже существует, происходит остановка и освобождение ресурсов
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        // Создаем новый MediaPlayer для звука двигателя
        String mediaPath = getClass().getResource("/engine.wav").toString();
        Media sound = new Media(mediaPath);
        mediaPlayer = new MediaPlayer(sound);

        // Настройка MediaPlayer
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Бесконечное воспроизведение
        mediaPlayer.setVolume(0.5); // Установка громкости

        // Запуск воспроизведения звука
        mediaPlayer.play();
    }

    @Override
    public void update(int maxWidth, int maxHeight) { // Обновление состояния объектов
        imageView.setX(imageView.getX() + speedX);
        imageView.setY(imageView.getY() + speedY);
        checkBoundaryCollision(maxWidth, maxHeight);
    }

    public void stopEngineSound() { // Функция для остановки воспроизведения звука двигателя
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}