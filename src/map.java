import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class map {

    public static final int EMPTY_TILE_CODE = 0;
    public static final int SOFT_TILE_CODE = 1;
    public static final int HARD_TILE_CODE = 2;
    public static final int EXTRA_HARD_TILE_CODE = 3;
    public static final int[] SUBCOMPONENT_ZERO = new int[]{0,0};
    public static final int[] SUBCOMPONENT_ONE = new int[]{-1,0};
    public static final int[] SUBCOMPONENT_TWO = new int[]{1,0};
    public static final int[] SUBCOMPONENT_THREE = new int[]{0,-1};
    public static final int[] SUBCOMPONENT_FOUR = new int[]{0,1};

    public static final ArrayList<int[]> COMPONENT_ZERO = new ArrayList<>(Arrays.asList(SUBCOMPONENT_ZERO));
    public static final ArrayList<int[]> COMPONENT_ONE = new ArrayList<>(Arrays.asList(SUBCOMPONENT_ZERO, SUBCOMPONENT_TWO));
    public static final ArrayList<int[]> COMPONENT_TWO = new ArrayList<>(Arrays.asList(SUBCOMPONENT_ZERO, SUBCOMPONENT_FOUR));
    public static final ArrayList<int[]> COMPONENT_THREE = new ArrayList<>(Arrays.asList(SUBCOMPONENT_ZERO, SUBCOMPONENT_TWO, SUBCOMPONENT_FOUR));
    public static final ArrayList<int[]> COMPONENT_FOUR = new ArrayList<>(Arrays.asList(SUBCOMPONENT_ZERO, SUBCOMPONENT_ONE, SUBCOMPONENT_TWO, SUBCOMPONENT_THREE, SUBCOMPONENT_FOUR));

    //TODO: will be final for non demo build
    public static int MAP_WIDTH;
    public static int MAP_HEIGHT;

    private final tile[][] mapInternal;
    private ArrayList<int[]> mapBlockers;
    static final int[] DEMO_WALL_SPAWN = new int[]{5, 7};

    public map(boolean demo) {
        if (demo) {
            MAP_WIDTH = 10;
            MAP_HEIGHT = 10;
            mapInternal = new tile[10][10];
            mapBlockers = new ArrayList<>();
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    mapInternal[x][y] = new tile(EMPTY_TILE_CODE);
                }
            }
            mapInternal[DEMO_WALL_SPAWN[0] - 1][DEMO_WALL_SPAWN[1]].setTileType(HARD_TILE_CODE);
            int[] newBlocker = new int[]{DEMO_WALL_SPAWN[0] - 1, DEMO_WALL_SPAWN[1]};
            mapBlockers.add(newBlocker);
            mapInternal[DEMO_WALL_SPAWN[0] - 2][DEMO_WALL_SPAWN[1]].setTileType(HARD_TILE_CODE);
            newBlocker = new int[]{DEMO_WALL_SPAWN[0] + 2, DEMO_WALL_SPAWN[1]};
            mapBlockers.add(newBlocker);
            mapInternal[DEMO_WALL_SPAWN[0] + 1][DEMO_WALL_SPAWN[1]].setTileType(HARD_TILE_CODE);
            newBlocker = new int[]{DEMO_WALL_SPAWN[0] + 1, DEMO_WALL_SPAWN[1]};
            mapBlockers.add(newBlocker);
            mapInternal[DEMO_WALL_SPAWN[0] + 2][DEMO_WALL_SPAWN[1]].setTileType(HARD_TILE_CODE);
            newBlocker = new int[]{DEMO_WALL_SPAWN[0] + 2, DEMO_WALL_SPAWN[1]};
            mapBlockers.add(newBlocker);
        }
        else {
            MAP_WIDTH = 20;
            MAP_HEIGHT = 20;
            mapInternal = new tile[MAP_WIDTH][MAP_HEIGHT];
            for (int y = 0; y < MAP_HEIGHT / 2; y++) {
                for (int x = 0; x < MAP_WIDTH; x++) {
                    mapInternal[x][y] = new tile(EMPTY_TILE_CODE);
                }
            }
            //procedural terrain creation
            //Sets aside one quarter of the map (pending tuning) to be made up of impassible/cover tiles, and randomly
            //splits said tile budget to be assigned partly to soft cover and partly to hard cover
            int tileBudget = (MAP_WIDTH * MAP_HEIGHT) / 8;
            mapBlockers = new ArrayList<>();
            int i = 0;
            int softBudget = ThreadLocalRandom.current().nextInt(tileBudget / 3, ((tileBudget * 2) / 3) + 1);
            int hardBudget = tileBudget - softBudget;

            //Procedural generation; attempts to exhaust both tile budgets
            while (softBudget > 0 && hardBudget > 0) {
                //Select random location in top half of the map
                int nextX = ThreadLocalRandom.current().nextInt(0, MAP_WIDTH);
                int nextY = ThreadLocalRandom.current().nextInt(0, MAP_HEIGHT / 2);
                //Select a random procedural component
                if (mapInternal[nextX][nextY].getTileType() == 0) {
                    ArrayList<int[]> component = switch (ThreadLocalRandom.current().nextInt(0, 5)) {
                        case 1 -> COMPONENT_ONE;
                        case 2 -> COMPONENT_TWO;
                        case 3 -> COMPONENT_THREE;
                        case 4 -> COMPONENT_FOUR;
                        default -> COMPONENT_ZERO;
                    };
                    //Allocate this component to a tile type which still has the budget for it
                    int tileType = 0;
                    if (component.size() <= softBudget && component.size() <= hardBudget) {
                        tileType = ThreadLocalRandom.current().nextInt(SOFT_TILE_CODE, HARD_TILE_CODE + 1);
                    } else if (component.size() <= softBudget && component.size() > hardBudget) {
                        tileType = SOFT_TILE_CODE;
                    } else if (component.size() > softBudget && component.size() <= hardBudget) {
                        tileType = HARD_TILE_CODE;
                    }

                    //If it cannot be supported by any budget, try again
                    if (tileType != 0) {
                        //Test that the component does not go out of bounds or overlap with an already placed component
                        boolean valid = true;
                        for (int[] coord : component) {
                            int testX = nextX + coord[0];
                            int testY = nextY + coord[1];
                            if (testX < 0 || testX >= MAP_WIDTH || testY < 0 || testY >= MAP_HEIGHT / 2 || mapInternal[testX][testY].getTileType() != 0) {
                                valid = false;
                                break;
                            }
                        }
                        // If the component is valid, update all relevant tiles and decrement the appropriate budget
                        if (valid) {
                            for (int[] coord : component) {
                                mapInternal[nextX + coord[0]][nextY + coord[1]].setTileType(tileType);
                                int[] newBlocker = {nextX + coord[0], nextY + coord[1]};
                                mapBlockers.add(newBlocker);
                                i++;
                            }
                            if (tileType == SOFT_TILE_CODE) softBudget -= component.size();
                            else if (tileType == HARD_TILE_CODE) hardBudget -= component.size();
                        }
                    }
                }
            }
            //mirrors procedural terrain from top half of map
            for (int y = 0; y < MAP_HEIGHT / 2; y++) {
                for (int x = 0; x < MAP_WIDTH; x++) {
                    mapInternal[MAP_WIDTH - (x + 1)][MAP_HEIGHT - (y + 1)] = new tile(mapInternal[x][y].getTileType());
                }
            }
        }
    }

    public tile getCell(int x, int y) {
        return mapInternal[x][y];
    }

    //For the purpose of compatibility with the A* library, this method converts the list into a 2d array
    public int[][] getMapBlockers() {
        int[][] returnBlockers = new int[mapBlockers.size()][2];
        for (int i = 0; i < mapBlockers.size(); i++){
            returnBlockers[i][0] = mapBlockers.get(i)[0];
            returnBlockers[i][1] = mapBlockers.get(i)[1];
        }
        return returnBlockers;
    }

    public void removeBlocker(int x, int y) {
        int[] blocker = {x, y};
        mapInternal[x][y].setTileType(EMPTY_TILE_CODE);
        mapBlockers.remove(blocker);
    }
}
