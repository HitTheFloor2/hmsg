package log;

import org.apache.log4j.*;


public class Log {
    public static Logger logger = null;

    /**
     * 初始化Log，建立全局可引用的Log
     * 为了性能考虑，在执行Log.logger.xxxx()时不使用反射进行调用者查询，故应在log内容中注明调用者
     * */
    public synchronized static void init(){
        if(logger != null){ return; }
        String path = System.getProperty("user.dir");
        PropertyConfigurator.configure(path+"/config/log4j.properties");
        logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
    }

    /**
     * debug优先级最低
     * */
    public static void debug(String address,String string){ Log.logger.debug("server["+address+"]: " + string);}
    /**
     * info级别的日志用于输出消息收发的细节，包括消息内容，消息回复过程等
     * */
    public static void info(String address,String string){
        Log.logger.info("server["+address+"]: " + string);
    }

    public static void warn(String address,String string){ Log.logger.warn("server["+address+"]: " + string);}
    public static void error(String address,String string){ Log.logger.error("server["+address+"]: " + string);}
}
