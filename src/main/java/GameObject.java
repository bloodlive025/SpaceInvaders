import java.io.Serializable;

public class GameObject implements Serializable {
    private static final long serialVersionUID = 3L; // Actualizado para la nueva versión
    int x, y, width, height;
    String type; // "SHIP", "ALIEN", "BULLET"
    boolean alive; // Para los aliens y ahora también para las naves
    boolean used; // Para las balas
    int playerId; // Para identificar a qué jugador pertenece este objeto
    String color; // Para los aliens (e.g., "CYAN", "MAGENTA", "YELLOW")
    int blockType = 0; // Nuevo campo para definir la forma del bloque (0-3)

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