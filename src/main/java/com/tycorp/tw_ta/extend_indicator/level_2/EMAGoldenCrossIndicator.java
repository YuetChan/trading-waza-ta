package com.tycorp.tw_ta.extend_indicator.level_2;

import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;

/**
 * Implementation for EMA golden cross
 */
public class EMAGoldenCrossIndicator extends CachedIndicator<Boolean> {

  private final EMAIndicator up;
  private final EMAIndicator low;

  public EMAGoldenCrossIndicator(EMAIndicator up, EMAIndicator low) {
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
