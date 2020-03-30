package util;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

public class MUtil {
    public static String InetSocketAddress2String(InetSocketAddress i){
        return i.getHostName()+":"+i.getPort();
    }
    public static InetSocketAddress String2InetSocketAddress(String address){
        String[] tmp = address.split(":");
        InetSocketAddress inetSocketAddress = new InetSocketAddress(tmp[0],Integer.valueOf(tmp[1]));
        return inetSocketAddress;
    }

    public static String getMD5Str(String str) throws Exception {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            return new BigInteger(1, md.digest()).toString(2);
        } catch (Exception e) {
            throw new Exception("MD5加密出现错误，" + e.toString());
        }
    }
}
