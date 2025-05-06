import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private static final int TILE_SIZE = 16; // Reducido de 32 a 16 para hacer todo más pequeño
    private static final int ROWS = 32;     // Incrementado de 16 a 32
    private static final int COLUMNS = 32;  // Incrementado de 16 a 32
    private int boardWidth = TILE_SIZE * COLUMNS;
    private int boardHeight = TILE_SIZE * ROWS;

    private Map<Integer, GameObject> ships = new ConcurrentHashMap<>(); // Thread-safe
    private List<GameObject> alienBlocks = Collections.synchronizedList(new ArrayList<>()); // Bloques individuales de aliens
    private List<GameObject> bullets = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> alienBullets = Collections.synchronizedList(new ArrayList<>()); // Balas de los aliens
    private int alienVelocityX = 1;
    private int alienCount = 0;
    private int score = 0;
    private boolean gameOver = false;
    private Random random = new Random();
    private long lastAlienShotTime = 0;

    // MODIFICADO: Se aumentó el intervalo base de disparo de 500ms a 1500ms
    private int alienShotInterval = 1500; // Milisegundos entre disparos (aumentado para reducir frecuencia)

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

            if (input.equals("LEFT") && ship.x - TILE_SIZE/2 >= 0) {
                ship.x -= TILE_SIZE/2;
            } else if (input.equals("RIGHT") && ship.x + ship.width + TILE_SIZE/2 <= boardWidth) {
                ship.x += TILE_SIZE/2;
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

            // Verificar si algún alien toca los bordes
            for (GameObject alien : alienBlocks) {
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
                for (GameObject alien : alienBlocks) {
                    if (alien.alive) {
                        alien.y += TILE_SIZE;
                    }
                }
            }

            // Lógica para que los aliens disparen
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlienShotTime > alienShotInterval) {
                // MODIFICADO: Añadido un elemento de aleatoriedad adicional para reducir disparos
                if (random.nextInt(100) < 40) { // Solo 40% de probabilidad de disparar cuando se cumple el intervalo
                    alienShoot();
                    lastAlienShotTime = currentTime;

                    // MODIFICADO: Reducida la velocidad de incremento de la dificultad
                    // Cuantos menos aliens haya, más rápido dispararán pero con límites más altos
                    alienShotInterval = Math.max(800, 2000 - (1000 - alienCount * 3));
                } else {
                    // Si no dispara, actualizamos el tiempo para que no intente inmediatamente
                    lastAlienShotTime = currentTime;
                }
            }

            // Mover balas del jugador y detectar colisiones
            Iterator<GameObject> bulletIter = bullets.iterator();
            while (bulletIter.hasNext()) {
                GameObject bullet = bulletIter.next();
                bullet.y -= 10; // Velocidad de la bala hacia arriba

                // Verificar colisiones con aliens
                for (GameObject alien : alienBlocks) {
                    if (!bullet.used && alien.alive && detectCollision(bullet, alien)) {
                        bullet.used = true;
                        alien.alive = false;
                        alienCount--;
                        score += 100;
                        System.out.println("Alien block hit! Score: " + score + ", Alien blocks left: " + alienCount);
                        break;  // Una bala solo puede golpear un bloque
                    }
                }

                // Eliminar balas usadas o fuera de pantalla
                if (bullet.used || bullet.y < 0) {
                    bulletIter.remove();
                }
            }

            // Mover balas de los aliens y detectar colisiones
            Iterator<GameObject> alienBulletIter = alienBullets.iterator();
            while (alienBulletIter.hasNext()) {
                GameObject alienBullet = alienBulletIter.next();
                alienBullet.y += 7; // Velocidad de la bala hacia abajo

                // Verificar colisiones con las naves de los jugadores
                for (GameObject ship : ships.values()) {
                    if (!alienBullet.used && detectCollision(alienBullet, ship)) {
                        alienBullet.used = true;
                        gameOver = true;
                        System.out.println("Player hit by alien bullet! Game over!");
                        break;
                    }
                }

                // Eliminar balas usadas o fuera de pantalla
                if (alienBullet.used || alienBullet.y > boardHeight) {
                    alienBulletIter.remove();
                }
            }

            // Pasar al siguiente nivel cuando no quedan aliens
            if (alienCount == 0) {
                score += 1000; // Puntos de bonificación
                System.out.println("Level completed! Bonus: 1000. New score: " + score);
                alienBlocks.clear();
                bullets.clear();
                alienBullets.clear();
                createAliens();
            }
        }
    }

    // Método para que los aliens disparen
    private void alienShoot() {
        if (alienBlocks.isEmpty()) return;

        // Encontrar aliens en la "primera línea" (los más bajos en cada columna)
        Map<Integer, GameObject> frontLineAliens = new HashMap<>();

        for (GameObject alien : alienBlocks) {
            if (!alien.alive) continue;

            // Usamos la coordenada X como clave para agrupar aliens por columna
            int column = alien.x / TILE_SIZE;

            // Si no hay un alien en esta columna o este alien está más abajo, actualizar
            if (!frontLineAliens.containsKey(column) ||
                    alien.y > frontLineAliens.get(column).y) {
                frontLineAliens.put(column, alien);
            }
        }

        if (frontLineAliens.isEmpty()) return;

        // MODIFICADO: Reducido el número máximo de disparadores
        // Seleccionar aleatoriamente 1-2 aliens para disparar (antes era 1-3)
        int shootersCount = Math.min(2, frontLineAliens.size());

        // MODIFICADO: Añadida probabilidad aleatoria para tener aún menos disparadores
        if (frontLineAliens.size() > 1 && random.nextInt(100) < 50) {
            shootersCount = 1; // 50% de probabilidades de que solo dispare un alienígena
        }

        List<GameObject> shooters = new ArrayList<>();

        // Convertir valores del mapa a una lista
        List<GameObject> frontLineList = new ArrayList<>(frontLineAliens.values());

        // Seleccionar aleatoriamente shootersCount aliens de la lista
        for (int i = 0; i < shootersCount; i++) {
            if (frontLineList.isEmpty()) break;

            int index = random.nextInt(frontLineList.size());
            GameObject shooter = frontLineList.get(index);
            shooters.add(shooter);
            frontLineList.remove(index);
        }

        // Hacer que los aliens seleccionados disparen
        for (GameObject shooter : shooters) {
            // Crear una bala en el centro-abajo del alien
            int bulletX = shooter.x + (shooter.width / 2) - (TILE_SIZE / 16);
            int bulletY = shooter.y + shooter.height;

            GameObject bullet = new GameObject(
                    bulletX, bulletY,
                    TILE_SIZE / 8, TILE_SIZE / 2,
                    "ALIEN_BULLET", -1
            );
            alienBullets.add(bullet);
        }
    }

    private void createAliens() {
        synchronized(gameStateLock) {
            alienBlocks.clear();
            String[] colors = {"CYAN", "MAGENTA", "YELLOW"};

            // Crear formaciones de naves alienígenas
            // Ahora creamos más filas y columnas de aliens
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 16; col++) {
                    // Dejamos espacios para formar "flotas" de naves
                    if ((row % 3 == 2) || (col % 4 == 3)) {
                        continue;  // Saltar para crear espacios
                    }

                    // Para crear forma de nave triangular
                    if (row % 3 == 0 && col % 4 != 1) {
                        continue;  // Saltar para crear forma triangular
                    }

                    GameObject alien = new GameObject(
                            TILE_SIZE + col * (TILE_SIZE),
                            TILE_SIZE + row * TILE_SIZE,
                            TILE_SIZE,
                            TILE_SIZE,
                            "ALIEN",
                            -1
                    );

                    // Definir forma específica
                    alien.blockType = (row % 3) + (col % 2);  // Variedad de formas

                    // Asignar color según la fila
                    alien.color = colors[row % colors.length];
                    alienBlocks.add(alien);
                }
            }

            alienCount = alienBlocks.size();
            System.out.println("Created " + alienCount + " alien blocks");
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
            alienBlocks.clear();
            bullets.clear();
            alienBullets.clear();

            // Reiniciar variables
            score = 0;
            gameOver = false;
            alienVelocityX = 1;

            // MODIFICADO: Reiniciar con un intervalo más largo (1500ms en lugar de 500ms)
            alienShotInterval = 1500;

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
            objects.addAll(alienBlocks);
            objects.addAll(bullets);
            objects.addAll(alienBullets);
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