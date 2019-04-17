/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author PlayDK
 */
public class WorldFindService {

    private static final Logger log = LogManager.getLogger(WorldFindService.class.getName());
    private final ReentrantReadWriteLock lock;
    private final HashMap<Integer, Integer> idToChannel;
    private final HashMap<String, Integer> nameToChannel;

    private WorldFindService() {
        log.info("正在启动[WorldFindService]");
        lock = new ReentrantReadWriteLock();
        idToChannel = new HashMap<>();
        nameToChannel = new HashMap<>();
    }

    public static WorldFindService getInstance() {
        return SingletonHolder.instance;
    }

    public void register(int chrId, String chrName, int channel) {
        lock.writeLock().lock();
        try {
            idToChannel.put(chrId, channel);
            nameToChannel.put(chrName.toLowerCase(), channel);
        } finally {
            lock.writeLock().unlock();
        }
        if (channel == -10) {
            System.out.println("玩家连接 - 角色ID: " + chrId + " 名字: " + chrName + " 进入商城");
        } else if (channel == -20) {
            System.out.println("玩家连接 - 角色ID: " + chrId + " 名字: " + chrName + " 进入拍卖");
        } else if (channel > -1) {
            System.out.println("玩家连接 - 角色ID: " + chrId + " 名字: " + chrName + " 频道: " + channel);
        } else {
            System.out.println("玩家连接 - 角色ID: " + chrId + " 未处理的频道...");
        }
    }

    public void forceDeregister(int chrId) {
        lock.writeLock().lock();
        try {
            idToChannel.remove(chrId);
        } finally {
            lock.writeLock().unlock();
        }
        System.out.println("玩家离开 - 角色ID: " + chrId);
    }

    public void forceDeregister(String chrName) {
        lock.writeLock().lock();
        try {
            nameToChannel.remove(chrName.toLowerCase());
        } finally {
            lock.writeLock().unlock();
        }
        System.out.println("玩家离开 - 角色名字: " + chrName);
    }

    public void forceDeregister(int chrId, String chrName) {
        lock.writeLock().lock();
        try {
            idToChannel.remove(chrId);
            nameToChannel.remove(chrName.toLowerCase());
        } finally {
            lock.writeLock().unlock();
        }
        System.out.println("玩家离开 - 角色ID: " + chrId + " 名字: " + chrName);
    }

    public void forceDeregisterEx(int chrId, String chrName) {
        lock.writeLock().lock();
        try {
            idToChannel.remove(chrId);
            nameToChannel.remove(chrName.toLowerCase());
        } finally {
            lock.writeLock().unlock();
        }
        System.out.println("清理卡号玩家 - 角色ID: " + chrId + " 名字: " + chrName);
    }

    /*
     * 通过角色的ID 找到角色的频道
     */
    public int findChannel(int chrId) {
        Integer ret;
        lock.readLock().lock();
        try {
            ret = idToChannel.get(chrId);
        } finally {
            lock.readLock().unlock();
        }
        if (ret != null) {
            if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret) == null) { //wha
                forceDeregister(chrId);
                return -1;
            }
            return ret;
        }
        return -1;
    }

    /*
     * 通过角色的名字 找到角色的频道
     */
    public int findChannel(String chrName) {
        Integer ret;
        lock.readLock().lock();
        try {
            ret = nameToChannel.get(chrName.toLowerCase());
        } finally {
            lock.readLock().unlock();
        }
        if (ret != null) {
            /*
             * 如果找到了这个角色 但是这个频道是空的 就删除这个角色注册到服务端的信息 返回 -1
             */
            if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret) == null) {
                forceDeregister(chrName);
                return -1;
            }
            return ret;
        }
        return -1;
    }

    /*
     * 好友列表获取 好友的在线信息
     */
    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (int i : characterIds) {
            int channel = findChannel(i);
            if (channel > 0) {
                foundsChars.add(new CharacterIdChannelPair(i, channel));
            }
        }
        Collections.sort(foundsChars);
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    /*
     * 通过角色名字 找到角色信息
     */
    public MapleCharacter findCharacterByName(String name) {
        int ch = findChannel(name);
        if (ch > 0) {
            return ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(name);
        }
        return null;
    }

    /*
     * 通过角色ID 找到角色信息
     */
    public MapleCharacter findCharacterById(int id) {
        int ch = findChannel(id);
        if (ch > 0) {
            return ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(id);
        }
        return null;
    }

    private static class SingletonHolder {

        protected static final WorldFindService instance = new WorldFindService();
    }
}
