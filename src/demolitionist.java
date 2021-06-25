import astar.Node;

import java.util.List;

public class demolitionist extends entity {

    public static final int BLOW_COVER_COOLDOWN = 3;
    private int blowCoverTracker;
    public static final int GRENADE_COOLDOWN = 4;
    public static final int GRENADE_DAMAGE = 2;
    public static final int GRENADE_RADIUS = 2;
    private int grenadeTracker;

    public boolean canDemolish() {
        return (this.blowCoverTracker == BLOW_COVER_COOLDOWN);
    }

    public boolean canGrenade() {
        return (this.grenadeTracker == GRENADE_COOLDOWN);
    }

    public demolitionist(String name, int healthCap, int moveSpeed, int baseDamage, int sightRange, int currentX, int currentY) {
        super(name, healthCap, moveSpeed, baseDamage, sightRange, currentX, currentY);
        blowCoverTracker = BLOW_COVER_COOLDOWN;
        grenadeTracker = GRENADE_COOLDOWN;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (blowCoverTracker < BLOW_COVER_COOLDOWN) blowCoverTracker++;
        if (grenadeTracker < GRENADE_COOLDOWN) grenadeTracker++;
    }

    public int blowCover(map gameMap, int targetX, int targetY) {
        if (this.alive && gameMap.getCell(targetX, targetY).getTileType() != map.EMPTY_TILE_CODE && gameMap.getCell(targetX, targetY).getTileType() != map.EXTRA_HARD_TILE_CODE && this.actionAvailable && this.canDemolish()) {
            List<Node> line = useAStarLOS(gameMap, currentX, currentY, targetX, targetY, 0);
            if (!line.isEmpty() && line.size() <= sightRange + 1) {
                gameMap.removeBlocker(targetX, targetY);
                blowCoverTracker = 0;
                actionAvailable = false;
                return 0;
            }
            else return 2;
        }
        else return 1;
    }

    //method is to be passed a list of living entities, regardless of team
    public int grenade(map gameMap, int targetX, int targetY, entity[] validTargets) {
        if (this.alive && this.actionAvailable && this.grenadeTracker == GRENADE_COOLDOWN && gameMap.getCell(targetX, targetY).getTileType() == map.EMPTY_TILE_CODE) {
            List<Node> line = useAStarLOS(gameMap, currentX, currentY, targetX, targetY, 0);
            if (!line.isEmpty() && line.size() <= sightRange + 1) {
                grenadeTracker = 0;
                actionAvailable = false;
                for (entity targetEntity : validTargets) {
                    List<Node> grenadeLine = useAStarLOS(gameMap, targetX, targetY, targetEntity.getCurrentX(), targetEntity.getCurrentY(), 0);
                    if (!grenadeLine.isEmpty() && grenadeLine.size() <= GRENADE_RADIUS + 1) targetEntity.changeHealth(-GRENADE_DAMAGE);
                }
                return 0;
            }
            else return 2;
        }
        else return 1;
    }
}
