package org.example.laba_1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.List;

public class Main extends Application {
    private MediaPlayer collisionSound;
    private MediaPlayer carCrashSound;
    private MediaPlayer oilSound;
    private Habitat habitat;
    private boolean isSimulationRunning = false;
    private long simulationStartTime = 0;
    private Text statsText;
    private boolean showTime = true;

    // Таймер для отслеживания времени с момента последнего звука коллизии
    private long lastCollisionSoundTime = 0;
    private static final long COLLISION_SOUND_COOLDOWN = 500_000_000L; // 0.5 секунды в наносекундах

    // Параметры генерации
    private static final double CAR_SPAWN_PROBABILITY = 0.8; // Вероятность генерации машины
    private static final double OIL_SPAWN_PROBABILITY = 0.4; // Вероятность генерации масла
    private static final long CAR_SPAWN_INTERVAL = 2_000_000_000L; // Интервал генерации машин (2 секунды)
    private static final long OIL_SPAWN_INTERVAL = 3_000_000_000L; // Интервал генерации масла (3 секунды)
    private long lastCarSpawnTime = 0; // Время последней генерации машины
    private long lastOilSpawnTime = 0; // Время последней генерации масла

    @Override
    public void start(Stage stage) {
        int x_size = 450; // размер окна по x
        int y_size = 600; // размер окна по y

        // Загрузка фонового изображения
        Image backgroundImage = new Image(getClass().getResourceAsStream("/background.png"), x_size, y_size, true, true);
        ImageView backgroundImageView = new ImageView(backgroundImage);

        // Создание контейнеров
        StackPane root = new StackPane();
        Pane objectPane = new Pane();
        VBox statsBox = new VBox();
        statsText = new Text();
        statsBox.getChildren().add(statsText);
        root.getChildren().addAll(backgroundImageView, objectPane, statsBox);

        // Инициализация среды
        habitat = new Habitat(objectPane, x_size, y_size);

        // Загрузка звуков
        loadSounds();

        // Обработка клавиш
        Scene scene = new Scene(root, x_size, y_size);
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case B -> startSimulation();
                case E -> {
                    statsText.setVisible(true);
                    stopSimulation();
                }
                case T -> {
                    showTime = !showTime;
                    statsText.setVisible(showTime);
                }
            }
        });

        // Главный цикл анимации
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isSimulationRunning) {
                    // Генерация машин и масла
                    spawnCars(now);
                    spawnOil(now);

                    // Обновление физики и проверка коллизий
                    habitat.update();
                    checkCollisions(now);
                    updateUI(now);
                }
            }
        }.start();

        // Настройка сцены
        stage.setTitle("Bavarskaya pogonia za maslom");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void loadSounds() {
        try {
            Media collisionMedia = new Media(getClass().getResource("/sound.wav").toString()); // звук, который играет при столкновении машины и масла
            Media crashMedia = new Media(getClass().getResource("/crash.wav").toString()); // звук, который играет при столкновении машины и машины
            Media oilMedia = new Media(getClass().getResource("/oil_sound.wav").toString()); // звук, который играет при столкновении масла и масла

            collisionSound = new MediaPlayer(collisionMedia);
            carCrashSound = new MediaPlayer(crashMedia);
            oilSound = new MediaPlayer(oilMedia);
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
        }
    }

    private void startSimulation() {
        isSimulationRunning = true;
        simulationStartTime = System.nanoTime();
        habitat.clear();

        // Перезапуск звуков
        if (collisionSound != null) collisionSound.stop();
        if (carCrashSound != null) carCrashSound.stop();
        if (oilSound != null) oilSound.stop();

        // Перезапуск звуков двигателей машин
        for (GameObject obj : habitat.getObjects()) {
            if (obj instanceof Car) {
                ((Car) obj).initEngineSound();
            }
        }
    }

    private void stopSimulation() {
        isSimulationRunning = false;
        showStatistics();

        // Остановка всех звуков
        if (collisionSound != null) collisionSound.stop();
        if (carCrashSound != null) carCrashSound.stop();
        if (oilSound != null) oilSound.stop();

        // Остановка звуков двигателей машин
        for (GameObject obj : habitat.getObjects()) {
            if (obj instanceof Car) {
                ((Car) obj).stopEngineSound();
            }
        }
    }

    double time_counter = 0;

    private void updateUI(long now) {
        if (showTime) {
            double elapsedSeconds = (now - simulationStartTime) / 1e9;
            statsText.setText(String.format("Time: %.1f s", elapsedSeconds));
            time_counter = elapsedSeconds;
        }
    }

    private void showStatistics() {
        long cars = habitat.getObjects().stream().filter(obj -> obj instanceof Car).count();
        long oils = habitat.getObjects().stream().filter(obj -> obj instanceof Oil).count();
        statsText.setFill(Color.RED);
        statsText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statsText.setText(String.format("Time: %.1f s\nCars: %d\nOils: %d", time_counter, cars, oils));
    }

    private void spawnCars(long now) {
        if (now - lastCarSpawnTime >= CAR_SPAWN_INTERVAL) {
            if (Math.random() < CAR_SPAWN_PROBABILITY) {
                habitat.spawnCar();
            }
            lastCarSpawnTime = now; // Обновление времени последней генерации
        }
    }

    private void spawnOil(long now) {
        if (now - lastOilSpawnTime >= OIL_SPAWN_INTERVAL) {
            if (Math.random() < OIL_SPAWN_PROBABILITY) {
                habitat.spawnOil();
            }
            lastOilSpawnTime = now; // Обновление времени последней генерации
        }
    }

    private void checkCollisions(long now) {
        List<GameObject> objects = habitat.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                GameObject obj1 = objects.get(i);
                GameObject obj2 = objects.get(j);
                if (isColliding(obj1.getImageView(), obj2.getImageView(), 20)) {
                    if (obj1 instanceof Car && obj2 instanceof Car) {
                        playCollisionSound(carCrashSound, now);
                    } else if (obj1 instanceof Oil && obj2 instanceof Oil) {
                        playCollisionSound(oilSound, now);
                    } else {
                        playCollisionSound(collisionSound, now);
                    }
                }
            }
        }
    }

    private boolean isColliding(ImageView a, ImageView b, double distance) {
        double ax = a.getX() + a.getFitWidth() / 2;
        double ay = a.getY() + a.getFitHeight() / 2;
        double bx = b.getX() + b.getFitWidth() / 2;
        double by = b.getY() + b.getFitHeight() / 2;

        return Math.abs(ax - bx) < distance && Math.abs(ay - by) < distance;
    }

    private void playCollisionSound(MediaPlayer player, long now) {
        if (now - lastCollisionSoundTime >= COLLISION_SOUND_COOLDOWN) {
            if (player != null) {
                player.stop();
                player.seek(player.getStartTime());
                player.play();
            }
            lastCollisionSoundTime = now;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}