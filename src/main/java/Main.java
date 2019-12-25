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
        InetSocketAddress local = null;
        if(args.length == 2){
            local = new InetSocketAddress(args[1],Integer.parseInt(args[0]));
        }


        Log.init();
        if(local == null){
            TestServer testServer8080 = new TestServer("127.0.0.1",8080);
            TestServer testServer8081 = new TestServer("127.0.0.1",8081);
            testServer8080.client.asyncAddServer(1,testServer8081.inetSocketAddress);
            testServer8080.client.writeMsg(1,"connect to 8081");
        }
        while(true){
            try{
                Thread.sleep(3000);
            }catch (Exception e){

            }

        }


    }

}
