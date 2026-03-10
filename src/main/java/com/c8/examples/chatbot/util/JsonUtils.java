package com.c8.examples.chatbot.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.c8.examples.chatbot.exception.TechnicalException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtils {

  private JsonUtils() {}

  private static ObjectMapper mapper;
  private static ObjectMapper mapperWithoutNull;

  public static Object eventuallyJsonNode(String someString) {
    try {
      return toJsonNode(someString);
    } catch (IOException e) {
      return someString;
    }
  }

  public static ObjectNode newObjectNode() {
    return getObjectMapper().createObjectNode();
  }

  public static JsonNode toJsonNode(InputStream is) throws IOException {
    return getObjectMapper().readTree(is);
  }

  public static JsonNode toJsonNode(String json) throws IOException {
    return getObjectMapper().readTree(json);
  }

  public static <T> T toObject(String json, Class<T> type) {
    try {
      return getObjectMapper().readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new TechnicalException("can't process " + json, e);
    }
  }

  public static <T> T toObject(InputStream json, Class<T> type) throws IOException {
    return toObject(new String(json.readAllBytes()), type);
  }

  public static <T> T toParametrizedObject(InputStream json, TypeReference<T> type)
      throws IOException {
    return toParametrizedObject(new String(json.readAllBytes()), type);
  }

  public static <T> T toParametrizedObject(String json, TypeReference<T> type) {
    try {
      return getObjectMapper().readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new TechnicalException(e);
    }
  }

  public static String toJsonWithoutNull(Object object) throws IOException {
    return getObjectMapperWithoutNull().writeValueAsString(object);
  }

  public static String toJson(Object object) throws IOException {
    return getObjectMapper().writeValueAsString(object);
  }

  public static void toJsonFile(Path path, Object object) throws IOException {
    if (!Files.exists(path.getParent())) {
      Files.createDirectories(path.getParent());
    }
    getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), object);
  }

  public static <T> T fromJsonFile(Path path, Class<T> type) {
    try {
      return getObjectMapper().readValue(path.toFile(), type);
    } catch (IOException e) {
      throw new TechnicalException(e);
    }
  }

  public static <T> T fromJsonFile(Path path, TypeReference<T> type) {
    try {
      return getObjectMapper().readValue(path.toFile(), type);
    } catch (IOException e) {
      throw new TechnicalException(e);
    }
  }

  private static ObjectMapper getObjectMapper() {
    if (mapper == null) {
      mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    return mapper;
  }

  private static ObjectMapper getObjectMapperWithoutNull() {
    if (mapperWithoutNull == null) {
      mapperWithoutNull = new ObjectMapper();
      mapperWithoutNull.enable(SerializationFeature.INDENT_OUTPUT);
      mapperWithoutNull.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      mapperWithoutNull.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    return mapperWithoutNull;
  }
}
