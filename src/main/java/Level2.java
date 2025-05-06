import java.util.List;

public class Level2 {
    private static final int TILE_SIZE = 16; // Reducido de 32 a 16 para hacer todo más pequeño
    private static final int COLUMNS = 32;  // Incrementado de 16 a 32
    private static int boardWidth = TILE_SIZE * COLUMNS;

    public static void bossLevel2(Object gameStateLock, List<GameObject> alienBlocks) {
        synchronized (gameStateLock) {
            alienBlocks.clear();

            // Crear un jefe en el centro de la pantalla
            GameObject boss = new GameObject(
                    boardWidth / 2 - TILE_SIZE * 2,
                    TILE_SIZE,
                    TILE_SIZE * 4,
                    TILE_SIZE * 2,
                    "BOSS",
                    -1);
            boss.color = "RED"; // Color del jefe
            alienBlocks.add(boss);

            System.out.println("Created boss alien block");
        }
    }
    

    public static void createAliens(Object gameStateLock, List<GameObject> alienBlocks) {
        synchronized(gameStateLock) {
            alienBlocks.clear();
            String[] colors = {"CYAN", "MAGENTA", "YELLOW"};

            // Crear formaciones de naves alienígenas
            // Ahora creamos más filas y columnas de aliens
            for (int row = 0; row < 6; row++) {
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
        }
    }

}
