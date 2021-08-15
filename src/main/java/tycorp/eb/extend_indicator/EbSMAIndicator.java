package tycorp.eb.extend_indicator;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.num.Num;

public class EbSMAIndicator extends SMAIndicator {

    private final int barCount;

    public EbSMAIndicator(Indicator<Num> indicator, int barCount) {
        super(indicator, barCount);
        this.barCount = barCount;
    }

    public int getBarCount(){ return this.barCount; }

}
