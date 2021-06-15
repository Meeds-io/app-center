package org.exoplatform.appcenter.dto;


import java.util.List;

public class ApplicationSearchResult extends Application {
    private List<String> excerpts;

    public ApplicationSearchResult() {
    }

    public ApplicationSearchResult(Long id,
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
                                   List<String> permissions,List<String> excerpts) {
        super( id,
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
         isChangedManually,permissions);
        this.excerpts = excerpts;
    }

    public List<String> getExcerpts() {
        return excerpts;
    }

    public void setExcerpts(List<String> excerpts) {
        this.excerpts = excerpts;
    }
}
