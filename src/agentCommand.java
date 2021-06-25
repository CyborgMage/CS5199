import astar.Node;
import org.apache.commons.collections4.iterators.ArrayListIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class agentCommand implements Comparable<agentCommand>{

    public static final int ATTACK_INHERENT = 30;
    public static final int MOVE_INHERENT = 10;
    public static final int SPRINT_INHERENT = 5;
    public static final int HEAL_INHERENT = 20;
    public static final int SMOKE_INHERENT = 15;
    public static final int DEMOLISH_INHERENT = 15;
    public static final int GRENADE_INHERENT = 20;
    public static final int SNIPE_INHERENT = 40;
    public static final int HIDE_INHERENT = 15;
    public static final int CHARGE_INHERENT = 20;

    public static final int AGGRESSIVE_CODE = 1;
    public static final int DEFENSIVE_CODE = 2;

    private agentAssistant parent;
    private entity invokingEntity;
    private Integer actionType;
    private Integer targetX;
    private Integer targetY;
    private entity targetEntity;
    private int value = 0;
    private ArrayList<String> reasons;
    private int personalityCode;

    public entity getInvokingEntity() {
        return invokingEntity;
    }

    public Integer getActionType() {
        return actionType;
    }

    public Integer getTargetX() {
        return targetX;
    }

    public Integer getTargetY() {
        return targetY;
    }

    public entity getTargetEntity() {
        return targetEntity;
    }

    public int getValue() {
        return value;
    }

    public void setInvokingEntity(entity invokingEntity) {
        this.invokingEntity = invokingEntity;
    }

    public void setActionType(Integer actionType) {
        this.actionType = actionType;
    }

    public void setTargetX(Integer targetX) {
        this.targetX = targetX;
    }

    public void setTargetY(Integer targetY) {
        this.targetY = targetY;
    }

    public void setTargetEntity(entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public ArrayList<String> getReasons() {
        return reasons;
    }

    public agentAssistant getParent() {
        return parent;
    }

    public agentCommand() {
    }

    public agentCommand(entity invokingEntity, Integer actionType, Integer targetX, Integer targetY, entity targetEntity, agentAssistant parent) {
        this.invokingEntity = invokingEntity;
        this.actionType = actionType;
        switch (actionType) {
            case gameLogic.ACTION_CODE_MOVE -> value = MOVE_INHERENT;
            case gameLogic.ACTION_CODE_SPRINT -> value = SPRINT_INHERENT;
            case gameLogic.ACTION_CODE_ATTACK -> value = ATTACK_INHERENT;
            case gameLogic.ACTION_CODE_CLASSONE -> {
                if (invokingEntity instanceof medic) value = HEAL_INHERENT;
                else if (invokingEntity instanceof demolitionist) value = DEMOLISH_INHERENT;
                else if (invokingEntity instanceof sniper) value = SNIPE_INHERENT;
                else value = CHARGE_INHERENT;
            }
            case gameLogic.ACTION_CODE_CLASSTWO -> {
                if (invokingEntity instanceof medic) value = SMOKE_INHERENT;
                else if (invokingEntity instanceof demolitionist) value = GRENADE_INHERENT;
                else if (invokingEntity instanceof sniper) value = HIDE_INHERENT;
                else value = CHARGE_INHERENT;
            }
            default -> value = 0;
        }
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetEntity = targetEntity;
        this.reasons = new ArrayList<>();
        this.parent = parent;
        this.personalityCode = parent.getVoiceCode();
    }

    public agentCommand(agentCommand oldCommand, agentAssistant parent) {
        this.invokingEntity = oldCommand.getInvokingEntity();
        this.actionType = oldCommand.getActionType();
        value = 0;
        this.targetX = oldCommand.getTargetX();
        this.targetY = oldCommand.getTargetY();
        this.targetEntity = oldCommand.getTargetEntity();
        this.reasons = new ArrayList<>();
        this.parent = parent;
        this.personalityCode = parent.getVoiceCode();
    }

    public void evaluate(map gameMap, entity[] alliedTeam, entity[] enemyTeam) {
        switch (actionType) {
            case gameLogic.ACTION_CODE_MOVE, gameLogic.ACTION_CODE_SPRINT -> evaluateMove(gameMap, enemyTeam);
            case gameLogic.ACTION_CODE_ATTACK -> evaluateAttack(gameMap);
            case gameLogic.ACTION_CODE_CLASSONE -> {
                if (invokingEntity instanceof medic) evaluateHeal();
                else if (invokingEntity instanceof demolitionist) evaluateDemolish(gameMap, alliedTeam, enemyTeam);
                else if (invokingEntity instanceof sniper) evaluateSnipe(gameMap);
                else evaluateCharge();
            }
            case gameLogic.ACTION_CODE_CLASSTWO -> {
                if (invokingEntity instanceof medic) evaluateCoverBuff(gameMap, enemyTeam);
                else if (invokingEntity instanceof demolitionist) evaluateGrenade(gameMap, alliedTeam, enemyTeam);
                else if (invokingEntity instanceof sniper) evaluateHide(gameMap, enemyTeam);
                else evaluateCharge();
            }
            default -> value += ThreadLocalRandom.current().nextInt(10);
        }

    }


    private void evaluateMove(map gameMap, entity[] enemyTeam) {
        //Add value for each adjacent piece of cover, harder cover is more valuable
        for (int i = -1; i <= 1; i++) {
            if (targetX + i >= 0 && targetX + 1 < map.MAP_WIDTH) {
                for (int j = -1; j <= 1; j++) {
                    if (targetY + j >= 0 && targetY + j < map.MAP_HEIGHT) {
                        value += 2 * gameMap.getCell(targetX + i, targetY + j).getTileType();
                        if (!reasons.contains("hasCover")) reasons.add("hasCover");
                    }
                }
            }
        }
        //TODO: in general, find a way to increase value of moves that enable other potentially valuable actions to be taken.
        for (entity enemy: enemyTeam) {
            List<Node> line = invokingEntity.useAStarLOS(gameMap, invokingEntity.getCurrentX(), invokingEntity.getCurrentY(), enemy.getCurrentX(), enemy.getCurrentY(), -1);
            if (line.size() > invokingEntity.getSightRange()) {
                int ref = line.size();
                line = invokingEntity.useAStarLOS(gameMap, targetX, targetY, enemy.getCurrentX(), enemy.getCurrentY(), -1);
                if (line.size() < ref) {
                    incrementValue(30, AGGRESSIVE_CODE);
                    if (!reasons.contains("approach")) reasons.add("approach");
                }
            }
        }
    }

    private void evaluateAttack(map gameMap) {
        //Extract hit chance based on algorithm in entity, multiply standard addition value by it.
        List<Node> line = invokingEntity.useAStarLOS(gameMap, invokingEntity.getCurrentX(), invokingEntity.getCurrentY(), targetEntity.getCurrentX(), targetEntity.getCurrentY(), 1);
        int chance = invokingEntity.getHitChance(line.size());
        if (chance >= 75) reasons.add("goodOdds");
        else if (chance >= 50) reasons.add("fairOdds");
        value = value * (chance / 100);
        //Also add large additional value if target health <= attack damage
        if (targetEntity.getHealth() <= invokingEntity.getBaseDamage()) {
            incrementValue(50, AGGRESSIVE_CODE);
            reasons.add("lethal");
        }
    }

    private void evaluateHeal() {
        //Massively increase value if target unit is within HP "danger zone"
        if (targetEntity.getHealth() <= invokingEntity.getBaseDamage()) {
            incrementValue(100, DEFENSIVE_CODE);
            reasons.add("lethalProtection");
        }
    }

    private void evaluateCoverBuff(map gameMap, entity[] enemyTeam) {
        //increment value for every enemy with LoS to target entity
        for (entity enemy : enemyTeam) {
            List<Node> line = enemy.useAStarLOS(gameMap, enemy.getCurrentX(), enemy.getCurrentY(), targetEntity.getCurrentX(), targetEntity.getCurrentY(), 1);
            if (!line.isEmpty() && line.size() <= enemy.getSightRange() + 1) {
                incrementValue(10, DEFENSIVE_CODE);
                if (!reasons.contains("lethal")) reasons.add("lethal");
                else if (!reasons.contains("lethalMultiple")) reasons.add("lethalMultiple");
            }
        }
        //similar to heal, increase value significantly if own HP is in danger zone
        if (targetEntity.getHealth() <= invokingEntity.getBaseDamage()) {
            incrementValue(30, DEFENSIVE_CODE);
            reasons.add("lethalProtection");
        }
    }

    private void evaluateDemolish(map gameMap, entity[] alliedTeam, entity[] enemyTeam) {
        //Increment value if target cover is adjacent to enemy unit, decrement if adjacent to allied unit.
        for (int i = -1; i <= 1; i++) {
            if (targetX + i >= 0 && targetX + i < map.MAP_WIDTH) {
                for (int j = -1; j <= 1; j++) {
                    if (targetY + j >= 0 && targetY + j < map.MAP_HEIGHT) {
                        entity occupant = gameMap.getCell(targetX + i, targetY + j).getOccupant();
                        if (occupant != null) {
                            if (Arrays.asList(alliedTeam).contains(occupant)) {
                                value -= 30;
                            }
                            else if (Arrays.asList(enemyTeam).contains(occupant)) {
                                value += 20;
                                if (!reasons.contains("affectsEnemy")) reasons.add("affectsEnemy");
                                else if (!reasons.contains("affectsEnemyMultiple")) reasons.add("affectsEnemyMultiple");
                            }
                        }
                    }
                }
            }
        }
        //TODO: Find some way to additionally increment value if demolish would grant self/ally/allies LoS to enemy unit
    }

    private void evaluateGrenade(map gameMap, entity[] alliedTeam, entity[] enemyTeam) {
        //Increment value for every enemy unit in LoS of grenade position.
        for (entity enemy : enemyTeam) {
            List<Node> line = enemy.useAStarLOS(gameMap, targetX, targetY, enemy.getCurrentX(), enemy.getCurrentY(), 0);
            if (!line.isEmpty() && line.size() <= demolitionist.GRENADE_RADIUS + 1) {
                if (enemy.getHealth() > demolitionist.GRENADE_DAMAGE) {
                    incrementValue(20, AGGRESSIVE_CODE);
                    if (!reasons.contains("hits")) reasons.add("hits");
                    else if (!reasons.contains("hitsMultiple")) reasons.add("hitsMultiple");
                }
                else {
                    incrementValue(40, AGGRESSIVE_CODE);
                    if (!reasons.contains("threatened")) reasons.add("threatened");
                    else if (!reasons.contains("threatenedMultiple")) reasons.add("threatenedMultiple");
                }
            }
        }
        //Decrement value for every allied unit in LoS of grenade position.
        for (entity ally : alliedTeam) {
            List<Node> line = ally.useAStarLOS(gameMap, targetX, targetY, ally.getCurrentX(), ally.getCurrentY(), 0);
            if (!line.isEmpty() && line.size() <= demolitionist.GRENADE_RADIUS + 1) {
                if (ally.getHealth() > demolitionist.GRENADE_DAMAGE) {
                    value -= 20;
                }
                else value -= 100;
            }
        }
        //Values may need to be adjusted on account of multiple potential sources of added value.
    }

    private void evaluateSnipe(map gameMap) {
        //Extract hit chance based on algorithm in entity, multiply standard addition value by it.
        List<Node> line = invokingEntity.useAStarLOS(gameMap, invokingEntity.getCurrentX(), invokingEntity.getCurrentY(), targetEntity.getCurrentX(), targetEntity.getCurrentY(), 1);
        int chance = invokingEntity.getHitChance(line.size());
        if (chance >= 75) reasons.add("goodOdds");
        else if (chance >= 50) reasons.add("fairOdds");
        value = value * (chance / 100);
        //Also add large additional value if target health <= attack damage
        if (targetEntity.getHealth() <= sniper.SNIPE_DAMAGE) {
            incrementValue(30, AGGRESSIVE_CODE);
            reasons.add("lethal");
        }
    }

    private void evaluateHide(map gameMap, entity[] enemyTeam) {
        for (entity enemy : enemyTeam) {
            List<Node> line = enemy.useAStarLOS(gameMap, enemy.getCurrentX(), enemy.getCurrentY(), invokingEntity.getCurrentX(), invokingEntity.getCurrentY(), 1);
            if (!line.isEmpty() && line.size() <= enemy.getSightRange() + 1) {
                incrementValue(10, DEFENSIVE_CODE);
                if (!reasons.contains("lethal")) reasons.add("lethal");
                else if (!reasons.contains("lethalMultiple")) reasons.add("lethalMultiple");
            }
        }
        if (invokingEntity.getHealth() <= invokingEntity.getBaseDamage()) {
            incrementValue(30, DEFENSIVE_CODE);
            reasons.add("lethalProtection");
        }
    }

    private void evaluateCharge() {
        //TODO: combine movement and lethal attack value addition
        if (targetEntity.getHealth() <= assault.CHARGE_DAMAGE) {
            incrementValue(50, AGGRESSIVE_CODE);
            reasons.add("lethal");
        }
        //TODO: reliably extract move destination and evaluate based on that
    }

    public void incrementValue(int value, int actionCode) {
        if (actionCode == AGGRESSIVE_CODE) {
            if (personalityCode == agentAssistant.VOICE_CODE_CAUTIOUS) value = value / 2;
            else if (personalityCode == agentAssistant.VOICE_CODE_RECKLESS) value = value * 2;
        }
        else if (actionCode == DEFENSIVE_CODE) {
            if (personalityCode == agentAssistant.VOICE_CODE_CAUTIOUS) value = value * 2;
            else if (personalityCode == agentAssistant.VOICE_CODE_RECKLESS) value = value / 2;
        }
        this.value += value;
    }

    @Override
    public int compareTo(agentCommand o) {
        return Integer.compare(this.value, o.getValue());
    }
}
