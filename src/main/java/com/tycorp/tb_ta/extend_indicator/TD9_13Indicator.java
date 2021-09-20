package com.tycorp.tb_ta.extend_indicator;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

/**
 * Implementation for Tom Demark indicator
 * Current implementation only supports Tom Demark set up
 */
public class TD9_13Indicator extends CachedIndicator<Integer> {

    private enum Setup {
        BULLISH, BEARISH
    }
    private ClosePriceIndicator closePriceI;

    public TD9_13Indicator(BarSeries barSeries) {
        super(barSeries);
        closePriceI = new ClosePriceIndicator(barSeries);
    }

    @Override
    protected Integer calculate(int index) {
        if(index < 5 || closePriceI.getBarSeries().getBarCount() < 6){
            return 0;
        }

        TD9_13Indicator.Setup initSetup =
                closePriceI.getValue(4).isGreaterThan(closePriceI.getValue(0))
                ? Setup.BULLISH : Setup.BEARISH;
        TD9_13Indicator.Setup currSetup = initSetup;

        int i = 5;
        while(currSetup.equals(initSetup) && i <= index) {
            currSetup = closePriceI.getValue(i).isGreaterThan(closePriceI.getValue(i - 4))
                    ? Setup.BULLISH : Setup.BEARISH;
            i ++;
        }

        if(currSetup.equals(initSetup)){
            return 0;
        }

        int currCount = currSetup.equals(Setup.BULLISH) ? 1 : -1;
        while(i <= index){
            TD9_13Indicator.Setup newSetup = closePriceI.getValue(i).isGreaterThan(closePriceI.getValue(i - 4))
                    ? Setup.BULLISH : Setup.BEARISH;
            if(currSetup.equals(newSetup)) {
                if(currSetup.equals(Setup.BULLISH)) {
                    currCount ++;
                    if(currCount == 10){
                        currCount = 0;
                    }
                }

                if(currSetup.equals(Setup.BEARISH)) {
                    currCount --;
                    if(currCount == -10){
                        currCount = 0;
                    }
                }

            }else {
                currSetup = newSetup;
                if(currSetup.equals(Setup.BULLISH)) {
                    currCount = 1;
                }

                if(currSetup.equals(Setup.BEARISH)) {
                    currCount = -1;
                }
            }
            i ++;
        }

        return currCount;
    }

}
