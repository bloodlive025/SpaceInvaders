import java.io.Serializable;

public class GameObject implements Serializable {
    private static final long serialVersionUID = 1L;
    int x, y, width, height;
    String type; // "SHIP", "ALIEN", "BULLET"
    boolean alive; // For aliens
    boolean used; // For bullets
    int playerId; // To identify which player this object belongs to
    String color; // For aliens (e.g., "CYAN", "MAGENTA", "YELLOW")

    public GameObject(int x, int y, int width, int height, String type, int playerId) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.playerId = playerId;
        this.alive = true;
        this.used = false;
    }
}