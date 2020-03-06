package org.exoplatform.appcenter.dto;

import java.io.Serializable;
import java.util.List;

public class ApplicationList implements Serializable {

  private static final long serialVersionUID = 8955981782215121831L;

  private List<Application> applications;

  private long              offset;

  private long              limit;

  private long              size;

  private boolean           canAddFavorite;

  public List<Application> getApplications() {
    return applications;
  }

  public void setApplications(List<Application> applications) {
    this.applications = applications;
  }

  public boolean isCanAddFavorite() {
    return canAddFavorite;
  }

  public void setCanAddFavorite(boolean canAddFavorite) {
    this.canAddFavorite = canAddFavorite;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public long getLimit() {
    return limit;
  }

  public void setLimit(long limit) {
    this.limit = limit;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

}
