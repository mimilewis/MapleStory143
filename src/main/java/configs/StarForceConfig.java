package configs;

import tools.config.Property;

public class StarForceConfig {

    @Property(key = "START_DOWN", defaultValue = "6")
    public static int START_DOWN;

    @Property(key = "CURSE_REDUCE_COUNT", defaultValue = "5")
    public static int CURSE_REDUCE_COUNT;

    @Property(key = "BONUS_TIME", defaultValue = "2")
    public static int BONUS_TIME;

    @Property(key = "START_CURSE", defaultValue = "25")
    public static int START_CURSE;
}
