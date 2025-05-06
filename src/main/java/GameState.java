import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private static final int TILE_SIZE = 16; // Reducido de 32 a 16 para hacer todo más pequeño
    private static final int ROWS = 32;     // Incrementado de 16 a 32
    private static final int COLUMNS = 32;  // Incrementado de 16 a 32
    private int boardWidth = TILE_SIZE * COLUMNS;
    private int boardHeight = TILE_SIZE * ROWS;

    private Map<Integer, GameObject> ships = new ConcurrentHashMap<>(); // Thread-safe
    // Nuevo: Mapa para rastrear si un jugador está activo o eliminado
    private Map<Integer, Boolean> activePlayerStatus = new ConcurrentHashMap<>();

    private List<GameObject> alienBlocks = Collections.synchronizedList(new ArrayList<>()); // Bloques individuales de aliens
    private List<GameObject> bullets = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> alienBullets = Collections.synchronizedList(new ArrayList<>()); // Balas de los aliens
    private int alienVelocityX = 1;
    private int alienCount = 0;
    private int score = 0;
    private boolean gameOver = false;
    // Nuevo: variable para verificar si todos los jugadores han sido eliminados
    private boolean allPlayersEliminated = false;
    // Nuevo: Variable para rastrear si el juego se ha iniciado realmente
    private boolean gameHasStarted = false;

    private Random random = new Random();
    private long lastAlienShotTime = 0;
    private int alienShotInterval = 1500; // Milisegundos entre disparos

    // Añadir sincronización
    private final Object gameStateLock = new Object();

    public GameState() {
        // Inicializar el juego con los aliens incluso sin jugadores
        createAliens();
    }

    public void addPlayer(int playerId) {
        synchronized(gameStateLock) {
            // CORRECCIÓN: Modificamos la fórmula para que funcione correctamente con playerId 0
            // Usar una fórmula de distribución que funcione bien para todos los ID, incluyendo 0
            int shipX = TILE_SIZE * 2 + (playerId * TILE_SIZE * 6);

            // Si la posición calculada está fuera de los límites, ajustarla
            shipX = Math.max(TILE_SIZE, Math.min(shipX, boardWidth - TILE_SIZE * 3));

            GameObject ship = new GameObject(shipX, boardHeight - TILE_SIZE * 2,
                    TILE_SIZE * 2, TILE_SIZE, "SHIP", playerId);
            ships.put(playerId, ship);
            // Marcar al jugador como activo
            activePlayerStatus.put(playerId, true);

            // Si era game over y se une un nuevo jugador, reiniciar el juego
            if (allPlayersEliminated && gameHasStarted) {
                resetGame();
            }

            // Marcar que el juego ha iniciado cuando se conecta al menos un jugador
            if (!gameHasStarted) {
                gameHasStarted = true;
                allPlayersEliminated = false;
                gameOver = false;
            }

            System.out.println("Player " + playerId + " added at position: " + shipX);
        }
    }

    public void removePlayer(int playerId) {
        synchronized(gameStateLock) {
            ships.remove(playerId);
            activePlayerStatus.remove(playerId);
            System.out.println("Player " + playerId + " removed from game state");

            // Verificar si no quedan jugadores activos
            checkAllPlayersEliminated();
        }
    }

    // Método modificado para verificar si todos los jugadores han sido eliminados
    private void checkAllPlayersEliminated() {
        // Solo considerar que todos los jugadores fueron eliminados si el juego ha iniciado
        // y no hay jugadores activos
        if (gameHasStarted && (ships.isEmpty() || !activePlayerStatus.containsValue(true))) {
            allPlayersEliminated = true;
            gameOver = true;
            System.out.println("All players have been eliminated. Game over!");
        }
    }

    // Nuevo método para eliminar un jugador específico
    private void eliminatePlayer(int playerId) {
        if (activePlayerStatus.containsKey(playerId)) {
            activePlayerStatus.put(playerId, false);
            System.out.println("Player " + playerId + " has been eliminated!");

            // Verificar si no quedan jugadores activos
            checkAllPlayersEliminated();
        }
    }

    public void handleInput(int playerId, String input) {
        synchronized(gameStateLock) {
            // Verificar si el juego ha terminado o el jugador no está activo
            if (allPlayersEliminated) {
                if (input.equals("RESTART")) {
                    resetGame();
                }
                return;
            }

            // CORRECCIÓN: Verificación mejorada para el estado activo del jugador
            // Comprobar específicamente si el playerId existe y está activo
            if (!activePlayerStatus.containsKey(playerId) || !activePlayerStatus.get(playerId)) {
                System.out.println("Player " + playerId + " is not active. Input ignored.");
                return;
            }

            GameObject ship = ships.get(playerId);
            if (ship == null) {
                System.out.println("Ship not found for player: " + playerId);
                return;
            }

            if (input.equals("LEFT") && ship.x - TILE_SIZE/2 >= 0) {
                ship.x -= TILE_SIZE/2;
                System.out.println("Player " + playerId + " moved LEFT to: " + ship.x);
            } else if (input.equals("RIGHT") && ship.x + ship.width + TILE_SIZE/2 <= boardWidth) {
                ship.x += TILE_SIZE/2;
                System.out.println("Player " + playerId + " moved RIGHT to: " + ship.x);
            } else if (input.equals("SHOOT")) {
                // Crear bala en el centro del barco
                int bulletX = ship.x + (ship.width / 2) - (TILE_SIZE / 16);
                GameObject bullet = new GameObject(bulletX, ship.y,
                        TILE_SIZE / 8, TILE_SIZE / 2, "BULLET", playerId);
                bullets.add(bullet);
                System.out.println("Player " + playerId + " SHOOT from position: " + ship.x);
            }
        }
    }


    public void update() {
        synchronized(gameStateLock) {
            // Si el juego no ha iniciado o no hay jugadores activos, no mostrar game over
            if (!gameHasStarted) {
                return;
            }

            // Si no hay jugadores activos, verificar si todos fueron eliminados
            if ((ships.isEmpty() || !activePlayerStatus.containsValue(true)) && !allPlayersEliminated) {
                checkAllPlayersEliminated();
                return;
            }

            if (allPlayersEliminated) return;

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
                    for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                        int playerId = entry.getKey();
                        GameObject ship = entry.getValue();

                        // Solo verificar colisiones para jugadores activos
                        if (activePlayerStatus.getOrDefault(playerId, false) && alien.y + alien.height >= ship.y) {
                            eliminatePlayer(playerId);
                            ship.alive = false; // Marcar la nave como no viva
                            System.out.println("Game over for player " + playerId + "! Aliens reached the ship!");
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
                if (random.nextInt(100) < 40) { // Solo 40% de probabilidad de disparar cuando se cumple el intervalo
                    alienShoot();
                    lastAlienShotTime = currentTime;

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
                for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                    int playerId = entry.getKey();
                    GameObject ship = entry.getValue();

                    // Solo verificar colisiones para jugadores activos
                    if (activePlayerStatus.getOrDefault(playerId, false) &&
                            !alienBullet.used && detectCollision(alienBullet, ship)) {
                        alienBullet.used = true;

                        // Eliminar solo al jugador golpeado, no terminar todo el juego
                        eliminatePlayer(playerId);
                        ship.alive = false; // Marcar la nave como no viva

                        System.out.println("Player " + playerId + " hit by alien bullet! Player eliminated!");
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

            // Eliminar naves destruidas de la visualización
            Iterator<Map.Entry<Integer, GameObject>> shipIter = ships.entrySet().iterator();
            while (shipIter.hasNext()) {
                Map.Entry<Integer, GameObject> entry = shipIter.next();
                if (!entry.getValue().alive) {
                    shipIter.remove();
                }
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

        // Seleccionar aleatoriamente 1-2 aliens para disparar (antes era 1-3)
        int shootersCount = Math.min(2, frontLineAliens.size());

        // Añadida probabilidad aleatoria para tener aún menos disparadores
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
            allPlayersEliminated = false;
            alienVelocityX = 1;
            alienShotInterval = 1500;
            activePlayerStatus.clear();

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

            // Solo incluir naves de jugadores activos
            for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                int playerId = entry.getKey();
                if (activePlayerStatus.getOrDefault(playerId, false)) {
                    objects.add(entry.getValue());
                }
            }

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
        return allPlayersEliminated;
    }
}