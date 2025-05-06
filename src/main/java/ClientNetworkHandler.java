import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

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

    public ClientNetworkHandler(String ip, int port) throws IOException {
        try {
            socket = new Socket(ip, port);

            // CRÍTICO: primero crear el output stream y flush antes de crear el input stream
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            // Lee el ID del jugador como un entero directo
            playerId = in.readInt();
            connected = true;
            System.out.println("Connected to server with playerId: " + playerId);
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
                        if (message.action.equals("UPDATE_STATE")) {
                            synchronized (this) {
                                gameObjects = message.objects;
                                score = message.score;
                                gameOver = message.gameOver;
                            }
                            updateClient();
                        }
                    } else {
                        System.err.println("Received unknown object type: " + obj.getClass().getName());
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading object: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost: " + e.getMessage());
            e.printStackTrace();
        } finally {
            disconnect();
        }
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
            System.out.println("Not connected - cannot send input");
            return;
        }

        try {
            Message message = new Message("PLAYER_INPUT");
            message.input = input;
            message.playerId = playerId;

            synchronized (out) {  // Sincronizar el acceso al output stream
                out.writeObject(message);
                out.flush();
                out.reset();  // Muy importante para evitar problemas de caché
            }
        } catch (IOException e) {
            System.err.println("Error sending input: " + e.getMessage());
            e.printStackTrace();
            disconnect();
        }
    }

    public synchronized ArrayList<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }

    public synchronized int getScore() {
        return score;
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
            System.out.println("Disconnecting from server...");
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Notificar al cliente
            if (client != null) {
                SwingUtilities.invokeLater(() -> {
                    client.connectionLost();
                });
            }
        }
    }
}