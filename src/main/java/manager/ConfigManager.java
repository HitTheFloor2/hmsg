package manager;

import log.Log;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * ConfigManager用于管理config文件夹下的配置信息
 * */
public class ConfigManager {
    public static String path = System.getProperty("user.dir");
    public static Properties properties = new Properties();

    public static String name = "Default";
    public static boolean debug = false;
    public static String address = "";
    public static InetSocketAddress local = null;
    public static HashMap<String,Object> propertiesMap = new HashMap<>();
    public static ArrayList<String> serverList = new ArrayList<>();
    //测试用，读取的是不同的config
    public static ArrayList<String> serverTestList = new ArrayList<>();

    public static void setServerAddress(String _address){
        address = _address;
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
        Log.info(address,"ConfigManager.init complete: localhost="+local.toString()+
                " debug="+debug+
                " serverMap="+ serverList.toString()
        );
    }
    /**
     * read config.properties
     * */
    public static void initConfig(){
        try{
            properties.load(new FileInputStream(path+"/config/config.properties"));
            // 按行读取配置
            debug = Boolean.valueOf(properties.getProperty("debug"));
            name = properties.getProperty("name");
            String ip = properties.getProperty("ip");
            int port = Integer.decode(properties.getProperty("port"));
            setLocal(ip,port);
            address = ip + ":" + Integer.toString(port);

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
                serverList.add(vs[0]+":"+Integer.parseInt(vs[1]));
            }
            if(debug == false){
                return;
            }
            prop = new Properties();
            prop.load(new FileInputStream(path+"/config/servers_test.properties"));
            for(String key: prop.stringPropertyNames()){
                String value = prop.getProperty(key);
                String[] vs = value.split(":");
                serverTestList.add(vs[0]+":"+Integer.parseInt(vs[1]));
            }

            Log.logger.info("ConfigManager.initServers: init serverMap completed.");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
