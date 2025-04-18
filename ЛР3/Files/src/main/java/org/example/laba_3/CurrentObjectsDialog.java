package org.example.laba_3;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.util.Map;
import java.util.TreeMap;

// Диалоговое окно для отображения текущих объектов в симуляции.
public class CurrentObjectsDialog extends Dialog<Void> {

    // Конструктор диалогового окна.
    public CurrentObjectsDialog(TreeMap<Long, GameObject> birthTimeMap) {
        // Настройка заголовков окна
        setTitle("Текущие объекты");
        setHeaderText("Список всех живых объектов");

        // Создание таблицы для отображения объектов
        TableView<ObjectInfo> table = new TableView<>();

        // Колонка для типа объекта (Машина/Масло)
        TableColumn<ObjectInfo, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Колонка для уникального ID объекта
        TableColumn<ObjectInfo, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Колонка для времени рождения (переведенного в секунды)
        TableColumn<ObjectInfo, String> birthTimeCol = new TableColumn<>("Время рождения (сек)");
        birthTimeCol.setCellValueFactory(new PropertyValueFactory<>("birthTime"));

        // Добавление колонок в таблицу
        table.getColumns().addAll(typeCol, idCol, birthTimeCol);

        // Заполнение таблицы данными из переданной TreeMap
        for (Map.Entry<Long, GameObject> entry : birthTimeMap.entrySet()) {
            GameObject obj = entry.getValue();
            table.getItems().add(new ObjectInfo(
                    obj instanceof Car ? "Машина" : "Масло",  // Определение типа объекта
                    String.valueOf(obj.getId()),              // Получение ID
                    String.format("%.2f", entry.getKey() / 1e9)  // Перевод времени в секунды
            ));
        }

        // Добавление таблицы в окно и кнопки OK
        getDialogPane().setContent(new VBox(table));
        getDialogPane().getButtonTypes().add(ButtonType.OK);
    }

    // Вспомогательный класс для хранения информации об объекте
    public static class ObjectInfo {
        private final String type;      // Тип объекта (Машина/Масло)
        private final String id;        // Уникальный идентификатор
        private final String birthTime; // Время рождения в секундах

        public ObjectInfo(String type, String id, String birthTime) {
            this.type = type;
            this.id = id;
            this.birthTime = birthTime;
        }

        // Геттеры для доступа к полям (используются PropertyValueFactory в TableColumn)
        public String getType() { return type; }
        public String getId() { return id; }
        public String getBirthTime() { return birthTime; }
    }
}