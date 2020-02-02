package test;

import io.netty.util.concurrent.FutureListener;

public class test3 {
    public static void main(String[] args) {
        long init = 1L << 63;
        int msgSeqNum = 45644;
        long timestamp = System.currentTimeMillis();
        int server_sender_id = 10;
        long part1 = (long)msgSeqNum;
        String temp = Long.toBinaryString(timestamp);
        String temp2 = temp.substring(temp.length()-25,temp.length()-1);
        long part2 = Long.parseUnsignedLong(temp2,2) << 32;
        long part3 = (long)server_sender_id << 56;

        long res = init | part1 | part2 | part3;
        System.out.println(Long.toBinaryString(init)+","+Long.toBinaryString(init).length());
        System.out.println(Long.toBinaryString(part1)+","+Long.toBinaryString(part1).length());
        System.out.println(Long.toBinaryString(part2)+","+Long.toBinaryString(part2).length());
        System.out.println(Long.toBinaryString(part3)+","+Long.toBinaryString(part3).length());
        System.out.println(Long.toBinaryString(res)+","+Long.toBinaryString(res).length());
    }
}
