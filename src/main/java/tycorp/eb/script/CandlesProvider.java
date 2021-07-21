package tycorp.eb.script;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.client.TdaClient;
import com.studerw.tda.model.history.Candle;
import com.studerw.tda.model.history.FrequencyType;
import com.studerw.tda.model.history.PriceHistReq;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static tycorp.eb.script.DateTimeHelper.getYesterdayMidNightZdt;
import static tycorp.eb.script.DateTimeHelper.zonedDateTimeToEpoch;

public class CandlesProvider {

    private static TdaClient CLIENT = new HttpTdaClient();

    private static FrequencyType DEFAULT_FREQUENCY_TYPE = FrequencyType.minute;
    private static int DEFAULT_FREQUENCY = 5;

    private static int DEFAULT_START_DATE = 730;
    private static ZonedDateTime getDefaultStartDate(){
        return ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/New_York"))
                            .minusDays(DEFAULT_START_DATE);
    }

    public static List<Candle> getCandlesSinceDefaultStartDate(String ticker){
        var request = PriceHistReq.Builder.priceHistReq()
                .withSymbol(ticker)
                .withStartDate(zonedDateTimeToEpoch(getDefaultStartDate()))
                .withExtendedHours(false)
                .withFrequencyType(DEFAULT_FREQUENCY_TYPE)
                .withFrequency(DEFAULT_FREQUENCY)
                .build();

        return  CLIENT.priceHistory(request).getCandles();
    }

    public static List<Candle> getCandlesSinceYesterday(String ticker){
        var request = PriceHistReq.Builder.priceHistReq()
                .withSymbol(ticker)
                .withStartDate(zonedDateTimeToEpoch(getYesterdayMidNightZdt()))
                .withExtendedHours(false)
                .withFrequencyType(DEFAULT_FREQUENCY_TYPE)
                .withFrequency(DEFAULT_FREQUENCY)
                .build();

        return CLIENT.priceHistory(request).getCandles();
    }

}
