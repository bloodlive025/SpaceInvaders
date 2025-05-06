import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameState gameState;
    private int playerId;
    private boolean running = true;

    public ClientHandler(Socket socket, GameState gameState, int playerId) throws IOException {
        this.socket = socket;
        this.gameState = gameState;
        this.playerId = playerId;

        try {
            // CRÍTICO: Primero crear el output stream antes del input stream
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush(); // Importante para establecer el header del stream

            // Ahora el input stream puede leer correctamente
            this.in = new ObjectInputStream(socket.getInputStream());

            // Envía primero el ID del jugador como un entero directo
            this.out.writeInt(playerId);
            this.out.flush();

            // Agregar jugador al estado del juego después de la configuración inicial
            gameState.addPlayer(playerId);

            // CORRECCIÓN: Enviar estado inicial del juego justo después de la conexión
            sendInitialState();

            System.out.println("ClientHandler initialized for player: " + playerId);
        } catch (IOException e) {
            System.err.println("Error initializing client handler: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ex) {
                // Ignorar
            }
            throw e;
        }
    }

    // CORRECCIÓN: Añadir método para enviar el estado inicial del juego
    private void sendInitialState() throws IOException {
        Message initialState = new Message("UPDATE_STATE");
        initialState.objects = gameState.getGameObjects();
        initialState.score = gameState.getScore();
        initialState.gameOver = gameState.isGameOver();

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

                            // CORRECCIÓN: Enviar actualización inmediata después de recibir input
                            sendMessage(createUpdateMessage());
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error reading message from client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // CORRECCIÓN: Método para crear mensaje de actualización
    private Message createUpdateMessage() {
        Message update = new Message("UPDATE_STATE");
        update.objects = gameState.getGameObjects();
        update.score = gameState.getScore();
        update.gameOver = gameState.isGameOver();
        return update;
    }

    public void sendMessage(Message message) throws IOException {
        if (socket.isClosed()) {
            throw new IOException("Socket is closed");
        }

        try {
            synchronized (out) {  // Sincronizar el acceso al output stream
                out.writeObject(message);
                out.flush();
                out.reset(); // Importante: resetear el caché de objetos
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
            disconnect();
            throw e;
        }
    }

    private void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Client socket closed for player: " + playerId);

                // Eliminar al jugador del estado del juego
                gameState.removePlayer(playerId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        disconnect();
    }
}