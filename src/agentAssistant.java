import astar.AStar;
import astar.Node;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO: switch this over to multithreading
public class agentAssistant implements Runnable {

    public static final int VOICE_CODE_ROBOTIC = 1;
    public static final int VOICE_CODE_CAUTIOUS = 2;
    public static final int VOICE_CODE_RECKLESS = 3;

    private final entity self;
    private map gameMap;
    private entity[] alliedTeam;
    private entity[] enemyTeam;
    private ArrayList<agentCommand> moves;
    private int voiceCode;

    public agentAssistant(entity self) {
        this.self = self;
    }

    public void setState(map stateMap, entity[] alliedTeam, entity[] enemyTeam) {
        this.gameMap = stateMap;
        this.alliedTeam = alliedTeam;
        this.enemyTeam = enemyTeam;
    }

    public void run() {
        moves = new ArrayList<>();
        //Identify legal movement actions
        if (self.isMoveAvailable() || self.isActionAvailable()) {
            for (int i = -self.moveSpeed; i <= self.moveSpeed; i++) {
                if (self.currentX + i >= 0 && self.currentX + i < map.MAP_WIDTH) {
                    for (int j = -self.moveSpeed; j <= self.moveSpeed; j++) {
                        if (self.currentY + j >= 0 && self.currentY + j < map.MAP_HEIGHT) {
                            if (gameMap.getCell(self.currentX + i, self.currentY + j).isEmpty()) {
                                List<Node> path = self.useAStar(gameMap, self.currentX + i, self.currentY + j, true);
                                if(!path.isEmpty() && path.size() <= self.getMoveSpeed() + 1) {
                                    if (self.isMoveAvailable()) {
                                        agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_MOVE, self.currentX + i, self.currentY + j, null, this);
                                        newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                                        moves.add(newCommand);
                                    }
                                    if (self.isActionAvailable()) {
                                        agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_SPRINT, self.currentX + i, self.currentY + j, null, this);
                                        newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                                        moves.add(newCommand);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (self.isActionAvailable()) {
            //Identify legal attacks
            for (entity target : enemyTeam) {
                List<Node> line = self.useAStarLOS(gameMap, self.getCurrentX(), self.getCurrentY(), target.getCurrentX(), target.getCurrentY(), 1);
                if (!line.isEmpty() && line.size() <= self.getSightRange() + 1) {
                    agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_ATTACK, null, null, target, this);
                    newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                    moves.add(newCommand);
                }
            }

            //Identify special actions based on unit type:
            if (self instanceof medic) {
                for (entity target: alliedTeam) {
                    //check adjacency for healing
                    if (((medic) self).canHeal() && Math.abs(self.getCurrentX() - target.getCurrentX()) <= 1 && Math.abs(self.getCurrentY() - target.getCurrentY()) <= 1) {
                        agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSONE, null, null, target, this);
                        newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                        moves.add(newCommand);
                    }
                    List<Node> line = self.useAStarLOS(gameMap, self.getCurrentX(), self.getCurrentY(), target.getCurrentX(), target.getCurrentY(), 0);
                    if (!line.isEmpty() && line.size() <= self.getSightRange() + 1) {
                        agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSTWO, null, null, target, this);
                        newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                        moves.add(newCommand);
                    }
                }
            }
            else if (self instanceof demolitionist) {
                for (int i = -self.getSightRange(); i <= self.getSightRange(); i++) {
                    if (self.currentX + i >= 0 && self.currentX + i < map.MAP_WIDTH) {
                        for (int j = -self.getSightRange(); j <= self.getSightRange(); j++) {
                            if (self.currentY + j >= 0 && self.currentY + j < map.MAP_HEIGHT) {
                                //evaluate for destroying cover
                                if (((demolitionist) self).canDemolish() && gameMap.getCell(self.currentX + i, self.currentY + j).getTileType() != map.EMPTY_TILE_CODE && gameMap.getCell(self.currentX + i, self.currentY + j).getTileType() != map.EXTRA_HARD_TILE_CODE) {
                                    List<Node> line = self.useAStarLOS(gameMap, self.getCurrentX(), self.getCurrentY(), self.getCurrentX() + i, self.getCurrentY() + j, 0);
                                    if (!line.isEmpty() && line.size() <= self.getSightRange() + 1) {
                                        agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSONE, self.getCurrentX() + i, self.getCurrentY() + j, null, this);
                                        newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                                        moves.add(newCommand);
                                    }
                                }
                                if (((demolitionist) self).canGrenade() && gameMap.getCell(self.currentX + i, self.currentY + j).getTileType() == map.EMPTY_TILE_CODE) {
                                    List<Node> line = self.useAStarLOS(gameMap, self.currentX, self.currentY, self.currentX + i, self.currentY + j, 0);
                                    if (!line.isEmpty() && line.size() <= self.getSightRange() + 1) {
                                        for (entity enemy : enemyTeam) {
                                            List<Node> grenadeLine = self.useAStarLOS(gameMap, self.currentX + i, self.currentY + j, enemy.getCurrentX(), enemy.getCurrentY(), 0);
                                            if (!grenadeLine.isEmpty() && grenadeLine.size() <= demolitionist.GRENADE_RADIUS + 1) {
                                                agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSTWO, self.getCurrentX() + i, self.getCurrentY() + j, null, this);
                                                newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                                                moves.add(newCommand);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (self instanceof sniper) {

                if (((sniper) self).canHide()) {
                    agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSTWO, null, null, null, this);
                    newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                    moves.add(newCommand);
                }

                if (((sniper) self).canSnipe()) {
                    for (entity target : enemyTeam) {
                        List<Node> line = self.useAStarLOS(gameMap, self.getCurrentX(), self.getCurrentY(), target.getCurrentX(), target.getCurrentY(), 1);
                        if (!line.isEmpty() && line.size() <= sniper.SNIPE_RANGE + 1) {
                            agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSONE, null, null, target, this);
                            newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                            moves.add(newCommand);
                        }
                    }
                }
            }
            else if (self instanceof assault) {
                if (((assault) self).canCharge()) {
                    for (entity target : enemyTeam) {
                        Node start = new Node(self.getCurrentX(), self.getCurrentY());
                        Node goal = new Node(target.getCurrentX(), target.getCurrentY());
                        AStar astar = new AStar(map.MAP_WIDTH, map.MAP_HEIGHT, start, goal, 10, 10);
                        //keep an eye on this one
                        int[][] units = ArrayUtils.removeElement(gameLogic.getTeamLocations(), new int[]{target.getCurrentX(), target.getCurrentY()});
                        int[][] blocks = ArrayUtils.addAll(gameMap.getMapBlockers(), units);
                        astar.setBlocks(blocks);
                        List<Node> path = astar.findPath();
                        if(!path.isEmpty() && (path.size() - 1) <= self.getMoveSpeed() + 1) {
                            agentCommand newCommand = new agentCommand(self, gameLogic.ACTION_CODE_CLASSONE, null, null, target, this);
                            newCommand.evaluate(gameMap, alliedTeam, enemyTeam);
                            moves.add(newCommand);
                        }
                    }
                }
            }
        }



        Collections.sort(moves);
    }

    public agentCommand getTopMove() {
        if (!moves.isEmpty()) return moves.get(moves.size() - 1);
        else return null;
    }
    public String getName() {
        return self.getName();
    }

    public entity getSelf() {
        return self;
    }

    public map getGameMap() {
        return gameMap;
    }

    public entity[] getAlliedTeam() {
        return alliedTeam;
    }

    public entity[] getEnemyTeam() {
        return enemyTeam;
    }

    public void setVoiceCode(int voiceCode) {
        this.voiceCode = voiceCode;
    }

    public int getVoiceCode() {
        return voiceCode;
    }
}
