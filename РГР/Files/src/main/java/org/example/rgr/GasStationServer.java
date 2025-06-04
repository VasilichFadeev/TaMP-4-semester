package com.example.rgr;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GasStationServer {
    private static final int START_PORT = 12345; // начальный порт для поиска
    private static final int END_PORT = 12350; // конечный порт для поиска
    private static ServerSocket serverSocket; // сокет сервера
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // список подключенных клиентов

    // основной метод запуска сервера
    public static void main(String[] args) {
        // ищем свободный порт в заданном диапазоне
        int selectedPort = findFreePort();
        if (selectedPort == -1) {
            System.err.println("Нет доступных портов в диапазоне " + START_PORT + "-" + END_PORT);
            return;
        }

        try {
            // создаем серверный сокет на найденном порту
            serverSocket = new ServerSocket(selectedPort, 0, InetAddress.getByName("0.0.0.0"));
            System.out.println("Сервер запущен на порту " + selectedPort);

            // основной цикл обработки подключений
            while (true) {
                // принимаем новое подключение
                Socket clientSocket = serverSocket.accept();
                // создаем обработчик для клиента
                ClientHandler handler = new ClientHandler(clientSocket);
                // добавляем в список клиентов
                clients.add(handler);
                // запускаем обработчик в отдельном потоке
                new Thread(handler).start();
                System.out.println("Клиент подключён: " + handler.getClientId());
                // рассылаем обновленный список клиентов
                broadcastClientList();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при запуске сервера на порту " + selectedPort + ": " + e.getMessage());
        }
    }

    // ищет свободный порт в заданном диапазоне
    private static int findFreePort() {
        for (int port = START_PORT; port <= END_PORT; port++) {
            try (ServerSocket ss = new ServerSocket()) {
                // настраиваем сокет для повторного использования
                ss.setReuseAddress(true);
                // пробуем занять порт
                ss.bind(new InetSocketAddress(port));
                System.out.println("Найден свободный порт: " + port);
                return port;
            } catch (IOException e) {
                System.out.println("Порт " + port + " занят, пробую следующий...");
            }
        }
        return -1; // если свободных портов не найдено
    }

    // класс для обработки подключений клиентов
    static class ClientHandler implements Runnable {
        private final Socket socket; // сокет клиента
        private final String clientId; // уникальный идентификатор клиента
        private final PrintWriter out; // поток вывода
        private final BufferedReader in; // поток ввода
        private final ObjectOutputStream objectOut; // поток для отправки объектов

        // конструктор обработчика клиента
        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            // создаем ID клиента из IP и порта
            this.clientId = new StringBuilder(socket.getInetAddress().getHostAddress())
                    .append(":")
                    .append(socket.getPort())
                    .toString();
            // инициализируем потоки обмена данными
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.objectOut = new ObjectOutputStream(socket.getOutputStream());
        }

        // возвращает идентификатор клиента
        public String getClientId() {
            return clientId;
        }

        // основной метод обработки клиента
        @Override
        public void run() {
            try {
                String inputLine;
                // читаем входящие сообщения
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Получено от клиента " + clientId + ": " + inputLine);

                    // обработка разных типов сообщений
                    if (inputLine.startsWith("REQUEST_STATE:")) {
                        // запрос состояния у другого клиента
                        String targetClientId = inputLine.substring(14);
                        forwardStateRequest(targetClientId);
                    } else if (inputLine.startsWith("ADD_CAR:")) {
                        // команда добавления машины
                        broadcastAddCar();
                    } else if (inputLine.startsWith("STATE:")) {
                        // получение состояния станции
                        String[] parts = inputLine.split(":");
                        int dataLength = Integer.parseInt(parts[1]);
                        byte[] data = new byte[dataLength];
                        socket.getInputStream().read(data);
                        broadcastState(data);
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка связи с клиентом " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    // удаляем клиента из списка при отключении
                    clients.remove(this);
                    socket.close();
                    System.out.println("Клиент отключился: " + clientId);
                    // обновляем список клиентов
                    broadcastClientList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // перенаправляет запрос состояния конкретному клиенту
        private void forwardStateRequest(String targetClientId) {
            for (ClientHandler client : clients) {
                if (client.getClientId().equals(targetClientId)) {
                    client.out.println("REQUEST_STATE:" + clientId);
                    break;
                }
            }
        }

        // рассылает команду добавления машины всем клиентам
        private void broadcastAddCar() {
            for (ClientHandler client : clients) {
                if (!client.getClientId().equals(clientId)) {
                    client.out.println("ADD_CAR:");
                }
            }
        }

        // рассылает состояние станции всем клиентам
        private void broadcastState(byte[] data) {
            for (ClientHandler client : clients) {
                if (!client.getClientId().equals(clientId)) {
                    try {
                        client.out.println("STATE:" + data.length);
                        client.objectOut.write(data);
                        client.objectOut.flush();
                    } catch (IOException e) {
                        System.err.println("Ошибка отправки состояния клиенту " + client.getClientId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    // рассылает актуальный список подключенных клиентов
    private static void broadcastClientList() {
        // формируем строку со списком клиентов
        String clientList = clients.stream()
                .map(ClientHandler::getClientId)
                .collect(Collectors.joining(","));
        System.out.println("Отправка списка клиентов: CLIENT_LIST:" + clientList);
        // отправляем каждому клиенту
        for (ClientHandler client : clients) {
            client.out.println("CLIENT_LIST:" + clientList);
        }
    }
}