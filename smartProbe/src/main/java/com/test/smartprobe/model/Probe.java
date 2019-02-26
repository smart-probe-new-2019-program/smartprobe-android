package com.test.smartprobe.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

/**
 * Created by abitha on 30/6/16.
 */
@JsonObject
public class Probe {

    @JsonField
    public String serialNumber;

    @JsonField
    public String probe_name;

    @JsonField
    public String alertHighLimit;

    @JsonField
    public String alertLowLimit;

    @JsonField
    public String warningHighLimit;

    @JsonField
    public String warningLowLimit;

    @JsonField
    public String samplePeriodUnits;

    @JsonField
    public String voltagemin;

    @JsonField
    public String defaultsensor;
}
