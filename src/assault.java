import astar.AStar;
import astar.Node;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class assault extends entity{

    public static int CHARGE_COOLDOWN = 3;
    public static int CHARGE_DAMAGE = 3;
    private int chargeTracker;

    public boolean canCharge() {
        return (this.chargeTracker == CHARGE_COOLDOWN);
    }

    public assault(String name, int healthCap, int moveSpeed, int baseDamage, int sightRange, int currentX, int currentY) {
        super(name, healthCap, moveSpeed, baseDamage, sightRange, currentX, currentY);
        this.chargeTracker = CHARGE_COOLDOWN;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (chargeTracker < CHARGE_COOLDOWN) chargeTracker++;
    }

    public int charge(map gameMap, entity target) {
        if (this.alive && target.isAlive() && this.actionAvailable && this.canCharge()) {
            Node start = new Node(currentX, currentY);
            Node goal = new Node(target.getCurrentX(), target.getCurrentY());
            AStar astar = new AStar(map.MAP_WIDTH, map.MAP_HEIGHT, start, goal, 10, 10);
            //keep an eye on this one
            int[][] units = ArrayUtils.removeElement(gameLogic.getTeamLocations(), new int[] {target.getCurrentX(), target.getCurrentY()});
            int[][] blocks = ArrayUtils.addAll(gameMap.getMapBlockers(), units);
            astar.setBlocks(blocks);
            List<Node> path = astar.findPath();
            //path size decremented for this check as it includes targeted enemy
            if (!path.isEmpty() && (path.size() - 1) <= moveSpeed + 1) {
                if (path.size() > 1) {
                    Node moveTo = path.get(path.size() - 2);
                    gameMap.getCell(currentX, currentY).setOccupant(null);
                    currentX = moveTo.getRow();
                    currentY = moveTo.getCol();
                    gameMap.getCell(currentX, currentY).setOccupant(this);
                }
                target.changeHealth(-CHARGE_DAMAGE);
                chargeTracker = 0;
                actionAvailable = false;
                return 0;
            }
            else return 2;
        }
        else return 1;
    }

    //TODO: draft second ability
}
