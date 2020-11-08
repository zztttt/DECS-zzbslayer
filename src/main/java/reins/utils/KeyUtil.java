package reins.utils;

public class KeyUtil {
    public static String generateKey(String... keys){
        String key = String.join("_", keys);
        return key;
    }

    public static String generateInternalKey(String ...keys){
        return "_" + generateKey(keys);
    }
}
