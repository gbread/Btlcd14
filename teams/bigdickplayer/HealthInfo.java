package bigdickplayer;

public class HealthInfo {
	static double health = 1;
	static boolean attacked = false;
	/**
	 * vraci true pokud na me nekdo zautocil
	 * 
	 * @param value
	 * @return
	 */
	static boolean setHealth(double value) {
		attacked = value < health;
		health = value;
		return attacked;
	}
}
