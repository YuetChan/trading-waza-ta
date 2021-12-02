package com.tycorp.tw_ta.extend_indicator.level_2;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.SMAIndicator;

/**
 * Implementation for consensio
 */
public class ConsensioIndicator extends CachedIndicator<Boolean>  {

  private final SMAIndicator up;
  private final SMAIndicator middle;
  private final SMAIndicator low;

  public ConsensioIndicator(SMAIndicator up, SMAIndicator middle, SMAIndicator low) {
    super(up);
    this.up = up;
    this.middle = middle;
    this.low = low;
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
