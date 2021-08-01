package tycorp.eb.extend_indicator;

import org.ta4j.core.indicators.CachedIndicator;

public class EbConsensioCrossIndicator extends CachedIndicator<Boolean> {

    private final EbSMAIndicator up;
    private final EbSMAIndicator middle;
    private final EbSMAIndicator low;

    public EbConsensioCrossIndicator(EbSMAIndicator up, EbSMAIndicator middle, EbSMAIndicator low) {
        super(up);
        this.up = up;
        this.middle = middle;
        this.low = low;

        var areMAsAligned = up.getBarCount() < middle.getBarCount()
                && middle.getBarCount() < low.getBarCount();
        if(!areMAsAligned) {
            throw new IllegalArgumentException("All MAs are not aligned ");
        }
    }

    @Override
    protected Boolean calculate(int index) {
        var i = index;
        if (i == 0) {
            return false;
        }

        var head = i;
        var tail = i - 1;

        var consensioed = up.getValue(tail).isGreaterThan(middle.getValue(tail))
                && middle.getValue(tail).isGreaterThan(low.getValue(tail));
        var consensio = up.getValue(head).isGreaterThan(middle.getValue(head))
                && middle.getValue(head).isGreaterThan(low.getValue(head));

        if(!consensioed && consensio) {
            return true;
        }

        return false;
    }
}
