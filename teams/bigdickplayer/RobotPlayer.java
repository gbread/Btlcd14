package bigdickplayer;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;
import bigdickplayer.Comms.AttackType;

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
	
	public static void run(RobotController rcIn) throws GameActionException{
		rc=rcIn;
		Comms.rc = rcIn;
		randall.setSeed(rc.getRobot().getID());
		
		if(rc.getType()==RobotType.HQ){
			rc.broadcast(101,VectorFunctions.locToInt(VectorFunctions.mldivide(rc.senseHQLocation(),bigBoxSize)));//this tells soldiers to stay near HQ to start
			rc.broadcast(102,-1);//and to remain in squad 1
			tryToSpawn();
			BreadthFirst.init(rc, bigBoxSize);
			rallyPoint = VectorFunctions.mladd(VectorFunctions.mldivide(VectorFunctions.mlsubtract(rc.senseEnemyHQLocation(),rc.senseHQLocation()),3),rc.senseHQLocation());
		}else{
			BreadthFirst.rc=rcIn;//slimmed down init
		}
		//rc.broadcast(0, 0);
		//MapLocation goal = getRandomLocation();
		//path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(goal,bigBoxSize), 100000);
		//VectorFunctions.printPath(path,bigBoxSize);
		
		Comms.initArrays();
		
		HealthInfo.setHealth(rc.getHealth()); 

		while(true) {
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
	static final double angleTurn = Math.PI/4; 
	static final double noiseTowerRadiusDiff = 2; 
	
	private static void runNoiseTower() throws GameActionException {
		Comms.noiseTowerBuildingInProgress();
		if (rc.isActive()) {
			
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
		// TODO poslat info pri utoku
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			MapLocation[] robotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc);

			for (int i = 0; i < enemyRobots.length; i++) {
				Comms.addOrUpdateEnemySolierLocation(enemyRobots[i].getID(), robotLocations[i]);
				Comms.addRobotIdToArray(AttackType.PastrAttack, enemyRobots[i].getID());
			}
		} else {
			Comms.clearAttackersArray(AttackType.PastrAttack);
		}
	}

	private static void runHQ() throws GameActionException {
		//TODO consider updating the rally point to an allied pastr 
		
		//tell them to go to the rally point
		//Comms.findPathAndBroadcast(1,rc.getLocation(),rallyPoint,bigBoxSize,2);
		
		//if the enemy builds a pastr, tell sqaud 2 to go there.
		MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
		if(enemyPastrs.length>0){
			//Comms.findPathAndBroadcast(2,rallyPoint,enemyPastrs[0],bigBoxSize,2);//for some reason, they are not getting this message
		}
		//rc.broadcast(0, 0);
		//after telling them where to go, consider spawning
		tryToSpawn();

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
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,15000,rc.getTeam().opponent());
		if(enemyRobots.length>0){//SHOOT AT, OR RUN TOWARDS, ENEMIES
			MapLocation[] robotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc);
			MapLocation closestEnemyLoc = VectorFunctions.findClosest(robotLocations, rc.getLocation());
			if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared+1){//close enough to shoot
				if(rc.isActive()){
					rc.attackSquare(closestEnemyLoc);
				}
			} else if (tryToMoveCloserToShoot) {//not close enough to shoot, so try to go shoot
				Direction towardClosest = rc.getLocation().directionTo(closestEnemyLoc);
				simpleMove(towardClosest);
			}
			return true;
		}
		return false;
	}
	
	private static void runSoldier() throws GameActionException {
		if (!rc.isActive()) return;
		
		if (rc.isConstructing()) {
			if (rc.getConstructingType() == RobotType.PASTR) {
				Comms.pastrBuildingInProgress(rc.getLocation());
			} else if (rc.getConstructingType() == RobotType.NOISETOWER) {
				Comms.noiseTowerBuildingInProgress();
			}
			
			return;
		}
			weHavePastr = Comms.doWeHavePastr();
			weHaveNoiseTower = Comms.doWeHaveTower();

		/*
		if (!weHavePastr && rc.senseCowsAtLocation(rc.getLocation()) > 0) {
			
			weHavePastr = true;
			Comms.pastrBuildingInProgress();
			rc.construct(RobotType.PASTR);
		} */
		
		//follow orders from HQ

		if(tryAttackSomeoneIfNear(false)){//SHOOT AT, OR RUN TOWARDS, ENEMIES
			
		} else if (!weHavePastr && rc.senseCowsAtLocation(rc.getLocation()) > 0) {
			
			weHavePastr = true;
			Comms.pastrBuildingInProgress(rc.getLocation());
			rc.construct(RobotType.PASTR);
		} else if (weHavePastr && !weHaveNoiseTower) {
			MapLocation[] pastrLocations = rc.sensePastrLocations(rc.getTeam());
			if (pastrLocations.length == 0) return;
			
			MapLocation pastrLocation = pastrLocations[0];
			if (rc.getLocation().distanceSquaredTo(pastrLocation) <= 2) {
				// TODO neco chytrejsiho.... jestli neni pobliz nepritel
				buildNoiseTower();
			} else {
			
				BasicPathing.tryToMove(rc.getLocation().directionTo(pastrLocation), true, rc, directionalLooks, allDirections, false);
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
			Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			BasicPathing.tryToMove(towardEnemy, true, rc, directionalLooks, allDirections, true);//was Direction.SOUTH_EAST
		} else if (Comms.getAttackersCount(AttackType.PastrAttack) > 0) {
			// nekdo utoci na pastr
			int[] attackingRobots = Comms.getAttacks(AttackType.PastrAttack);
			int[][] soldiers = Comms.getEnemySoldiersAndLocations();
			int soldierToAttackId = -1;
			
			outerloop:
			for (int i = 0; i < soldiers.length; i++) {
				for (int j = 0; j < attackingRobots.length; j++) {
					if (soldiers[i][0] == attackingRobots[j]) {
						soldierToAttackId = i;
						break outerloop;
					}					
				}
			}
			if (soldierToAttackId == -1) {
				Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				BasicPathing.guardLocation(towardEnemy, Comms.getOurPastrLocation(), true, rc, directionalLooks, allDirections, true);

			} else {
				
				Direction towardEnemy = rc.getLocation().directionTo(VectorFunctions.intToLoc(soldiers[soldierToAttackId][1]));
				BasicPathing.tryToMove(towardEnemy, true, rc, directionalLooks, allDirections, true);//was Direction.SOUTH_EAST
			}
			
		} else {
			// mame pastr a vez, tak je budeme chranit
			MapLocation[] pastrLocations = rc.sensePastrLocations(rc.getTeam());
			if (pastrLocations.length == 0) return;
			
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

	private static void simpleMove(Direction chosenDirection) throws GameActionException{
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