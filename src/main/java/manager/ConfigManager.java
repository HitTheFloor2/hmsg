package manager;

import log.Log;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;

/**
 * ConfigManager用于管理config文件夹下的配置信息
 * */
public class ConfigManager {
    public static String path = System.getProperty("user.dir");
    public static Properties properties = new Properties();

    public static Integer serverid = -1;
    public static InetSocketAddress local = null;
    public static HashMap<String,Object> propertiesMap = new HashMap<>();
    public static HashMap<Integer,InetSocketAddress> serverMap = new HashMap<>();

    public static void setServerid(int id){
        serverid = id;
    }
    public static void setLocal(String ip,int port){
        local = new InetSocketAddress(ip,port);
    }
    public static void init(){
        initConfig();
        initServers();

        Log.logger.info("ConfigManager.init complete: localhost="+local.toString()+" serverid="+serverid+" serverMap="+
                serverMap.toString());
    }
    public static void initConfig(){
        try{
            properties.load(new FileInputStream(path+"/config/config.properties"));
            serverid = Integer.decode(properties.getProperty("id"));
            String ip = properties.getProperty("ip");
            int port = Integer.decode(properties.getProperty("port"));
            setLocal(ip,port);



        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void initServers(){
        try{
            String path = System.getProperty("user.dir");
            Properties prop = new Properties();
            prop.load(new FileInputStream(path+"/config/servers.properties"));
            for(String key: prop.stringPropertyNames()){
                String value = prop.getProperty(key);
                String[] vs = value.split(":");
                serverMap.put(Integer.parseInt(key),new InetSocketAddress(vs[0],Integer.parseInt(vs[1])));
            }
            //Log.logger.info("ConfigManager.initServers: init serverMap completed.");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
