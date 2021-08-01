package tycorp.eb.script;

import lombok.*;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString
public class EbCandle {

    private double open;
    private double high;
    private double low;
    private double close;

    private ZonedDateTime startTimeZdt;

}
