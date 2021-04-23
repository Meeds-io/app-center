package org.exoplatform.appcenter.dto;


import java.util.List;

public class ApplicationSearchResult extends Application {
    private List<String> excerpts;

    public ApplicationSearchResult() {
    }

    public ApplicationSearchResult(List<String> excerpts) {
        this.excerpts = excerpts;
    }

    public List<String> getExcerpts() {
        return excerpts;
    }

    public void setExcerpts(List<String> excerpts) {
        this.excerpts = excerpts;
    }
}
