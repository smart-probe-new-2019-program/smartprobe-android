package com.test.smartprobe.model;

import com.bluelinelabs.logansquare.JsonMapper;
import com.bluelinelabs.logansquare.LoganSquare;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unsafe,unchecked")
public final class JSONDetails$$JsonObjectMapper extends JsonMapper<JSONDetails> {
  private static final JsonMapper<Probe> COM_TEST_SMARTPROBE_MODEL_PROBE__JSONOBJECTMAPPER = LoganSquare.mapperFor(Probe.class);

  @Override
  public JSONDetails parse(JsonParser jsonParser) throws IOException {
    JSONDetails instance = new JSONDetails();
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
  public void parseField(JSONDetails instance, String fieldName, JsonParser jsonParser) throws IOException {
    if ("curdate".equals(fieldName)) {
      instance.curdate = jsonParser.getValueAsString(null);
    } else if ("probes".equals(fieldName)) {
      if (jsonParser.getCurrentToken() == JsonToken.START_ARRAY) {
        ArrayList<Probe> collection1 = new ArrayList<Probe>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
          Probe value1;
          value1 = COM_TEST_SMARTPROBE_MODEL_PROBE__JSONOBJECTMAPPER.parse(jsonParser);
          collection1.add(value1);
        }
        instance.probes = collection1;
      } else {
        instance.probes = null;
      }
    } else if ("result".equals(fieldName)) {
      instance.result = jsonParser.getValueAsString(null);
    }
  }

  @Override
  public void serialize(JSONDetails object, JsonGenerator jsonGenerator, boolean writeStartAndEnd) throws IOException {
    if (writeStartAndEnd) {
      jsonGenerator.writeStartObject();
    }
    if (object.curdate != null) {
      jsonGenerator.writeStringField("curdate", object.curdate);
    }
    final List<Probe> lslocalprobes = object.probes;
    if (lslocalprobes != null) {
      jsonGenerator.writeFieldName("probes");
      jsonGenerator.writeStartArray();
      for (Probe element1 : lslocalprobes) {
        if (element1 != null) {
          COM_TEST_SMARTPROBE_MODEL_PROBE__JSONOBJECTMAPPER.serialize(element1, jsonGenerator, true);
        }
      }
      jsonGenerator.writeEndArray();
    }
    if (object.result != null) {
      jsonGenerator.writeStringField("result", object.result);
    }
    if (writeStartAndEnd) {
      jsonGenerator.writeEndObject();
    }
  }
}
