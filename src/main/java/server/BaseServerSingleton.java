package server;


/**
 * 单例创建BaseServer实例
 * */
public class BaseServerSingleton {
    private static volatile BaseServer instance = null;

    public static BaseServer getInstance(int id, String ip, int port){
        if(instance == null){
            synchronized (BaseServerSingleton.class){
                if(instance == null){
                    instance = new BaseServer(id,ip,port);
                }
            }
        }
        return instance;
    }
}
