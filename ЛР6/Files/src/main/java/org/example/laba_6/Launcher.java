package org.example.laba_6;

public class Launcher {
    public static void main(String[] args) {
        new Thread(() -> {
            System.out.println("Starting TCPServer...");
            TCPServer.main(new String[0]);
        }).start();
        System.out.println("Starting Main application...");
        Main.main(args);
    }
}