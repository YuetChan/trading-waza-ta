package com.tycorp.tw_ta.extend_indicator;

import org.ta4j.core.indicators.CachedIndicator;

/**
 * Implementation for consensio indicator
 */
public class GoldenCrossIndicator extends CachedIndicator<Boolean> {

    private final TwSMAIndicator up;
    private final TwSMAIndicator middle;
    private final TwSMAIndicator low;

    public GoldenCrossIndicator(TwSMAIndicator up, TwSMAIndicator middle, TwSMAIndicator low) {
        super(up);
        this.up = up;
        this.middle = middle;
        this.low = low;

        boolean areMAsAligned = up.getBarCount() < middle.getBarCount()
                && middle.getBarCount() < low.getBarCount();
        if(!areMAsAligned) {
            throw new IllegalArgumentException("All MAs are not aligned ");
        }
    }

    @Override
    protected Boolean calculate(int index) {
        int i = index;
        if (i == 0) {
            return false;
        }

        int head = i;
        int tail = i - 1;

        boolean consensioed = up.getValue(tail).isGreaterThan(middle.getValue(tail))
                && middle.getValue(tail).isGreaterThan(low.getValue(tail));
        boolean consensio = up.getValue(head).isGreaterThan(middle.getValue(head))
                && middle.getValue(head).isGreaterThan(low.getValue(head));

        if(!consensioed && consensio) {
            return true;
        }

        return false;
    }
}
