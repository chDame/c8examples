package com.c8.examples.chatbot.model;

import io.camunda.client.api.command.ClientException;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.SearchResponsePage;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
public class RspSearchResponse<T> implements SearchResponse<T> {
  private List<T> items;
  private SearchResponsePage page;

  public RspSearchResponse(final List<T> items, final SearchResponsePage page) {
    this.items = items;
    this.page = page;
  }

  @Override
  public T singleItem() {
    List<T> items = this.items();
    if (items.isEmpty()) {
      return null;
    } else if (items.size() > 1) {
      throw new ClientException("Expecting only one item but got " + items.size());
    } else {
      return items.get(0);
    }
  }
}
