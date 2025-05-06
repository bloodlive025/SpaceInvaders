package client;
import javax.swing.*;

import game.GameObject;
import game.GameRenderer;

import java.awt.*;
import java.awt.event.*;

public class GameClient extends JPanel implements KeyListener {
    private ClientNetworkHandler networkHandler;
    private GameRenderer renderer;
    private int playerId;
    private boolean connectedToServer = false;
    private boolean playerEliminated = false;

    public GameClient(String ip, int port) throws Exception {
        setPreferredSize(new Dimension(512, 512));
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);

        try {
            networkHandler = new ClientNetworkHandler(ip, port);
            networkHandler.setClient(this);
            renderer = new GameRenderer();
            playerId = networkHandler.getPlayerId();
            connectedToServer = true;
            networkHandler.start();
            System.out.println("Clinete inicializado para palyerId: " + playerId);
        } catch (Exception e) {
            System.err.println("Fallor la incializacion del cliente: " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (connectedToServer) {
            renderer.render(g, networkHandler.getGameObjects(), networkHandler.getScore(playerId), networkHandler.isGameOver());


            boolean playerShipExists = false;
            for (GameObject obj : networkHandler.getGameObjects()) {
                if (obj.getType().equals("SHIP") && obj.getPlayerId() == playerId) {
                    playerShipExists = true;
                    break;
                }
            }

            if (!playerShipExists && !networkHandler.isGameOver()) {
                playerEliminated = true;
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.drawString("¡Has sido eliminado!", 150, 250);
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                g.drawString("Observando a otros jugadores...", 150, 280);
            }
        } else {
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

        if (playerEliminated && e.getKeyCode() != KeyEvent.VK_ENTER) {
            return;
        }

        if (networkHandler.isGameOver()) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                networkHandler.sendInput("RESTART");
                playerEliminated = false;
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

    public void connectionLost() {
        connectedToServer = false;
        repaint();
        JOptionPane.showMessageDialog(this,
                "Se ha perdido la conexión con el servidor.\nReinicia la aplicación para volver a conectar.",
                "Error de Conexión", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String ip = JOptionPane.showInputDialog("Introduce la dirección IP del servidor:", "localhost");
            if (ip == null || ip.trim().isEmpty()) {
                System.exit(0);
            }
            int port = 12345;
            JFrame frame = new JFrame("Space Invaders Mejorado - Cliente");
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