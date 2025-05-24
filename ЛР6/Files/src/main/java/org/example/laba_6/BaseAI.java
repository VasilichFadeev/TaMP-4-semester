package org.example.laba_6;

import javafx.application.Platform;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BaseAI<T extends GameObject> {
    public volatile boolean running = true;
    public volatile boolean paused = false;
    public Thread thread;
    public final Object lock = new Object();
    protected final List<T> objects = new CopyOnWriteArrayList<>();

    public void addObject(T object) {
        synchronized (lock) {
            objects.add(object);
        }
    }

    public void removeObject(T object) {
        synchronized (lock) {
            objects.remove(object);
        }
    }

    public void pauseAI() {
        synchronized (lock) {
            paused = true;
        }
    }

    public void resumeAI() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }

        if ((thread == null || !thread.isAlive()) && running) {
            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && running) {
                    synchronized (lock) {
                        while (paused && running) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    if (!running) break;

                    Platform.runLater(this::updateAllAI);

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
            lock.notifyAll();
        }
    }

    protected abstract void updateAllAI();
}