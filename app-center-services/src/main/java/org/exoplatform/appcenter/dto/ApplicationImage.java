package org.exoplatform.appcenter.dto;

/**
 * @author Ayoub Zayati
 */
public class ApplicationImage {

  private String fileBody;

  private String fileName;
  
  public ApplicationImage() {
  }

  public ApplicationImage(String fileBody, String fileName) {
    this.fileBody = fileBody;
    this.fileName = fileName;
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

}
