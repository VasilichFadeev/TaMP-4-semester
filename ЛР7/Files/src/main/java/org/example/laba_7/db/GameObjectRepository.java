package org.example.laba_7.db;

import org.example.laba_7.Car;
import org.example.laba_7.GameObject;
import org.example.laba_7.Oil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameObjectRepository implements Repository {
    public GameObjectRepository() {
        initTable();
    }

    // Запись в таблицу
    @Override
    public void save(GameObject obj) {

        String sql = "INSERT INTO game_objects (id, type, x, y, speed_x, speed_y, birth_time, lifetime, target_pos_x, target_pos_y) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, obj.getId());
            pstmt.setString(2, obj.getClass().getSimpleName());
            pstmt.setDouble(3, obj.getX());
            pstmt.setDouble(4, obj.getY());
            pstmt.setDouble(5, obj.speedX);
            pstmt.setDouble(6, obj.speedY);
            pstmt.setLong(7, obj.birthTime);
            pstmt.setLong(8, obj.lifetime);

            if (obj instanceof Car) {
                Car car = (Car) obj;
                pstmt.setDouble(9, car.targetPosX);
                pstmt.setDouble(10, car.targetPosY);
            } else {
                pstmt.setNull(9, Types.DOUBLE);
                pstmt.setNull(10, Types.DOUBLE);
            }

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Загрузка таблицы
    @Override
    public List<GameObject> loadByType(String type) {
        List<GameObject> objects = new ArrayList<>();
        String sql = "SELECT * FROM game_objects WHERE type = ?";


        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, type);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                GameObject obj;

                if ("Car".equals(type)) {
                    Car car = new Car();
                    car.targetPosX = rs.getDouble("target_pos_x");
                    car.targetPosY = rs.getDouble("target_pos_y");
                    car.initEngineSound();
                    obj = car;
                } else {
                    obj = new Oil();
                }

                // Установка общих свойств
                int id = rs.getInt("id");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double speedX = rs.getDouble("speed_x");
                double speedY = rs.getDouble("speed_y");
                long birthTime = rs.getLong("birth_time");
                long lifetime = rs.getLong("lifetime");

                obj.id = id;
                obj.setPosition(x, y);
                obj.speedX = speedX;
                obj.speedY = speedY;
                obj.birthTime = birthTime;
                obj.lifetime = lifetime;

                objects.add(obj);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return objects;
    }

    // SQL командой удаление значений в таблице
    @Override
    public void deleteAll(String type) {
        String sql = "DELETE FROM game_objects WHERE type = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Создание таблицы game_objects
    public static void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS game_objects (
                id INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                x DOUBLE PRECISION NOT NULL,
                y DOUBLE PRECISION NOT NULL,
                speed_x DOUBLE PRECISION NOT NULL,
                speed_y DOUBLE PRECISION NOT NULL,
                birth_time BIGINT NOT NULL,
                lifetime BIGINT NOT NULL,
                target_pos_x DOUBLE PRECISION,
                target_pos_y DOUBLE PRECISION,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE OR REPLACE FUNCTION clear_objects_by_type(obj_type VARCHAR)
            RETURNS VOID AS $$
            BEGIN
                DELETE FROM game_objects WHERE type = obj_type;
            END;
            $$ LANGUAGE plpgsql;
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
