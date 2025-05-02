package org.example.laba_4;

import javafx.application.Platform;

public abstract class BaseAI {
    public volatile boolean running = true;
    public volatile boolean paused = false;
    public Thread thread;
    private final Object lock = new Object(); // Объект для синхронизации

    public void pauseAI() {
        synchronized (lock) {
            paused = true;
        }
    }

    public void resumeAI() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll(); // Пробуждаем все ожидающие потоки
        }

        if ((thread == null || !thread.isAlive()) && running) {
            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && running) {
                    synchronized (lock) {
                        while (paused && running) {
                            try {
                                lock.wait(); // Ожидаем снятия паузы
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    if (!running) break;

                    Platform.runLater(this::updateAI);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }
    }

    public void stopAI() {
        synchronized (lock) {
            running = false;
            paused = false;
            lock.notifyAll(); // Пробуждаем поток для завершения
        }
    }

    public abstract void updateAI();
    public abstract void generateFinishPos(double start_pos_x, double end_pos_x, double start_pos_y, double end_pos_y);
}