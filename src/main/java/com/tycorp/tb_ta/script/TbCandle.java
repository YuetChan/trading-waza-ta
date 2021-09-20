package com.tycorp.tb_ta.script;

import lombok.*;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
@ToString
public class TbCandle {

    private double open;
    private double high;
    private double low;
    private double close;

    private ZonedDateTime startTimeZdt;

}
