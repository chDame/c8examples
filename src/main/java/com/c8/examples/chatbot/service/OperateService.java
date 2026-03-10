package com.c8.examples.chatbot.service;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.c8.examples.chatbot.util.JsonUtils;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
@EnableCaching
public class OperateService {

  private static final Logger LOG = LoggerFactory.getLogger(OperateService.class);

  @Autowired private CamundaClient camundaClient;

  public SearchResponse<ProcessInstance> getProcessInstances(
      String bpmnProcessId, ProcessInstanceState state, Integer pageSize, String after) {
    try {
      var query =
          camundaClient
              .newProcessInstanceSearchRequest()
              .filter(
                  f -> {
                    f.processDefinitionId(bpmnProcessId);
                    if (state != null) {
                      f.state(state);
                    }
                  })
              .page(
                  p -> {
                    p.limit(pageSize);
                    if (after != null) {
                      p.after(after);
                    }
                  });
      return query.send().join();
    } catch (Exception e) {
      LOG.error("Error fetching process instances", e);
      return null;
    }
  }

  public List<Variable> getVariables(Long processInstanceKey) {
    try {
      SearchResponse<Variable> response =
          camundaClient
              .newVariableSearchRequest()
              .filter(f -> f.processInstanceKey(processInstanceKey).scopeKey(processInstanceKey))
              .page(p -> p.limit(200))
              .send()
              .join();
      return response.items();
    } catch (Exception e) {
      LOG.error("Error fetching variables for process instance " + processInstanceKey, e);
      return new ArrayList<>();
    }
  }

  public Map<String, Object> getVariablesAsMap(Long processInstanceKey) {
    return mapVariables(getVariables(processInstanceKey));
  }

  public Map<String, Object> mapVariables(List<Variable> variables) {
    try {
      Map<String, Object> result = new HashMap<>();
      for (Variable var : variables) {
        try {
          JsonNode nodeValue = JsonUtils.toJsonNode(var.getValue());

          if (nodeValue.canConvertToLong()) {
            result.put(var.getName(), nodeValue.asLong());
          } else if (nodeValue.isBoolean()) {
            result.put(var.getName(), nodeValue.asBoolean());
          } else if (nodeValue.isTextual()) {
            result.put(var.getName(), nodeValue.textValue());
          } else if (nodeValue.isArray()) {
            result.put(
                var.getName(),
                JsonUtils.toParametrizedObject(var.getValue(), new TypeReference<List<?>>() {}));
          } else {
            result.put(
                var.getName(),
                JsonUtils.toParametrizedObject(
                    var.getValue(), new TypeReference<Map<String, Object>>() {}));
          }
        } catch (JsonEOFException e) {
          // variable is too huge and truncated.
        }
      }
      return result;
    } catch (IOException e) {
      LOG.error("Error mapping variables", e);
      return new HashMap<>();
    }
  }

  public Map<Long, Map<String, Object>> getVariables(List<ProcessInstance> processInstances) {
    try {
      Map<Long, Future<Map<String, Object>>> futures = new HashMap<>();
      Map<Long, Map<String, Object>> instanceMap = new HashMap<>();
      for (ProcessInstance instance : processInstances) {
        futures.put(
            instance.getProcessInstanceKey(),
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return getVariablesAsMap(instance.getProcessInstanceKey());
                  } catch (Exception e) {
                    return null;
                  }
                }));
      }
      for (Map.Entry<Long, Future<Map<String, Object>>> varFutures : futures.entrySet()) {
        Map<String, Object> vars = varFutures.getValue().get();
        instanceMap.put(varFutures.getKey(), vars);
      }
      futures.clear();
      return instanceMap;
    } catch (ExecutionException | InterruptedException e) {
      LOG.error("Error loading instances variables", e);
      return new HashMap<>();
    }
  }

}
