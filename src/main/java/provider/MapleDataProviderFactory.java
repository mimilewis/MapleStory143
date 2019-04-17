package provider;

import java.io.File;

public class MapleDataProviderFactory {

    private final static String wzPath = System.getProperty("wzpath");

    private static MapleDataProvider getWZ(Object in) {
        if (in instanceof File) {
            return new MapleDataProvider((File) in);
        }
        throw new IllegalArgumentException("Can't create data provider for input " + in);
    }

    public static MapleDataProvider getDataProvider(Object in) {
        return getWZ(in);
    }

    public static File fileInWZPath(String filename) {
        return new File(wzPath, filename);
    }
}
