import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L; // Incrementado para evitar problemas de compatibilidad

    // Atributos
    String action; // e.g., "UPDATE_STATE", "PLAYER_INPUT"
    ArrayList<GameObject> objects; // Estado del juego
    int score;
    boolean gameOver;
    String input; // e.g., "LEFT", "RIGHT", "SHOOT", "RESTART"
    int playerId;

    public Message(String action) {
        this.action = action;
        this.objects = new ArrayList<>();
        this.score = 0;
        this.gameOver = false;
        this.input = "";
        this.playerId = -1;
    }

    @Override
    public String toString() {
        return "Message{" +
                "action='" + action + '\'' +
                ", playerId=" + playerId +
                ", input='" + input + '\'' +
                ", objects.size=" + (objects != null ? objects.size() : 0) +
                ", score=" + score +
                ", gameOver=" + gameOver +
                '}';
    }
}