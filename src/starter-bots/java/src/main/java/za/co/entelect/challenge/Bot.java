package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.State;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.*;

public class Bot {

    private static final int maxSpeed = 9;

    private Random random;
    private GameState gameState;
    private Car opponent;
    private Car myCar;

    private final static Command DO_NOTHING = new DoNothingCommand();
    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;
    }

    public Command run() {
        // My car's position
        int my_block = myCar.position.block;
        int my_lane = myCar.position.lane;
        int my_speed = myCar.speed;

        // Opponent's position
        int opp_block = opponent.position.block;
        int opp_lane = opponent.position.lane;

        // Inisialisasi Variable Lokal
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        int numObstacleRight = numberObstacle(my_lane+1, my_block);
        int numObstacleLeft = numberObstacle(my_lane-1, my_block);
        int numObstacleForward = numberObstacleIfAccelerate(my_lane, my_block);
        int numObstacleForwardIfBoost = numberObstacleIfBoost(my_lane, my_block);
        int safetyWay = min(numObstacleForward, min(numObstacleLeft,numObstacleRight));
        int numPowerUpsRight = numberPowerUps(my_lane+1, my_block);
        int numPowerUpsLeft = numberPowerUps(my_lane-1, my_block);
        int numPowerUpsForward = numberPowerUps(my_lane, my_block);
        int maxPowerUps = max(numPowerUpsForward, max(numPowerUpsLeft, numPowerUpsRight));

        // Melakukan perbaikan pada mobil
        if(myCar.damage >= 1) {
            return FIX;
        }
        // Pengecekan Obstacle
        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.WALL) || blocks.contains(Terrain.OIL_SPILL)) {
            if (numObstacleLeft > 0 && numObstacleForward > 0 && numObstacleRight > 0 && hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            // Mengambil jalur yang paling aman
            if (numObstacleForward == safetyWay) {
                if (numObstacleLeft == safetyWay && numPowerUpsLeft == maxPowerUps) {
                    return TURN_LEFT;
                }
                if (numObstacleRight == safetyWay && numPowerUpsRight == maxPowerUps) {
                    return TURN_RIGHT;
                }
                return ACCELERATE;
                // Mengambil jalur dengan obstacle paling sedikit
            } else {
                if (numObstacleLeft < numObstacleRight) {
                    return TURN_LEFT;
                } else if (numObstacleRight < numObstacleLeft) {
                    return TURN_RIGHT;
                } else {
                    // Mengambil jalur yang memiliki power up
                    if (numPowerUpsLeft > numPowerUpsRight) {
                        return TURN_LEFT;
                    } else if (numPowerUpsRight > numPowerUpsLeft) {
                        return TURN_RIGHT;
                    } else {
                        // Mengambil jalur di tengah (lane 2 dan 3)
                        if (my_lane == 3 || my_lane == 4) {
                            return TURN_LEFT;
                        } else {
                            return TURN_RIGHT;
                        }
                    }
                }
            }
        }

        // Penggunaan BOOST
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && numObstacleForwardIfBoost == 0) {
            return BOOST;
        }

        // Mempertahankan kecepatan mobil
        if (my_speed <= 3) {
            return ACCELERATE;
        }

        // Apabila posisi berada di depan mobil lawan, gunakan TWEET dan OIL
        if (my_block > opp_block) {
            if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
                return new TweetCommand(opp_lane, opp_block+16);
            }
            if ((hasPowerUp(PowerUps.OIL, myCar.powerups) && (my_lane == opp_lane)) || (my_speed == maxSpeed)) {
                return OIL;
            }
            // Apabila posisi berada di belakang atau sejajar mobil lawan, gunakan EMP dan TWEET
        } else {
            if (hasPowerUp(PowerUps.EMP, myCar.powerups) && (abs(my_lane-opp_lane) <= 1)) {
                return EMP;
            }
            if (hasPowerUp(PowerUps.TWEET, myCar.powerups)) {
                return new TweetCommand(opp_lane, opp_block+16);
            }
        }

        // Jika tidak ada BOOST dan tidak ada obstacle, maka ACCELERATE
        if (!myCar.boosting && my_speed != maxSpeed && numObstacleForward == 0) {
            return ACCELERATE;
        }

        // Tidak melakukan apa-apa jika seluruh kondisi di atas tidak memenuhi
        return DO_NOTHING;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        // Mengembalikan nilai True jika PowerUps tersedia
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    private List<Object> getBlocksInFront(int lane, int block) {
        // Mengembalikan objek-objek yang ada pada jalur saat ini
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + myCar.speed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }

    private int numberObstacle(int lane, int block) {
        // Mengembalikan jumlah rintangan yang ada pada block
        if (lane < 1 || lane > 4) {
            return 99;
        } else {
            int total = 0;
            List<Object> blocks = getBlocksInFront(lane, block);
            for (Object o : blocks) { // karena block index 0 yang sedang ditempati
                if (o == Terrain.OIL_SPILL || o == Terrain.MUD) {
                    total += 1;
                }
                if (o == Terrain.WALL) {
                    total += 2;
                }
            }
            return total;
        }
    }

    private int numberObstacleIfBoost(int lane, int block) {
        // Mengembalikan jumlah rintangan yang ada pada block saat menggunakan BOOST
        if (lane < 1 || lane > 4) {
            return 99;
        } else {
            List<Lane[]> map = gameState.lanes;
            List<Object> blocks = new ArrayList<>();
            int startBlock = map.get(0)[0].position.block;
            int total = 0;

            Lane[] laneList = map.get(lane - 1);
            for (int i = max(block - startBlock, 0); i <= block - startBlock + 15; i++) {
                if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                    break;
                }
                if (laneList[i].terrain == Terrain.OIL_SPILL || laneList[i].terrain == Terrain.MUD) {
                    total += 1;
                }
                if (laneList[i].terrain == Terrain.WALL) {
                    total += 2;
                }
            }
            return total;
        }
    }

    private int numberObstacleIfAccelerate(int lane, int block) {
        // Mengembalikan jumlah rintangan yang ada pada block saat ACCELERATE
        if (lane < 1 || lane > 4) {
            return 99;
        } else {
            List<Lane[]> map = gameState.lanes;
            int startBlock = ((Lane[])map.get(0))[0].position.block;
            int[] speedState = new int[]{0, 3, 6, 8, 9};
            int index = 5;
            int total = 0;

            Lane[] laneList = map.get(lane - 1);
            for(int k = 0; k < speedState.length; ++k) {
                if (this.myCar.speed == speedState[k]) {
                    index = k;
                    break;
                }
            }
            if (myCar.speed == 5) {
                for (int i = max(block - startBlock, 0); i <= block - startBlock + 6; i++) {
                    if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                        break;
                    }
                    if (laneList[i].terrain == Terrain.MUD) {
                        total++;
                    }

                    if (laneList[i].terrain == Terrain.WALL) {
                        total += 2;
                    }

                    if (laneList[i].terrain == Terrain.OIL_SPILL) {
                        total++;
                    }
                }
            } else if (index < 4) {
                for (int i = max(block - startBlock, 0); i <= block - startBlock + speedState[index+1]; i++) {
                    if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                        break;
                    }
                    if (laneList[i].terrain == Terrain.MUD) {
                        total++;
                    }

                    if (laneList[i].terrain == Terrain.WALL) {
                        total += 2;
                    }

                    if (laneList[i].terrain == Terrain.OIL_SPILL) {
                        total++;
                    }
                }
            } else if (index == 4) {
                for (int i = max(block - startBlock, 0); i <= block - startBlock + 9; i++) {
                    if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                        break;
                    }
                    if (laneList[i].terrain == Terrain.MUD) {
                        total++;
                    }

                    if (laneList[i].terrain == Terrain.WALL) {
                        total += 2;
                    }

                    if (laneList[i].terrain == Terrain.OIL_SPILL) {
                        total++;
                    }
                }
            } else {
                for (int i = max(block - startBlock, 0); i <= block - startBlock + 15; i++) {
                    if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                        break;
                    }
                    if (laneList[i].terrain == Terrain.MUD) {
                        total++;
                    }

                    if (laneList[i].terrain == Terrain.WALL) {
                        total += 2;
                    }

                    if (laneList[i].terrain == Terrain.OIL_SPILL) {
                        total++;
                    }
                }
            }

            return total;
        }
    }

    private int numberPowerUps(int lane, int block) {
        // Mengembalikan jumlah power ups yang tersedia pada jalur
        if (lane < 1 || lane > 4) {
            return -99;
        } else {
            int total = 0;
            List<Object> blocks = getBlocksInFront(lane, block);
            for (Object o : blocks) {
                if (o == Terrain.BOOST || o == Terrain.EMP) {
                    total += 5;
                }
                if (o == Terrain.TWEET || o == Terrain.LIZARD) {
                    total += 3;
                }
                if (o == Terrain.OIL_POWER) {
                    total += 1;
                }
            }
            return total;
        }
    }
}
