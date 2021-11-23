package com.tycorp.tw_ta.extend_indicator.level_2;

import com.tycorp.tw_ta.extend_indicator.level_1.TwEMAIndicator;
import org.ta4j.core.indicators.CachedIndicator;

public class EMAGoldenCrossIndicator extends CachedIndicator<Boolean> {

    private final TwEMAIndicator up;
    private final TwEMAIndicator low;

    public EMAGoldenCrossIndicator(TwEMAIndicator up, TwEMAIndicator low) {
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
