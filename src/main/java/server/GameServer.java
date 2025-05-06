package server;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

import client.ClientHandler;
import game.GameState;
import messages.Message;

public class GameServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private GameState gameState;
    private boolean running = true;
    private Timer gameTimer;
    private static final int UPDATE_INTERVAL = 1000 / 30;

    public GameServer(int port) throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            gameState = new GameState();
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Could not start server on port " + port);
            throw e;
        }
    }

    public void start() {
        new Thread(() -> {
            while (running) {
                try {
                    System.out.println("Waiting for client connections...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    int playerId = clients.size();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, gameState, playerId);
                    clients.add(clientHandler);
                    clientHandler.start();
                    System.out.println("Client handler started. Total clients: " + clients.size());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        gameTimer = new Timer("GameLoop");
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    gameState.update();
                    if (!clients.isEmpty()) {
                        broadcastState();
                    }
                } catch (Exception e) {
                    System.err.println("Error in game loop: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 0, UPDATE_INTERVAL);

        System.out.println("Game loop started");
    }

    private void broadcastState() {
        if (clients.isEmpty()) return;

        Message message = new Message("UPDATE_STATE");
        message.setObjects(gameState.getGameObjects());
        message.setGameOver(gameState.isGameOver());
        message.getPlayerScores().putAll(gameState.getPlayerScores()); // Use getter method

        List<ClientHandler> disconnectedClients = new ArrayList<>();
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                System.err.println("Error sending message to client. Marking for removal.");
                disconnectedClients.add(client);
            }
        }

        for (ClientHandler client : disconnectedClients) {
            clients.remove(client);
            System.out.println("Client removed. Remaining clients: " + clients.size());
        }
    }

    public void stop() {
        running = false;
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        for (ClientHandler client : clients) {
            try {
                client.interrupt();
            } catch (Exception e) {}
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped");
    }

    public static void main(String[] args) {
        int port = 12345;
        try {
            GameServer server = new GameServer(port);
            server.start();
            System.out.println("Press Ctrl+C to stop the server");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop();
            }));
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}