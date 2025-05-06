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
    // CORRECCIÓN: Añadir variable para controlar intentos de reconexión
    private boolean reconnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private String serverIp;
    private int serverPort;

    public ClientNetworkHandler(String ip, int port) throws IOException {
        // CORRECCIÓN: Guardar IP y puerto para posibles reconexiones
        this.serverIp = ip;
        this.serverPort = port;

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

            // CORRECCIÓN: Añadir tiempo de espera para timeout de socket
            socket.setSoTimeout(10000); // 10 segundos de timeout
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
                } catch (SocketTimeoutException e) {
                    // CORRECCIÓN: Manejar timeout - podría intentar ping al servidor
                    System.out.println("Socket timeout - checking connection status");
                    if (!pingServer()) {
                        throw new IOException("Server not responding");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Connection lost: " + e.getMessage());
            e.printStackTrace();

            // CORRECCIÓN: Intento de reconexión automática
            if (!reconnecting && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                attemptReconnect();
            } else {
                disconnect();
            }
        }
    }

    // CORRECCIÓN: Método para verificar la conexión con el servidor
    private boolean pingServer() {
        try {
            if (socket.isClosed() || !socket.isConnected()) {
                return false;
            }

            // Envía un mensaje simple para verificar conexión
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

    // CORRECCIÓN: Método para intentar reconexión
    private void attemptReconnect() {
        reconnecting = true;
        reconnectAttempts++;

        new Thread(() -> {
            try {
                System.out.println("Attempting to reconnect... (Attempt " + reconnectAttempts + ")");

                // Cerrar recursos actuales
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                // Intentar nueva conexión
                socket = new Socket(serverIp, serverPort);

                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();

                in = new ObjectInputStream(socket.getInputStream());

                // Leer ID de jugador
                playerId = in.readInt();
                connected = true;
                reconnecting = false;

                socket.setSoTimeout(10000);

                System.out.println("Reconnected successfully with playerId: " + playerId);

                // Reiniciar el hilo principal
                this.run();

            } catch (IOException e) {
                System.err.println("Reconnection failed: " + e.getMessage());
                reconnecting = false;

                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    try {
                        Thread.sleep(2000); // Esperar antes de reintentar
                        attemptReconnect();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    disconnect();

                    // Notificar al cliente
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
            System.out.println("Not connected - cannot send input");
            return;
        }

        try {
            Message message = new Message("PLAYER_INPUT");
            message.input = input;
            message.playerId = playerId;

            System.out.println("Sending input: " + input + " for player: " + playerId);

            synchronized (out) {  // Sincronizar el acceso al output stream
                out.writeObject(message);
                out.flush();
                out.reset();  // Muy importante para evitar problemas de caché
            }
        } catch (IOException e) {
            System.err.println("Error sending input: " + e.getMessage());
            e.printStackTrace();

            // CORRECCIÓN: No desconectar inmediatamente, intentar reconectar
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