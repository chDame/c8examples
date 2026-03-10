package com.c8.examples.chatbot.model;

import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Task implements UserTask {
  private Long userTaskKey;
  private String name;
  private UserTaskState state;
  private String assignee;
  private String elementId;
  private Long elementInstanceKey;
  private List<String> candidateGroups;
  private List<String> candidateUsers;
  private String bpmnProcessId;
  private Long processDefinitionKey;
  private Long processInstanceKey;
  private Long formKey;
  private OffsetDateTime creationDate;
  private OffsetDateTime completionDate;
  private OffsetDateTime followUpDate;
  private OffsetDateTime dueDate;
  private String tenantId;
  private String externalFormReference;
  private Integer processDefinitionVersion;
  private Map<String, String> customHeaders;
  private Integer priority;

  private Map<String, Object> variables;
}
