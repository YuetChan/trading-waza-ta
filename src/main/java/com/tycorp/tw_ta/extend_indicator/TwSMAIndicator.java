package com.tycorp.tw_ta.extend_indicator;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.num.Num;

/**
 * Implementation for SMA indicator
 * This implementation provides bar count in contrast to SMAIndicator
 */
public class TwSMAIndicator extends SMAIndicator {

    private final int barCount;

    public TwSMAIndicator(Indicator<Num> indicator, int barCount) {
        super(indicator, barCount);
        this.barCount = barCount;
    }

    public int getBarCount(){
        return this.barCount;
    }

}
