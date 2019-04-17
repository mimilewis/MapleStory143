/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import lombok.extern.log4j.Log4j2;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;

@Log4j2
public class CheckLandersMac {

    public static boolean checkMac(MapleClient c) {
        String mac = c.getMac();
        String host = "127.0.0.1";
        int port = 20130;
        StringBuilder sb = new StringBuilder();
        try {//建立连接后就可以往服务端写数据了
            try (Socket client = new Socket(host, port)) { //与服务端建立连接
                Reader reader;
                try (Writer writer = new OutputStreamWriter(client.getOutputStream())) { //建立连接后就可以往服务端写数据了
                    final String wrms = "100|" + mac + "\r\n";
                    writer.write(wrms);
                    writer.flush();
                    //写完以后进行读操作
                    reader = new InputStreamReader(client.getInputStream());
                    char chars[] = new char[128];
                    int len;
                    String temp;
                    int index;
                    while ((len = reader.read(chars)) != -1) {
                        temp = new String(chars, 0, len);
                        if ((index = temp.indexOf("eof")) != -1) {
                            sb.append(temp.substring(0, index));
                            break;
                        }
                        sb.append(temp);
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            log.error("登录器检测出错", e);
        }
        return sb.toString().endsWith("no");
    }

    public static boolean sendGateMessage(Object message) {
        String host = "127.0.0.1";
        int port = 20130;
        StringBuilder sb = new StringBuilder();
        try {
            //建立连接后就可以往服务端写数据了
            try (Socket client = new Socket(host, port)) {
                Reader reader;
                try ( //与服务端建立连接
                      Writer writer = new OutputStreamWriter(client.getOutputStream())) { //建立连接后就可以往服务端写数据了
                    writer.write(message.toString() + "\r\n");
                    writer.flush();
                    //写完以后进行读操作
                    reader = new InputStreamReader(client.getInputStream());
                    char chars[] = new char[128];
                    int len;
                    String temp;
                    int index;
                    while ((len = reader.read(chars)) != -1) {
                        temp = new String(chars, 0, len);
                        if ((index = temp.indexOf("eof")) != -1) {
                            sb.append(temp.substring(0, index));
                            break;
                        }
                        sb.append(temp);
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            log.error("发送消息出错", e);
        }
        return sb.toString().endsWith("no");
    }
}
