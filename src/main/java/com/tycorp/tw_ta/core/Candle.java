package com.tycorp.tw_ta.core;

import lombok.*;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString
public class Candle {

    private double open;
    private double high;
    private double low;
    private double close;

    private ZonedDateTime startTimeZdt;

}
