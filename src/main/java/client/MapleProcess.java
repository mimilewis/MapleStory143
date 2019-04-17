/*
  获得进程
 */
package client;

public class MapleProcess {

    private final String path, MD5, dir, name, ext;

    public MapleProcess(String path, String MD5) {
        this.path = path;
        this.MD5 = MD5;
        String[] names = path.split("\\\\");
        String name_ = path;
        if (names.length > 1) {
            StringBuilder sb = new StringBuilder();
            name_ = names[names.length - 1];
            for (int i = 0; i < names.length - 1; i++) {
                sb.append(names[i]).append("\\");
            }
            dir = sb.toString();
        } else {
            dir = null;
        }
        names = name_.split("\\.");
        if (names.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.length - 1; i++) {
                sb.append(names[i]);
                if (i < names.length - 2) {
                    sb.append(".");
                }
            }
            this.name = sb.toString();
        } else {
            this.name = name_;
        }
        ext = names.length > 1 ? names[names.length - 1] : null;
    }

    public String getPath() {
        return path;
    }

    public String getMD5() {
        return MD5;
    }

    public String getDir() {
        return dir;
    }

    public String getName() {
        return name;
    }

    public String getExt() {
        return ext;
    }
}
