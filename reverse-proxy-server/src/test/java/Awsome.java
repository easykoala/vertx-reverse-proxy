import io.vertx.core.MultiMap;

import java.util.Map;

public class Awsome {
    public static void main(String[] args) {
        MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();
        multiMap.add("Fruits", "Apple");
        multiMap.add("Fruits", "Pear");
        multiMap.add("Fruits", "Banana");
        multiMap.add("Vegetables", "eggplant");
        for (Map.Entry<String, String> entry : multiMap) {
            System.out.println(entry);
        }
    }

}
