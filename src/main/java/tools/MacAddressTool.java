/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author PlayDK
 */
public class MacAddressTool {

    /*
     * 取机器的mac地址和本机IP地址
     * True = 取IP地址
     */
    public static String getMacAddress(boolean ipAddress) {
        String macs = null;
        String localip = null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress inetAddress;
            boolean finded = false; // 是否找到外网IP  
            while (netInterfaces.hasMoreElements() && !finded) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    inetAddress = address.nextElement();
                    String ip = inetAddress.getHostAddress();
                    if (ip.contains(":") || ip.startsWith("221.231.") || ip.equalsIgnoreCase("127.0.0.1")) {
                        continue;
                    }
                    //System.out.println(ni.getName() + " - " + inetAddress.getHostAddress() + " - " + inetAddress.isSiteLocalAddress() + " - " + !inetAddress.isLoopbackAddress());
                    if (!inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress()) { //外网
                        localip = inetAddress.getHostAddress();
                        byte[] mac = ni.getHardwareAddress();
                        if (mac == null) {
                            continue;
                        }
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            if (i != 0) {
                                sb.append("-");
                            }
                            String str = Integer.toHexString(mac[i] & 0xFF);
                            sb.append(str.length() == 1 ? 0 + str : str);
                        }
                        macs = sb.toString().toUpperCase();
                        //System.out.println("外网 - localip: " + localip);
                        //System.out.println("外网 - macs: " + macs);
                        finded = true;
                        break;
                    } else if (inetAddress.isSiteLocalAddress() && !inetAddress.isLoopbackAddress()) { //内网
                        localip = inetAddress.getHostAddress();
                        byte[] mac = ni.getHardwareAddress();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            if (i != 0) {
                                sb.append("-");
                            }
                            String str = Integer.toHexString(mac[i] & 0xFF);
                            sb.append(str.length() == 1 ? 0 + str : str);
                        }
                        macs = sb.toString().toUpperCase();
                        //System.out.println("内网 - localip: " + localip);
                        //System.out.println("内网 - macs: " + macs);
                        finded = true;
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ipAddress ? localip : macs;
    }
}
