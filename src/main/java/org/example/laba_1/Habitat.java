package org.example.laba_1;

import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.ImageView;

public class Habitat {
    private final List<GameObject> objects = new ArrayList<>();
    private final Pane pane;
    private final int width;
    private final int height;

    public Habitat(Pane pane, int width, int height) {
        this.pane = pane;
        this.width = width;
        this.height = height;
    }

    public void spawnCar() {
        Car car = new Car();
        objects.add(car);
        pane.getChildren().add(car.getImageView());
        car.getImageView().setX(Math.random() * (width - 120));
        car.getImageView().setY(Math.random() * (height - 80));
    }

    public void spawnOil() {
        Oil oil = new Oil();
        objects.add(oil);
        pane.getChildren().add(oil.getImageView());
        oil.getImageView().setX(Math.random() * (width - 80));
        oil.getImageView().setY(Math.random() * (height - 80));
    }

    public void update() {
        for (GameObject obj : objects) {
            obj.update(width, height); // Вызов метода update из интерфейса IBehaviour
        }
    }

    public void clear() {
        pane.getChildren().removeIf(node -> node instanceof ImageView);
        objects.clear();
    }

    public List<GameObject> getObjects() {
        return new ArrayList<>(objects);
    }
}