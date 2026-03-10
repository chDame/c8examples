package com.c8.examples.chatbot.model;

import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.SearchResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ProcessInstancesResult {
  private List<ProcessInstance> items;

  private Map<Long, Map<String, Object>> variables;

  private Long total;

  private String lastCursor;

  public ProcessInstancesResult() {}

  public ProcessInstancesResult(
      SearchResponse<ProcessInstance> result, Map<Long, Map<String, Object>> variables) {
    this.items = result.items();
    this.total = result.page().totalItems();
    this.lastCursor = result.page().endCursor();
    this.variables = variables;
  }
}
