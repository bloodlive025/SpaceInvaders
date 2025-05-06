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

    @Override
    public void run() {
        try {
            while (running && !socket.isClosed()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        if (message.action.equals("PLAYER_INPUT")) {
                            gameState.handleInput(playerId, message.input);
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