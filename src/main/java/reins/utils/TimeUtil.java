package reins.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reins.config.DecsAlgConfig;

@Component
public class TimeUtil {
    @Autowired
    DecsAlgConfig decsAlgConfig;

    public long getCurrentAbsoluteMinute(){
        return System.currentTimeMillis() / 1000 / (60 * 60);
    }

    public long getCurrentTimeWindow(){
        return minuteToKey(getCurrentAbsoluteMinute());
    }

    public long minuteToKey(long minute){
        return minute / decsAlgConfig.TIME_WINDOW;
    }
}
