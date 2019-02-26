package com.test.smartprobe.model;

import com.bluelinelabs.logansquare.JsonMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

@SuppressWarnings("unsafe,unchecked")
public final class Probe$$JsonObjectMapper extends JsonMapper<Probe> {
  @Override
  public Probe parse(JsonParser jsonParser) throws IOException {
    Probe instance = new Probe();
    if (jsonParser.getCurrentToken() == null) {
      jsonParser.nextToken();
    }
    if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
      jsonParser.skipChildren();
      return null;
    }
    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      String fieldName = jsonParser.getCurrentName();
      jsonParser.nextToken();
      parseField(instance, fieldName, jsonParser);
      jsonParser.skipChildren();
    }
    return instance;
  }

  @Override
  public void parseField(Probe instance, String fieldName, JsonParser jsonParser) throws IOException {
    if ("alertHighLimit".equals(fieldName)) {
      instance.alertHighLimit = jsonParser.getValueAsString(null);
    } else if ("alertLowLimit".equals(fieldName)) {
      instance.alertLowLimit = jsonParser.getValueAsString(null);
    } else if ("defaultsensor".equals(fieldName)) {
      instance.defaultsensor = jsonParser.getValueAsString(null);
    } else if ("probe_name".equals(fieldName)) {
      instance.probe_name = jsonParser.getValueAsString(null);
    } else if ("samplePeriodUnits".equals(fieldName)) {
      instance.samplePeriodUnits = jsonParser.getValueAsString(null);
    } else if ("serialNumber".equals(fieldName)) {
      instance.serialNumber = jsonParser.getValueAsString(null);
    } else if ("voltagemin".equals(fieldName)) {
      instance.voltagemin = jsonParser.getValueAsString(null);
    } else if ("warningHighLimit".equals(fieldName)) {
      instance.warningHighLimit = jsonParser.getValueAsString(null);
    } else if ("warningLowLimit".equals(fieldName)) {
      instance.warningLowLimit = jsonParser.getValueAsString(null);
    }
  }

  @Override
  public void serialize(Probe object, JsonGenerator jsonGenerator, boolean writeStartAndEnd) throws IOException {
    if (writeStartAndEnd) {
      jsonGenerator.writeStartObject();
    }
    if (object.alertHighLimit != null) {
      jsonGenerator.writeStringField("alertHighLimit", object.alertHighLimit);
    }
    if (object.alertLowLimit != null) {
      jsonGenerator.writeStringField("alertLowLimit", object.alertLowLimit);
    }
    if (object.defaultsensor != null) {
      jsonGenerator.writeStringField("defaultsensor", object.defaultsensor);
    }
    if (object.probe_name != null) {
      jsonGenerator.writeStringField("probe_name", object.probe_name);
    }
    if (object.samplePeriodUnits != null) {
      jsonGenerator.writeStringField("samplePeriodUnits", object.samplePeriodUnits);
    }
    if (object.serialNumber != null) {
      jsonGenerator.writeStringField("serialNumber", object.serialNumber);
    }
    if (object.voltagemin != null) {
      jsonGenerator.writeStringField("voltagemin", object.voltagemin);
    }
    if (object.warningHighLimit != null) {
      jsonGenerator.writeStringField("warningHighLimit", object.warningHighLimit);
    }
    if (object.warningLowLimit != null) {
      jsonGenerator.writeStringField("warningLowLimit", object.warningLowLimit);
    }
    if (writeStartAndEnd) {
      jsonGenerator.writeEndObject();
    }
  }
}
