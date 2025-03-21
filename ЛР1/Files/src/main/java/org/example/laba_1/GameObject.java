package org.example.laba_1;

import javafx.scene.image.ImageView;

public abstract class GameObject implements IBehaviour {
    protected ImageView imageView;
    protected double speedX;
    protected double speedY;

    public ImageView getImageView() {
        return imageView;
    }

    @Override // Реализация метода update из интерфейса IBehaviour
    public abstract void update(int maxWidth, int maxHeight);

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
}