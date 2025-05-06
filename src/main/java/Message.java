import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L;

    String action; // e.g., "UPDATE_STATE", "PLAYER_INPUT"
    ArrayList<GameObject> objects; // Game state
    int score;
    boolean gameOver;
    String input; // e.g., "LEFT", "RIGHT", "SHOOT", "RESTART"
    int playerId;
    Map<Integer, Integer> playerScores = new HashMap<>(); // Individual player scores

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
                ", playerScores=" + playerScores +
                '}';
    }
}