package org.exoplatform.appcenter.dto;

/**
 * @author Ayoub Zayati
 */
public class ApplicationImage {

  private Long   id;

  private String fileBody;

  private String fileName;

  public ApplicationImage(Long id, String fileName, String fileBody) {
    this.id = id;
    this.fileName = fileName;
    this.fileBody = fileBody;
  }

  /**
   * @return the fileBody
   */
  public String getFileBody() {
    return fileBody;
  }

  /**
   * @param fileBody the fileBody to set
   */
  public void setFileBody(String fileBody) {
    this.fileBody = fileBody;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @param fileName the fileName to set
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
