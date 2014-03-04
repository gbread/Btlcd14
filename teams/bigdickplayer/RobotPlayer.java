package bigdickplayer;

import battlecode.common.*;
import bigdickplayer.Comms.AttackType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RobotPlayer{
	
	static RobotController rc;
	static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	static ArrayList<MapLocation> path = new ArrayList<MapLocation>();
	static int bigBoxSize = 5;
	
	static boolean weHavePastr = false;
	static boolean weHaveNoiseTower = false;
	
	//HQ data:
	static MapLocation rallyPoint;
	
	//SOLDIER data:
	static int myBand = 100;
	static int pathCreatedRound = -1;

	private static Robot[] enemyRobots;
	private static MapLocation[] robotLocations = new MapLocation[0];
	private static MapLocation closestEnemyLoc = null;
	private static boolean didShot = false;
	private static MapLocation didShotAt = null;
	private static int didShotAtId = -1;
	private static int closestEnemyID;
	private static boolean hadPastr = false;
	

    static MapLocation maxCow = null;

    public static void run(RobotController rcIn) throws GameActionException {
        rc=rcIn;

		Comms.rc = rcIn;
		randall.setSeed(rc.getRobot().getID());
		
		if(rc.getType()==RobotType.HQ){
			rc.broadcast(101,VectorFunctions.locToInt(VectorFunctions.mldivide(rc.senseHQLocation(),bigBoxSize)));//this tells soldiers to stay near HQ to start
			rc.broadcast(102,-1);//and to remain in squad 1
			tryToSpawn();
			BreadthFirst.init(rc, bigBoxSize);
			rallyPoint = VectorFunctions.mladd(VectorFunctions.mldivide(VectorFunctions.mlsubtract(rc.senseEnemyHQLocation(),rc.senseHQLocation()),3),rc.senseHQLocation());
		} else {
			BreadthFirst.rc=rcIn;//slimmed down init
		}
		//rc.broadcast(0, 0);
		//MapLocation goal = getRandomLocation();
		//path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(goal,bigBoxSize), 100000);
		//VectorFunctions.printPath(path,bigBoxSize);
		
		Comms.initArrays();
		

		while(true) {
			HealthInfo.setHealth(rc.getHealth()); 
			closestEnemyLoc = null;
			enemyRobots = rc.senseNearbyGameObjects(Robot.class,15000,rc.getTeam().opponent());
			if (enemyRobots.length > 0) {
				MapLocation[] maplocs = VectorFunctions.robotsToLocations(enemyRobots, rc);

				int hqindex = Arrays.asList(maplocs).indexOf(rc.senseEnemyHQLocation());
				if (hqindex >= 0) {
					maplocs[hqindex] = maplocs[maplocs.length-1];
					robotLocations = new MapLocation[maplocs.length - 1];
					System.arraycopy(maplocs, 0, robotLocations, 0, maplocs.length-1);
					
					Robot[] robs = new Robot[enemyRobots.length - 1];
					System.arraycopy(enemyRobots, 0, robs, 0, robs.length);
					if (hqindex != enemyRobots.length - 1)
						robs[hqindex] = enemyRobots[enemyRobots.length - 1];
					
					enemyRobots = robs;
					
				} else {
					robotLocations = maplocs;
				}
				if (enemyRobots.length > 0) {
					closestEnemyLoc = VectorFunctions.findClosest(robotLocations, rc.getLocation());
					closestEnemyID = rc.senseObjectAtLocation(closestEnemyLoc).getID();
					boolean was = false;
					for (int i = 0; i < enemyRobots.length; i++) {
						was |= (enemyRobots[i].getID() == didShotAtId);
						Comms.addOrUpdateEnemySolierLocation(enemyRobots[i].getID(), robotLocations[i]);
					}
					if (!was) {
						Comms.deleteEnemySoldierAndLocation(didShotAtId);
						Comms.deleteAttackerFromArrays(didShotAtId);
					}
				}
			}

			weHavePastr = Comms.doWeHavePastr();
			weHaveNoiseTower = Comms.doWeHaveTower();
			hadPastr = rc.sensePastrLocations(rc.getTeam()).length > 0;

			try{
				switch (rc.getType()) {
				case HQ:
					runHQ();
					break;
				case SOLDIER:
					runSoldier();
					break;
				case PASTR:
					runPastr();
					break;
				case NOISETOWER:
					runNoiseTower();
					break;
				}
				
			}catch (Exception e){
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	static double noiseTowerAngle = 0;
	static double noiseTowerRadius = 0;
	private static int[][] soldierAndLocations;	
	static final double angleTurn = Math.PI/4; 
	static final double noiseTowerRadiusDiff = 2; 
	
	private static void runNoiseTower() throws GameActionException {
		if (rc.isActive()) {
			if (weHavePastr)
				Comms.noiseTowerBuildingInProgress();
			else {
				rc.selfDestruct();
				Comms.noiseTowerDestroyed();
			}
			
			if (enemyRobots.length > 0) {
				for (int i = 0; i < enemyRobots.length; i++) {
					Comms.addRobotIdToArray(AttackType.PastrAttack, enemyRobots[i].getID());
				}
			} else {
				Comms.clearAttackersArray(AttackType.PastrAttack);
			}
			
			int radiusSquared = 300;
			
			if (noiseTowerAngle > 2*Math.PI) {
				noiseTowerAngle -= 2*Math.PI;
			}
			
			MapLocation thisLocation = rc.getLocation();
			
			MapLocation relativeLocation = new MapLocation((int)(Math.cos(noiseTowerAngle)*noiseTowerRadius), (int)(-Math.sin(noiseTowerAngle)*noiseTowerRadius));

			MapLocation boomLocation = VectorFunctions.mladd(thisLocation, relativeLocation);

			rc.attackSquare(boomLocation);
			
			if (Comms.getOurPastrLocation().distanceSquaredTo(boomLocation) < 60) {
				noiseTowerRadius = (int)Math.sqrt(radiusSquared);
				noiseTowerAngle += angleTurn;
			}

			noiseTowerRadius -= noiseTowerRadiusDiff;
			
		}
	}

	private static void runPastr() throws GameActionException {
		Comms.pastrBuildingInProgress(rc.getLocation());

		if (enemyRobots.length > 0) {
			for (int i = 0; i < enemyRobots.length; i++) {
				System.out.println("PASTR");
				Comms.addRobotIdToArray(AttackType.PastrAttack, enemyRobots[i].getID());
			}
		} else {
		//	Comms.clearAttackersArray(AttackType.PastrAttack);
		}
	}

	private static void runHQ() throws GameActionException {
		
		//tell them to go to the rally point
		//Comms.findPathAndBroadcast(1,rc.getLocation(),rallyPoint,bigBoxSize,2);
		
		//if the enemy builds a pastr, tell sqaud 2 to go there.
		MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
		if(enemyPastrs.length>0){
			//Comms.findPathAndBroadcast(2,rallyPoint,enemyPastrs[0],bigBoxSize,2);//for some reason, they are not getting this message
		}
		
		if (weHavePastr && hadPastr && rc.sensePastrLocations(rc.getTeam()).length == 0) {
			System.out.println("Znicenej PASTR HQ");
			Comms.pastrDestroyed();
			hadPastr = false;
		}
		//rc.broadcast(0, 0);
		//after telling them where to go, consider spawning
		tryToSpawn();
		
		// DEBUG
		
		if (Clock.getRoundNum() < 500 ) {
			if (Comms.getEnemySoldiersAndLocations().length > 0) {
				
				int[][] enemies = Comms.getEnemySoldiersAndLocations();
				System.out.print("All enemies soldiers: ");
				for (int i = 0; i < enemies.length; i++) {
					System.out.print(enemies[i][0] + ":");
					
					System.out.print(VectorFunctions.intToLoc(enemies[i][1]) + " ");
				}
				System.out.println();
			}
			if (Comms.getAttackersCount(AttackType.PastrAttack) > 0) {
				System.out.println("Pastr attackers count: " + Comms.getAttackersCount(AttackType.PastrAttack));
				System.out.println("Pastr attackers: " + Arrays.toString(Comms.getAttacks(AttackType.PastrAttack)));
			}
			if (Comms.getAttackersCount(AttackType.SoldierAttack) > 0) {
				System.out.println("Soldier attackers count: " + Comms.getAttackersCount(AttackType.SoldierAttack));
				System.out.println("Soldier attackers: " + Arrays.toString(Comms.getAttacks(AttackType.SoldierAttack)));
			}
			//System.out.println("Pastr attackers: " + Arrays.toString(Comms.getAttacks(AttackType.PastrAttack)));
			//System.out.println(Comms.getEnemySoldiersAndLocations());
		}
		

	}


    public static void tryToSpawn() throws GameActionException {
        if(rc.isActive()&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			for(int i=0;i<8;i++){
				Direction trialDir = allDirections[i];
				if(rc.canMove(trialDir)){
					rc.spawn(trialDir);
					break;
				}
			}
		}
	}
	
	static boolean tryAttackSomeoneIfNear(boolean tryToMoveCloserToShoot) throws GameActionException {
		if(enemyRobots.length>0){//SHOOT AT, OR RUN TOWARDS, ENEMIES
			if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared+1){//close enough to shoot
				if(rc.isActive()){
					rc.attackSquare(closestEnemyLoc);
					didShotAt = closestEnemyLoc;
					GameObject obj = rc.senseObjectAtLocation(closestEnemyLoc);
					if (obj != null)
						didShotAtId = obj.getID();
					// TODO prasarna
					return true;
				}
			} else if (tryToMoveCloserToShoot) {//not close enough to shoot, so try to go shoot
				Direction towardClosest = rc.getLocation().directionTo(closestEnemyLoc);
				simpleMove(towardClosest);
			}
			
		}
		return false;
	}
	
	private static void runSoldier() throws GameActionException {
		if (!rc.isActive()) return;
		
		if (HealthInfo.attacked) {
			Comms.addRobotIdToArray(AttackType.SoldierAttack, didShotAtId);
		}
		
		if (rc.isConstructing()) {
			if (rc.getConstructingType() == RobotType.PASTR) {
				if (HealthInfo.attacked && HealthInfo.health < 0.2) {
					System.out.println("Znicenej PASTR SOLDIER");
					Comms.pastrDestroyed();
					rc.selfDestruct();
					return;
				} else {
					Comms.pastrBuildingInProgress(rc.getLocation());
				}
			} else if (rc.getConstructingType() == RobotType.NOISETOWER) {
				Comms.noiseTowerBuildingInProgress();
			}
			
			return;
		}
		
        if (maxCow == null) {
            maxCow = findCows();
        }

		/*
		if (!weHavePastr && rc.senseCowsAtLocation(rc.getLocation()) > 0) {
			
			weHavePastr = true;
			Comms.pastrBuildingInProgress();
			rc.construct(RobotType.PASTR);
		} */
						
		MapLocation closestSoldierAttacker = VectorFunctions.closestRobotToLocation(Comms.getAttacks(AttackType.SoldierAttack), rc.getLocation(), rc);
		
		soldierAndLocations = Comms.getEnemySoldiersAndLocations();
		
		//follow orders from HQ
		
			
			
		if(didShot = (closestEnemyLoc != null && tryAttackSomeoneIfNear(false))){//SHOOT AT, OR RUN TOWARDS, ENEMIES
			
		} else if (Comms.getAttackersCount(AttackType.PastrAttack) > 0) {
			// nekdo utoci na pastr
			int[] attackingRobots = Comms.getAttacks(AttackType.PastrAttack);
			
			int soldierToAttackId = -1;
			
			outerloop:
			for (int i = 0; i < soldierAndLocations.length; i++) {
				for (int j = 0; j < attackingRobots.length; j++) {
					if (soldierAndLocations[i][0] == attackingRobots[j]) {
						soldierToAttackId = i;
						break outerloop;
					}					
				}
			}
			if (soldierToAttackId == -1) {
				// TODO nejblizsiho
				Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				BasicPathing.guardLocation(towardEnemy, Comms.getOurPastrLocation(), true, rc, directionalLooks, allDirections, true);

			} else {
				
				Direction towardEnemy = rc.getLocation().directionTo(VectorFunctions.intToLoc(soldierAndLocations[soldierToAttackId][1]));
				BasicPathing.tryToMove(towardEnemy, true, rc, directionalLooks, allDirections, true);//was Direction.SOUTH_EAST
			}
			
		} else if (closestSoldierAttacker != null && closestSoldierAttacker.distanceSquaredTo(rc.getLocation()) < 20) {
			 BasicPathing.tryToMove(rc.getLocation().directionTo(closestSoldierAttacker), true, rc, directionalLooks, allDirections, true);
				
		} else if(soldierAndLocations.length > 0 && enemyRobots.length == 0 && (!weHavePastr || Comms.getOurPastrLocation().distanceSquaredTo(closestSoldierAttacker) < 30)) {
			// posun k nejblizsimu robotu
			int minDistIndex = 0;
			for (int i = 1; i < soldierAndLocations.length; i++) {
				if (soldierAndLocations[i][1] < soldierAndLocations[minDistIndex][1]) {
					minDistIndex = i;
				}
			}
			// TODO tady je problem.... asi v podmince... kdyz chci zdrhnout. Pridam tohle telo do podminky a
			// zajistim, abych zbytecne neutocil
			BasicPathing.tryToMove(rc.getLocation().directionTo(VectorFunctions.intToLoc(soldierAndLocations[minDistIndex][1])), true, rc, directionalLooks, allDirections, true);
		} else if (!weHavePastr && maxCow.distanceSquaredTo(rc.getLocation())<5) {
			
			weHavePastr = true;
			Comms.pastrBuildingInProgress(rc.getLocation());
			rc.construct(RobotType.PASTR);
		} else if (weHavePastr && !weHaveNoiseTower) {
		
			MapLocation pastrLocation = Comms.getOurPastrLocation();
			if (rc.getLocation().distanceSquaredTo(pastrLocation) <= 2) {
				// TODO neco chytrejsiho.... jestli neni pobliz nepritel
				buildNoiseTower();
			} else {
			
				BasicPathing.tryToMove(rc.getLocation().directionTo(pastrLocation), true, rc, directionalLooks, allDirections, true);
			}
			
		} else if (!weHavePastr) {
			//NAVIGATION BY DOWNLOADED PATH
			/*rc.setIndicatorString(0, "team "+myBand+", path length "+path.size());
			if(path.size()<=1){
				//check if a new path is available
				int broadcastCreatedRound = rc.readBroadcast(myBand);
				if(pathCreatedRound<broadcastCreatedRound){
					rc.setIndicatorString(1, "downloading path");
					pathCreatedRound = broadcastCreatedRound;
					path = Comms.downloadPath();
				}
			}
			if(path.size()>0){
				//follow breadthFirst path
				Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
				BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
			}
			*/
			
			// TODO NAJDEME NEJBLIZSI KRAVY (chytreji)

			//Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			//BasicPathing.tryToMove(towardEnemy, true, rc, directionalLooks, allDirections, true);//was Direction.SOUTH_EAST

 
            Direction towardEnemy = rc.getLocation().directionTo(maxCow);
            BasicPathing.tryToMove(towardEnemy, true, rc, directionalLooks, allDirections, true);//was Direction.SOUTH_EAST
        
		} else {
			// mame pastr a vez, tak je budeme chranit
			MapLocation pastrLocation = Comms.getOurPastrLocation();
			
			//MapLocation pastrLoc = rc.getLocation().directionTo(pastrLocation);
			BasicPathing.guardLocation(pastrLocation.directionTo(rc.senseEnemyHQLocation()), pastrLocation, true, rc, directionalLooks, allDirections, true);
		}
		
		//Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		//simpleMove(towardEnemy);
		
	}
	
	private static void buildNoiseTower() throws GameActionException {
		weHaveNoiseTower = true;
		Comms.noiseTowerBuildingInProgress();
		rc.construct(RobotType.NOISETOWER);
	}

	private static MapLocation getRandomLocation() {
		return new MapLocation(randall.nextInt(rc.getMapWidth()),randall.nextInt(rc.getMapHeight()));
	}

    /**
     * Najde kravy (snad)
     *
     * @return
     */
    private static MapLocation findCows() {

    	// TODO ulozime si nak pozice krav do bool[][], abychom vedeli, kde se da urcite chodit...
    	// TODO neco lepsiho nez astar (milanuv) by nebylo? 
    	// varianta 1: Zkusime se proste co nejdriv dostat na kravu a pak pujdeme jen po kravach.....
    	
        double[][] cows = rc.senseCowGrowth(); //Stoji 100 bytecodu!! (TODO:mozna volat jen jednou pokud je pole staticke a nekam si ho ulozit)

        double max = Double.MIN_VALUE;
        int x = cows.length / 2;
        int y = cows[0].length / 2;

        MapLocation ourHQLoc = rc.senseHQLocation();
        MapLocation enemyHQLoc = rc.senseEnemyHQLocation();
        
        
        for (int i = 0; i < cows.length; i++) {
            double[] cowRow = cows[i];
            for (int j = 0; j < cowRow.length; j++) {
                double cow = cowRow[j];
                
                double addedValue = Math.pow(enemyHQLoc.distanceSquaredTo(new MapLocation(i, j)),2)*cow/ourHQLoc.distanceSquaredTo(new MapLocation(i, j));
                
                if (addedValue > max) {
                    max = addedValue;
                    x = i;
                    y = j;
                }
            }
        }

        return new MapLocation(x, y);
    }

    private static void simpleMove(Direction chosenDirection) throws GameActionException {
        if(rc.isActive()){
			for(int directionalOffset:directionalLooks){
				int forwardInt = chosenDirection.ordinal();
				Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
				if(rc.canMove(trialDir)){
					rc.move(trialDir);
					break;
				}
			}
		}
	}
	
}