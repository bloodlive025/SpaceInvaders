import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {
    private static final int TILE_SIZE = 16;
    private static final int ROWS = 32;
    private static final int COLUMNS = 32;
    private int boardWidth = TILE_SIZE * COLUMNS;
    private int boardHeight = TILE_SIZE * ROWS;

    private Map<Integer, GameObject> ships = new ConcurrentHashMap<>();
    private Map<Integer, Boolean> activePlayerStatus = new ConcurrentHashMap<>();
    private List<GameObject> alienBlocks = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> bullets = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> alienBullets = Collections.synchronizedList(new ArrayList<>());
    private List<GameObject> walls = Collections.synchronizedList(new ArrayList<>());
    private int alienVelocityX = 1;
    private int alienCount = 0;
    private Map<Integer, Integer> playerScores = new ConcurrentHashMap<>();
    private boolean gameOver = false;
    private boolean allPlayersEliminated = false;
    private boolean gameHasStarted = false;
    private Random random = new Random();
    private long lastAlienShotTime = 0;
    private long lastBossShotTime = 0;
    private long lastFinalAlienShotTime = 0;
    private long lastTeleportTime = 0;
    private int alienShotInterval = 1500;
    private int bossShotInterval = 1000;
    private int finalAlienShotInterval = 1200;
    private int teleportInterval = 5000;
    private int currentLevel = 1;
    private final Object gameStateLock = new Object();

    public GameState() {
        createAliens();
    }

    public void addPlayer(int playerId) {
        synchronized(gameStateLock) {
            int shipX = TILE_SIZE * 2 + (playerId * TILE_SIZE * 6);
            shipX = Math.max(TILE_SIZE, Math.min(shipX, boardWidth - TILE_SIZE * 3));
            GameObject ship = new GameObject(shipX, boardHeight - TILE_SIZE * 2,
                    TILE_SIZE * 2, TILE_SIZE, "SHIP", playerId);
            ships.put(playerId, ship);
            activePlayerStatus.put(playerId, true);
            playerScores.putIfAbsent(playerId, 0);

            if (allPlayersEliminated && gameHasStarted) {
                resetGame();
            }

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
            playerScores.remove(playerId);
            System.out.println("Player " + playerId + " removed from game state");

            checkAllPlayersEliminated();
        }
    }

    private void checkAllPlayersEliminated() {
        if (gameHasStarted && (ships.isEmpty() || !activePlayerStatus.containsValue(true))) {
            allPlayersEliminated = true;
            gameOver = true;
            System.out.println("All players have been eliminated. Game over!");
        }
    }

    private void eliminatePlayer(int playerId) {
        if (activePlayerStatus.containsKey(playerId)) {
            activePlayerStatus.put(playerId, false);
            System.out.println("Player " + playerId + " has been eliminated!");
            checkAllPlayersEliminated();
        }
    }

    public void handleInput(int playerId, String input) {
        synchronized(gameStateLock) {
            if (allPlayersEliminated) {
                if (input.equals("RESTART")) {
                    resetGame();
                }
                return;
            }

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
            if (!gameHasStarted) {
                return;
            }

            if ((ships.isEmpty() || !activePlayerStatus.containsValue(true)) && !allPlayersEliminated) {
                checkAllPlayersEliminated();
                return;
            }

            if (allPlayersEliminated) return;

            boolean changeDirection = false;
            for (GameObject alien : alienBlocks) {
                if (alien.alive) {
                    if (alien.type.equals("FINAL_BOSS")) {
                        alien.x += alienVelocityX * 1.5;
                    } else {
                        alien.x += alienVelocityX;
                    }
                    if (alien.x + alien.width >= boardWidth || alien.x <= 0) {
                        changeDirection = true;
                    }
                    for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                        int playerId = entry.getKey();
                        GameObject ship = entry.getValue();
                        if (activePlayerStatus.getOrDefault(playerId, false) &&
                            !alien.type.equals("BOSS") && !alien.type.equals("FINAL_BOSS") &&
                            alien.y + alien.height >= ship.y) {
                            eliminatePlayer(playerId);
                            ship.alive = false;
                            System.out.println("Game over for player " + playerId + "! Aliens reached the ship!");
                        }
                    }
                }
            }

            if (changeDirection) {
                alienVelocityX *= -1;
                for (GameObject alien : alienBlocks) {
                    if (alien.alive && !alien.type.equals("BOSS") && !alien.type.equals("FINAL_BOSS")) {
                        alien.y += alien.type.equals("FINAL_ALIEN") ? TILE_SIZE / 2 : TILE_SIZE;
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentLevel == 3 && currentTime - lastTeleportTime > teleportInterval) {
                for (GameObject alien : alienBlocks) {
                    if (alien.alive && alien.type.equals("FINAL_BOSS")) {
                        alien.x = random.nextInt(boardWidth - alien.width + 1);
                        lastTeleportTime = currentTime;
                        System.out.println("Final boss teleported to x: " + alien.x);
                        break;
                    }
                }
            }

            if (currentTime - lastAlienShotTime > alienShotInterval && currentLevel != 3) {
                if (random.nextInt(100) < 40) {
                    alienShoot(false);
                    lastAlienShotTime = currentTime;
                    alienShotInterval = Math.max(800, 2000 - (1000 - alienCount * 3));
                } else {
                    lastAlienShotTime = currentTime;
                }
            }

            if (currentTime - lastFinalAlienShotTime > finalAlienShotInterval && currentLevel == 3) {
                if (random.nextInt(100) < 50) {
                    alienShoot(false);
                    lastFinalAlienShotTime = currentTime;
                } else {
                    lastFinalAlienShotTime = currentTime;
                }
            }

            if (currentTime - lastBossShotTime > bossShotInterval) {
                if (random.nextInt(100) < (currentLevel == 3 ? 25 : 20)) {
                    alienShoot(true);
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
                        if (alien.type.equals("FINAL_BOSS")) {
                            alien.health--;
                            if (alien.health <= 0) {
                                alien.alive = false;
                                alienCount--;
                            }
                        } else {
                            alien.alive = false;
                            alienCount--;
                        }
                        int playerId = bullet.playerId;
                        int points = alien.type.equals("FINAL_BOSS") ? 1000 : 100;
                        playerScores.compute(playerId, (k, v) -> v == null ? points : v + points);
                        System.out.println("Alien block hit by player " + playerId + "! Score: " + playerScores.get(playerId) + ", Alien blocks left: " + alienCount);
                        break;
                    }
                }
                for (GameObject wall : walls) {
                    if (!bullet.used && wall.alive && detectCollision(bullet, wall)) {
                        bullet.used = true;
                        wall.health--;
                        if (wall.health <= 0) {
                            wall.alive = false;
                            System.out.println("Wall at (" + wall.x + ", " + wall.y + ") destroyed by player " + bullet.playerId);
                        } else {
                            System.out.println("Wall at (" + wall.x + ", " + wall.y + ") hit, health: " + wall.health);
                        }
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
                alienBullet.x += alienBullet.velocityX;
                alienBullet.y += alienBullet.velocityY;
                for (GameObject wall : walls) {
                    if (!alienBullet.used && wall.alive && detectCollision(alienBullet, wall)) {
                        alienBullet.used = true;
                        System.out.println("Alien bullet blocked by wall at (" + wall.x + ", " + wall.y + ")");
                        break;
                    }
                }
                for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                    int playerId = entry.getKey();
                    GameObject ship = entry.getValue();
                    if (activePlayerStatus.getOrDefault(playerId, false) &&
                            !alienBullet.used && detectCollision(alienBullet, ship)) {
                        alienBullet.used = true;
                        eliminatePlayer(playerId);
                        ship.alive = false;
                        System.out.println("Player " + playerId + " hit by alien bullet! Player eliminated!");
                        break;
                    }
                }
                if (alienBullet.used || alienBullet.y > boardHeight || alienBullet.x < 0 || alienBullet.x > boardWidth) {
                    alienBulletIter.remove();
                }
            }

            if (alienCount == 0) {
                for (Integer playerId : playerScores.keySet()) {
                    playerScores.compute(playerId, (k, v) -> v == null ? 1000 : v + 1000);
                }
                System.out.println("Level " + currentLevel + " completed! Bonus: 1000 added to all players.");
                alienBlocks.clear();
                bullets.clear();
                alienBullets.clear();
                currentLevel++;
                if (currentLevel == 2) {
                    bossLevel2();
                } else if (currentLevel == 3) {
                    finalLevel3();
                } else {
                    currentLevel = 1;
                    createAliens();
                    for (Integer playerId : playerScores.keySet()) {
                        playerScores.compute(playerId, (k, v) -> v == null ? 2000 : v + 2000);
                    }
                    System.out.println("Game completed! Bonus: 2000 added to all players.");
                }
            }

            Iterator<Map.Entry<Integer, GameObject>> shipIter = ships.entrySet().iterator();
            while (shipIter.hasNext()) {
                Map.Entry<Integer, GameObject> entry = shipIter.next();
                if (!entry.getValue().alive) {
                    shipIter.remove();
                }
            }
        }
    }

    private void alienShoot(boolean isBossShot) {
        if (alienBlocks.isEmpty()) return;

        if (isBossShot) {
            for (GameObject alien : alienBlocks) {
                if (alien.alive && (alien.type.equals("BOSS") || alien.type.equals("FINAL_BOSS"))) {
                    int bulletX = alien.x + (alien.width / 2);
                    int bulletY = alien.y + alien.height;
                    String bulletType = alien.type.equals("FINAL_BOSS") ? "FINAL_BOSS_BULLET" : "BOSS_BULLET";
                    int bulletWidth = alien.type.equals("FINAL_BOSS") ? TILE_SIZE * 3 / 8 : TILE_SIZE / 4;
                    int bulletHeight = alien.type.equals("FINAL_BOSS") ? TILE_SIZE * 3 / 4 : TILE_SIZE;
                    if (alien.type.equals("FINAL_BOSS")) {
                        GameObject bullet1 = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet1.velocityX = 0;
                        bullet1.velocityY = 7;
                        GameObject bullet2 = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet2.velocityX = -3.5;
                        bullet2.velocityY = 6;
                        GameObject bullet3 = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet3.velocityX = 3.5;
                        bullet3.velocityY = 6;
                        alienBullets.add(bullet1);
                        alienBullets.add(bullet2);
                        alienBullets.add(bullet3);
                    } else {
                        GameObject bullet = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet.velocityX = 0;
                        bullet.velocityY = 7;
                        alienBullets.add(bullet);
                    }
                    break;
                }
            }
        } else {
            Map<Integer, GameObject> frontLineAliens = new HashMap<>();
            for (GameObject alien : alienBlocks) {
                if (!alien.alive || alien.type.equals("BOSS") || alien.type.equals("FINAL_BOSS")) continue;
                int column = alien.x / TILE_SIZE;
                if (!frontLineAliens.containsKey(column) ||
                        alien.y > frontLineAliens.get(column).y) {
                    frontLineAliens.put(column, alien);
                }
            }
            if (frontLineAliens.isEmpty()) return;
            int shootersCount = currentLevel == 3 ? Math.min(3, frontLineAliens.size()) : Math.min(2, frontLineAliens.size());
            if (frontLineAliens.size() > 1 && random.nextInt(100) < 50) {
                shootersCount = 1;
            }
            List<GameObject> shooters = new ArrayList<>(frontLineAliens.values());
            for (int i = 0; i < shootersCount && !shooters.isEmpty(); i++) {
                int index = random.nextInt(shooters.size());
                GameObject shooter = shooters.get(index);
                shooters.remove(index);
                int bulletX = shooter.x + (shooter.width / 2) - (TILE_SIZE / 16);
                int bulletY = shooter.y + shooter.height;
                GameObject bullet = new GameObject(bulletX, bulletY,
                        TILE_SIZE / 8, TILE_SIZE / 2, "ALIEN_BULLET", -1);
                bullet.velocityX = 0;
                bullet.velocityY = 7;
                alienBullets.add(bullet);
            }
        }
    }

    private void createAliens() {
        synchronized(gameStateLock) {
            alienBlocks.clear();
            walls.clear();
            String[] colors = {"CYAN", "MAGENTA", "YELLOW"};
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 16; col++) {
                    if ((row % 3 == 2) || (col % 4 == 3)) continue;
                    if (row % 3 == 0 && col % 4 != 1) continue;
                    GameObject alien = new GameObject(
                            TILE_SIZE + col * TILE_SIZE,
                            TILE_SIZE + row * TILE_SIZE,
                            TILE_SIZE, TILE_SIZE, "ALIEN", -1);
                    alien.blockType = (row % 3) + (col % 2);
                    alien.color = colors[row % colors.length];
                    alienBlocks.add(alien);
                }
            }
            // Create walls
            int[] wallXPositions = {TILE_SIZE * 4, TILE_SIZE * 12, TILE_SIZE * 20, TILE_SIZE * 28};
            for (int x : wallXPositions) {
                GameObject wall = new GameObject(x, boardHeight - TILE_SIZE * 4,
                        TILE_SIZE, TILE_SIZE, "WALL", -1);
                wall.health = 3;
                wall.alive = true;
                walls.add(wall);
            }
            alienCount = alienBlocks.size();
            currentLevel = 1;
            System.out.println("Created " + alienCount + " alien blocks and " + walls.size() + " walls for Level 1");
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
            System.out.println("Created boss and " + (alienCount - 1) + " new alien blocks for Level 2");
        }
    }

    private void finalLevel3() {
        synchronized(gameStateLock) {
            alienBlocks.clear();
            GameObject finalBoss = new GameObject(
                    boardWidth / 2 - TILE_SIZE * 3,
                    TILE_SIZE,
                    TILE_SIZE * 5,
                    TILE_SIZE * 3,
                    "FINAL_BOSS",
                    -1
            );
            finalBoss.color = "PURPLE";
            finalBoss.health = 5;
            alienBlocks.add(finalBoss);
            String[] colors = {"RED", "PINK", "WHITE"};
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 10; col++) {
                    GameObject finalAlien = new GameObject(
                            TILE_SIZE + col * (TILE_SIZE * 3),
                            TILE_SIZE * 5 + row * (TILE_SIZE * 2),
                            TILE_SIZE,
                            TILE_SIZE,
                            "FINAL_ALIEN",
                            -1
                    );
                    finalAlien.color = colors[row % colors.length];
                    finalAlien.blockType = row % 3;
                    alienBlocks.add(finalAlien);
                }
            }
            alienCount = alienBlocks.size();
            System.out.println("Created final boss and " + (alienCount - 1) + " final alien blocks for Level 3");
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
            walls.clear();
            gameOver = false;
            allPlayersEliminated = false;
            alienVelocityX = 1;
            alienShotInterval = 1500;
            bossShotInterval = 1000;
            finalAlienShotInterval = 1200;
            activePlayerStatus.clear();
            playerScores.clear();
            currentLevel = 1;
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
            for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                int playerId = entry.getKey();
                if (activePlayerStatus.getOrDefault(playerId, false)) {
                    objects.add(entry.getValue());
                }
            }
            objects.addAll(alienBlocks);
            objects.addAll(bullets);
            objects.addAll(alienBullets);
            objects.addAll(walls);
            return objects;
        }
    }

    public int getScore(int playerId) {
        synchronized(gameStateLock) {
            return playerScores.getOrDefault(playerId, 0);
        }
    }

    public Map<Integer, Integer> getPlayerScores() {
        synchronized(gameStateLock) {
            return new HashMap<>(playerScores);
        }
    }

    public boolean isGameOver() {
        return allPlayersEliminated;
    }
}