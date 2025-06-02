package org.example.laba_6;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TCPServer {
    private static final int START_PORT = 12345;
    private static final int END_PORT = 12350; // Диапазон портов для поиска свободного
    private static ServerSocket serverSocket;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        int selectedPort = findFreePort();
        if (selectedPort == -1) {
            System.err.println("Нет доступных портов в диапазоне " + START_PORT + "-" + END_PORT);
            return;
        }

        try {
            serverSocket = new ServerSocket(selectedPort, 0, InetAddress.getByName("0.0.0.0"));
            System.out.println("Сервер запущен на порту " + selectedPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
                System.out.println("Клиент подключён: " + handler.getClientId());
                broadcastClientList();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при запуске сервера на порту " + selectedPort + ": " + e.getMessage());
        }
    }

    // Метод поиска первого свободного порта в диапазоне
    private static int findFreePort() {
        for (int port = START_PORT; port <= END_PORT; port++) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(port));
                System.out.println("Найден свободный порт: " + port);
                return port;
            } catch (IOException e) {
                System.out.println("Порт " + port + " занят, пробую следующий...");
            }
        }
        return -1; // Нет свободных портов
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;
        private final PrintWriter out;
        private final BufferedReader in;
        private final ObjectOutputStream objectOut;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.clientId = UUID.randomUUID().toString();
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.objectOut = new ObjectOutputStream(socket.getOutputStream());
        }

        public String getClientId() {
            return clientId;
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Получено от клиента " + clientId + ": " + inputLine);
                    if (inputLine.startsWith("REQUEST_SIMULATION:")) {
                        String targetClientId = inputLine.substring(18);
                        forwardSimulationRequest(targetClientId, false);
                    } else if (inputLine.startsWith("REQUEST_APPEND_SIMULATION:")) {
                        String targetClientId = inputLine.substring(25);
                        forwardSimulationRequest(targetClientId, true);
                    } else if (inputLine.startsWith("SIMULATION_STATE:") || inputLine.startsWith("SIMULATION_STATE_APPEND:")) {
                        String[] parts = inputLine.split(":");
                        boolean isAppend = inputLine.startsWith("SIMULATION_STATE_APPEND:");
                        int senderId = Integer.parseInt(parts[1]);
                        int dataLength = Integer.parseInt(parts[2]);
                        byte[] data = new byte[dataLength];
                        socket.getInputStream().read(data);
                        forwardSimulationState(senderId, data, isAppend);
                    }
                }
            } catch (IOException e) {
                System.err.println("Ошибка связи с клиентом " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                    System.out.println("Клиент отключился: " + clientId);
                    broadcastClientList();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void forwardSimulationRequest(String targetClientId, boolean isAppend) {
            for (ClientHandler client : clients) {
                if (client.getClientId().equals(targetClientId)) {
                    client.out.println(isAppend ? "REQUEST_APPEND_SIMULATION:" + clientId : "REQUEST_SIMULATION:" + clientId);
                    break;
                }
            }
        }

        private void forwardSimulationState(int senderId, byte[] data, boolean isAppend) {
            for (ClientHandler client : clients) {
                if (!client.getClientId().equals(clientId)) {
                    try {
                        client.out.println((isAppend ? "SIMULATION_STATE_APPEND:" : "SIMULATION_STATE:") + senderId + ":" + data.length);
                        client.objectOut.write(data);
                        client.objectOut.flush();
                    } catch (IOException e) {
                        System.err.println("Ошибка отправки состояния клиенту " + client.getClientId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void broadcastClientList() {
        String clientList = clients.stream()
                .map(ClientHandler::getClientId)
                .collect(Collectors.joining(","));
        System.out.println("Отправка списка клиентов: CLIENT_LIST:" + clientList);
        for (ClientHandler client : clients) {
            client.out.println("CLIENT_LIST:" + clientList);
        }
    }
}