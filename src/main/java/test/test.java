package test;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class test {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        List<String> list = new ArrayList<String>();
        list.add("233");
        list.add("666");
        try {
            String jsonStr = mapper.writeValueAsString(list);
            System.out.println(jsonStr);

            List<String> stringList = mapper.readValue(jsonStr,List.class);
            System.out.println(stringList.get(0));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
