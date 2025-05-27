package org.example.laba_6;

public class Launcher {
    public static void main(String[] args) {
        // Start TCP server in a separate thread
        new Thread(() -> {
            System.out.println("Starting TCPServer...");
            TCPServer.main(new String[0]);
        }).start();

        // Start JavaFX client application
        System.out.println("Starting Main application...");
        Main.main(args);
    }
}