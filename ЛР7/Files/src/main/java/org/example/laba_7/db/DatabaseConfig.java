package org.example.laba_7.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// docker-compose - конфигурация для docker compose. Он используется для определения и запуска postgresql. В services определяются сервисы, которые будут запущены (у нас только postgres). Там где image: ... - это оффициальный образ postgresql из docker hub. Затем настраиваем окружение, куда монтируется том postgres_data, для сохранения базы после перезапуска контейнера и определение самого тома.
// DatabaseConfig - нужен для установки соединения с бд postgre. В URL подключаемся к бд (используется jdbc, который подключается к ней. также задаем postgre и остальные данные для коннекта)
// Если надо сказать все изменения по проекту, то в module-info добавляем команду для возможности использования java sql
public class DatabaseConfig {
    private static final String URL = "jdbc:postgresql://localhost:5432/Bavarskaya_pogonia_za_maslom";
    private static final String USER = "user";
    private static final String PASSWORD = "12345";
// Сам коннект. drivermanager (который является хранилищем драйверов jdbc использует драйвер getconnection для подключения)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}