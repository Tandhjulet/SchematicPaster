package dk.tandhjulet.util;

public class MathUtils {
	public static final long pairInt(int x, int y) {
		return (((long) x) << 32) | (y & 0xffffffffL);
	}

	public static final int roundInt(double value) {
		return (int) (value < 0 ? (value == (int) value) ? value : value - 1 : value);
	}
}
