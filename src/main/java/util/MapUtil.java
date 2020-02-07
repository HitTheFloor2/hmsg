package util;

import java.util.HashMap;
import java.util.Map;

public class MapUtil {
    public static Object unsafeSearchKey(HashMap<Object,Object> map, Object value){
        for (Map.Entry entry : map.entrySet()) {
            if(value.equals(entry.getValue())){
                return entry.getKey();
            }
        }
        return null;
    }
}
