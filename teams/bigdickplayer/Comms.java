package bigdickplayer;

import java.util.ArrayList;

import battlecode.common.*;

public class Comms{
	
	static RobotController rc;
	static int[] lengthOfEachPath = new int[100];
	
	static final int PASTR_CHANNEL = 65000;
	static final int NOISE_TOWER_CHANNEL = 65001;
	static final int PASTR_LOCATION_CHANNEL = 65002;
	
	// TODO utoky - chceme vedet na co a odkud zautocil nepritel
	static final int SOLDIER_ATTACKS_CHANNEL = 65010;
	static final int PASTR_OR_TOWER_ATTACKS_CHANNEL = 64050;
	
	enum AttackType {
		SoldierAttack, PastrAttack, HQAttack;
		int getChannel() {
			switch (this) {
				case SoldierAttack:
				case HQAttack:
					return SOLDIER_ATTACKS_CHANNEL;
				case PastrAttack:
					return PASTR_OR_TOWER_ATTACKS_CHANNEL;
				default:
					return -1;
			}
			
		}
	}
	
	// set of methods to handle with arrays of maplocations
	private static MapLocation[] getMapLocationsArray(int channel) throws GameActionException {
		int count = rc.readBroadcast(channel);
		MapLocation[] result = new MapLocation[count];
		for (int i = 0; i < result.length; i++) {
			result[i] = VectorFunctions.intToLoc(rc.readBroadcast(channel + 1 + i));
		}
		return result;
	}
	static int getMapLocationsArrayCount(int channel) throws GameActionException {
		return rc.readBroadcast(channel);
	}
	private static int addMapLocationToArray(int channel, MapLocation location) throws GameActionException {
		int count = getMapLocationsArrayCount(channel);
		rc.broadcast(channel + ++count, VectorFunctions.locToInt(location));
		return count;
	}
	private static boolean deleteMapLocationFromArray(int channel, int index) throws GameActionException {
		int count = getMapLocationsArrayCount(channel);
		if (index >= count) return false;
		int lastItem = rc.readBroadcast(channel + count--);
		rc.broadcast(channel + index + 1, lastItem);
		return true;
	}
	
 	public static MapLocation[] getAttacks(AttackType type) throws GameActionException {
		int channel = type.getChannel();
		return getMapLocationsArray(channel);
	}
 	static int getAttackersCount(AttackType type) throws GameActionException {
		int channel = type.getChannel();
		return getMapLocationsArrayCount(channel);
 	}
 	static int addMapLocationToArray(AttackType type, MapLocation location) throws GameActionException {
 		int channel = type.getChannel();
		return addMapLocationToArray(channel, location);
 	}
 	static boolean deleteAttackerFromArray(AttackType type, int index) throws GameActionException {
 		int channel = type.getChannel();
		return deleteMapLocationFromArray(channel, index);
 	}
	
	public static ArrayList<MapLocation> downloadPath() throws GameActionException {
		ArrayList<MapLocation> downloadedPath = new ArrayList<MapLocation>();
		int locationInt = rc.readBroadcast(RobotPlayer.myBand+1);
		while(locationInt>=0){
			downloadedPath.add(VectorFunctions.intToLoc(locationInt));
			locationInt = rc.readBroadcast(RobotPlayer.myBand+1+downloadedPath.size());
		}
		RobotPlayer.myBand = -locationInt*100;
		return downloadedPath;
	}
	

	public static void findPathAndBroadcast(int bandID,MapLocation start, MapLocation goal, int bigBoxSize, int joinSquadNo) throws GameActionException{
		//tell robots where to go
		//the unit will not pathfind if the broadcast goal (for this band ID) is the same as the one currently on the message channel
		int band = bandID*100;
		MapLocation pathGoesTo = VectorFunctions.intToLoc(rc.readBroadcast(band+lengthOfEachPath[bandID]));
		if(!pathGoesTo.equals(VectorFunctions.mldivide(goal,bigBoxSize))){
			ArrayList<MapLocation> path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(goal,bigBoxSize), 100000);
			rc.broadcast(band, Clock.getRoundNum());
			for(int i=path.size()-1;i>=0;i--){
				rc.broadcast(band+i+1, VectorFunctions.locToInt(path.get(i)));
			}
			lengthOfEachPath[bandID]= path.size();
			rc.broadcast(band+lengthOfEachPath[bandID]+1, -joinSquadNo);
		}
	}
	
	public static void pastrBuildingInProgress(MapLocation location) {
		try {
			rc.broadcast(PASTR_CHANNEL, 1);
			rc.broadcast(PASTR_LOCATION_CHANNEL, VectorFunctions.locToInt(location));
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void noiseTowerBuildingInProgress() {
		try {
			rc.broadcast(NOISE_TOWER_CHANNEL, 1);
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static boolean doWeHavePastr() throws GameActionException {
		return rc.readBroadcast(PASTR_CHANNEL) == 1;
	}
	public static MapLocation getOurPastrLocation() throws GameActionException {
		return VectorFunctions.intToLoc(rc.readBroadcast(PASTR_LOCATION_CHANNEL));
	}	
	public static boolean doWeHaveTower() throws GameActionException {
		return rc.readBroadcast(NOISE_TOWER_CHANNEL) == 1;
	}
}