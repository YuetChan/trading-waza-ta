package tycorp.eb.script;

import java.time.*;

public class EbDateTimeHelper {

    public static Long zonedDateTimeToEpoch(ZonedDateTime zdt){
        Instant instant = zdt.toInstant();
        return instant.toEpochMilli();
    }

}
