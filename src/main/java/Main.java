import log.Log;
import manager.ConfigManager;
import server.BaseServer;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String args[]){

        int serverid = 0;
        Log.init();
        Log.logger.info("Main() Log init completed!");
        InetSocketAddress local = null;

        ConfigManager.init();



        if(ConfigManager.debug){
            BaseServer baseServer8080 = new BaseServer(1,"127.0.0.1",8080);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BaseServer baseServer8081 = new BaseServer(2,"127.0.0.1",8081);
            BaseServer baseServer8082 = new BaseServer(3,"127.0.0.1",8082);
            BaseServer baseServer8083 = new BaseServer(4,"127.0.0.1",8083);

        }
        else{
            //1 192.168.0.100 8080
            BaseServer baseServer = new BaseServer(ConfigManager.serverid,ConfigManager.local);
        }







    }

}
