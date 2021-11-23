package com.tycorp.tw_ta.extend_indicator.level_2;

import com.tycorp.tw_ta.extend_indicator.level_1.TwSMAIndicator;
import org.ta4j.core.indicators.CachedIndicator;

public class ConsensioIndicator extends CachedIndicator<Boolean>  {

    private final TwSMAIndicator up;
    private final TwSMAIndicator middle;
    private final TwSMAIndicator low;

    public ConsensioIndicator(TwSMAIndicator up, TwSMAIndicator middle, TwSMAIndicator low) {
        super(up);
        this.up = up;
        this.middle = middle;
        this.low = low;

        boolean areCountsAligned = up.getBarCount() < middle.getBarCount()
                && middle.getBarCount() < low.getBarCount();
        if(!areCountsAligned) {
            throw new IllegalArgumentException("All bar counts are not aligned ");
        }
    }

    @Override
    protected Boolean calculate(int i) {
        if (i == 0) {
            return false;
        }

        boolean consensio = up.getValue(i).isGreaterThan(middle.getValue(i))
                && middle.getValue(i).isGreaterThan(low.getValue(i));

        if(consensio) {
            return true;
        }

        return false;
    }
}
