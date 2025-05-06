import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private static final int TILE_SIZE = 32;
    private static final int ROWS = 16;
    private static final int COLUMNS = 16;
    private int boardWidth = TILE_SIZE * COLUMNS;
    private int boardHeight = TILE_SIZE * ROWS;

    private Map<Integer, GameObject> ships = new ConcurrentHashMap<>(); // Thread-safe
    private List<GameObject> aliens = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> bullets = Collections.synchronizedList(new ArrayList<>());
    private int alienVelocityX = 1;
    private int alienCount = 0;
    private int score = 0;
    private boolean gameOver = false;
    private Random random = new Random();

    // Añadir sincronización
    private final Object gameStateLock = new Object();

    public GameState() {
        // Inicializar el juego con los aliens incluso sin jugadores
        createAliens();
    }

    public void addPlayer(int playerId) {
        synchronized(gameStateLock) {
            // Posicionar barco centrado según número de jugador
            int shipX = boardWidth / 2 - TILE_SIZE;
            if (ships.size() > 0) {
                // Si hay más de un jugador, distribuir las naves
                shipX = TILE_SIZE * 2 + (playerId * TILE_SIZE * 3);
            }

            // Asegurar que el barco esté dentro de los límites
            shipX = Math.max(0, Math.min(shipX, boardWidth - TILE_SIZE * 2));

            GameObject ship = new GameObject(shipX, boardHeight - TILE_SIZE * 2,
                    TILE_SIZE * 2, TILE_SIZE, "SHIP", playerId);
            ships.put(playerId, ship);

            System.out.println("Player " + playerId + " added at position: " + shipX);
        }
    }

    public void removePlayer(int playerId) {
        synchronized(gameStateLock) {
            ships.remove(playerId);
            System.out.println("Player " + playerId + " removed from game state");

            // Si no quedan jugadores, reiniciar el juego
            if (ships.isEmpty() && gameOver) {
                resetGame();
            }
        }
    }

    public void handleInput(int playerId, String input) {
        synchronized(gameStateLock) {
            if (gameOver) {
                if (input.equals("RESTART")) {
                    resetGame();
                }
                return;
            }

            GameObject ship = ships.get(playerId);
            if (ship == null) {
                System.out.println("Ship not found for player: " + playerId);
                return;
            }

            if (input.equals("LEFT") && ship.x - TILE_SIZE >= 0) {
                ship.x -= TILE_SIZE;
            } else if (input.equals("RIGHT") && ship.x + ship.width + TILE_SIZE <= boardWidth) {
                ship.x += TILE_SIZE;
            } else if (input.equals("SHOOT")) {
                // Crear bala en el centro del barco
                int bulletX = ship.x + (ship.width / 2) - (TILE_SIZE / 16);
                GameObject bullet = new GameObject(bulletX, ship.y,
                        TILE_SIZE / 8, TILE_SIZE / 2, "BULLET", playerId);
                bullets.add(bullet);
            }
        }
    }

    public void update() {
        synchronized(gameStateLock) {
            // Si no hay jugadores y no es game over, no actualizar
            if (ships.isEmpty() && !gameOver) {
                return;
            }

            if (gameOver) return;

            // Mover aliens
            boolean changeDirection = false;
            for (GameObject alien : aliens) {
                if (alien.alive) {
                    alien.x += alienVelocityX;

                    // Verificar si los aliens tocan los bordes
                    if (alien.x + alien.width >= boardWidth || alien.x <= 0) {
                        changeDirection = true;
                    }

                    // Verificar colisión con naves
                    for (GameObject ship : ships.values()) {
                        if (alien.y + alien.height >= ship.y) {
                            gameOver = true;
                            System.out.println("Game over! Aliens reached the ships!");
                        }
                    }
                }
            }

            // Cambiar dirección de los aliens si es necesario
            if (changeDirection) {
                alienVelocityX *= -1;

                // Mover aliens hacia abajo
                for (GameObject alien : aliens) {
                    if (alien.alive) {
                        alien.y += TILE_SIZE;
                    }
                }
            }

            // Mover balas y detectar colisiones
            Iterator<GameObject> bulletIter = bullets.iterator();
            while (bulletIter.hasNext()) {
                GameObject bullet = bulletIter.next();
                bullet.y -= 10; // Velocidad de la bala

                // Verificar colisiones con aliens
                for (GameObject alien : aliens) {
                    if (!bullet.used && alien.alive && detectCollision(bullet, alien)) {
                        bullet.used = true;
                        alien.alive = false;
                        alienCount--;
                        score += 100;
                        System.out.println("Alien hit! Score: " + score + ", Aliens left: " + alienCount);
                    }
                }

                // Eliminar balas usadas o fuera de pantalla
                if (bullet.used || bullet.y < 0) {
                    bulletIter.remove();
                }
            }

            // Pasar al siguiente nivel cuando no quedan aliens
            if (alienCount == 0) {
                score += 1000; // Puntos de bonificación
                System.out.println("Level completed! Bonus: 1000. New score: " + score);
                aliens.clear();
                bullets.clear();
                createAliens();
            }
        }
    }

    private void createAliens() {
        synchronized(gameStateLock) {
            aliens.clear();
            String[] colors = {"CYAN", "MAGENTA", "YELLOW"};

            // Crear 5 filas de 8 aliens
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 8; col++) {
                    GameObject alien = new GameObject(
                            TILE_SIZE + col * (TILE_SIZE ),
                            TILE_SIZE + row * TILE_SIZE,
                            TILE_SIZE,
                            TILE_SIZE,
                            "ALIEN",
                            -1
                    );
                    // Asignar color según la fila
                    alien.color = colors[row % colors.length];
                    aliens.add(alien);
                }
            }

            alienCount = aliens.size();
            System.out.println("Created " + alienCount + " aliens");
        }
    }

    private boolean detectCollision(GameObject a, GameObject b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    private void resetGame() {
        synchronized(gameStateLock) {
            // Guardar los IDs de los jugadores
            Set<Integer> playerIds = new HashSet<>(ships.keySet());

            // Limpiar todos los objetos
            ships.clear();
            aliens.clear();
            bullets.clear();

            // Reiniciar variables
            score = 0;
            gameOver = false;
            alienVelocityX = 1;

            // Crear nuevos aliens
            createAliens();

            // Volver a añadir jugadores
            for (int id : playerIds) {
                addPlayer(id);
            }

            System.out.println("Game reset with " + playerIds.size() + " players");
        }
    }

    public ArrayList<GameObject> getGameObjects() {
        synchronized(gameStateLock) {
            ArrayList<GameObject> objects = new ArrayList<>();
            objects.addAll(ships.values());
            objects.addAll(aliens);
            objects.addAll(bullets);
            return objects;
        }
    }

    public int getScore() {
        return score;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}