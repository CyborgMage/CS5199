import astar.Node;

import java.util.List;

public class medic extends entity {

    public static final int HEAL_COOLDOWN = 4;
    public static final int HEAL_VAL = 3;
    private int healTracker;

    public boolean canHeal() {
        return (this.healTracker == HEAL_COOLDOWN);
    }

    public medic(String name, int healthCap, int moveSpeed, int baseDamage, int sightRange, int currentX, int currentY) {
        super(name, healthCap, moveSpeed, baseDamage, sightRange, currentX, currentY);
        healTracker = HEAL_COOLDOWN;
    }

    @Override
    public void refresh() {
        super.refresh();
        if (healTracker < HEAL_COOLDOWN) healTracker++;
    }

    public int heal(entity target) {
        if (this.alive && target.isAlive() && this.actionAvailable && this.canHeal() && Math.abs(currentX - target.getCurrentX()) <= 1 && Math.abs(currentY - target.getCurrentY()) <= 1) {
            target.changeHealth(HEAL_VAL);
            healTracker = 0;
            actionAvailable = false;
            return 0;
        }
        else return 1;
    }

    public int applyCoverBuff(map gameMap, entity target) {
        if (this.alive && target.isAlive() && this.actionAvailable) {
            List<Node> line = useAStarLOS(gameMap, this.currentX, this.currentY, target.getCurrentX(), target.getCurrentY(), 0);
            if (!line.isEmpty() && line.size() <= sightRange + 1) {
                target.setHasCoverBuff(true);
                actionAvailable = false;
                return 0;
            }
            else return 2;
        }
        else return 1;
    }
}
