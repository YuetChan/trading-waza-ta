package tycorp.eb.script;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;

import java.util.List;

public class SetupDetector {

    public static enum Setup { SMA_CROSSOVER, TD_9_AND_13, TD_YUET_VARIANCE }

    public static boolean isSatified(List<Setup> setups, BarSeries barSeries){
        Rule rule = null;

        for(var setup : setups){
            if(setup.equals(Setup.SMA_CROSSOVER)){
                var closePrice = new ClosePriceIndicator(barSeries);

                var sma20 = new SMAIndicator(closePrice, 20);
                var sma50 = new SMAIndicator(closePrice, 50);
                var sma200 = new SMAIndicator(closePrice, 200);

                rule = rule.and(new CrossedDownIndicatorRule(sma200, sma50))
                        .and(new CrossedDownIndicatorRule(sma200, sma20))
                        .and(new CrossedDownIndicatorRule(sma50, sma20));
            }
        }

        return rule.isSatisfied(barSeries.getEndIndex());
    }

}
