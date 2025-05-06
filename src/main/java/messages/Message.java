package messages;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import game.GameObject;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L;

    private String action; // e.g., "UPDATE_STATE", "PLAYER_INPUT"
    private ArrayList<GameObject> objects; // Game state
    private int score;
    private boolean gameOver;
    private String input; // e.g., "LEFT", "RIGHT", "SHOOT", "RESTART"
    private int playerId;
    private Map<Integer, Integer> playerScores = new HashMap<>(); // Individual player scores

    public Message(String action) {
        this.action = action;
        this.objects = new ArrayList<>();
        this.score = 0;
        this.gameOver = false;
        this.input = "";
        this.playerId = -1;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
    
    public void setObjects(ArrayList<GameObject> objects) {
        this.objects = objects;
    }

    public ArrayList<GameObject> getObjects() {
        return objects;
    }
    
    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isGameOver() {
        return gameOver;
    }
    
    public void setInput(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
    
    public void setPlayerScores(Map<Integer, Integer> playerScores) {
        this.playerScores = playerScores;
    }
    public Map<Integer, Integer> getPlayerScores() {
        return playerScores;
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