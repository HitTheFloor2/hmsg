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


    public static boolean debug = false;
    public static Integer serverid = -1;
    public static InetSocketAddress local = null;
    public static HashMap<String,Object> propertiesMap = new HashMap<>();
    public static HashMap<Integer,InetSocketAddress> serverMap = new HashMap<>();
    //测试用，读取的是不同的config
    public static HashMap<Integer,InetSocketAddress> serverMapTest = new HashMap<>();

    public static void setServerId(int id){
        serverid = id;
    }
    public static void setDebug(boolean d){
        debug = d;
    }
    public static void setLocal(String ip,int port){
        local = new InetSocketAddress(ip,port);
    }
    public static void init(){
        initConfig();
        initServers();
        Log.info(serverid,"ConfigManager.init complete: localhost="+local.toString()+" serverid="+serverid+" serverMap="+
                serverMap.toString());
    }
    /**
     * read config.properties
     * */
    public static void initConfig(){
        try{
            properties.load(new FileInputStream(path+"/config/config.properties"));
            // 按行读取配置
            debug = Boolean.valueOf(properties.getProperty("debug"));
            serverid = Integer.decode(properties.getProperty("id"));
            String ip = properties.getProperty("ip");
            int port = Integer.decode(properties.getProperty("port"));
            setLocal(ip,port);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * read servers.properties and fill serverMap
     * */
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
            if(debug == false){
                return;
            }
            prop = new Properties();
            prop.load(new FileInputStream(path+"/config/servers_test.properties"));
            for(String key: prop.stringPropertyNames()){
                String value = prop.getProperty(key);
                String[] vs = value.split(":");
                serverMapTest.put(Integer.parseInt(key),new InetSocketAddress(vs[0],Integer.parseInt(vs[1])));
            }

            Log.logger.info("ConfigManager.initServers: init serverMap completed.");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
