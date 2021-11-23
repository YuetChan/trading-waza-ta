package com.tycorp.tw_ta.extend_indicator.level_1;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.num.Num;

public class TwEMAIndicator extends EMAIndicator {

    private final int barCount;

    public TwEMAIndicator(Indicator<Num> indicator, int barCount) {
        super(indicator, barCount);
        this.barCount = barCount;
    }

    public int getBarCount(){
        return this.barCount;
    }
}
