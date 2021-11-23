package com.tycorp._ta.extend_indicator.level_2;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.SMAIndicator;

/**
 * Implementation for SMA golden cross
 */
public class SMAGoldenCrossIndicator extends CachedIndicator<Boolean> {

    private final SMAIndicator up;
    private final SMAIndicator low;

    public SMAGoldenCrossIndicator(SMAIndicator up, SMAIndicator low) {
        super(up);
        this.up = up;
        this.low = low;
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
