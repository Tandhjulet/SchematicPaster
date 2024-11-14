package net.dashmc.util;

public class MathUtils {
	public static final long pairInt(int x, int y) {
		return (((long) x) << 32) | (y & 0xffffffffL);
	}
}
