package tycorp.eb.script;

import java.time.*;

public class DateTimeHelper {

    public static Long zonedDateTimeToEpoch(ZonedDateTime zdt){
        Instant instant = zdt.toInstant();
        return instant.toEpochMilli();
    }

    public static ZonedDateTime getNthDayAgoMidNightZdt(int days){ return getYesterdayMidNightZdt().minusDays(days); }
    public static ZonedDateTime getYesterdayMidNightZdt(){
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneId.of("America/New_York"));
        LocalDateTime todayMidnight = LocalDateTime.of(today, midnight);
        return todayMidnight.minusDays(1).atZone(ZoneId.of("America/New_York"));
    }

    public static ZonedDateTime getTodayMidNightZdt(){ return getYesterdayMidNightZdt().plusDays(1); }

}
