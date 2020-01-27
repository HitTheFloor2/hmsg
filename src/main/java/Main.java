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
        ConfigManager.setDebug(true);



        if(ConfigManager.debug){
            BaseServer baseServer8080 = new BaseServer(1,"127.0.0.1",8080);
            BaseServer baseServer8081 = new BaseServer(2,"127.0.0.1",8081);
            //TestServer testServer8082 = new TestServer(3,"127.0.0.1",8082);
            //TestServer testServer8083 = new TestServer(4,"127.0.0.1",8083);
        }
        else{
            //1 192.168.0.100 8080
            BaseServer baseServer = new BaseServer(ConfigManager.serverid,ConfigManager.local);
        }







    }

}
