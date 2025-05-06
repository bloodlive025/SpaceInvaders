package game;
import java.io.Serializable;

public class GameObject implements Serializable {
    private static final long serialVersionUID = 3L; // Actualizado para la nueva versión
    private int x, y, width, height;
    private String type; // "SHIP", "ALIEN", "BULLET"
    private boolean alive; // Para los aliens y ahora también para las naves
    private boolean used; // Para las balas
    private int playerId; // Para identificar a qué jugador pertenece este objeto
    private String color; // Para los aliens (e.g., "CYAN", "MAGENTA", "YELLOW")
    int blockType = 0; // Nuevo campo para definir la forma del bloque (0-3)
    int health;
    private double velocityX, velocityY;

    public GameObject(int x, int y, int width, int height, String type, int playerId) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.playerId = playerId;
        this.alive = true;
        this.used = false;
        health = 3;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isAlive() {
        return alive;
    }
    
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getBlockType() {
        return blockType;
    }
    
    public void setBlockType(int blockType) {
        this.blockType = blockType;
    }
    
    public int getHealth() {
        return health;
    }
    
    public void setHealth(int health) {
        this.health = health;
    }
    
    public double getVelocityX() {
        return velocityX;
    }
    
    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }
    
    public double getVelocityY() {
        return velocityY;
    }
    
    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

}