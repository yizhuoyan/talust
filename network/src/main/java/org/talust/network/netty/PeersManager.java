/*
 * MIT License
 *
 * Copyright (c) 2017-2018 talust.org talust.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package org.talust.network.netty;

import com.alibaba.fastjson.JSONObject;
import org.talust.common.tools.Configure;
import org.talust.common.tools.FileUtil;
import org.talust.common.tools.IpUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

public class PeersManager {
    private static PeersManager instance = new PeersManager();

    private PeersManager() {
    }

    public static PeersManager get() {
        return instance;
    }

    private String peerConfigPath = Configure.CONFIG_PATH;
    private String peerConfigFilePath = peerConfigPath + File.separator + "ConnectionConfig.json";
    private String peersFileDirPath = Configure.PEERS_PATH;
    private String peerPath = peersFileDirPath + File.separator + "peers.json";
    public String peerCont = "";

    public void initPeers() {
        peerConfigInit();
        File file = new File(peersFileDirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File peerFile = new File(peerPath);
        try {
            if (!peerFile.exists()) {
                peerFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(peerFile);
                fos.write("{}".getBytes());
                fos.close();
                peerCont = "{}";
            } else {
                peerCont = FileUtil.fileToTxt(peerFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void peerConfigInit() {
        try {
            File config = new File(peerConfigFilePath);
            JSONObject peerConfig = JSONObject.parseObject(FileUtil.fileToTxt(config));
            Configure.setMaxPassivityConnectCount(peerConfig.getInteger("MAX_PASSIVITY_CONNECT_COUNT"));
            Configure.setMaxActiveConnectCount(peerConfig.getInteger("MAX_ACTIVE_CONNECT_COUNT"));
            Configure.setMaxSuperActivrConnectCount(peerConfig.getInteger("MAX_SUPER_PASSIVITY_CONNECT_COUNT"));
            Configure.setMaxSuperPassivityConnectCount(peerConfig.getInteger("MAX_SUPER_ACTIVE_CONNECT_COUNT"));
            Configure.setNodeServerAddr(peerConfig.getString("NODE_SERVER_ADDR"));
            Configure.setGenesisServerAddr(peerConfig.getString("GENESIS_SERVER_ADDR"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入JSON文件
     */
    public void writePeersFile(String peers) {
        try {
            File peerFile = new File(peerPath);
            FileOutputStream fos = new FileOutputStream(peerFile);
            fos.write(peers.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPeer(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (!IpUtil.internalIp(address.getAddress())) {
                try {
                    File peerFile = new File(peerPath);
                    JSONObject nowPeers = JSONObject.parseObject(FileUtil.fileToTxt(peerFile));
                    nowPeers.put(ip, "1");
                    FileOutputStream fos = new FileOutputStream(peerFile);
                    fos.write(nowPeers.toJSONString().getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void addPeer(JSONObject peers) {
        Set<String> ips = peers.keySet();
        for(String ip :ips){
            try{
                InetAddress address = InetAddress.getByName(ip);
                if(IpUtil.internalIp(address.getAddress())){
                    peers.remove(ip);
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            File peerFile = new File(peerPath);
            JSONObject nowPeers = JSONObject.parseObject(FileUtil.fileToTxt(peerFile));
            nowPeers.putAll(peers);
            FileOutputStream fos = new FileOutputStream(peerFile);
            fos.write(nowPeers.toJSONString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void removePeerList(List<String> ips) {
        try {
            File peerFile = new File(peerPath);
            JSONObject nowPeers = JSONObject.parseObject(FileUtil.fileToTxt(peerFile));
            for (String ip : ips) {
                nowPeers.remove(ip);
            }
            FileOutputStream fos = new FileOutputStream(peerFile);
            fos.write(nowPeers.toJSONString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removePeer(String ip) {
        try {
            File peerFile = new File(peerPath);
            JSONObject nowPeers = JSONObject.parseObject(FileUtil.fileToTxt(peerFile));
            nowPeers.remove(ip);
            FileOutputStream fos = new FileOutputStream(peerFile);
            fos.write(nowPeers.toJSONString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
