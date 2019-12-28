import com.google.protobuf.InvalidProtocolBufferException;
import log.Log;
import manager.ConfigManager;
import protobuf.*;
import server.TestServer;
import server.TestServerClient;
import server.TimeServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Main {
    public static void main(String args[]){
        boolean debug = false;
        int serverid = 0;
        Log.init();
        Log.logger.info("Main() Log init completed!");
        InetSocketAddress local = null;

        ConfigManager.init();



        if(debug){
            TestServer testServer8080 = new TestServer(1,"192.168.0.100",8080);
            TestServer testServer8081 = new TestServer(2,"192.168.0.100",8081);
        }
        else{
            //1 192.168.0.100 8080
            TestServer testServer = new TestServer(ConfigManager.serverid,ConfigManager.local);
        }







    }

}
