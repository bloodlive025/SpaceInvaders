import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameState gameState;
    private int playerId;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, GameState gameState, int playerId) throws IOException {
        this.socket = socket;
        this.gameState = gameState;
        this.playerId = playerId;

        try {
            // Set socket timeout to avoid hanging on read operations
            socket.setSoTimeout(10000); // 10 seconds timeout

            // Initialize output stream first
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();

            // Initialize input stream
            this.in = new ObjectInputStream(socket.getInputStream());

            // Send player ID to client
            this.out.writeInt(playerId);
            this.out.flush();

            // Add player to game state
            gameState.addPlayer(playerId);

            // Send initial game state
            sendInitialState();

            System.out.println("ClientHandler initialized for player: " + playerId);
        } catch (IOException e) {
            System.err.println("Error initializing client handler for player " + playerId + ": " + e.getMessage());
            closeResources();
            throw e;
        }
    }

    private void sendInitialState() throws IOException {
        Message initialState = new Message("UPDATE_STATE");
        initialState.objects = gameState.getGameObjects();
        initialState.score = gameState.getScore(playerId);
        initialState.gameOver = gameState.isGameOver();
        initialState.playerScores.putAll(gameState.getPlayerScores());

        sendMessage(initialState);
        System.out.println("Initial game state sent to player: " + playerId);
    }

    @Override
    public void run() {
        try {
            while (running && !socket.isClosed()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        System.out.println("Received message from player " + playerId + ": " + message);

                        if (message.action.equals("PLAYER_INPUT")) {
                            System.out.println("Player " + playerId + " input: " + message.input);
                            gameState.handleInput(playerId, message.input);

                            // Send immediate update after input
                            sendMessage(createUpdateMessage());
                        }
                    } else {
                        System.err.println("Received unknown object type from player " + playerId + ": " + (obj != null ? obj.getClass().getName() : "null"));
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading message from client " + playerId + ": " + e.getMessage());
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    System.out.println("Socket timeout for player " + playerId + " - checking connection status");
                    if (!checkConnection()) {
                        throw new IOException("Client not responding");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client " + playerId + " disconnected: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private boolean checkConnection() {
        try {
            if (socket.isClosed() || !socket.isConnected()) {
                return false;
            }
            // Send a ping message to check if client is still responsive
            Message ping = new Message("PING");
            sendMessage(ping);
            return true;
        } catch (IOException e) {
            System.err.println("Connection check failed for player " + playerId + ": " + e.getMessage());
            return false;
        }
    }

    private Message createUpdateMessage() {
        Message update = new Message("UPDATE_STATE");
        update.objects = gameState.getGameObjects();
        update.score = gameState.getScore(playerId);
        update.gameOver = gameState.isGameOver();
        update.playerScores.putAll(gameState.getPlayerScores());
        return update;
    }

    public void sendMessage(Message message) throws IOException {
        if (socket.isClosed()) {
            throw new IOException("Socket is closed for player " + playerId);
        }

        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client " + playerId + ": " + e.getMessage());
            disconnect();
            throw e;
        }
    }

    private void disconnect() {
        running = false;
        closeResources();
        gameState.removePlayer(playerId);
        System.out.println("Client handler for player " + playerId + " disconnected");
    }

    private void closeResources() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing input stream for player " + playerId + ": " + e.getMessage());
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing output stream for player " + playerId + ": " + e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Client socket closed for player: " + playerId);
            }
        } catch (IOException e) {
            System.err.println("Error closing socket for player " + playerId + ": " + e.getMessage());
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        disconnect();
    }
}