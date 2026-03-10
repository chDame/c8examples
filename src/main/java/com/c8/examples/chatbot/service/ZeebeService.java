package com.c8.examples.chatbot.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.response.*;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.worker.JobHandler;
import io.camunda.client.api.worker.JobWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ZeebeService {

  @Autowired private CamundaClient camundaClient;

  public List<DocumentReferenceResponse> createFiles(List<MultipartFile> files) throws IOException {
    List<DocumentReferenceResponse> docs = new ArrayList<>();
    for (MultipartFile f : files) {
      docs.add(createFile(f));
    }
    return docs;
  }

  public DocumentReferenceResponse createFile(MultipartFile file) throws IOException {
    DocumentReferenceBatchResponse response =
        camundaClient
            .newCreateDocumentBatchCommand()
            .addDocument()
            .content(file.getInputStream())
            .contentType(file.getContentType())
            .fileName(file.getOriginalFilename())
            .timeToLive(Duration.ofDays(1))
            .done()
            .send()
            .join(); // currentFile.getSize()
    return response.getCreatedDocuments().getFirst();
  }

  public Map<String, Object> createFile(Path file) throws IOException {
    DocumentReferenceBatchResponse response =
        camundaClient
            .newCreateDocumentBatchCommand()
            .addDocument()
            .content(new FileInputStream(file.toFile()))
            .contentType(Files.probeContentType(file))
            .fileName(file.getFileName().toString())
            .timeToLive(Duration.ofDays(1))
            .done()
            .send()
            .join(); // currentFile.getSize()
    DocumentReferenceResponse createdDoc = response.getCreatedDocuments().getFirst();
    // create link
    String docPreviewUrl =
        camundaClient
            .newCreateDocumentLinkCommand(createdDoc.getDocumentId())
            .storeId(createdDoc.getStoreId())
            .contentHash(createdDoc.getContentHash())
            .timeToLive(Duration.ofHours(1))
            .send()
            .join()
            .getUrl();

    String key = file.getFileName().toString();
    key = key.substring(0, key.lastIndexOf("."));
    return Map.of(key, List.of(createdDoc), key + "PreviewUrl", docPreviewUrl);
  }

  public void handleFiles(Map<String, Object> variables, List<MultipartFile> files)
      throws IOException {
    // pickerKeys to be added to files and to reattach created doc to var.
    List<String> pickerKeys = (List<String>) variables.get("filesPickerKeys");
    // doc links for previews
    if (files != null) {
      int i = 0;
      for (MultipartFile currentFile : files) {
        DocumentReferenceBatchResponse response =
            camundaClient
                .newCreateDocumentBatchCommand()
                .addDocument()
                .content(currentFile.getInputStream())
                .contentType(currentFile.getContentType())
                .fileName(currentFile.getOriginalFilename())
                .timeToLive(Duration.ofDays(1))
                .done()
                .send()
                .join(); // currentFile.getSize()
        DocumentReferenceResponse createdDoc = response.getCreatedDocuments().getFirst();
        // create link
        String docPreviewUrl =
            camundaClient
                .newCreateDocumentLinkCommand(createdDoc.getDocumentId())
                .storeId(createdDoc.getStoreId())
                .contentHash(createdDoc.getContentHash())
                .timeToLive(Duration.ofHours(1))
                .send()
                .join()
                .getUrl();

        String pickerKey = pickerKeys.get(i++);
        createdDoc.getMetadata().getCustomProperties().put("pickerKey", pickerKey);
        updateFileVariable(variables, pickerKey, createdDoc, docPreviewUrl);
      }
    }
    variables.remove("filesPickerKeys");
  }

  private boolean updateFileVariable(
      Map<String, Object> variables,
      String pickerKey,
      DocumentReferenceResponse createdDoc,
      String docPreviewUrl) {
    for (Map.Entry<String, Object> var : variables.entrySet()) {
      if (var.getValue() instanceof String && ((String) var.getValue()).equals(pickerKey)) {
        var.setValue(List.of(createdDoc));
        variables.put(var.getKey() + "PreviewUrl", docPreviewUrl);
        return true;
      } else if (var.getValue() instanceof Map subMap) {
        return updateFileVariable(subMap, pickerKey, createdDoc, docPreviewUrl);
      }
    }
    return false;
  }

  public ProcessInstanceEvent startInstance(String bpmnProcessId, Map<String, Object> variables) {
    return camundaClient
        .newCreateInstanceCommand()
        .bpmnProcessId(bpmnProcessId)
        .latestVersion()
        .variables(variables)
        .send()
        .join();
  }

  public PublishMessageResponse publishMessage(
      String messageName, String correlationKey, Map<String, Object> variables) {
    return camundaClient
        .newPublishMessageCommand()
        .messageName(messageName)
        .correlationKey(correlationKey)
        .variables(variables)
        .send()
        .join();
  }

  public BroadcastSignalResponse broadcast(String signalName, Map<String, Object> variables) {
    return camundaClient
        .newBroadcastSignalCommand()
        .signalName(signalName)
        .variables(variables)
        .send()
        .join();
  }

  public InputStream getDoc(String docId, String contentHash) {
    return camundaClient.newDocumentContentGetRequest(docId).contentHash(contentHash).send().join();
  }

  public JobWorker createWorker(String taskDefinition, JobHandler handler) {
    return camundaClient
        .newWorker()
        .jobType(taskDefinition)
        .handler(handler)
        .name(taskDefinition)
        .maxJobsActive(30)
        .streamEnabled(true)
        .open();
  }

  public CamundaFuture<SearchResponse<UserTask>> userTaskQuery(
      Long processInstanceKey, String activityId) {
    return camundaClient
        .newUserTaskSearchRequest()
        .filter(
            filter -> {
              filter.elementId(activityId).processInstanceKey(processInstanceKey);
            })
        .send();
  }

  public CamundaClient getCamundaClient() {
    return camundaClient;
  }
}
