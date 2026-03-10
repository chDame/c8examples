package com.c8.examples.chatbot.service;

import com.c8.examples.chatbot.model.RspSearchResponse;
import com.c8.examples.chatbot.model.Task;
import com.c8.examples.chatbot.model.TaskSearch;
import com.c8.examples.chatbot.util.JsonUtils;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.Form;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.search.response.Variable;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class TaskListService {

  @Autowired private CamundaClient camundaClient;

  public Task getTask(Long userTaskKey) {
    try {
      UserTask item = camundaClient.newUserTaskGetRequest(userTaskKey).send().join();
      return convert(item);
    } catch (Exception e) {
      return null;
    }
  }

  public SearchResponse<Task> getTasks(TaskSearch taskSearch) {
    try {
      SearchResponse<UserTask> response =
          camundaClient
              .newUserTaskSearchRequest()
              .filter(
                  f -> {
                    if (taskSearch.getProcessInstanceKey() != null) {
                      f.processInstanceKey(taskSearch.getProcessInstanceKey());
                    }
                    if (taskSearch.getState() != null) {
                      f.state(UserTaskState.valueOf(taskSearch.getState()));
                    }
                    if (taskSearch.getTaskDefinitionId() != null) {
                      f.elementId(taskSearch.getTaskDefinitionId());
                    }
                    if (taskSearch.getAssignee() != null
                        && StringUtils.isNotBlank(taskSearch.getAssignee())) {
                      f.assignee(taskSearch.getAssignee());
                    }
                    if (taskSearch.getGroup() != null
                        && StringUtils.isNotBlank(taskSearch.getGroup())) {
                      f.candidateGroup(taskSearch.getGroup());
                    }
                    if (taskSearch.getFilterVariables() != null
                        && !taskSearch.getFilterVariables().isEmpty()) {
                      // for(Map.Entry<String, Object> filter :
                      // taskSearch.getFilterVariables().entrySet()) {
                      f.localVariables(taskSearch.getFilterVariables());
                      // }
                    }
                    // TODO: Variable filters from taskSearch.getFilterVariables() would need custom
                    // implementation
                  })
              .sort(s -> s.creationDate().asc())
              .page(
                  p -> {
                    p.limit(taskSearch.getPageSize() != null ? taskSearch.getPageSize() : 50);

                    if (taskSearch.getAfter() != null) {
                      p.after(taskSearch.getAfter());
                    }
                  })
              .send()
              .join();
      return fetchVariables(response);
    } catch (Exception e) {
      return null;
    }
  }

  public Map<String, Object> getVariables(Task task) {
    try {
      SearchResponse<Variable> processInstanceVariables =
          camundaClient
              .newVariableSearchRequest()
              .filter(f -> f.processInstanceKey(task.getProcessInstanceKey()))
              .page(p -> p.limit(200))
              .send()
              .join();
      SearchResponse<Variable> taskVariables =
          camundaClient
              .newVariableSearchRequest()
              .filter(f -> f.scopeKey(task.getElementInstanceKey()))
              .page(p -> p.limit(200))
              .send()
              .join();
      List<Variable> variables = new ArrayList<>();
      if (processInstanceVariables != null && processInstanceVariables.items() != null) {
        variables.addAll(processInstanceVariables.items());
      }
      if (taskVariables != null && taskVariables.items() != null) {
        variables.addAll(taskVariables.items());
      }
      return mapVariables(variables);
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  public SearchResponse<Task> getTasks(UserTaskState state, Integer pageSize) {
    try {
      SearchResponse<UserTask> response =
          camundaClient
              .newUserTaskSearchRequest()
              .filter(f -> f.state(state))
              .page(p -> p.limit(pageSize != null ? pageSize : 50))
              .send()
              .join();
      return fetchVariables(response);
    } catch (Exception e) {
      return null;
    }
  }

  public void claim(Long userTaskKey, String assignee) {
    try {
      camundaClient
          .newAssignUserTaskCommand(userTaskKey)
          .assignee(assignee)
          .allowOverride(true)
          .send()
          .join();
    } catch (Exception e) {
      // Log error
    }
  }

  public void unclaim(Long userTaskKey) {
    try {
      camundaClient.newUnassignUserTaskCommand(userTaskKey).send().join();
    } catch (Exception e) {
      // Log error
    }
  }

  public void completeTask(Long userTaskKey, Map<String, Object> variables) {
    try {
      camundaClient.newCompleteUserTaskCommand(userTaskKey).variables(variables).send().join();
    } catch (Exception e) {
      // Log error
    }
  }

  public String getInitForm(Long processDefinitionKey) {
    Form f = camundaClient.newProcessDefinitionGetFormRequest(processDefinitionKey).send().join();
    return (String) f.getSchema();
  }

  public String getForm(Long urerTaskKey) {
    Form f = camundaClient.newUserTaskGetFormRequest(urerTaskKey).send().join();
    return (String) f.getSchema();
  }

  private Task convert(UserTask item) {
    if (item == null) {
      return null;
    }
    Task result = new Task();
    BeanUtils.copyProperties(item, result);
    return result;
  }

  public Map<String, Object> mapVariables(List<Variable> variables) throws IOException {
    Map<String, Object> result = new HashMap<>();
    for (Variable var : variables) {
      if (!var.isTruncated()) {
        result.put(var.getName(), JsonUtils.toJsonNode(var.getValue()));
      }
    }
    return result;
  }

  private SearchResponse<Task> fetchVariables(SearchResponse<UserTask> response) {
    Map<Long, Future<Boolean>> futures = new HashMap<>();
    List<Task> result = new ArrayList<>();
    for (UserTask item : response.items()) {
      Task task = convert(item);
      result.add(task);
      futures.put(
          task.getUserTaskKey(),
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  task.setVariables(getVariables(task));
                  return true;
                } catch (Exception e) {
                  return false;
                }
              }));
    }
    for (Map.Entry<Long, Future<Boolean>> loadingFuture : futures.entrySet()) {
      try {
        loadingFuture.getValue().get();
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    return new RspSearchResponse<>(result, response.page());
  }

  public List<Task> getTasks(List<Long> processInstances, String state) {
    try {
      Map<Long, Future<List<Task>>> futures = new HashMap<>();
      List<Task> tasks = new ArrayList<>();
      for (Long instanceKey : processInstances) {
        futures.put(
            instanceKey,
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    SearchResponse<Task> response =
                        getTasks(
                            new TaskSearch().setState(state).setProcessInstanceKey(instanceKey));
                    if (response != null && response.items() != null) {
                      return response.items();
                    }
                    return null;
                  } catch (Exception e) {
                    return null;
                  }
                }));
      }
      for (Map.Entry<Long, Future<List<Task>>> tasksFutures : futures.entrySet()) {
        List<Task> futTasks = tasksFutures.getValue().get();
        if (futTasks != null) {
          tasks.addAll(futTasks);
        }
      }
      futures.clear();
      return tasks;
    } catch (ExecutionException | InterruptedException e) {
      return new ArrayList<>();
    }
  }
}
