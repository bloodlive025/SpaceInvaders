package game;
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

            if (input.equals("LEFT") && ship.getX() - TILE_SIZE/2 >= 0) {
                ship.setX(ship.getX() - TILE_SIZE/2);
                System.out.println("Player " + playerId + " moved LEFT to: " + ship.getX());
            } else if (input.equals("RIGHT") && ship.getX() + ship.getWidth() + TILE_SIZE/2 <= boardWidth) {
                ship.setX(ship.getX() + TILE_SIZE/2);
                System.out.println("Player " + playerId + " moved RIGHT to: " + ship.getX());
            } else if (input.equals("SHOOT")) {
                int bulletX = ship.getX() + (ship.getWidth() / 2) - (TILE_SIZE / 16);
                GameObject bullet = new GameObject(bulletX, ship.getY(),
                        TILE_SIZE / 8, TILE_SIZE / 2, "BULLET", playerId);
                bullets.add(bullet);
                System.out.println("Player " + playerId + " SHOOT from position: " + ship.getX());
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
                if (alien.isAlive()) {
                    if (alien.getType().equals("FINAL_BOSS")) {
                        alien.setX((int)(alien.getX() + alienVelocityX * 1.5));
                    } else {
                        alien.setX(alien.getX() + alienVelocityX);
                    }
                    if (alien.getX() + alien.getWidth() >= boardWidth || alien.getX() <= 0) {
                        changeDirection = true;
                    }
                    for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                        int playerId = entry.getKey();
                        GameObject ship = entry.getValue();
                        if (activePlayerStatus.getOrDefault(playerId, false) &&
                            !alien.getType().equals("BOSS") && !alien.getType().equals("FINAL_BOSS") &&
                            alien.getY() + alien.getHeight() >= ship.getY()) {
                            eliminatePlayer(playerId);
                            ship.setAlive(false);
                            System.out.println("Game over for player " + playerId + "! Aliens reached the ship!");
                        }
                    }
                }
            }

            if (changeDirection) {
                alienVelocityX *= -1;
                for (GameObject alien : alienBlocks) {
                    if (alien.isAlive() && !alien.getType().equals("BOSS") && !alien.getType().equals("FINAL_BOSS")) {
                        alien.setY(alien.getY() + (alien.getType().equals("FINAL_ALIEN") ? TILE_SIZE / 2 : TILE_SIZE));
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            if (currentLevel == 3 && currentTime - lastTeleportTime > teleportInterval) {
                for (GameObject alien : alienBlocks) {
                    if (alien.isAlive() && alien.getType().equals("FINAL_BOSS")) {
                        alien.setX(random.nextInt(boardWidth - alien.getWidth() + 1));
                        lastTeleportTime = currentTime;
                        System.out.println("Final boss teleported to x: " + alien.getX());
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
                bullet.setY(bullet.getY() - 10);
                for (GameObject alien : alienBlocks) {
                    if (!bullet.isUsed() && alien.isAlive() && detectCollision(bullet, alien)) {
                        bullet.setUsed(true);
                        if (alien.getType().equals("FINAL_BOSS")) {
                            alien.setHealth(alien.getHealth() - 1);
                            if (alien.getHealth() <= 0) {
                                alien.setAlive(false);
                                alienCount--;
                            }
                        } else {
                            alien.setAlive(false);
                            alienCount--;
                        }
                        int playerId = bullet.getPlayerId();
                        int points = alien.getType().equals("FINAL_BOSS") ? 1000 : 100;
                        playerScores.compute(playerId, (k, v) -> v == null ? points : v + points);
                        System.out.println("Alien block hit by player " + playerId + "! Score: " + playerScores.get(playerId) + ", Alien blocks left: " + alienCount);
                        break;
                    }
                }
                for (GameObject wall : walls) {
                    if (!bullet.isUsed() && wall.isAlive() && detectCollision(bullet, wall)) {
                        bullet.setUsed(true);
                        wall.setHealth(wall.getHealth() - 1);
                        if (wall.getHealth() <= 0) {
                            wall.setAlive(false);
                            System.out.println("Wall at (" + wall.getX() + ", " + wall.getY() + ") destroyed by player " + bullet.getPlayerId());
                        } else {
                            System.out.println("Wall at (" + wall.getX() + ", " + wall.getY() + ") hit, health: " + wall.getHealth());
                        }
                        break;
                    }
                }
                if (bullet.isUsed() || bullet.getY() < 0) {
                    bulletIter.remove();
                }
            }

            Iterator<GameObject> alienBulletIter = alienBullets.iterator();
            while (alienBulletIter.hasNext()) {
                GameObject alienBullet = alienBulletIter.next();
                alienBullet.setX((int)(alienBullet.getX() + alienBullet.getVelocityX()));
                alienBullet.setY((int)(alienBullet.getY() + alienBullet.getVelocityY()));
                for (GameObject wall : walls) {
                    if (!alienBullet.isUsed() && wall.isAlive() && detectCollision(alienBullet, wall)) {
                        alienBullet.setUsed(true);
                        System.out.println("Alien bullet blocked by wall at (" + wall.getX() + ", " + wall.getY() + ")");
                        break;
                    }
                }
                for (Map.Entry<Integer, GameObject> entry : ships.entrySet()) {
                    int playerId = entry.getKey();
                    GameObject ship = entry.getValue();
                    if (activePlayerStatus.getOrDefault(playerId, false) &&
                            !alienBullet.isUsed() && detectCollision(alienBullet, ship)) {
                        alienBullet.setUsed(true);
                        eliminatePlayer(playerId);
                        ship.setAlive(false);
                        System.out.println("Player " + playerId + " hit by alien bullet! Player eliminated!");
                        break;
                    }
                }
                if (alienBullet.isUsed() || alienBullet.getY() > boardHeight || alienBullet.getX() < 0 || alienBullet.getX() > boardWidth) {
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
                if (!entry.getValue().isAlive()) {
                    shipIter.remove();
                }
            }
        }
    }

    private void alienShoot(boolean isBossShot) {
        if (alienBlocks.isEmpty()) return;

        if (isBossShot) {
            for (GameObject alien : alienBlocks) {
                if (alien.isAlive() && (alien.getType().equals("BOSS") || alien.getType().equals("FINAL_BOSS"))) {
                    int bulletX = alien.getX() + (alien.getWidth() / 2);
                    int bulletY = alien.getY() + alien.getHeight();
                    String bulletType = alien.getType().equals("FINAL_BOSS") ? "FINAL_BOSS_BULLET" : "BOSS_BULLET";
                    int bulletWidth = alien.getType().equals("FINAL_BOSS") ? TILE_SIZE * 3 / 8 : TILE_SIZE / 4;
                    int bulletHeight = alien.getType().equals("FINAL_BOSS") ? TILE_SIZE * 3 / 4 : TILE_SIZE;
                    if (alien.getType().equals("FINAL_BOSS")) {
                        GameObject bullet1 = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet1.setVelocityX(0);
                        bullet1.setVelocityY(7);
                        GameObject bullet2 = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet2.setVelocityX(-3.5);
                        bullet2.setVelocityY(6);
                        GameObject bullet3 = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet3.setVelocityX(3.5);
                        bullet3.setVelocityY(6);
                        alienBullets.add(bullet1);
                        alienBullets.add(bullet2);
                        alienBullets.add(bullet3);
                    } else {
                        GameObject bullet = new GameObject(bulletX - bulletWidth / 2, bulletY,
                                bulletWidth, bulletHeight, bulletType, -1);
                        bullet.setVelocityX(0);
                        bullet.setVelocityY(7);
                        alienBullets.add(bullet);
                    }
                    break;
                }
            }
        } else {
            Map<Integer, GameObject> frontLineAliens = new HashMap<>();
            for (GameObject alien : alienBlocks) {
                if (!alien.isAlive() || alien.getType().equals("BOSS") || alien.getType().equals("FINAL_BOSS")) continue;
                int column = alien.getX() / TILE_SIZE;
                if (!frontLineAliens.containsKey(column) ||
                        alien.getY() > frontLineAliens.get(column).getY()) {
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
                int bulletX = shooter.getX() + (shooter.getWidth() / 2) - (TILE_SIZE / 16);
                int bulletY = shooter.getY() + shooter.getHeight();
                GameObject bullet = new GameObject(bulletX, bulletY,
                        TILE_SIZE / 8, TILE_SIZE / 2, "ALIEN_BULLET", -1);
                bullet.setVelocityX(0);
                bullet.setVelocityY(7);
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
                    alien.setBlockType((row % 3) + (col % 2));
                    alien.setColor(colors[row % colors.length]);
                    alienBlocks.add(alien);
                }
            }
            // Create walls
            int[] wallXPositions = {TILE_SIZE * 4, TILE_SIZE * 12, TILE_SIZE * 20, TILE_SIZE * 28};
            for (int x : wallXPositions) {
                GameObject wall = new GameObject(x, boardHeight - TILE_SIZE * 4,
                        TILE_SIZE, TILE_SIZE, "WALL", -1);
                wall.setHealth(3);
                wall.setAlive(true);
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
            boss.setColor("RED");
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
                    newAlien.setColor(colors[row % colors.length]);
                    newAlien.setBlockType(row % 3);
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
            finalBoss.setColor("PURPLE");
            finalBoss.setHealth(5);
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
                    finalAlien.setColor(colors[row % colors.length]);
                    finalAlien.setBlockType(row % 3);
                    alienBlocks.add(finalAlien);
                }
            }
            alienCount = alienBlocks.size();
            System.out.println("Created final boss and " + (alienCount - 1) + " final alien blocks for Level 3");
        }
    }

    private boolean detectCollision(GameObject a, GameObject b) {
        return a.getX() < b.getX() + b.getWidth() &&
               a.getX() + a.getWidth() > b.getX() &&
               a.getY() < b.getY() + b.getHeight() &&
               a.getY() + a.getHeight() > b.getY();
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