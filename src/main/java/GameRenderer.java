import java.awt.*;
import java.util.*;

public class GameRenderer {
    public void render(Graphics g, ArrayList<GameObject> objects, int score, boolean gameOver) {
        // Usar Graphics2D para mejor calidad gráfica
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (GameObject obj : objects) {
            if (obj.type.equals("SHIP")) {
                drawShip(g2d, obj);
            } else if (obj.type.equals("ALIEN") && obj.alive) {
                drawAlien(g2d, obj);
            } else if (obj.type.equals("BULLET") && !obj.used) {
                drawBullet(g2d, obj);
            }
        }

        g2d.setColor(Color.white);
        g2d.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g2d.drawString("Game Over: " + score, 10, 35);
        } else {
            g2d.drawString(String.valueOf(score), 10, 35);
        }
    }

    private void drawShip(Graphics2D g, GameObject ship) {
        g.setColor(Color.green);

        // Dibujar un triángulo para la nave del jugador
        int[] xPoints = {
                ship.x + ship.width/2,           // Punta
                ship.x,                           // Esquina izquierda
                ship.x + ship.width               // Esquina derecha
        };

        int[] yPoints = {
                ship.y,                           // Punta (arriba)
                ship.y + ship.height,             // Base izquierda
                ship.y + ship.height              // Base derecha
        };

        g.fillPolygon(xPoints, yPoints, 3);

        // Añadir detalles a la nave
        g.setColor(Color.DARK_GRAY);
        g.fillRect(ship.x + ship.width/4, ship.y + ship.height/2,
                ship.width/2, ship.height/2);
    }

    private void drawAlien(Graphics2D g, GameObject alien) {
        // Establecer color según el tipo de alien
        if (alien.color.equals("CYAN")) g.setColor(Color.cyan);
        else if (alien.color.equals("MAGENTA")) g.setColor(Color.magenta);
        else if (alien.color.equals("YELLOW")) g.setColor(Color.yellow);

        // Variedad de formas según blockType
        switch (alien.blockType) {
            case 0: // Forma triangular - nave pequeña
                int[] xPoints = {
                        alien.x + alien.width/2,         // Punta
                        alien.x,                         // Esquina izquierda
                        alien.x + alien.width            // Esquina derecha
                };

                int[] yPoints = {
                        alien.y + alien.height,          // Punta (abajo)
                        alien.y,                         // Base izquierda
                        alien.y                          // Base derecha
                };

                g.fillPolygon(xPoints, yPoints, 3);
                break;

            case 1: // Forma ovalada - nave mediana
                g.fillOval(alien.x, alien.y, alien.width, alien.height);
                break;

            case 2: // Forma de rombo - nave grande
                int[] xRombo = {
                        alien.x + alien.width/2,         // Punta superior
                        alien.x + alien.width,           // Derecha
                        alien.x + alien.width/2,         // Punta inferior
                        alien.x                          // Izquierda
                };

                int[] yRombo = {
                        alien.y,                         // Punta superior
                        alien.y + alien.height/2,        // Derecha
                        alien.y + alien.height,          // Punta inferior
                        alien.y + alien.height/2         // Izquierda
                };

                g.fillPolygon(xRombo, yRombo, 4);
                break;

            default: // Forma rectangular para otros tipos
                g.fillRect(alien.x, alien.y, alien.width, alien.height);
                break;
        }

        // Añadir detalles a los aliens
        g.setColor(Color.BLACK);
        switch (alien.blockType) {
            case 0:
            case 2:
                // Ojos para formas triangulares y rombos
                g.fillOval(alien.x + alien.width/4, alien.y + alien.height/4,
                        alien.width/6, alien.height/6);
                g.fillOval(alien.x + alien.width*5/8, alien.y + alien.height/4,
                        alien.width/6, alien.height/6);
                break;
            case 1:
                // Línea central para óvalos
                g.drawLine(alien.x, alien.y + alien.height/2,
                        alien.x + alien.width, alien.y + alien.height/2);
                break;
        }
    }

    private void drawBullet(Graphics2D g, GameObject bullet) {
        g.setColor(Color.white);
        // Dibujar un rayo láser en lugar de un simple rectángulo
        g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);

        // Añadir efecto de brillo
        g.setColor(Color.yellow);
        g.fillRect(bullet.x + bullet.width/4, bullet.y,
                bullet.width/2, bullet.height/2);
    }
}