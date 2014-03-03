package bigdickplayer;

import java.util.ArrayList;

import battlecode.common.*;
import bigdickplayer.Comms.AttackType;

public class VectorFunctions {
	public static MapLocation findClosest(MapLocation[] manyLocs, MapLocation point){
		int closestDist = 10000000;
		int challengerDist = closestDist;
		MapLocation closestLoc = null;
		for(MapLocation m:manyLocs){
			challengerDist = point.distanceSquaredTo(m);
			if(challengerDist<closestDist){
				closestDist = challengerDist;
				closestLoc = m;
			}
		}
		return closestLoc;
	}
	public static int findClosestIndex(MapLocation[] manyLocs, MapLocation point){
		int closestDist = 10000000;
		int challengerDist = closestDist;
//		MapLocation closestLoc = null;
		int result = 0;
		
		for (int i = 0; i < manyLocs.length; i++) {
			challengerDist = point.distanceSquaredTo(manyLocs[i]);
			if(challengerDist<closestDist){
				closestDist = challengerDist;
//				closestLoc = manyLocs[i];
				result = i;
			}
		}
		return result;
	}
	
	public static MapLocation mladd(MapLocation m1, MapLocation m2){
		return new MapLocation(m1.x+m2.x,m1.y+m2.y);
	}
	
	public static MapLocation mlsubtract(MapLocation m1, MapLocation m2){
		return new MapLocation(m1.x-m2.x,m1.y-m2.y);
	}
	
	public static MapLocation mldivide(MapLocation bigM, int divisor){
		return new MapLocation(bigM.x/divisor, bigM.y/divisor);
	}
	
	public static MapLocation mlmultiply(MapLocation bigM, int factor){
		return new MapLocation(bigM.x*factor, bigM.y*factor);
	}
	
	public static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	public static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
	
	public static void printPath(ArrayList<MapLocation> path, int bigBoxSize){
		for(MapLocation m:path){
			MapLocation actualLoc = bigBoxCenter(m,bigBoxSize);
			System.out.println("("+actualLoc.x+","+actualLoc.y+")");
		}
	}
	public static MapLocation bigBoxCenter(MapLocation bigBoxLoc, int bigBoxSize){
		return mladd(mlmultiply(bigBoxLoc,bigBoxSize),new MapLocation(bigBoxSize/2,bigBoxSize/2));
	}
	public static MapLocation[] robotsToLocations(Robot[] robotList,RobotController rc) throws GameActionException{
		MapLocation[] robotLocations = new MapLocation[robotList.length];
		for(int i=0;i<robotList.length;i++){
			Robot anEnemy = robotList[i];
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			robotLocations[i] = anEnemyInfo.location;
		}
		return robotLocations;
	}
	public static MapLocation closestRobotToLocation(int[] robotIdList, MapLocation loc, RobotController rc) throws GameActionException{
		int[][] enemySoldiers = Comms.getEnemySoldiersAndLocations();
		MapLocation[] attackersLoc = new MapLocation[robotIdList.length];
		for (int i = 0; i < robotIdList.length; i++) {
			attackersLoc[i] = rc.senseEnemyHQLocation();
			for (int j = 0; j < enemySoldiers.length; j++) {
				if (enemySoldiers[j][0] == robotIdList[i]) {
					attackersLoc[i] = VectorFunctions.intToLoc(enemySoldiers[j][1]);
					break;
				}
			}				
		}
		return VectorFunctions.findClosest(attackersLoc, loc);
	}
}

