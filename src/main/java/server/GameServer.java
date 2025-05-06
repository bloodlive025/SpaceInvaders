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
            System.out.println("Servidor iniciado en el puerto " + port);
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor en el puerto " + port);
            throw e;
        }
    }

    public void start() {
        new Thread(() -> {
            while (running) {
                try {
                    System.out.println("Esperando conexiones de clientes...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());
                    int playerId = clients.size();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, gameState, playerId);
                    clients.add(clientHandler);
                    clientHandler.start();
                    System.out.println("Manejador de cliente iniciado. Total de clientes: " + clients.size());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error al aceptar la conexión del cliente: " + e.getMessage());
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
                    System.err.println("Error en el bucle del juego: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 0, UPDATE_INTERVAL);

        System.out.println("Bucle del juego iniciado");
    }

    private void broadcastState() {
        if (clients.isEmpty()) return;

        Message message = new Message("UPDATE_STATE");
        message.setObjects(gameState.getGameObjects());
        message.setGameOver(gameState.isGameOver());
        message.getPlayerScores().putAll(gameState.getPlayerScores());

        List<ClientHandler> disconnectedClients = new ArrayList<>();
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                System.err.println("Error al enviar mensaje al cliente. Marcando para eliminación.");
                disconnectedClients.add(client);
            }
        }

        for (ClientHandler client : disconnectedClients) {
            clients.remove(client);
            System.out.println("Cliente eliminado. Clientes restantes: " + clients.size());
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
        System.out.println("Servidor detenido");
    }

    public static void main(String[] args) {
        int port = 12345;
        try {
            GameServer server = new GameServer(port);
            server.start();
            System.out.println("Presiona Ctrl+C para detener el servidor");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Apagando el servidor...");
                server.stop();
            }));
        } catch (IOException e) {
            System.err.println("El servidor no pudo iniciarse: " + e.getMessage());
            e.printStackTrace();
        }
    }
}