import java.awt.*;
import java.util.*;

public class GameRenderer {
    public void render(Graphics g, ArrayList<GameObject> objects, int score, boolean gameOver) {
        for (GameObject obj : objects) {
            if (obj.type.equals("SHIP")) {
                g.setColor(Color.green);
                g.fillRect(obj.x, obj.y, obj.width, obj.height);
            } else if (obj.type.equals("ALIEN") && obj.alive) {
                if (obj.color.equals("CYAN")) g.setColor(Color.cyan);
                else if (obj.color.equals("MAGENTA")) g.setColor(Color.magenta);
                else if (obj.color.equals("YELLOW")) g.setColor(Color.yellow);
                g.fillRect(obj.x, obj.y, obj.width, obj.height);
            } else if (obj.type.equals("BULLET") && !obj.used) {
                g.setColor(Color.white);
                g.fillRect(obj.x, obj.y, obj.width, obj.height);
            }
        }

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + score, 10, 35);
        } else {
            g.drawString(String.valueOf(score), 10, 35);
        }
    }
}