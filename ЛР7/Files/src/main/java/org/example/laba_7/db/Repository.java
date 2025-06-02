package org.example.laba_7.db;

import org.example.laba_7.GameObject;

import java.util.List;
// Паттерн Repository (интерфейс)
public interface Repository {
    void save(GameObject obj);
    List<GameObject> loadByType(String type);
    void deleteAll(String type);
}