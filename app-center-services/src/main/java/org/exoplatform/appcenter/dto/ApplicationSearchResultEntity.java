package org.exoplatform.appcenter.dto;

import java.util.List;

public class ApplicationSearchResultEntity extends Application{
    private static final long serialVersionUID = -8094706197096178451L;

    private List<String> excerpts;

    public ApplicationSearchResultEntity() {
    }

    public ApplicationSearchResultEntity(Long id,
                                         String title,
                                         String url,
                                         String helpPageURL,
                                         Long imageFileId,
                                         String imageFileBody,
                                         String imageFileName,
                                         String description,
                                         boolean isSystem,
                                         boolean active,
                                         boolean isMandatory,
                                         boolean isMobile,
                                         boolean isChangedManually,
                                         List<String> permissions,
                                         List<String> excerpts) {
        super(id,
              title,
              url,
              helpPageURL,
              imageFileId,
              imageFileBody,
              imageFileName,
              description,
              isSystem,
              active,
              isMandatory,
              isMobile,
              isChangedManually,
              permissions);
        this.excerpts = excerpts;
    }

    public List<String> getExcerpts() {
        return excerpts;
    }

    public void setExcerpts(List<String> excerpts) {
        this.excerpts = excerpts;
    }
}
