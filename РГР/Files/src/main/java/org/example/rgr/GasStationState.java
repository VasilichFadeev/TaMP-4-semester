package org.example.rgr;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GasStationState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<GasPump> pumps;
    private final Queue<Car> carQueue;

    public GasStationState(List<GasPump> pumps, Queue<Car> carQueue) {
        this.pumps = pumps;
        this.carQueue = new ConcurrentLinkedQueue<>(carQueue);
    }

    public List<GasPump> getPumps() {
        return pumps;
    }

    public Queue<Car> getCarQueue() {
        return carQueue;
    }
}