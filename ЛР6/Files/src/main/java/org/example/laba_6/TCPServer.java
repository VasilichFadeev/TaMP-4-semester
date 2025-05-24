package org.example.laba_6;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TCPServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен на порту " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                System.out.println("Подключился клиент: " + clientHandler.getClientId());
                broadcastClientList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;
        private final PrintWriter out;
        private final BufferedReader in;
        private ObjectOutputStream objectOut;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.clientId = UUID.randomUUID().toString();
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            try {
                this.objectOut = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.err.println("Ошибка инициализации ObjectOutputStream для клиента " + clientId + ": " + e.getMessage());
            }
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
                        forwardSimulationRequest(targetClientId);
                    } else if (inputLine.startsWith("SIMULATION_STATE:")) {
                        String[] parts = inputLine.split(":");
                        int senderId = Integer.parseInt(parts[1]);
                        int dataLength = Integer.parseInt(parts[2]);
                        byte[] data = new byte[dataLength];
                        socket.getInputStream().read(data);
                        forwardSimulationState(senderId, data);
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

        private void forwardSimulationRequest(String targetClientId) {
            for (ClientHandler client : clients) {
                if (client.getClientId().equals(targetClientId)) {
                    client.out.println("REQUEST_SIMULATION:" + clientId);
                    break;
                }
            }
        }

        private void forwardSimulationState(int senderId, byte[] data) {
            for (ClientHandler client : clients) {
                if (!client.getClientId().equals(clientId)) {
                    try {
                        client.out.println("SIMULATION_STATE:" + senderId + ":" + data.length);
                        client.objectOut.write(data);
                        client.objectOut.flush();
                    } catch (IOException e) {
                        System.err.println("Ошибка отправки состояния симуляции клиенту " + client.getClientId() + ": " + e.getMessage());
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