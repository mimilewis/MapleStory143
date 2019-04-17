package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

/**
 * Provides a class with tools for working with IP addresses, in both strings
 * and as long integers.
 *
 * @author Nol888
 * @version 0.1
 * @since Revision 890
 */
public class IPAddressTool {

    /**
     * Converts a dotted-quad IP (
     * <code>127.0.0.1</code>) and turns it into a long integer IP.
     *
     * @param dottedQuad The IP address in dotted-quad form.
     * @return The IP as a long integer.
     * @throws RuntimeException
     */
    public static long dottedQuadToLong(String dottedQuad) throws RuntimeException {
        final String[] quads = dottedQuad.split("\\.");
        if (quads.length != 4) {
            throw new RuntimeException("Invalid IP Address format.");
        }
        long ipAddress = 0;
        for (int i = 0; i < 4; i++) {
            ipAddress += Integer.parseInt(quads[i]) % 256 * (long) Math.pow(256, 4 - i);
        }
        return ipAddress;
    }

    /**
     * Converts a long integer IP into a dotted-quad IP.
     *
     * @param longIP The IP as a long integer.
     * @return The IP as a dotted-quad string.
     * @throws RuntimeException
     */
    public static String longToDottedQuad(long longIP) throws RuntimeException {
        StringBuilder ipAddress = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int quad = (int) (longIP / (long) Math.pow(256, 4 - i));
            longIP -= quad * (long) Math.pow(256, 4 - i);
            if (i > 0) {
                ipAddress.append(".");
            }
            if (quad > 255) {
                throw new RuntimeException("Invalid long IP address.");
            }
            ipAddress.append(quad);
        }
        return ipAddress.toString();
    }

    public static String getLocalIP() {
        String ipAddrStr = "";
        byte[] ipAddr;
        try {
            ipAddr = InetAddress.getLocalHost().getAddress();
        } catch (UnknownHostException e) {
            return null;
        }
        for (int i = 0; i < ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i] & 0xFF;
        }
        return ipAddrStr;
    }

    public static void getLocalIPs() {
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface nif = netInterfaces.nextElement();
                Enumeration<InetAddress> iparray = nif.getInetAddresses();
                while (iparray.hasMoreElements()) {
                    System.out.println("IP:" + iparray.nextElement().getHostAddress());
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 获取CPU序列号
     *
     * @return
     */
    public static String getCPUSerial() {
        String result = "";
        try {
            File file = File.createTempFile("CPUSerial", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);
            String vbs = "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
                    + "Set colItems = objWMIService.ExecQuery _ \n"
                    + "   (\"Select * from Win32_Processor\") \n"
                    + "For Each objItem in colItems \n"
                    + "    Wscript.Echo objItem.ProcessorId \n"
                    + "    exit for  ' do the first cpu only! \n" + "Next \n";
            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
            file.delete();
        } catch (Exception e) {
            System.out.println(e.toString());
            result = null;
        }
        if (result == null || result.trim().length() < 1) {
            result = null;
        }
        return result.trim();
    }

    /**
     * 获取硬盘序列号
     *
     * @param drive 盘符
     * @return
     */
    public static String getHardDiskSN(String drive) {
        String result = "";
        try {
            File file = File.createTempFile("disksn", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);
            String vbs = "Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n"
                    + "Set colDrives = objFSO.Drives\n"
                    + "Set objDrive = colDrives.item(\""
                    + drive
                    + "\")\n"
                    + "Wscript.Echo objDrive.SerialNumber"; // see note
            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
            file.delete();
        } catch (Exception e) {
            result = null;
        }
        return result.trim();
    }

    public static String getkey() {
        String result = "";
        String cpu = getCPUSerial();
        String diskSN = getHardDiskSN("c");
        String mac = MacAddressTool.getMacAddress(false);
        if (cpu != null) {
            //int num = cpu.length();
            //result += cpu.substring(num - 8, num);
            result += cpu;
        }
        if (diskSN != null) {
            result += diskSN;
        }
        if (mac != null) {
            result += mac;
        }
        return encryptToMD5(result.trim());
    }

    /**
     * 进行MD5加密
     *
     * @param info 要加密的信息
     * @return String 加密后的字符串
     */
    public static String encryptToMD5(String info) {
        byte[] digesta = null;
        try {
            // 得到一个md5的消息摘要
            MessageDigest alga = MessageDigest.getInstance("MD5");
            // 添加要进行计算摘要的信息
            alga.update(info.getBytes());
            // 得到该摘要
            digesta = alga.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // 将摘要转为字符串
        //String rs = byte2hex(digesta);
        return byte2hex(digesta);
    }

    /**
     * 将二进制转化为16进制字符串
     *
     * @param b 二进制字节数组
     * @return String
     */
    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (byte aB : b) {
            stmp = Integer.toHexString(aB & 0xFF);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }
}
