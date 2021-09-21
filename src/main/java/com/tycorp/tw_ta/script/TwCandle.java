package com.tycorp.tw_ta.script;

import lombok.*;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString
public class TwCandle {

    private double open;
    private double high;
    private double low;
    private double close;

    private ZonedDateTime startTimeZdt;

}
