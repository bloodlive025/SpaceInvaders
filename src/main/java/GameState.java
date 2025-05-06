import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private static final int TILE_SIZE = 16;
    private static final int ROWS = 32;
    private static final int COLUMNS = 32;
    private int boardWidth = TILE_SIZE * COLUMNS;
    private int boardHeight = TILE_SIZE * ROWS;

    private Map<Integer, GameObject> ships = new ConcurrentHashMap<>();
    private List<GameObject> alienBlocks = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> bullets = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> alienBullets = Collections.synchronizedList(new ArrayList<>());
    private int alienVelocityX = 1;
    private int alienCount = 0;
    private int score = 0;
    private boolean gameOver = false;
    private Random random = new Random();
    private long lastAlienShotTime = 0;
    private long lastBossShotTime = 0;

    private int alienShotIntervals = 1500; // Intervalo para aliens y nuevos enemigos
    private int bossShotInterval = 1000;   // Intervalo más corto para el jefe

    private final Object gameStateLock = new Object();

    public GameState() {
        createAliens();
    }

    public void addPlayer(int playerId) {
        synchronized(gameStateLock) {
            int shipX = boardWidth / 2 - TILE_SIZE;
            if (ships.size() > 0) {
                shipX = TILE_SIZE * 2 + (playerId * TILE_SIZE * 3);
            }
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
                int bulletX = ship.x + (ship.width / 2) - (TILE_SIZE / 16);
                GameObject bullet = new GameObject(bulletX, ship.y,
                        TILE_SIZE / 8, TILE_SIZE / 2, "BULLET", playerId);
                bullets.add(bullet);
            }
        }
    }

    public void update() {
        synchronized(gameStateLock) {
            if (ships.isEmpty() && !gameOver) {
                return;
            }

            if (gameOver) return;

            boolean changeDirection = false;

            for (GameObject alien : alienBlocks) {
                if (alien.alive) {
                    alien.x += alienVelocityX;

                    if (alien.x + alien.width >= boardWidth || alien.x <= 0) {
                        changeDirection = true;
                    }

                    for (GameObject ship : ships.values()) {
                        if (alien.type.equals("BOSS")) continue; // El jefe no baja
                        if (alien.y + alien.height >= ship.y) {
                            gameOver = true;
                            System.out.println("Game over! Aliens reached the ships!");
                        }
                    }
                }
            }

            if (changeDirection) {
                alienVelocityX *= -1;
                for (GameObject alien : alienBlocks) {
                    if (alien.alive && !alien.type.equals("BOSS")) {
                        alien.y += TILE_SIZE; // Solo los no-jefes bajan
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlienShotTime > alienShotIntervals) {
                if (random.nextInt(100) < 40) {
                    alienShoot(false); // Disparos de aliens/NEW_ALIEN
                    lastAlienShotTime = currentTime;
                    alienShotIntervals = Math.max(800, 2000 - (1000 - alienCount * 3));
                } else {
                    lastAlienShotTime = currentTime;
                }
            }

            if (currentTime - lastBossShotTime > bossShotInterval) {
                if (random.nextInt(100) < 20) {
                    alienShoot(true); // Disparos del jefe
                    lastBossShotTime = currentTime;
                } else {
                    lastBossShotTime = currentTime;
                }
            }

            Iterator<GameObject> bulletIter = bullets.iterator();
            while (bulletIter.hasNext()) {
                GameObject bullet = bulletIter.next();
                bullet.y -= 10;

                for (GameObject alien : alienBlocks) {
                    if (!bullet.used && alien.alive && detectCollision(bullet, alien)) {
                        bullet.used = true;
                        alien.alive = false;
                        alienCount--;
                        score += alien.type.equals("BOSS") ? 500 : 100; // Más puntos por el jefe
                        System.out.println("Alien block hit! Score: " + score + ", Alien blocks left: " + alienCount);
                        break;
                    }
                }

                if (bullet.used || bullet.y < 0) {
                    bulletIter.remove();
                }
            }

            Iterator<GameObject> alienBulletIter = alienBullets.iterator();
            while (alienBulletIter.hasNext()) {
                GameObject alienBullet = alienBulletIter.next();
                alienBullet.y += 7;

                for (GameObject ship : ships.values()) {
                    if (!alienBullet.used && detectCollision(alienBullet, ship)) {
                        alienBullet.used = true;
                        gameOver = true;
                        System.out.println("Player hit by alien bullet! Game over!");
                        break;
                    }
                }

                if (alienBullet.used || alienBullet.y > boardHeight) {
                    alienBulletIter.remove();
                }
            }

            if (alienCount == 0) {
                score += 1000;
                System.out.println("Level completed! Bonus: 1000. New score: " + score);
                alienBlocks.clear();
                bullets.clear();
                alienBullets.clear();
                bossLevel2();
            }
        }
    }

    private void alienShoot(boolean isBossShot) {
        if (alienBlocks.isEmpty()) return;

        if (isBossShot) {
            for (GameObject alien : alienBlocks) {
                if (alien.alive && alien.type.equals("BOSS")) {
                    int bulletX = alien.x + (alien.width / 2) - (TILE_SIZE / 8);
                    int bulletY = alien.y + alien.height;
                    GameObject bullet = new GameObject(
                            bulletX, bulletY,
                            TILE_SIZE / 4, TILE_SIZE,
                            "BOSS_BULLET", -1
                    );
                    alienBullets.add(bullet);
                    break;
                }
            }
        } else {
            Map<Integer, GameObject> frontLineAliens = new HashMap<>();
            for (GameObject alien : alienBlocks) {
                if (!alien.alive || alien.type.equals("BOSS")) continue;

                int column = alien.x / TILE_SIZE;
                if (!frontLineAliens.containsKey(column) ||
                        alien.y > frontLineAliens.get(column).y) {
                    frontLineAliens.put(column, alien);
                }
            }

            if (frontLineAliens.isEmpty()) return;

            int shootersCount = Math.min(2, frontLineAliens.size());
            if (frontLineAliens.size() > 1 && random.nextInt(100) < 50) {
                shootersCount = 1;
            }

            List<GameObject> shooters = new ArrayList<>();
            List<GameObject> frontLineList = new ArrayList<>(frontLineAliens.values());

            for (int i = 0; i < shootersCount; i++) {
                if (frontLineList.isEmpty()) break;
                int index = random.nextInt(frontLineList.size());
                GameObject shooter = frontLineList.get(index);
                shooters.add(shooter);
                frontLineList.remove(index);
            }

            for (GameObject shooter : shooters) {
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
    }

    private void createAliens() {
        synchronized(gameStateLock) {
            alienBlocks.clear();
            String[] colors = {"CYAN", "MAGENTA", "YELLOW"};

            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 16; col++) {
                    if ((row % 3 == 2) || (col % 4 == 3)) {
                        continue;
                    }
                    if (row % 3 == 0 && col % 4 != 1) {
                        continue;
                    }

                    GameObject alien = new GameObject(
                            TILE_SIZE + col * (TILE_SIZE),
                            TILE_SIZE + row * TILE_SIZE,
                            TILE_SIZE,
                            TILE_SIZE,
                            "ALIEN",
                            -1
                    );
                    alien.blockType = (row % 3) + (col % 2);
                    alien.color = colors[row % colors.length];
                    alienBlocks.add(alien);
                }
            }

            alienCount = alienBlocks.size();
            System.out.println("Created " + alienCount + " alien blocks");
        }
    }

    private void bossLevel2() {
        synchronized(gameStateLock) {
            alienBlocks.clear();

            GameObject boss = new GameObject(
                    boardWidth / 2 - TILE_SIZE * 2,
                    TILE_SIZE,
                    TILE_SIZE * 4,
                    TILE_SIZE * 2,
                    "BOSS",
                    -1
            );
            boss.color = "RED";
            alienBlocks.add(boss);

            String[] colors = {"CYAN", "MAGENTA", "YELLOW", "ORANGE"};
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 8; col++) {
                    GameObject newAlien = new GameObject(
                            TILE_SIZE * 2 + col * (TILE_SIZE * 3),
                            TILE_SIZE * 5 + row * (TILE_SIZE * 2),
                            TILE_SIZE * 2,
                            TILE_SIZE * 2,
                            "NEW_ALIEN",
                            -1
                    );
                    newAlien.color = colors[row % colors.length];
                    newAlien.blockType = row % 3;
                    alienBlocks.add(newAlien);
                }
            }

            alienCount = alienBlocks.size();
            System.out.println("Created boss and " + (alienCount - 1) + " new alien blocks for level 2");
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
            Set<Integer> playerIds = new HashSet<>(ships.keySet());
            ships.clear();
            alienBlocks.clear();
            bullets.clear();
            alienBullets.clear();

            score = 0;
            gameOver = false;
            alienVelocityX = 1;
            alienShotIntervals = 1500;
            bossShotInterval = 1000;

            createAliens();

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