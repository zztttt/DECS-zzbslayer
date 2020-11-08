package reins.utils;

public class TimeUtil {
    public static long getCurrentAbsoluteHour(){
        return System.currentTimeMillis() / 1000 / (60 * 60);
    }
}
