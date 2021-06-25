import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class gameLogic {

    //TODO:changed for testing
    public static final int TEAM_SIZE = 4;

    static final int PLACEDHOLDER_HP = 5;
    static final int PLACEDHOLDER_SPEED = 4;
    static final int PLACEHOLDER_DMG = 2;
    static final int PLACEHOLDER_RANGE = 5;

    static final int GEN_MEDIC = 1;
    static final int GEN_DEMO = 2;
    static final int GEN_SNIPER = 0;
    static final int GEN_ASSAULT = 3;

    public static final int ACTION_CODE_MOVE = 0;
    public static final int ACTION_CODE_SPRINT = 1;
    public static final int ACTION_CODE_ATTACK = 2;
    public static final int ACTION_CODE_CLASSONE = 3;
    public static final int ACTION_CODE_CLASSTWO = 4;

    private static map gameMap;
    private static entity[] teamOne;
    private static entity[] teamTwo;
    private static ArrayList<agentAssistant> agentsOne;
    private static ArrayList<agentAssistant> agentsTwo;
    private static int[][] teamLocations;
    private static int gameState = 0;
    private static boolean teamOneActive = true;

    public static int[][] getTeamLocations() {
        return teamLocations;
    }

    static final boolean DEMONSTRATION = true;
    static final int[] DEMO_BLUE_SPAWN = new int[]{5, 0};
    static final int[] DEMO_RED_SPAWN = new int[]{7, 6};

    public static void main(String[] args) {
        agentsOne = new ArrayList<>();
        agentsTwo = new ArrayList<>();
        if (!DEMONSTRATION) {
            gameMap = new map(DEMONSTRATION);
            teamOne = new entity[TEAM_SIZE];
            teamTwo = new entity[TEAM_SIZE];
            teamLocations = new int[TEAM_SIZE * 2][2];
            for (int i = 0; i <= GEN_ASSAULT; i++) {
                //TODO: potentially review for any class stat variations, variety on spawn location
                int spawnX = (map.MAP_WIDTH / 2) - 1;
                int spawnY = 0;
                int j = 1;
                while (!gameMap.getCell(spawnX, spawnY).isEmpty()) {
                    if (spawnX + j < map.MAP_WIDTH && gameMap.getCell(spawnX + j, spawnY).isEmpty()) spawnX += j;
                    else if (spawnX - j >= 0 && gameMap.getCell(spawnX - j, spawnY).isEmpty()) spawnX -= j;
                    else j++;
                }
                entity unitOne;
                String name = "b" + (i + 1);
                switch (i) {
                    case GEN_MEDIC -> unitOne = new medic(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    case GEN_DEMO -> unitOne = new demolitionist(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    case GEN_SNIPER -> unitOne = new sniper(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    case GEN_ASSAULT -> unitOne = new assault(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    default -> unitOne = new entity(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);

                }
                teamOne[i] = unitOne;
                gameMap.getCell(spawnX, spawnY).setOccupant(unitOne);
                teamLocations[i][0] = spawnX;
                teamLocations[i][1] = spawnY;
                agentAssistant agent = new agentAssistant(unitOne);
                if (i+1 <= agentAssistant.VOICE_CODE_RECKLESS) agent.setVoiceCode(i+1);
                else agent.setVoiceCode(ThreadLocalRandom.current().nextInt(1, agentAssistant.VOICE_CODE_RECKLESS + 1));
                agentsOne.add(agent);
                int spawnY2 = map.MAP_HEIGHT - (1 + spawnY);
                entity unitTwo;
                name = "r" + (i + 1);
                switch (i) {
                    case GEN_MEDIC -> unitTwo = new medic(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    case GEN_DEMO -> unitTwo = new demolitionist(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    case GEN_SNIPER -> unitTwo = new sniper(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    case GEN_ASSAULT -> unitTwo = new assault(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);
                    default -> unitTwo = new entity(name, PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, spawnX, spawnY);

                }
                teamTwo[i] = unitTwo;
                teamLocations[i + TEAM_SIZE][0] = spawnX;
                teamLocations[i + TEAM_SIZE][1] = spawnY2;
                agent = new agentAssistant(unitTwo);
                if (i+1 <= agentAssistant.VOICE_CODE_RECKLESS) agent.setVoiceCode(i+1);
                else agent.setVoiceCode(ThreadLocalRandom.current().nextInt(1, agentAssistant.VOICE_CODE_RECKLESS + 1));
                agentsTwo.add(agent);
                gameMap.getCell(spawnX, spawnY2).setOccupant(unitTwo);
            }
        }
        else {
            gameMap = new map(DEMONSTRATION);
            teamOne = new entity[1];
            teamTwo = new entity[1];
            teamLocations = new int[2][2];
            teamOne[0] = new assault("b1", PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, DEMO_BLUE_SPAWN[0], DEMO_BLUE_SPAWN[1]);
            agentAssistant agent = new agentAssistant(teamOne[0]);
            agent.setVoiceCode(ThreadLocalRandom.current().nextInt(1, agentAssistant.VOICE_CODE_RECKLESS + 1));
            agentsOne.add(agent);
            gameMap.getCell(DEMO_BLUE_SPAWN[0], DEMO_BLUE_SPAWN[1]).setOccupant(teamOne[0]);
            teamLocations[0][0] = DEMO_BLUE_SPAWN[0];
            teamLocations[0][1] = DEMO_BLUE_SPAWN[1];
            teamTwo[0] = new sniper("r1", PLACEDHOLDER_HP, PLACEDHOLDER_SPEED, PLACEHOLDER_DMG, PLACEHOLDER_RANGE, DEMO_RED_SPAWN[0], DEMO_RED_SPAWN[1]);
            agent = new agentAssistant(teamTwo[0]);
            agent.setVoiceCode(ThreadLocalRandom.current().nextInt(1, agentAssistant.VOICE_CODE_RECKLESS + 1));
            agentsTwo.add(agent);
            gameMap.getCell(DEMO_RED_SPAWN[0], DEMO_RED_SPAWN[1]).setOccupant(teamOne[0]);
            teamLocations[1][0] = DEMO_RED_SPAWN[0];
            teamLocations[1][1] = DEMO_RED_SPAWN[1];
        }

        while (gameState == 0) {
            /*TODO: Main game loop
             * For whole of one team: allow agents to discuss and plan, sanity check and carry out moves. Once full team submits
             * finished acting signal of some kind, switch to other team and repeat.
             * On action execution check for changes in map content or unit status. End game if all units on one team die.*/
            if (gameEnded() != 0) gameState = 1;
            else {
                entity[] currentTeamList;
                entity[] enemyTeamList;
                ArrayList<agentAssistant> currentAgents;
                if (teamOneActive) {
                    currentTeamList = getLiveEntites()[0];
                    currentAgents = agentsOne;
                    enemyTeamList = getLiveEntites()[1];
                }
                else {
                    currentTeamList = getLiveEntites()[1];
                    currentAgents = agentsTwo;
                    enemyTeamList = getLiveEntites()[0];
                }
                for (entity x : currentTeamList) {
                    x.refresh();
                }
                boolean teamActive = false;
                do {
                    tempRender(gameMap);
                    //Attempt to run the agent until it selects an output command
                    ArrayList<agentCommand> commands = new ArrayList<>();
                    ArrayList<Thread> threads = new ArrayList<>();
                    for (agentAssistant x: currentAgents) {
                        x.setState(gameMap, currentTeamList, enemyTeamList);
                        Thread t = new Thread(x);
                        threads.add(t);
                        t.start();
                    }
                    for (int i = 0; i < currentAgents.size(); i++) {
                        //TODO: proper exception handling
                        try {
                            threads.get(i).join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        agentCommand topMove = currentAgents.get(i).getTopMove();
                        if (topMove != null) commands.add(topMove);
                    }
                    agentCommand currentCommand = holdDiscussion(commands, currentAgents, teamOneActive);
                    //Signal format: invoking entity, action type, target X, target Y, target entity. Not all fields may be used.
                    entity invokingEntity = currentCommand.getInvokingEntity();
                    Integer actionType = currentCommand.getActionType();
                    Integer targetX = currentCommand.getTargetX();
                    Integer targetY = currentCommand.getTargetY();
                    entity targetEntity = currentCommand.getTargetEntity();
                    int returnCode = -1;
                    switch (actionType) {
                        case ACTION_CODE_MOVE:
                            returnCode = invokingEntity.move(gameMap, targetX, targetY);
                            break;
                        case ACTION_CODE_SPRINT:
                            returnCode = invokingEntity.sprint(gameMap, targetX, targetY);
                            break;
                        case ACTION_CODE_ATTACK:
                            returnCode = invokingEntity.attack(gameMap, targetEntity);
                            break;
                        case ACTION_CODE_CLASSONE:
                            if (invokingEntity instanceof medic) returnCode = ((medic) invokingEntity).heal(targetEntity);
                            else if (invokingEntity instanceof demolitionist) returnCode = ((demolitionist) invokingEntity).blowCover(gameMap, targetX, targetY);
                            else if (invokingEntity instanceof sniper) returnCode = ((sniper) invokingEntity).snipe(gameMap, targetEntity);
                            else if (invokingEntity instanceof assault) returnCode = ((assault) invokingEntity).charge(gameMap, targetEntity);
                            break;
                        case ACTION_CODE_CLASSTWO:
                            if (invokingEntity instanceof medic) returnCode = ((medic) invokingEntity).applyCoverBuff(gameMap, targetEntity);
                            else if (invokingEntity instanceof demolitionist) returnCode = ((demolitionist) invokingEntity).grenade(gameMap, targetX, targetY, ArrayUtils.addAll(getLiveEntites()[0], getLiveEntites()[1]));
                            else if (invokingEntity instanceof sniper) returnCode = ((sniper) invokingEntity).hide();
                                //TODO: change to second ability
                            else if (invokingEntity instanceof assault) returnCode = ((assault) invokingEntity).charge(gameMap, targetEntity);
                            break;
                            }
                            if (returnCode != 0 && returnCode != 3) {
                                //TODO: keep list of sorted moves, move to next one on errors like this
                                System.out.println("Error on instruction type " + actionType + ", return code " + returnCode);
                                return;
                            }
                    if (gameEnded() != 0) {
                        gameState = 1;
                        break;
                    }
                    else {
                        for (entity member : enemyTeamList) {
                            if (member.isJustDied()) {
                                gameMap.getCell(member.getCurrentX(), member.getCurrentY()).setOccupant(null);
                                if (teamOneActive) {
                                    for (agentAssistant agent : agentsTwo) {
                                        if (agent.getSelf().equals(member)) {
                                            agentsTwo.remove(agent);
                                            break;
                                        }
                                    }
                                }
                                else {
                                    for (agentAssistant agent : agentsOne) {
                                        if (agent.getSelf().equals(member)) {
                                            agentsOne.remove(agent);
                                            break;
                                        }
                                    }
                                }
                                member.setJustDied(false);
                            }
                        }

                        for (int i = 0; i < currentTeamList.length; i++) {
                            entity member = currentTeamList[i];
                            if (member.isAlive() && (member.isActionAvailable() && member.isMoveAvailable())) {
                                teamActive = true;
                                break;
                            } else if (i+1 == currentTeamList.length) {
                                teamActive = false;
                                teamOneActive = !teamOneActive;
                            }
                        }
                    }
                } while (teamActive);
            }
        }
        tempRender(gameMap);
        System.out.println("Game over");
        if (gameEnded() == 1) System.out.println("Team two wins.");
        else if (gameEnded() == 2) System.out.println("Team one wins.");
    }

    private static int gameEnded() {
        boolean teamOneWipe = true;
        for (entity member : teamOne) {
            if (member.isAlive()) {
                teamOneWipe = false;
                break;
            }
        }
        if (teamOneWipe) return 1;
        boolean teamTwoWipe = true;
        for (entity member : teamTwo) {
            if (member.isAlive()) {
                teamTwoWipe = false;
                break;
            }
        }
        if (teamTwoWipe) return 2;
        return 0;
    }

    public static void tempRender(map gameMap) {
        int limitX;
        int limitY;
        if (DEMONSTRATION) {
            limitX = 10;
            limitY = 10;
        }
        else {
            limitX = map.MAP_WIDTH;
            limitY = map.MAP_HEIGHT;
        }
        for (int i = 0; i < limitY; i++) {
            for (int j = 0; j < limitX; j++) {
                String rep;
                tile currentTile = gameMap.getCell(j,i);
                if (currentTile.getOccupant() != null) {
                    if (currentTile.getOccupant() instanceof medic) rep = "+";
                    else if(currentTile.getOccupant() instanceof sniper) rep = "0";
                    else if(currentTile.getOccupant() instanceof demolitionist) rep = "D";
                    else if(currentTile.getOccupant() instanceof assault) rep = "/";
                    else rep = "O";
                }
                else {
                    rep = switch (currentTile.getTileType()) {
                        case map.EMPTY_TILE_CODE -> "-";
                        case map.SOFT_TILE_CODE -> "~";
                        case map.HARD_TILE_CODE -> "X";
                        case map.EXTRA_HARD_TILE_CODE -> "{}";
                        default -> "?";
                    };
                }
                System.out.print(rep + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void updateBlockPos(int oldX, int oldY, int newX, int newY) {
        for (int i = 0; i < teamLocations.length; i++) {
            if (teamLocations[i][0] == oldX && teamLocations[i][1] == oldY) {
                teamLocations[i][0] = newX;
                teamLocations[i][1] = newY;
                break;
            }
        }
    }

    //TODO: see if dead/inactive agents are properly removed
    public static agentCommand holdDiscussion(ArrayList<agentCommand> moves, ArrayList<agentAssistant> agents, boolean focusedTeam) {
        for (agentCommand move: moves) {
            if (focusedTeam) {
                StringBuilder outString = new StringBuilder().append("Agent ").append(move.getParent().getName()).append(" suggests action type ");
                switch (move.getActionType()) {
                    case ACTION_CODE_MOVE -> outString.append("move.");
                    case ACTION_CODE_SPRINT -> outString.append("sprint.");
                    case ACTION_CODE_ATTACK -> outString.append("attack.");
                    case ACTION_CODE_CLASSONE -> outString.append("classOne.");
                    case ACTION_CODE_CLASSTWO -> outString.append("classTwo.");
                    default -> outString.append(move.getActionType());
                }
                System.out.println(outString.toString());
                System.out.println("Stated reasons:");
                //TODO: Introduce more variety with multiple sentences for one tag/voice
                for (String x : move.getReasons()) {
                    int y = ThreadLocalRandom.current().nextInt(1, 3);
                    if (move.getParent().getVoiceCode() == agentAssistant.VOICE_CODE_ROBOTIC) {
                        switch (x) {
                            case "hasCover" -> {
                                if (y == 1)
                                    System.out.println("Defensible location identified. Recommend repositioning to these co-ordinates.");
                                else
                                    System.out.println("Self-preservation subroutine approves this tactical position.");
                            }
                            case "goodOdds" -> {
                                if (y == 1)
                                    System.out.println("Attack vector shows high probability of success. Strongly recommend opening fire.");
                                else System.out.println("Optimal firing solution detected.");
                            }
                            case "fairOdds" -> {
                                if (y == 1)
                                    System.out.println("Attack vector shows moderate probability of success. Recommend opening fire.");
                                else System.out.println("Firing solution resolved. Ready to enact.");
                            }
                            case "lethalProtection" -> {
                                if (y == 1)
                                    System.out.println("Action contributes to reduced probability of allied termination.");
                                else
                                    System.out.println("Triage subroutine identifies this as a preferable course of action.");
                            }
                            //TODO: address same code being used for cover buff evaluation, maybe add more codes
                            case "lethal" -> {
                                if (y == 1)
                                    System.out.println("Action potentially leads to immediate hostile termination. Strongly advised.");
                                else
                                    System.out.println("High priority tactical decision identified. Context: elimination of enemy unit.");
                            }
                            case "lethalMultiple" -> {
                                if (y == 1)
                                    System.out.println("Multiple threatening hostiles detected. Defensive action strongly advised.");
                                else
                                    System.out.println("Extreme tactical risk identified. Self-preservation considered maximum priority.");
                            }
                            case "affectsEnemy" -> {
                                if (y == 1)
                                    System.out.println("Action creates disadvantageous tactical situation for hostile unit. Advised.");
                                else System.out.println("Recommend impeding hostile action at this location.");
                            }
                            case "affectsEnemyMultiple" -> {
                                if (y == 1)
                                    System.out.println("Action creates disadvantageous tactical situation for multiple hostile units. Strongly advised.");
                                else
                                    System.out.println("Recommend large-scale impediment of hostile action at this location.");
                            }
                            case "hits" -> {
                                if (y == 1) System.out.println("Action damages hostile unit. Advised.");
                                else
                                    System.out.println("Probabilistic analysis favours opportunity for guaranteed damage.");
                            }
                            case "hitsMultiple" -> {
                                if (y == 1)
                                    System.out.println("Action damages multiple hostile units. Strongly advised.");
                                else
                                    System.out.println("Probabilistic analysis strongly favours opportunity for guaranteed damage to multiple targets.");
                            }
                            case "threatens" -> {
                                if (y == 1)
                                    System.out.println("Action inflicts lethal damage on hostile unit. Advised.");
                                else
                                    System.out.println("Opportunity for guaranteed hostile elimination identified. Recommend immediate action.");
                            }
                            case "threatensMultiple" -> {
                                if (y == 1)
                                    System.out.println("Action inflicts lethal damage on multiple hostile unit. Strongly advised.");
                                else
                                    System.out.println("Opportunity for guaranteed elimination of multiple hostiles identified. Highly recommend immediate action.");
                            }
                            default -> System.out.println(x);
                        }
                    } else if (move.getParent().getVoiceCode() == agentAssistant.VOICE_CODE_CAUTIOUS) {
                        switch (x) {
                            case "hasCover" -> {
                                if (y == 1)
                                    System.out.println("Rule 5: Keep your head down. This seems like a defensible spot.");
                                else
                                    System.out.println("I’m gonna go ahead and keep my head down, try not to get into too much trouble.");
                            }
                            case "goodOdds" -> {
                                if (y == 1) System.out.println("I like these odds.");
                                else System.out.println("Seems like a nice, clean shot to me.");
                            }
                            case "fairOdds" -> {
                                if (y == 1) System.out.println("This is worth a shot, at least.");
                                else System.out.println("Might as well get some lead downrange.");
                            }
                            case "lethalProtection" -> {
                                if (y == 1)
                                    System.out.println("Rule 1: kill them before they kill you. Which of course means you should make it harder for them to kill you.");
                                else System.out.println("I’m not letting anyone die out here.");
                            }
                            case "lethal" -> {
                                if (y == 1) System.out.println("Rule 1: kill them before they kill you.");
                                else
                                    System.out.println("That’s a kill shot right there, and I think we should take it.");
                            }
                            case "lethalMultiple" -> {
                                if (y == 1)
                                    System.out.println("Should maybe stay safe, that's one too many bad guys for my comfort.");
                                else System.out.println("Would be a real good idea to hunker down right about now.");
                            }
                            case "affectsEnemy" -> {
                                if (y == 1)
                                    System.out.println("Rule 8: Every death is a death by inches. Let’s push them an inch or two closer, yeah?");
                                else
                                    System.out.println("This’ll make things harder for them if we’re just a little bit patient.");
                            }
                            case "affectsEnemyMultiple" -> {
                                if (y == 1)
                                    System.out.println("Seems to me like we can ruin a whole lot of days at once by doing this.");
                                else System.out.println("Rule 3: Efficiency will save your life in the long run.");
                            }
                            case "hits" -> {
                                if (y == 1) System.out.println("Rule 11: A reliable hit is always worthwhile.");
                                else
                                    System.out.println("No luck, no guesswork, just some guaranteed pain for the other guy. I say we do this.");
                            }
                            case "hitsMultiple" -> {
                                if (y == 1)
                                    System.out.println("They were kind enough to bunch up for us. Let’s make them regret that, yeah?");
                                else
                                    System.out.println("We call this a ‘target rich environment’, let’s take advantage of it.");
                            }
                            case "threatens" -> {
                                if (y == 1)
                                    System.out.println("Rule 1: kill them before they kill you. This one right here is guaranteed.");
                                else System.out.println("That’s a dead man walking. We should fix that last part.");
                            }
                            case "threatensMultiple" -> {
                                if (y == 1)
                                    System.out.println("They’re all lined up, let’s knock them down. I love it when a plan comes together.");
                                else System.out.println("Let’s clean this place up!");
                            }
                            default -> System.out.println(x);
                        }
                    } else if (move.getParent().getVoiceCode() == agentAssistant.VOICE_CODE_RECKLESS) {
                        switch (x) {
                            case "hasCover" -> {
                                if (y == 1) System.out.println("Tch, fine, I’ll stay out of trouble");
                                else System.out.println("I’ll hide here if it’ll keep you quiet for five minutes.");
                            }
                            case "goodOdds" -> {
                                if (y == 1) System.out.println("Looking forward to seeing this shot land.");
                                else System.out.println("Ooh, they’re wide open, I can’t wait.");
                            }
                            case "fairOdds" -> {
                                if (y == 1)
                                    System.out.println("My trigger finger is itching, let’s scratch that itch.");
                                else System.out.println("I’m bored and this is as good a shot as any.");
                            }
                            case "lethalProtection" -> {
                                if (y == 1)
                                    System.out.println("If anyone here dies, I’m stuck filling out paperwork at base. This is just a bit less boring.");
                                else
                                    System.out.println("If anyone’s killing you, it’s going to be me when we get back to the boxing ring in base!");
                            }
                            case "lethal" -> {
                                if (y == 1) System.out.println("Knock ‘em down!");
                                else System.out.println("I’m looking to tally up one more.");
                            }
                            case "lethalMultiple" -> {
                                if (y == 1)
                                    System.out.println("That's, what, how many of them? Maybe we should be careful for once.");
                                else System.out.println("Oh, real nice of them, ganging up on one of us like that.");
                            }
                            case "affectsEnemy" -> {
                                if (y == 1)
                                    System.out.println("Maybe I’ll try for some of this ‘delayed gratification’ my shrink keeps telling me about.");
                                else System.out.println("This should make it easier to actually hit them for once.");
                            }
                            case "affectsEnemyMultiple" -> {
                                if (y == 1) System.out.println("I see a whole lot of suckers, all in one place.");
                                else
                                    System.out.println("Man, this is almost exciting with all of them gathered up like that.");
                            }
                            case "hits" -> {
                                if (y == 1) System.out.println("I love the smell of high explosives in the morning.");
                                else System.out.println("I’ve got something with their name on it.");
                            }
                            case "hitsMultiple" -> {
                                if (y == 1)
                                    System.out.println("Love it when they do this! I get to hit ‘em all at once!");
                                else
                                    System.out.println("Are they having a party or something? Who cares, let’s crash it.");
                            }
                            case "threatens" -> {
                                if (y == 1)
                                    System.out.println("I feel like making sure someone has a very, very bad day.");
                                else System.out.println("Say goodnight, sunshine!");
                            }
                            case "threatensMultiple" -> {
                                if (y == 1) System.out.println("Hahahaha! It’s like shooting fish in a barrel!");
                                else System.out.println("And now for the grand finale!");
                            }
                            default -> System.out.println(x);
                        }
                    } else {
                        System.out.println(x);
                    }
                }
            }
        }
        //TODO: Convert such that each move is "evaluated" by the other agents and has their value predictions added onto it.
        //TODO: Also consider having peer agents articulate their reasons for supporting/make discussion more clear
        for (agentCommand move : moves) {
            for (agentAssistant agent: agents) {
                if (move.getInvokingEntity() != agent.getSelf()) {
                    agentCommand temp = new agentCommand(move, agent);
                    temp.evaluate(agent.getGameMap(), agent.getAlliedTeam(), agent.getEnemyTeam());
                    move.incrementValue(temp.getValue(), 0);
                }
            }
        }
        Collections.sort(moves);
        agentCommand topMove = moves.get(moves.size() - 1);
        if (focusedTeam)
        System.out.println("Highest value move suggested by " + topMove.getInvokingEntity().getName());
        return topMove;
    }

    public static entity[][] getLiveEntites() {
        ArrayList<entity> entityListOne = new ArrayList<>();
        ArrayList<entity> entityListTwo = new ArrayList<>();

        //TODO: start at the end and iterate backwards/use synchronous
        for (int i = teamOne.length - 1; i >= 0; i--) {
            if (teamOne[i].isAlive()) {
               entityListOne.add(teamOne[i]);
            }
        }
        for (int i = teamTwo.length - 1; i >= 0; i--) {
            if (teamTwo[i].isAlive()) {
                entityListTwo.add(teamTwo[i]);
            }
        }
        return new entity[][]{entityListOne.toArray(entity[]:: new), entityListTwo.toArray(entity[]::new)};
    }
}
