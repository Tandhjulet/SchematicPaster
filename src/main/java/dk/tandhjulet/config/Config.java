package dk.tandhjulet.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;

public class Config extends OkaeriConfig {
	@Getter
	@Comment("Max chunks to place pr. second. Set to -1 to disable.")
	private Integer maxChunksPerSecond = -1;
}
