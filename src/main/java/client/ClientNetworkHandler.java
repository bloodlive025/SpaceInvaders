package client;
import javax.swing.*;

import game.GameObject;
import messages.Message;

import java.net.*;
import java.io.*;
import java.util.*;

public class ClientNetworkHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ArrayList<GameObject> gameObjects = new ArrayList<>();
    private int score = 0;
    private boolean gameOver = false;
    private int playerId;
    private GameClient client;
    private boolean connected = false;
    private boolean reconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private String serverIp;
    private int serverPort;
    private Map<Integer, Integer> playerScores = new HashMap<>();

    public ClientNetworkHandler(String ip, int port) throws IOException {
        this.serverIp = ip;
        this.serverPort = port;
        try {
            socket = new Socket(ip, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            playerId = in.readInt();
            connected = true;
            System.out.println("Conectado al servidor con ID de jugador: " + playerId);
            socket.setSoTimeout(10000);
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            throw new IOException("Failed to connect to server at " + ip + ":" + port, e);
        }
    }

    @Override
    public void run() {
        try {
            while (connected) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        if (message.getAction().equals("UPDATE_STATE")) {
                            synchronized (this) {
                                gameObjects = message.getObjects();
                                gameOver = message.isGameOver();
                                playerScores.clear();
                                playerScores.putAll(message.getPlayerScores());
                                score = playerScores.getOrDefault(playerId, 0);
                            }
                            updateClient();
                        } else {
                            System.err.println("Received unknown object type: " + obj.getClass().getName());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading object: " + e.getMessage());
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    System.out.println("Tiempo de espera del socket - verificando estado de la conexión");
                    if (!pingServer()) {
                        throw new IOException("Server not responding");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost: " + e.getMessage());
            e.printStackTrace();
            if (!reconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                attemptReconnect();
            } else {
                disconnect();
            }
        }
    }

    private boolean pingServer() {
        try {
            if (socket.isClosed() || !socket.isConnected()) {
                return false;
            }
            synchronized (out) {
                Message ping = new Message("PING");
                out.writeObject(ping);
                out.flush();
                out.reset();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Ping failed: " + e.getMessage());
            return false;
        }
    }

    private void attemptReconnect() {
        reconnecting = true;
        reconnectAttempts++;
        new Thread(() -> {
            try {
                System.out.println("Intentando reconectar... (Intento " + reconnectAttempts + ")");
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                socket = new Socket(serverIp, serverPort);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                playerId = in.readInt();
                connected = true;
                reconnecting = false;
                socket.setSoTimeout(10000);
                System.out.println("Reconexión exitosa con ID de jugador: " + playerId);
                this.run();
            } catch (IOException e) {
                System.err.println("Reconnection failed: " + e.getMessage());
                reconnecting = false;
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    try {
                        Thread.sleep(2000);
                        attemptReconnect();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    disconnect();
                    if (client != null) {
                        SwingUtilities.invokeLater(() -> {
                            client.connectionLost();
                        });
                    }
                }
            }
        }).start();
    }

    private void updateClient() {
        if (client != null) {
            SwingUtilities.invokeLater(() -> {
                client.repaint();
            });
        }
    }

    public void sendInput(String input) {
        if (!connected) {
            System.out.println("No conectado - no se puede enviar entrada");
            return;
        }
        try {
            Message message = new Message("PLAYER_INPUT");
            message.setInput(input);
            message.setPlayerId(playerId);
            System.out.println("Enviando entrada: " + input + " para el jugador: " + playerId);
            synchronized (out) {
                out.writeObject(message);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("Error sending input: " + e.getMessage());
            e.printStackTrace();
            if (!reconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                attemptReconnect();
            } else {
                disconnect();
            }
        }
    }

    public synchronized ArrayList<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }

    public synchronized int getScore(int playerId) {
        return playerScores.getOrDefault(playerId, 0);
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setClient(GameClient client) {
        this.client = client;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void disconnect() {
        if (connected) {
            connected = false;
            System.out.println("Desconectando del servidor...");
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (client != null) {
                SwingUtilities.invokeLater(() -> {
                    client.connectionLost();
                });
            }
        }
    }
}