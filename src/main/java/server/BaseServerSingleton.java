package server;


/**
 * 单例创建BaseServer实例
 * 调试时暂时不需要单例模式
 * */
public class BaseServerSingleton {
    private static volatile BaseServer instance = null;

    public static BaseServer getInstance(int id, String ip, int port){
        if(instance == null){
            synchronized (BaseServerSingleton.class){
                if(instance == null){
                    instance = new BaseServer("",ip,port);
                }
            }
        }
        return instance;
    }
}
