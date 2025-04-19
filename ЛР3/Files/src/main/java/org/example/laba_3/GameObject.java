package org.example.laba_3;

import javafx.scene.image.ImageView;
import java.util.Objects;

public abstract class GameObject implements IBehaviour {
    protected ImageView imageView;
    protected double speedX;
    protected double speedY;
    protected long birthTime; // Время рождения объекта (наносекунды)
    protected long lifetime;  // Время жизни объекта (наносекунды)
    protected int id;         // Уникальный идентификатор объекта

    public GameObject() {
        this.id = generateUniqueId();
    }

    // Генерирует уникальный идентификатор для объекта
    private int generateUniqueId() {
        int newId;
        do {
            newId = (int)(Math.random() * Integer.MAX_VALUE);
        } while (!Habitat.getInstance().isIdUnique(newId));
        return newId;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public long getBirthTime() {
        return birthTime;
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    public int getId() {
        return id;
    }

    // Проверяет, истекло ли время жизни объекта
    public boolean isExpired(long currentTime) {
        return currentTime - birthTime >= lifetime;
    }

    @Override
    public abstract void update(int maxWidth, int maxHeight);

    // Обрабатывает столкновение с границами области
    protected void checkBoundaryCollision(double maxWidth, double maxHeight) {
        double x = imageView.getX();
        double y = imageView.getY();
        double width = imageView.getFitWidth();
        double height = imageView.getFitHeight();

        if (x <= 0 || x + width >= maxWidth) speedX = -speedX;
        if (y <= 0 || y + height >= maxHeight) speedY = -speedY;

        imageView.setX(Math.max(0, Math.min(x, maxWidth - width)));
        imageView.setY(Math.max(0, Math.min(y, maxHeight - height)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameObject that = (GameObject) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + id +
                ", birthTime=" + birthTime +
                ", lifetime=" + lifetime + "]";
    }
}