package game;
import java.awt.*;
import java.util.*;

public class GameRenderer {
    public void render(Graphics g, ArrayList<GameObject> objects, int score, boolean gameOver) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (GameObject obj : objects) {
            if (obj.getType().equals("SHIP")) {
                drawShip(g2d, obj);
            } else if (obj.getType().equals("ALIEN") && obj.isAlive()) {
                drawAlien(g2d, obj);
            } else if (obj.getType().equals("BOSS") && obj.isAlive()) {
                drawBoss(g2d, obj);
            } else if (obj.getType().equals("NEW_ALIEN") && obj.isAlive()) {
                drawNewAlien(g2d, obj);
            } else if (obj.getType().equals("FINAL_BOSS") && obj.isAlive()) {
                drawFinalBoss(g2d, obj);
            } else if (obj.getType().equals("FINAL_ALIEN") && obj.isAlive()) {
                drawFinalAlien(g2d, obj);
            } else if (obj.getType().equals("BULLET") && !obj.isUsed()) {
                drawBullet(g2d, obj);
            } else if (obj.getType().equals("ALIEN_BULLET") && !obj.isUsed()) {
                drawAlienBullet(g2d, obj);
            } else if (obj.getType().equals("BOSS_BULLET") && !obj.isUsed()) {
                drawBossBullet(g2d, obj);
            } else if (obj.getType().equals("FINAL_BOSS_BULLET") && !obj.isUsed()) {
                drawFinalBossBullet(g2d, obj);
            } else if (obj.getType().equals("WALL") && obj.isAlive()) {
                drawWall(g2d, obj);
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
        int[] xPoints = {
                ship.getX() + ship.getWidth()/2,
                ship.getX(),
                ship.getX() + ship.getWidth()
        };
        int[] yPoints = {
                ship.getY(),
                ship.getY() + ship.getHeight(),
                ship.getY() + ship.getHeight()
        };
        g.fillPolygon(xPoints, yPoints, 3);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(ship.getX() + ship.getWidth()/4, ship.getY() + ship.getHeight()/2,
                ship.getWidth()/2, ship.getHeight()/2);
    }

    private void drawAlien(Graphics2D g, GameObject alien) {
        if (alien.getColor().equals("CYAN")) g.setColor(Color.cyan);
        else if (alien.getColor().equals("MAGENTA")) g.setColor(Color.magenta);
        else if (alien.getColor().equals("YELLOW")) g.setColor(Color.yellow);

        switch (alien.getBlockType()) {
            case 0:
                int[] xPoints = {
                        alien.getX() + alien.getWidth()/2,
                        alien.getX(),
                        alien.getX() + alien.getWidth()
                };
                int[] yPoints = {
                        alien.getY() + alien.getHeight(),
                        alien.getY(),
                        alien.getY()
                };
                g.fillPolygon(xPoints, yPoints, 3);
                break;
            case 1:
                g.fillOval(alien.getX(), alien.getY(), alien.getWidth(), alien.getHeight());
                break;
            case 2:
                int[] xRombo = {
                        alien.getX() + alien.getWidth()/2,
                        alien.getX() + alien.getWidth(),
                        alien.getX() + alien.getWidth()/2,
                        alien.getX()
                };
                int[] yRombo = {
                        alien.getY(),
                        alien.getY() + alien.getHeight()/2,
                        alien.getY() + alien.getHeight(),
                        alien.getY() + alien.getHeight()/2
                };
                g.fillPolygon(xRombo, yRombo, 4);
                break;
            default:
                g.fillRect(alien.getX(), alien.getY(), alien.getWidth(), alien.getHeight());
                break;
        }

        g.setColor(Color.BLACK);
        switch (alien.getBlockType()) {
            case 0:
            case 2:
                g.fillOval(alien.getX() + alien.getWidth()/4, alien.getY() + alien.getHeight()/4,
                        alien.getWidth()/6, alien.getHeight()/6);
                g.fillOval(alien.getX() + alien.getWidth()*5/8, alien.getY() + alien.getHeight()/4,
                        alien.getWidth()/6, alien.getHeight()/6);
                break;
            case 1:
                g.drawLine(alien.getX(), alien.getY() + alien.getHeight()/2,
                        alien.getX() + alien.getWidth(), alien.getY() + alien.getHeight()/2);
                break;
        }
    }

    private void drawBoss(Graphics2D g, GameObject boss) {
        g.setColor(new Color(128, 0, 128));
        int[] xHexagon = {
                boss.getX() + boss.getWidth() / 2,
                boss.getX() + boss.getWidth() * 3 / 4,
                boss.getX() + boss.getWidth() * 3 / 4,
                boss.getX() + boss.getWidth() / 2,
                boss.getX() + boss.getWidth() / 4,
                boss.getX() + boss.getWidth() / 4
        };
        int[] yHexagon = {
                boss.getY(),
                boss.getY() + boss.getHeight() / 4,
                boss.getY() + boss.getHeight() * 3 / 4,
                boss.getY() + boss.getHeight(),
                boss.getY() + boss.getHeight() * 3 / 4,
                boss.getY() + boss.getHeight() / 4
        };
        g.fillPolygon(xHexagon, yHexagon, 6);
        g.setColor(new Color(255, 0, 255));
        g.fillOval(boss.getX() + boss.getWidth() / 4, boss.getY() + boss.getHeight() / 4,
                boss.getWidth() / 2, boss.getHeight() / 2);
        g.setColor(Color.BLACK);
        g.fillOval(boss.getX() + boss.getWidth() * 3 / 8, boss.getY() + boss.getHeight() * 3 / 8,
                boss.getWidth() / 8, boss.getHeight() / 8);
        g.fillOval(boss.getX() + boss.getWidth() * 5 / 8 - boss.getWidth() / 8, boss.getY() + boss.getHeight() * 3 / 8,
                boss.getWidth() / 8, boss.getHeight() / 8);
        g.setColor(new Color(0, 255, 255));
        g.drawLine(boss.getX() + boss.getWidth() / 4, boss.getY() + boss.getHeight() / 2,
                boss.getX() + boss.getWidth() * 3 / 4, boss.getY() + boss.getHeight() / 2);
        g.drawLine(boss.getX() + boss.getWidth() / 2, boss.getY() + boss.getHeight() / 4,
                boss.getX() + boss.getWidth() / 2, boss.getY() + boss.getHeight() * 3 / 4);
        g.setColor(new Color(128, 0, 128));
        int[] xLeftArm = {
                boss.getX(),
                boss.getX() - boss.getWidth() / 4,
                boss.getX()
        };
        int[] yLeftArm = {
                boss.getY() + boss.getHeight() / 2,
                boss.getY() + boss.getHeight() * 3 / 8,
                boss.getY() + boss.getHeight() * 5 / 8
        };
        g.fillPolygon(xLeftArm, yLeftArm, 3);
        int[] xRightArm = {
                boss.getX() + boss.getWidth(),
                boss.getX() + boss.getWidth() * 5 / 4,
                boss.getX() + boss.getWidth()
        };
        int[] yRightArm = {
                boss.getY() + boss.getHeight() / 2,
                boss.getY() + boss.getHeight() * 3 / 8,
                boss.getY() + boss.getHeight() * 5 / 8
        };
        g.fillPolygon(xRightArm, yRightArm, 3);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(boss.getX() + boss.getWidth() * 3 / 8, boss.getY() + boss.getHeight() * 3 / 8,
                boss.getWidth() / 4, boss.getHeight() / 4);
    }

    private void drawNewAlien(Graphics2D g, GameObject alien) {
        if (alien.getColor().equals("CYAN")) g.setColor(Color.cyan);
        else if (alien.getColor().equals("MAGENTA")) g.setColor(Color.magenta);
        else if (alien.getColor().equals("YELLOW")) g.setColor(Color.yellow);
        else if (alien.getColor().equals("ORANGE")) g.setColor(new Color(255, 165, 0));

        int[] xStar = {
                alien.getX() + alien.getWidth() / 2,
                alien.getX() + alien.getWidth() * 3 / 5,
                alien.getX() + alien.getWidth() * 3 / 4,
                alien.getX() + alien.getWidth() / 2,
                alien.getX() + alien.getWidth() / 4,
                alien.getX() + alien.getWidth() * 2 / 5
        };
        int[] yStar = {
                alien.getY(),
                alien.getY() + alien.getHeight() / 3,
                alien.getY() + alien.getHeight() * 2 / 3,
                alien.getY() + alien.getHeight(),
                alien.getY() + alien.getHeight() * 2 / 3,
                alien.getY() + alien.getHeight() / 3
        };
        g.fillPolygon(xStar, yStar, 6);
        g.setColor(Color.BLACK);
        g.fillOval(alien.getX() + alien.getWidth() * 3 / 8, alien.getY() + alien.getHeight() * 3 / 8,
                alien.getWidth() / 4, alien.getHeight() / 4);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(alien.getX() + alien.getWidth() * 7 / 16, alien.getY() + alien.getHeight() * 7 / 16,
                alien.getWidth() / 8, alien.getHeight() / 8);
    }

    private void drawFinalBoss(Graphics2D g, GameObject boss) {
        g.setColor(new Color(75, 0, 130)); // Indigo
        int[] xOctagon = {
                boss.getX() + boss.getWidth() / 2,
                boss.getX() + boss.getWidth() * 3 / 4,
                boss.getX() + boss.getWidth(),
                boss.getX() + boss.getWidth() * 3 / 4,
                boss.getX() + boss.getWidth() / 2,
                boss.getX() + boss.getWidth() / 4,
                boss.getX(),
                boss.getX() + boss.getWidth() / 4
        };
        int[] yOctagon = {
                boss.getY(),
                boss.getY() + boss.getHeight() / 4,
                boss.getY() + boss.getHeight() / 2,
                boss.getY() + boss.getHeight() * 3 / 4,
                boss.getY() + boss.getHeight(),
                boss.getY() + boss.getHeight() * 3 / 4,
                boss.getY() + boss.getHeight() / 2,
                boss.getY() + boss.getHeight() / 4
        };
        g.fillPolygon(xOctagon, yOctagon, 8);
        g.setColor(Color.CYAN);
        g.fillOval(boss.getX() + boss.getWidth() / 4, boss.getY() + boss.getHeight() / 4,
                boss.getWidth() / 2, boss.getHeight() / 2);
        g.setColor(Color.WHITE);
        g.fillOval(boss.getX() + boss.getWidth() * 3 / 8, boss.getY() + boss.getHeight() * 3 / 8,
                boss.getWidth() / 4, boss.getHeight() / 4);
        g.setColor(new Color(255, 255, 255, 100));
        int[] xLeftArm = {
                boss.getX(),
                boss.getX() - boss.getWidth() / 4,
                boss.getX()
        };
        int[] yLeftArm = {
                boss.getY() + boss.getHeight() / 2,
                boss.getY() + boss.getHeight() / 4,
                boss.getY() + boss.getHeight() * 3 / 4
        };
        g.fillPolygon(xLeftArm, yLeftArm, 3);
        int[] xRightArm = {
                boss.getX() + boss.getWidth(),
                boss.getX() + boss.getWidth() * 5 / 4,
                boss.getX() + boss.getWidth()
        };
        int[] yRightArm = {
                boss.getY() + boss.getHeight() / 2,
                boss.getY() + boss.getHeight() / 4,
                boss.getY() + boss.getHeight() * 3 / 4
        };
        g.fillPolygon(xRightArm, yRightArm, 3);
    }

    private void drawFinalAlien(Graphics2D g, GameObject alien) {
        if (alien.getColor().equals("RED")) g.setColor(Color.RED);
        else if (alien.getColor().equals("PINK")) g.setColor(Color.PINK);
        else if (alien.getColor().equals("WHITE")) g.setColor(Color.WHITE);

        int[] xPentagon = {
                alien.getX() + alien.getWidth() / 2,
                alien.getX() + alien.getWidth(),
                alien.getX() + alien.getWidth() * 3 / 4,
                alien.getX() + alien.getWidth() / 4,
                alien.getX()
        };
        int[] yPentagon = {
                alien.getY(),
                alien.getY() + alien.getHeight() / 3,
                alien.getY() + alien.getHeight(),
                alien.getY() + alien.getHeight(),
                alien.getY() + alien.getHeight() / 3
        };
        g.fillPolygon(xPentagon, yPentagon, 5);
        g.setColor(new Color(255, 0, 0, 100));
        g.fillOval(alien.getX() + alien.getWidth() / 4, alien.getY() + alien.getHeight() / 4,
                alien.getWidth() / 2, alien.getHeight() / 2);
    }

    private void drawBullet(Graphics2D g, GameObject bullet) {
        g.setColor(Color.white);
        g.fillRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
        g.setColor(Color.yellow);
        g.fillRect(bullet.getX() + bullet.getWidth()/4, bullet.getY(),
                bullet.getWidth()/2, bullet.getHeight()/2);
    }

    private void drawAlienBullet(Graphics2D g, GameObject bullet) {
        g.setColor(Color.red);
        int[] xPoints = {
                bullet.getX() + bullet.getWidth()/2,
                bullet.getX() + bullet.getWidth(),
                bullet.getX() + bullet.getWidth()/2,
                bullet.getX()
        };
        int[] yPoints = {
                bullet.getY(),
                bullet.getY() + bullet.getHeight()/2,
                bullet.getY() + bullet.getHeight(),
                bullet.getY() + bullet.getHeight()/2
        };
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(new Color(255, 128, 0));
        g.fillOval(bullet.getX() + bullet.getWidth()/4, bullet.getY() + bullet.getHeight()/4,
                bullet.getWidth()/2, bullet.getHeight()/2);
    }

    private void drawBossBullet(Graphics2D g, GameObject bullet) {
        g.setColor(new Color(128, 0, 128));
        int[] xPoints = {
                bullet.getX() + bullet.getWidth()/2,
                bullet.getX() + bullet.getWidth(),
                bullet.getX() + bullet.getWidth()/2,
                bullet.getX()
        };
        int[] yPoints = {
                bullet.getY(),
                bullet.getY() + bullet.getHeight()/2,
                bullet.getY() + bullet.getHeight(),
                bullet.getY() + bullet.getHeight()/2
        };
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(new Color(255, 0, 255));
        g.fillOval(bullet.getX() + bullet.getWidth()/4, bullet.getY() + bullet.getHeight()/4,
                bullet.getWidth()/2, bullet.getHeight()/2);
    }

    private void drawFinalBossBullet(Graphics2D g, GameObject bullet) {
        g.setColor(new Color(138, 43, 226));
        int[] xPoints = {
                bullet.getX() + bullet.getWidth()/2,
                bullet.getX() + bullet.getWidth(),
                bullet.getX() + bullet.getWidth()/2,
                bullet.getX()
        };
        int[] yPoints = {
                bullet.getY(),
                bullet.getY() + bullet.getHeight()/2,
                bullet.getY() + bullet.getHeight(),
                bullet.getY() + bullet.getHeight()/2
        };
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(Color.YELLOW);
        g.fillOval(bullet.getX() + bullet.getWidth()/4, bullet.getY() + bullet.getHeight()/4,
                bullet.getWidth()/2, bullet.getHeight()/2);
    }

    private void drawWall(Graphics2D g, GameObject wall) {
        float opacity = wall.getHealth() == 3 ? 1.0f : wall.getHealth() == 2 ? 0.66f : 0.33f;
        g.setColor(new Color(0.5f, 0.5f, 0.5f, opacity)); // Gray with health-based opacity
        g.fillRect(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
        g.setColor(Color.WHITE);
        g.drawRect(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight()); // White border
    }
}