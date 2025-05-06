import java.awt.*;
import java.util.*;

public class GameRenderer {
    public void render(Graphics g, ArrayList<GameObject> objects, int score, boolean gameOver) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (GameObject obj : objects) {
            if (obj.type.equals("SHIP")) {
                drawShip(g2d, obj);
            } else if (obj.type.equals("ALIEN") && obj.alive) {
                drawAlien(g2d, obj);
            } else if (obj.type.equals("BOSS") && obj.alive) {
                drawBoss(g2d, obj);
            } else if (obj.type.equals("NEW_ALIEN") && obj.alive) {
                drawNewAlien(g2d, obj);
            } else if (obj.type.equals("FINAL_BOSS") && obj.alive) {
                drawFinalBoss(g2d, obj);
            } else if (obj.type.equals("FINAL_ALIEN") && obj.alive) {
                drawFinalAlien(g2d, obj);
            } else if (obj.type.equals("BULLET") && !obj.used) {
                drawBullet(g2d, obj);
            } else if (obj.type.equals("ALIEN_BULLET") && !obj.used) {
                drawAlienBullet(g2d, obj);
            } else if (obj.type.equals("BOSS_BULLET") && !obj.used) {
                drawBossBullet(g2d, obj);
            } else if (obj.type.equals("FINAL_BOSS_BULLET") && !obj.used) {
                drawFinalBossBullet(g2d, obj);
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
                ship.x + ship.width/2,
                ship.x,
                ship.x + ship.width
        };
        int[] yPoints = {
                ship.y,
                ship.y + ship.height,
                ship.y + ship.height
        };
        g.fillPolygon(xPoints, yPoints, 3);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(ship.x + ship.width/4, ship.y + ship.height/2,
                ship.width/2, ship.height/2);
    }

    private void drawAlien(Graphics2D g, GameObject alien) {
        if (alien.color.equals("CYAN")) g.setColor(Color.cyan);
        else if (alien.color.equals("MAGENTA")) g.setColor(Color.magenta);
        else if (alien.color.equals("YELLOW")) g.setColor(Color.yellow);

        switch (alien.blockType) {
            case 0:
                int[] xPoints = {
                        alien.x + alien.width/2,
                        alien.x,
                        alien.x + alien.width
                };
                int[] yPoints = {
                        alien.y + alien.height,
                        alien.y,
                        alien.y
                };
                g.fillPolygon(xPoints, yPoints, 3);
                break;
            case 1:
                g.fillOval(alien.x, alien.y, alien.width, alien.height);
                break;
            case 2:
                int[] xRombo = {
                        alien.x + alien.width/2,
                        alien.x + alien.width,
                        alien.x + alien.width/2,
                        alien.x
                };
                int[] yRombo = {
                        alien.y,
                        alien.y + alien.height/2,
                        alien.y + alien.height,
                        alien.y + alien.height/2
                };
                g.fillPolygon(xRombo, yRombo, 4);
                break;
            default:
                g.fillRect(alien.x, alien.y, alien.width, alien.height);
                break;
        }

        g.setColor(Color.BLACK);
        switch (alien.blockType) {
            case 0:
            case 2:
                g.fillOval(alien.x + alien.width/4, alien.y + alien.height/4,
                        alien.width/6, alien.height/6);
                g.fillOval(alien.x + alien.width*5/8, alien.y + alien.height/4,
                        alien.width/6, alien.height/6);
                break;
            case 1:
                g.drawLine(alien.x, alien.y + alien.height/2,
                        alien.x + alien.width, alien.y + alien.height/2);
                break;
        }
    }

    private void drawBoss(Graphics2D g, GameObject boss) {
        g.setColor(new Color(128, 0, 128));
        int[] xHexagon = {
                boss.x + boss.width / 2,
                boss.x + boss.width * 3 / 4,
                boss.x + boss.width * 3 / 4,
                boss.x + boss.width / 2,
                boss.x + boss.width / 4,
                boss.x + boss.width / 4
        };
        int[] yHexagon = {
                boss.y,
                boss.y + boss.height / 4,
                boss.y + boss.height * 3 / 4,
                boss.y + boss.height,
                boss.y + boss.height * 3 / 4,
                boss.y + boss.height / 4
        };
        g.fillPolygon(xHexagon, yHexagon, 6);
        g.setColor(new Color(255, 0, 255));
        g.fillOval(boss.x + boss.width / 4, boss.y + boss.height / 4,
                boss.width / 2, boss.height / 2);
        g.setColor(Color.BLACK);
        g.fillOval(boss.x + boss.width * 3 / 8, boss.y + boss.height * 3 / 8,
                boss.width / 8, boss.height / 8);
        g.fillOval(boss.x + boss.width * 5 / 8 - boss.width / 8, boss.y + boss.height * 3 / 8,
                boss.width / 8, boss.height / 8);
        g.setColor(new Color(0, 255, 255));
        g.drawLine(boss.x + boss.width / 4, boss.y + boss.height / 2,
                boss.x + boss.width * 3 / 4, boss.y + boss.height / 2);
        g.drawLine(boss.x + boss.width / 2, boss.y + boss.height / 4,
                boss.x + boss.width / 2, boss.y + boss.height * 3 / 4);
        g.setColor(new Color(128, 0, 128));
        int[] xLeftArm = {
                boss.x,
                boss.x - boss.width / 4,
                boss.x
        };
        int[] yLeftArm = {
                boss.y + boss.height / 2,
                boss.y + boss.height * 3 / 8,
                boss.y + boss.height * 5 / 8
        };
        g.fillPolygon(xLeftArm, yLeftArm, 3);
        int[] xRightArm = {
                boss.x + boss.width,
                boss.x + boss.width * 5 / 4,
                boss.x + boss.width
        };
        int[] yRightArm = {
                boss.y + boss.height / 2,
                boss.y + boss.height * 3 / 8,
                boss.y + boss.height * 5 / 8
        };
        g.fillPolygon(xRightArm, yRightArm, 3);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(boss.x + boss.width * 3 / 8, boss.y + boss.height * 3 / 8,
                boss.width / 4, boss.height / 4);
    }

    private void drawNewAlien(Graphics2D g, GameObject alien) {
        if (alien.color.equals("CYAN")) g.setColor(Color.cyan);
        else if (alien.color.equals("MAGENTA")) g.setColor(Color.magenta);
        else if (alien.color.equals("YELLOW")) g.setColor(Color.yellow);
        else if (alien.color.equals("ORANGE")) g.setColor(new Color(255, 165, 0));

        int[] xStar = {
                alien.x + alien.width / 2,
                alien.x + alien.width * 3 / 5,
                alien.x + alien.width * 3 / 4,
                alien.x + alien.width / 2,
                alien.x + alien.width / 4,
                alien.x + alien.width * 2 / 5,
        };
        int[] yStar = {
                alien.y,
                alien.y + alien.height / 3,
                alien.y + alien.height * 2 / 3,
                alien.y + alien.height,
                alien.y + alien.height * 2 / 3,
                alien.y + alien.height / 3
        };
        g.fillPolygon(xStar, yStar, 6);
        g.setColor(Color.BLACK);
        g.fillOval(alien.x + alien.width * 3 / 8, alien.y + alien.height * 3 / 8,
                alien.width / 4, alien.height / 4);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(alien.x + alien.width * 7 / 16, alien.y + alien.height * 7 / 16,
                alien.width / 8, alien.height / 8);
    }

    private void drawFinalBoss(Graphics2D g, GameObject boss) {
        g.setColor(new Color(75, 0, 130)); // Indigo
        int[] xOctagon = {
                boss.x + boss.width / 2,
                boss.x + boss.width * 3 / 4,
                boss.x + boss.width,
                boss.x + boss.width * 3 / 4,
                boss.x + boss.width / 2,
                boss.x + boss.width / 4,
                boss.x,
                boss.x + boss.width / 4
        };
        int[] yOctagon = {
                boss.y,
                boss.y + boss.height / 4,
                boss.y + boss.height / 2,
                boss.y + boss.height * 3 / 4,
                boss.y + boss.height,
                boss.y + boss.height * 3 / 4,
                boss.y + boss.height / 2,
                boss.y + boss.height / 4
        };
        g.fillPolygon(xOctagon, yOctagon, 8);
        g.setColor(Color.CYAN);
        g.fillOval(boss.x + boss.width / 4, boss.y + boss.height / 4,
                boss.width / 2, boss.height / 2);
        g.setColor(Color.WHITE);
        g.fillOval(boss.x + boss.width * 3 / 8, boss.y + boss.height * 3 / 8,
                boss.width / 4, boss.height / 4);
        g.setColor(new Color(255, 255, 255, 100));
        int[] xLeftArm = {
                boss.x,
                boss.x - boss.width / 4,
                boss.x
        };
        int[] yLeftArm = {
                boss.y + boss.height / 2,
                boss.y + boss.height / 4,
                boss.y + boss.height * 3 / 4
        };
        g.fillPolygon(xLeftArm, yLeftArm, 3);
        int[] xRightArm = {
                boss.x + boss.width,
                boss.x + boss.width * 5 / 4,
                boss.x + boss.width
        };
        int[] yRightArm = {
                boss.y + boss.height / 2,
                boss.y + boss.height / 4,
                boss.y + boss.height * 3 / 4
        };
        g.fillPolygon(xRightArm, yRightArm, 3);
    }

    private void drawFinalAlien(Graphics2D g, GameObject alien) {
        if (alien.color.equals("RED")) g.setColor(Color.RED);
        else if (alien.color.equals("PINK")) g.setColor(Color.PINK);
        else if (alien.color.equals("WHITE")) g.setColor(Color.WHITE);

        int[] xPentagon = {
                alien.x + alien.width / 2,
                alien.x + alien.width,
                alien.x + alien.width * 3 / 4,
                alien.x + alien.width / 4,
                alien.x
        };
        int[] yPentagon = {
                alien.y,
                alien.y + alien.height / 3,
                alien.y + alien.height,
                alien.y + alien.height,
                alien.y + alien.height / 3
        };
        g.fillPolygon(xPentagon, yPentagon, 5);
        g.setColor(new Color(255, 0, 0, 100));
        g.fillOval(alien.x + alien.width / 4, alien.y + alien.height / 4,
                alien.width / 2, alien.height / 2);
    }

    private void drawBullet(Graphics2D g, GameObject bullet) {
        g.setColor(Color.white);
        g.fillRect(bullet.x, bullet.y, bullet.width, bullet.height);
        g.setColor(Color.yellow);
        g.fillRect(bullet.x + bullet.width/4, bullet.y,
                bullet.width/2, bullet.height/2);
    }

    private void drawAlienBullet(Graphics2D g, GameObject bullet) {
        g.setColor(Color.red);
        int[] xPoints = {
                bullet.x + bullet.width/2,
                bullet.x + bullet.width,
                bullet.x + bullet.width/2,
                bullet.x
        };
        int[] yPoints = {
                bullet.y,
                bullet.y + bullet.height/2,
                bullet.y + bullet.height,
                bullet.y + bullet.height/2
        };
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(new Color(255, 128, 0));
        g.fillOval(bullet.x + bullet.width/4, bullet.y + bullet.height/4,
                bullet.width/2, bullet.height/2);
    }

    private void drawBossBullet(Graphics2D g, GameObject bullet) {
        g.setColor(new Color(128, 0, 128));
        int[] xPoints = {
                bullet.x + bullet.width/2,
                bullet.x + bullet.width,
                bullet.x + bullet.width/2,
                bullet.x
        };
        int[] yPoints = {
                bullet.y,
                bullet.y + bullet.height/2,
                bullet.y + bullet.height,
                bullet.y + bullet.height/2
        };
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(new Color(255, 0, 255));
        g.fillOval(bullet.x + bullet.width/4, bullet.y + bullet.height/4,
                bullet.width/2, bullet.height/2);
    }

    private void drawFinalBossBullet(Graphics2D g, GameObject bullet) {
        g.setColor(new Color(138, 43, 226)); // Purple
        int[] xPoints = {
                bullet.x + bullet.width/2,
                bullet.x + bullet.width,
                bullet.x + bullet.width/2,
                bullet.x
        };
        int[] yPoints = {
                bullet.y,
                bullet.y + bullet.height/2,
                bullet.y + bullet.height,
                bullet.y + bullet.height/2
        };
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(Color.YELLOW);
        g.fillOval(bullet.x + bullet.width/4, bullet.y + bullet.height/4,
                bullet.width/2, bullet.height/2);
    }
}