package org.exoplatform.appcenter.dto;

import java.io.Serializable;

public class ApplicationOrder implements Serializable {
  /** Application id */
  private Long   id;

  /** Application order */
  private Long   order;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrder() {
    return order;
  }

  public void setOrder(Long order) {
    this.order = order;
  }
}
