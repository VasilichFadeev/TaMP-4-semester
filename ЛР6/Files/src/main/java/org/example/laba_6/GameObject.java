package org.example.laba_6;

import javafx.scene.image.ImageView;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class GameObject implements IBehaviour, Serializable {
    protected ImageView imageView;
    protected double speedX;
    protected double speedY;
    protected long birthTime; // Время рождения объекта (наносекунды)
    protected long lifetime;  // Время жизни объекта (наносекунды)
    protected int id;         // Уникальный идентификатор объекта
    private static final long serialVersionUID = 1L;

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

    // GameObject.java
    public synchronized void setPosition(double x, double y) {
        imageView.setX(x);
        imageView.setY(y);
    }

    public synchronized double getX() {
        return imageView.getX();
    }

    public synchronized double getY() {
        return imageView.getY();
    }

    public String serialize() {
        return String.format("id=%d\nbirthTime=%.2f\nlifetime=%.2f\nx=%.2f\ny=%.2f\nspeedX=%.2f\nspeedY=%.2f",
                id, birthTime / 1e9, lifetime / 1e9,
                getX(), getY(), speedX, speedY);
    }

    public static GameObject deserialize(String[] lines) {
        Map<String, String> props = new HashMap<>();
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length == 2) {
                props.put(parts[0].trim(), parts[1].trim().replace(',', '.')); // Заменяем запятые на точки
            }
        }

        // Проверяем, что все обязательные поля присутствуют
        if (!props.containsKey("id") || !props.containsKey("birthTime") ||
                !props.containsKey("lifetime") || !props.containsKey("x") ||
                !props.containsKey("y") || !props.containsKey("speedX") ||
                !props.containsKey("speedY")) {
            throw new IllegalArgumentException("Недостаточно данных для десериализации объекта");
        }

        GameObject obj;
        if (lines[0].contains("Car")) {
            obj = new Car();
        } else {
            obj = new Oil();
        }

        try {
            obj.id = Integer.parseInt(props.get("id"));
            obj.birthTime = (long) (parseDoubleWithComma(props.get("birthTime")) * 1_000_000_000L);
            obj.lifetime = (long) (parseDoubleWithComma(props.get("lifetime")) * 1_000_000_000L);
            obj.setPosition(
                    parseDoubleWithComma(props.get("x")),
                    parseDoubleWithComma(props.get("y"))
            );
            obj.speedX = parseDoubleWithComma(props.get("speedX"));
            obj.speedY = parseDoubleWithComma(props.get("speedY"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ошибка формата данных при десериализации: " + e.getMessage(), e);
        }

        return obj;
    }

    private static double parseDoubleWithComma(String value) {
        return Double.parseDouble(value.replace(',', '.'));
    }
}

