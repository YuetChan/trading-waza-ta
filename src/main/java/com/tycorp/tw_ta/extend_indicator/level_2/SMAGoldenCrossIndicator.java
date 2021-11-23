package com.tycorp.tw_ta.extend_indicator.level_2;

import com.tycorp.tw_ta.extend_indicator.level_1.TwSMAIndicator;
import org.ta4j.core.indicators.CachedIndicator;

/**
 * Implementation for golden cross indicator
 */
public class SMAGoldenCrossIndicator extends CachedIndicator<Boolean> {

    private final TwSMAIndicator up;
    private final TwSMAIndicator low;

    public SMAGoldenCrossIndicator(TwSMAIndicator up, TwSMAIndicator low) {
        super(up);
        this.up = up;
        this.low = low;

        boolean areCountsAligned = up.getBarCount() < low.getBarCount();
        if(!areCountsAligned) {
            throw new IllegalArgumentException("Incorrect bar count");
        }
    }

    @Override
    protected Boolean calculate(int i) {
        if (i == 0) {
            return false;
        }

        int curr = i;
        int prev = i - 1;

        boolean crossed = up.getValue(prev).isGreaterThan(low.getValue(prev));
        boolean justCrossed = up.getValue(curr).isGreaterThan(low.getValue(curr));

        if(!crossed && justCrossed) {
            return true;
        }

        return false;
    }
}
