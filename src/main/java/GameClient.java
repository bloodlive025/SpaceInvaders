import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameClient extends JPanel implements KeyListener {
    private ClientNetworkHandler networkHandler;
    private GameRenderer renderer;
    private int playerId;
    private boolean connectedToServer = false;

    public GameClient(String ip, int port) throws Exception {
        setPreferredSize(new Dimension(512, 512));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        try {
            // Inicializar conexión de red
            networkHandler = new ClientNetworkHandler(ip, port);
            networkHandler.setClient(this); // Importante: establecer referencia al cliente
            renderer = new GameRenderer();
            playerId = networkHandler.getPlayerId();
            connectedToServer = true;

            // Iniciar hilo de red
            networkHandler.start();

            System.out.println("Client initialized with playerId: " + playerId);
        } catch (Exception e) {
            System.err.println("Failed to initialize game client: " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (connectedToServer) {
            renderer.render(g, networkHandler.getGameObjects(), networkHandler.getScore(), networkHandler.isGameOver());

            // Mostrar instrucciones
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString("Flechas ← → para mover, Espacio para disparar", 10, 490);
            if (networkHandler.isGameOver()) {
                g.drawString("Presiona ENTER para reiniciar", 10, 505);
            }
        } else {
            // Renderizar mensaje de desconexión
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Desconectado del servidor", 100, 250);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Reinicia la aplicación para volver a conectar", 80, 280);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!connectedToServer) return;

        if (networkHandler.isGameOver()) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                networkHandler.sendInput("RESTART");
                return;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            networkHandler.sendInput("LEFT");
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            networkHandler.sendInput("RIGHT");
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            networkHandler.sendInput("SHOOT");
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    // Método para manejar la pérdida de conexión
    public void connectionLost() {
        connectedToServer = false;
        repaint();

        JOptionPane.showMessageDialog(this,
                "Se ha perdido la conexión con el servidor.\nReinicia la aplicación para volver a conectar.",
                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        // Configurar la UI en el hilo EDT
        SwingUtilities.invokeLater(() -> {
            String ip = JOptionPane.showInputDialog("Introduce la dirección IP del servidor:", "localhost");
            if (ip == null || ip.trim().isEmpty()) {
                System.exit(0);
            }

            int port = 12345;

            JFrame frame = new JFrame("Space Invaders - Cliente");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            try {
                GameClient client = new GameClient(ip, port);
                frame.add(client);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                client.requestFocus();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error al conectar con el servidor: " + e.getMessage(),
                        "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}