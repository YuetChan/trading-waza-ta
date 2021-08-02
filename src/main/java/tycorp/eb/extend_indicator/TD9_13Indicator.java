package tycorp.eb.extend_indicator;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class TD9_13Indicator extends CachedIndicator<Integer> {

    private enum Setup { BULLISH, BEARISH }
    private ClosePriceIndicator closePriceIndicator;

    public TD9_13Indicator(BarSeries barSeries) {
        super(barSeries);
        closePriceIndicator = new ClosePriceIndicator(barSeries);
    }

    @Override
    protected Integer calculate(int index) {
        if(index < 5 || closePriceIndicator.getBarSeries().getBarCount() < 6){
            return 0;
        }

        var initSetup =
                closePriceIndicator.getValue(4).isGreaterThan(closePriceIndicator.getValue(0))
                ? Setup.BULLISH : Setup.BEARISH;
        var currSetup = initSetup;

        var i = 5;
        while(currSetup.equals(initSetup) && i <= index){
            currSetup = closePriceIndicator.getValue(i).isGreaterThan(closePriceIndicator.getValue(i - 4))
                    ? Setup.BULLISH : Setup.BEARISH;
            i ++;
        }

        if(currSetup.equals(initSetup)){
            return 0;
        }

        var currCount = currSetup.equals(Setup.BULLISH) ? 1 : -1;
        while(i <= index){
            var newSetup = closePriceIndicator.getValue(i).isGreaterThan(closePriceIndicator.getValue(i - 4))
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
