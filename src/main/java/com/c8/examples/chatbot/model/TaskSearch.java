package com.c8.examples.chatbot.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Accessors(chain = true)
@Getter
@Setter
public class TaskSearch {

  private Boolean assigned;

  private String state;

  private String assignee;

  private String group;

  private Map<String, Object> filterVariables;

  private Integer pageSize;

  private Integer page;

  private String after;

  private String processDefinitionKey;

  private String taskDefinitionId;

  private Long processInstanceKey;

  private List<String> fetchVariables;
}
