package log;

import org.apache.log4j.*;


public class Log {
    public static Logger logger ;
    /**
     * 初始化Log，建立全局可引用的Log
     * 为了性能考虑，在执行Log.logger.xxxx()时不使用反射进行调用者查询，故应在log内容中注明调用者
     * */
    public static void init(){
        String path = System.getProperty("user.dir");
        PropertyConfigurator.configure(path+"/config/log4j.properties");
        logger = Logger.getRootLogger();
        logger.setLevel(Level.DEBUG);
    }


}
