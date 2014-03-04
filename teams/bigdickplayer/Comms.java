package bigdickplayer;

import java.util.ArrayList;

import battlecode.common.*;

public class Comms{
	
	static RobotController rc;
	static int[] lengthOfEachPath = new int[100];
	
	static final int PASTR_CHANNEL = 65000;
	static final int NOISE_TOWER_CHANNEL = 65001;
	static final int PASTR_LOCATION_CHANNEL = 65002;
	// TODO
	static final int PASTR_UNDER_FIRE_CHANNEL = 65003;
	
	// TODO utoky - chceme vedet na co a odkud zautocil nepritel
	// EDIT: asi budou stacit pozice nepratel
	static final int SOLDIER_ATTACKS_CHANNEL = 65010;
	static final int PASTR_OR_TOWER_ATTACKS_CHANNEL = 64050;
	static final int ENEMY_SOLDIERS_LOCATIONS_CHANNEL = 64500;
	static final int ENEMY_SOLDIERS_LOCATIONS_TUPLE_SIZE = 2;
	static final int ENEMY_SOLDIERS_LOCATIONS_TUPLE_INDEX_ROBOTID = 0;
	static final int ENEMY_SOLDIERS_LOCATIONS_TUPLE_INDEX_LOCATION = 1;
	
	public static void initArrays() throws GameActionException {
		setTupleSizeOfArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, ENEMY_SOLDIERS_LOCATIONS_TUPLE_SIZE);
	}
	
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
	
	
	private static int getTupleSizeOfArray(int channel) throws GameActionException {
		return rc.readBroadcast(channel + 1);
	}
	private static void setTupleSizeOfArray(int channel, int tupleSize) throws GameActionException {
		rc.broadcast(channel + 1, tupleSize);
	}
	private static int getSizeOfTupleArray(int channel) throws GameActionException {
		return rc.readBroadcast(channel);
	}
	private static void setSizeOfTupleArray(int channel, int size) throws GameActionException {
		rc.broadcast(channel, size);
	}

	private static int[][] getTupleArray(int channel) throws GameActionException {
		int size = getSizeOfTupleArray(channel);
		int tupleSize = getTupleSizeOfArray(channel);
		int[][] result = new int[size][tupleSize];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < tupleSize; j++) {
				result[i][j] = rc.readBroadcast(channel+2+i*tupleSize + j);
			}
		}
		return result;
	}
	private static void addToTupleArray(int channel, int[] tuple, int index) throws GameActionException {
		for (int i = 0; i < tuple.length; i++) {
			rc.broadcast(channel + 2 + index*tuple.length + i, tuple[i]);
		}
	}
	private static void addToTupleArray(int channel, int[] tuple) throws GameActionException {
		int size = getSizeOfTupleArray(channel);
		//int tupleSize = getTupleSizeOfArray(channel);
		setSizeOfTupleArray(channel, ++size);
		addToTupleArray(channel, tuple, size-1);
	}
	private static int[] getTupleFromArrayAt(int channel, int index) throws GameActionException {
		int tupleSize = getTupleSizeOfArray(channel);
		int[] result = new int[tupleSize];
		for (int j = 0; j < tupleSize; j++) {
			result[j] = rc.readBroadcast(channel+2+index*tupleSize + j);
		}
		return result;
	}
	private static int getIndexOfTupleInArray(int channel, int tupleSize, int value, int indexInTuple) throws GameActionException {
		int count = getSizeOfTupleArray(channel);
		for (int i = 0; i < count; i++) {
			int[] tuple = getTupleFromArrayAt(channel, i);
			if (tuple[indexInTuple] == value) {
				return i;
			}
		}
		return -1;
	}
	private static void deleteIndexOfTupleArray(int channel, int index, int tupleSize) throws GameActionException {
		int size = getSizeOfTupleArray(channel);
		setSizeOfTupleArray(channel, --size);
		for (int i = 0; i < tupleSize; i++) {
			int val = rc.readBroadcast(channel+2+size*tupleSize + i);
			rc.broadcast(channel+2+index*tupleSize + i, val);
		}
	}
	

	static int[][] getEnemySoldiersAndLocations() throws GameActionException {
		return getTupleArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL);
	}
	static boolean addOrUpdateEnemySolierLocation(int robotId, MapLocation location) throws GameActionException {
		int index = getIndexOfTupleInArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, ENEMY_SOLDIERS_LOCATIONS_TUPLE_SIZE, 
								robotId, ENEMY_SOLDIERS_LOCATIONS_TUPLE_INDEX_ROBOTID);
		int[] tuple = new int[ENEMY_SOLDIERS_LOCATIONS_TUPLE_SIZE];
		tuple[ENEMY_SOLDIERS_LOCATIONS_TUPLE_INDEX_ROBOTID] = robotId;
		tuple[ENEMY_SOLDIERS_LOCATIONS_TUPLE_INDEX_LOCATION] = VectorFunctions.locToInt(location);
		
		if (index < 0)
		{
			addToTupleArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, tuple);
			return true;
		} else {
			addToTupleArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, tuple, index);
			return false;
		}
		
	}
	static void clearEnemySoldiersAndLocations() throws GameActionException {
		setSizeOfTupleArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, 0);
	}
	static void deleteEnemySoldierAndLocation(int robotId) throws GameActionException {
		int index = getIndexOfTupleInArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, ENEMY_SOLDIERS_LOCATIONS_TUPLE_SIZE,
											robotId, ENEMY_SOLDIERS_LOCATIONS_TUPLE_INDEX_ROBOTID);
		if (index >= 0) {
			deleteIndexOfTupleArray(ENEMY_SOLDIERS_LOCATIONS_CHANNEL, index, ENEMY_SOLDIERS_LOCATIONS_TUPLE_SIZE);
		}
	}
	
	
	// set of methods to handle with arrays of maplocations

	private static int[] getArray(int channel) throws GameActionException {
		int count = rc.readBroadcast(channel);
		int[] result = new int[count];
		for (int i = 0; i < result.length; i++) {
			result[i] = rc.readBroadcast(channel + 1 + i);
		}
		return result;
	}
	static int getArrayCount(int channel) throws GameActionException {
		return rc.readBroadcast(channel);
	}
	static void setArrayCount(int channel, int count) throws GameActionException {
		rc.broadcast(channel, count);
	}
	private static int addIntToArray(int channel, int value) throws GameActionException {
		int count = getArrayCount(channel);
		rc.broadcast(channel + (++count), value);
		setArrayCount(channel, count);
		return count;
	}
	private static boolean deleteValueFromArray(int channel, int index) throws GameActionException {
		int count = getArrayCount(channel);
		if (index >= count) return false;
		int lastItem = rc.readBroadcast(channel + count--);
		rc.broadcast(channel + index + 1, lastItem);
		setArrayCount(channel, count);
		return true;
	}
	private static int clear(int channel) throws GameActionException {
		int count = getArrayCount(channel);
		rc.broadcast(channel, 0);
		return count;
	}
	private static int findValue(int channel, int value) throws GameActionException {
		int count = getArrayCount(channel);
		for (int i = 0; i < count; i++) {
			int val = rc.readBroadcast(channel + 1 + i);
			if (val == value) return i;
		}
		return -1;
	}
	
  	public static int[] getAttacks(AttackType type) throws GameActionException {
		int channel = type.getChannel();
		return getArray(channel);
	}
 	static int getAttackersCount(AttackType type) throws GameActionException {
		int channel = type.getChannel();
		return getArrayCount(channel);
 	}
 	static int addRobotIdToArray(AttackType type, int RobotId) throws GameActionException {
 		int channel = type.getChannel();
 		if (findValue(channel, RobotId) == -1)
 			return addIntToArray(channel, RobotId);
 		return 0;
 	}
 	static boolean deleteAttackerFromArrayAt(AttackType type, int index) throws GameActionException {
 		int channel = type.getChannel();
		return deleteValueFromArray(channel, index);
 	}
 	static int clearAttackersArray(AttackType type) throws GameActionException {
		int channel = type.getChannel();
		return clear(channel);
		
 	}
 	public static void deleteAttackerFromArrays(int robotId) throws GameActionException {
		deleteAttackerFromArray(AttackType.PastrAttack, robotId);
		deleteAttackerFromArray(AttackType.SoldierAttack, robotId);
	}
	private static void deleteAttackerFromArray(AttackType pastrattack, int robotId) throws GameActionException {
		int channel = pastrattack.getChannel();
		int index = findValue(channel, robotId);
		if (index > 0)
			deleteAttackerFromArrayAt(pastrattack, index);
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
	
	public static void pastrDestroyed() throws GameActionException {
		rc.broadcast(PASTR_CHANNEL, 0);
	}
	public static void pastrBuildingInProgress(MapLocation location) throws GameActionException {
			rc.broadcast(PASTR_CHANNEL, 1);
			rc.broadcast(PASTR_LOCATION_CHANNEL, VectorFunctions.locToInt(location));
	}

	public static void noiseTowerBuildingInProgress() throws GameActionException {
			rc.broadcast(NOISE_TOWER_CHANNEL, 1);
	}
	public static void noiseTowerDestroyed() throws GameActionException {
		rc.broadcast(NOISE_TOWER_CHANNEL, 0);
	}
	public static boolean doWeHavePastr() throws GameActionException {
		return rc.readBroadcast(PASTR_CHANNEL) != 0;
	}
	public static MapLocation getOurPastrLocation() throws GameActionException {
		return VectorFunctions.intToLoc(rc.readBroadcast(PASTR_LOCATION_CHANNEL));
	}	
	public static boolean doWeHaveTower() throws GameActionException {
		return rc.readBroadcast(NOISE_TOWER_CHANNEL) == 1;
	}
	
}