import log.Log;
import manager.ConfigManager;
import server.BaseServer;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String args[]){

        Log.init();
        Log.logger.info("Main() Log init completed!");

        ConfigManager.init();

        if(ConfigManager.debug){
            BaseServer baseServer8080 = new BaseServer("first","127.0.0.1",8080);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BaseServer baseServer8081 = new BaseServer("second","127.0.0.1",8081);
            BaseServer baseServer8082 = new BaseServer("third","127.0.0.1",8082);
            BaseServer baseServer8083 = new BaseServer("fourth","127.0.0.1",8083);

        }
        else{
            //1 192.168.0.100 8080
            try{
                BaseServer baseServer = new BaseServer(ConfigManager.name,ConfigManager.address,ConfigManager.serverList);
                //Thread.sleep(5000);
                //BaseServer baseServer1 = new BaseServer(ConfigManager.name,"127.0.0.1:8081",ConfigManager.serverList);

            }catch (Exception e){

            }

        }







    }

}
