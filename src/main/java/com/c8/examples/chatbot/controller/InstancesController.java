package com.c8.examples.chatbot.controller;

import com.c8.examples.chatbot.exception.OperateException;
import com.c8.examples.chatbot.model.ProcessInstancesResult;
import com.c8.examples.chatbot.service.OperateService;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/instances")
public class InstancesController {

  private static final Logger LOG = LoggerFactory.getLogger(InstancesController.class);
  private final OperateService operateService;

  public InstancesController(
      OperateService operateService) {
    this.operateService = operateService;
  }

  @GetMapping("/{bpmnProcessId}/{state}")
  public ProcessInstancesResult getProcessInstances(
      @PathVariable String bpmnProcessId,
      @PathVariable ProcessInstanceState state,
      @RequestParam(required = false) Integer pageSize,
      @RequestParam(required = false) String after)
      throws OperateException {
    if (pageSize == null) {
      pageSize = 10;
    }
    SearchResponse<ProcessInstance> result =
        operateService.getProcessInstances(bpmnProcessId, state, pageSize, after);
    return new ProcessInstancesResult(result, operateService.getVariables(result.items()));
  }
}
