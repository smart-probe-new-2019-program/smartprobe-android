package com.test.smartprobe.model;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import java.util.List;

/**
 * Created by abitha on 30/6/16.
 */
@JsonObject
public class JSONDetails {

    @JsonField
    public String result;

    @JsonField
    public List<Probe> probes;

    @JsonField
    public String curdate;

}
