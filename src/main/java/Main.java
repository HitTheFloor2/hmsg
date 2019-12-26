import com.google.protobuf.InvalidProtocolBufferException;
import log.Log;
import protobuf.*;
import server.TestServer;
import server.TestServerClient;

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

        if(debug){
            TestServer testServer8080 = new TestServer(1,"192.168.0.100",8080);
            TestServer testServer8081 = new TestServer(2,"192.168.0.100",8081);
        }
        else{
            //1 192.168.0.100 8080
            if(args.length == 3){
                serverid = Integer.parseInt(args[0]);
                local = new InetSocketAddress(args[1],Integer.parseInt(args[2]));
                Log.logger.info("Main: local server host : "+local.toString());
                TestServer testServer = new TestServer(serverid,local);

            }




            if(local == null){
                TestServer testServer8080 = new TestServer(0,8080);
                TestServer testServer8081 = new TestServer(1,8081);
                //testServer8080.client.writeMsg(1,"connect to 8081");
            }
        }







    }

}
