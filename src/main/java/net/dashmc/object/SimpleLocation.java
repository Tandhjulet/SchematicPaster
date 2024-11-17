package net.dashmc.object;

import lombok.Getter;

public class SimpleLocation {
	@Getter
	private int x, y, z;

	public SimpleLocation(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
