import astar.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class entity {

    protected String name;

    protected final int healthCap;
    protected int health;
    protected final int moveSpeed;
    protected final int baseDamage;
    protected final int sightRange;

    protected int currentX;
    protected int currentY;

    protected boolean alive;
    protected boolean justDied;
    protected boolean moveAvailable;
    protected boolean actionAvailable;
    protected boolean hasCoverBuff;

    public int getHealthCap() {
        return healthCap;
    }

    public int getHealth() {
        return health;
    }

    public int getMoveSpeed() {
        return moveSpeed;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getSightRange() {
        return sightRange;
    }

    public void changeHealth(int change) {
        if (health + change > healthCap) {
            health = healthCap;
        }
        else if (health + change <= 0) {
            health = 0;
            alive = false;
            justDied = true;
        }
        else health = health + change;
    }

    public int getCurrentX() {
        return currentX;
    }

    public int getCurrentY() {
        return currentY;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isJustDied() {
        return justDied;
    }

    public void setJustDied(boolean justDied) {
        this.justDied = justDied;
    }

    public boolean isMoveAvailable() {
        return moveAvailable;
    }

    public boolean isActionAvailable() {
        return actionAvailable;
    }

    public boolean isHasCoverBuff() {
        return hasCoverBuff;
    }

    public void setHasCoverBuff(boolean hasCoverBuff) {
        this.hasCoverBuff = hasCoverBuff;
    }

    public int getHitChance(int dist) {
        return 96 - (dist * dist);
    }

    public String getName() {
        return name;
    }

    public entity(String name, int healthCap, int moveSpeed, int baseDamage, int sightRange, int currentX, int currentY) {
        this.name = name;
        this.healthCap = healthCap;
        this.health = healthCap;
        this.moveSpeed = moveSpeed;
        this.baseDamage = baseDamage;
        this.sightRange = sightRange;
        this.currentX = currentX;
        this.currentY = currentY;
        this.alive = true;
        this.justDied = false;
        this.moveAvailable = true;
        this.actionAvailable = true;
        this.hasCoverBuff = false;
    }

    public void refresh(){
        moveAvailable = true;
        actionAvailable = true;
        hasCoverBuff = false;
    }

    //TODO: debug excessively long paths?
    public List<Node> useAStar(map gameMap, int x, int y, boolean blockers) {
        Node start = new Node(currentX, currentY);
        Node goal = new Node(x,y);
        AStar astar = new AStar(map.MAP_WIDTH, map.MAP_HEIGHT, start, goal, 10, 10);
        //Review in case location of polling entity needs to be removed from blocker list
        if (blockers) {
            int[][] blocks = ArrayUtils.addAll(gameMap.getMapBlockers(), gameLogic.getTeamLocations());
            astar.setBlocks(blocks);
        }
        return astar.findPath();
    }

    //TODO: test validation of straight line route to target. Potential workaround fix: add immediately adjacent terrain (plus all tiles which do not move immediately closer to the target) as blockers
    public List<Node> useAStarLOS(map gameMap, int x, int y, int targetX, int targetY, int tolerance) {
        Node start = new Node(x, y);
        Node goal = new Node(targetX,targetY);
        AStar astar = new AStar(map.MAP_WIDTH, map.MAP_HEIGHT, start, goal, 10, 10);
        List<Node> path = astar.findPath();
        if (tolerance > 0) {
            for (int i = 0; i < path.size() - (1 + tolerance); i++) {
                Node losTile = path.get(path.size() - 2);
                int losType = gameMap.getCell(losTile.getRow(), losTile.getCol()).getTileType();
                if (!(losType == map.EMPTY_TILE_CODE || losType == map.SOFT_TILE_CODE)) {
                    path.clear();
                    break;
                }
            }
        }
        return path;
    }

    public int move(map gameMap, int x, int y) {
        //all sanity checking kept in this function for reference, could be moved later
        if (this.alive && this.moveAvailable && gameMap.getCell(x,y).isEmpty()) {
            List<Node> path = useAStar(gameMap, x, y, true);
            //if valid route found
            //Movespeed incremeted by 1 as A* implementation includes initial node in calculation
            if(!path.isEmpty() && path.size() <= moveSpeed + 1){
                gameMap.getCell(currentX,currentY).setOccupant(null);
                currentX = x;
                currentY = y;
                gameMap.getCell(currentX,currentY).setOccupant(this);
                moveAvailable = false;
                return 0;
            }
            //if no route found or shortest route > moveSpeed
            else {
                return 2;
            }
        }
        else {
            //attempted to move when move unavailable, sanity check
            return 1;
        }
    }

    public int sprint(map gameMap, int x, int y) {
        //copies move function using action point instead, could be more elegantly implemented
        if (this.alive && this.actionAvailable && gameMap.getCell(x,y).isEmpty()) {
            List<Node> path = useAStar(gameMap, x, y, true);
            //if valid route found
            if(!path.isEmpty() && path.size() <= moveSpeed + 1){
                gameMap.getCell(currentX,currentY).setOccupant(null);
                currentX = x;
                currentY = y;
                gameMap.getCell(currentX,currentY).setOccupant(this);
                actionAvailable = false;
                return 0;
            }
            //if no route found or shortest route > moveSpeed
            else {
                return 2;
            }
        }
        else {
            //attempted to move when move unavailable, sanity check
            return 1;
        }
    }

    public int attack(map gameMap, entity target) {
        //all sanity checking kept in this function for reference, could be moved later
        if (this.alive && target.isAlive() && this.actionAvailable) {
            List<Node> line = useAStarLOS(gameMap, this.currentX, this.currentY, target.getCurrentX(), target.getCurrentY(), 1);
            //if valid route found
            if(!line.isEmpty() && line.size() <= sightRange + 1) {
                actionAvailable = false;
                //base attack roll system, can be refined later
                int chance = this.getHitChance(line.size());
                Node coverTile = line.get(line.size() - 2);
                int coverType = gameMap.getCell(coverTile.getRow(), coverTile.getCol()).getTileType();
                if (coverType != map.EMPTY_TILE_CODE) {
                    if (coverType == map.SOFT_TILE_CODE && !target.isHasCoverBuff()) chance =- 25;
                    else chance -= 50;
                }
                else if (target.isHasCoverBuff()) chance -= 25;
                //sanity checks for maximum/minimum roll values
                if (chance < 5) chance = 5;
                else if (chance > 95) chance = 95;
                int roll = ThreadLocalRandom.current().nextInt(1, 101);
                if (roll <= chance) {
                    target.changeHealth(-baseDamage);
                    return 0;
                }
                else {
                    return 3;
                }
            }
            //if no route found, shortest route > sightRange or shortest route has line of sight blocked
            else {
                return 2;
            }
        }
        else {
            //attempted to act when move unavailable, sanity check
            return 1;
        }
    }
}
