package org.exoplatform.appcenter.dto;

import java.io.Serializable;
import java.util.List;

public class ApplicationList implements Serializable {

  private static final long serialVersionUID = 8955981782215121831L;

  private List<Application> applications;

  private boolean           canAddFavorite;

  private long              totalApplications;

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

  public long getTotalApplications() {
    return totalApplications;
  }

  public void setTotalApplications(long totalApplications) {
    this.totalApplications = totalApplications;
  }

}
