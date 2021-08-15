package tycorp.eb.lib;

import java.time.*;

public class DefaultDateTimeHelper {

    public static Long zonedDateTimeToEpoch(ZonedDateTime zdt) {
        return zdt.toInstant().toEpochMilli();
    }

    public static ZonedDateTime truncateTime(ZonedDateTime zdt) {
        var zoneId  = zdt.getZone();
        return zdt.toLocalDate().atStartOfDay(zoneId);
    }

}
