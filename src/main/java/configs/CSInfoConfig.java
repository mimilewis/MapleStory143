package configs;

import tools.config.Property;

public class CSInfoConfig {

    /**
     * 商城信息封包
     */
    @Property(key = "cash.cashshoppack", defaultValue = "")
    public static String CASH_CASHSHOPPACK;
    /**
     * 品克缤敲敲乐第一阶段奖励
     */
    @Property(key = "cash.pbtapreward1", defaultValue = "")
    public static String CASH_PBTAPREWARD1;
    /**
     * 品克缤敲敲乐第二阶段奖励
     */
    @Property(key = "cash.pbtapreward2", defaultValue = "")
    public static String CASH_PBTAPREWARD2;
    /**
     * 品克缤敲敲乐第三阶段奖励
     */
    @Property(key = "cash.pbtapreward3", defaultValue = "")
    public static String CASH_PBTAPREWARD3;
}
