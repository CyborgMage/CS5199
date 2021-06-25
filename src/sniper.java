import astar.Node;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class sniper extends entity {

    public static final int SNIPE_RANGE = 8;
    public static final int SNIPE_DAMAGE = 3;
    public static final int SNIPE_COOLDOWN = 3;
    private int snipeTracker;

    public static final int HIDE_COOLDOWN = 2;
    private int hideTracker;

    public boolean canSnipe() {
        return (this.snipeTracker == SNIPE_COOLDOWN);
    }

    public boolean canHide() {
        return (this.hideTracker == HIDE_COOLDOWN);
    }

    public int getSnipeChance(int dist) {
        return 50 + (dist * dist);
    }



    public sniper(String name, int healthCap, int moveSpeed, int baseDamage, int sightRange, int currentX, int currentY) {
        super(name, healthCap, moveSpeed, baseDamage, sightRange, currentX, currentY);
        this.snipeTracker = SNIPE_COOLDOWN;
        this.hideTracker = HIDE_COOLDOWN;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (snipeTracker < SNIPE_COOLDOWN) snipeTracker++;
        if (hideTracker < HIDE_COOLDOWN) hideTracker++;
    }

    public int snipe(map gameMap, entity target) {
        //all sanity checking kept in this function for reference, could be moved later
        if (this.alive && target.isAlive() && this.actionAvailable && this.snipeTracker == SNIPE_COOLDOWN) {
            List<Node> line = useAStarLOS(gameMap, this.currentX, this.currentY, target.getCurrentX(), target.getCurrentY(), 1);
            //if valid route found
            if(!line.isEmpty() && line.size() <= SNIPE_RANGE + 1) {
                actionAvailable = false;
                snipeTracker = 0;
                //base attack roll system, can be refined later; better at range?
                int chance = this.getSnipeChance(line.size());
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
                    target.changeHealth(-SNIPE_DAMAGE);
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

    //placeholder ish action
    public int hide() {
        if (this.alive && this.actionAvailable && hideTracker == HIDE_COOLDOWN) {
            hideTracker = 0;
            actionAvailable = false;
            this.hasCoverBuff = true;
            return 0;
        }
        else return 1;
    }
}
