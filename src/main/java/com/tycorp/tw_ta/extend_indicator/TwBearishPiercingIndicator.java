package com.tycorp.tw_ta.extend_indicator;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;

/**
 * Implementation for bearish piercing candle pattern
 */
public class TwBearishPiercingIndicator extends CachedIndicator<Boolean> {

    private OpenPriceIndicator openPriceI;
    private ClosePriceIndicator closePriceI;

    protected TwBearishPiercingIndicator(BarSeries barSeries) {
        super(barSeries);
        openPriceI = new OpenPriceIndicator(barSeries);
        closePriceI = new ClosePriceIndicator(barSeries);
    }

    @Override
    protected Boolean calculate(int index) {
        if(index < 1 || closePriceI.getBarSeries().getBarCount() < 1) {
            return false;
        }

        double yesterdayClose = closePriceI.getValue(index - 1).doubleValue();

        boolean openHigherThenOrEqualYesterdayClose = openPriceI.getValue(index).doubleValue() >= yesterdayClose;
        boolean closeLowerThenYesterdayClose = closePriceI.getValue(index).doubleValue() < yesterdayClose;

        return openHigherThenOrEqualYesterdayClose && closeLowerThenYesterdayClose;
    }

}
