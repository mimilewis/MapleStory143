package provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MapleDataProvider {

    private static final Logger log = LogManager.getLogger();
    private final File root;
    private final MapleDataDirectoryEntry rootForNavigation;

    public MapleDataProvider(File fileIn) {
        root = fileIn;
        rootForNavigation = new MapleDataDirectoryEntry(fileIn.getName(), 0, 0, null);
        fillMapleDataEntitys(root, rootForNavigation);
    }

    private void fillMapleDataEntitys(File lroot, MapleDataDirectoryEntry wzdir) {
        for (File file : lroot.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory() && !fileName.endsWith(".img")) {
                MapleDataDirectoryEntry newDir = new MapleDataDirectoryEntry(fileName, 0, 0, wzdir);
                wzdir.addDirectory(newDir);
                fillMapleDataEntitys(file, newDir);
            } else if (fileName.endsWith(".xml")) { // get the real size here?
                wzdir.addFile(new MapleDataFileEntry(fileName.substring(0, fileName.length() - 4), 0, 0, wzdir));
            }
        }
    }

    public MapleData getData(String path) {
        File dataFile = new File(root, path + ".xml");
        File imageDataDir = new File(root, path);
        /*
         * if (!dataFile.exists()) { throw new RuntimeException("Datafile " +
         * path + " does not exist in " + root.getAbsolutePath());
         * }
         */
        if (!dataFile.exists()) {
            return null;
        }
        FileInputStream fis;
        try {
            fis = new FileInputStream(dataFile);
        } catch (FileNotFoundException e) {
            log.error("Datafile " + path + " does not exist in " + root.getAbsolutePath(), e);
            throw new RuntimeException("Datafile " + path + " does not exist in " + root.getAbsolutePath());
        }
        MapleData domMapleData;
        try {
            domMapleData = new MapleData(fis, imageDataDir.getParentFile());
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return domMapleData;
    }

    public MapleData getReturnData(String path) {
        File dataFile = new File(root, path + ".xml");
        File imageDataDir = new File(root, path);

        FileInputStream fis;
        try {
            fis = new FileInputStream(dataFile);
        } catch (FileNotFoundException e) {
            return null;
        }
        MapleData domMapleData;
        try {
            domMapleData = new MapleData(fis, imageDataDir.getParentFile());
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return domMapleData;
    }

    public MapleDataDirectoryEntry getRoot() {
        return rootForNavigation;
    }
}
