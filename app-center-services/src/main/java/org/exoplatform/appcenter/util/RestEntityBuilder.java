package org.exoplatform.appcenter.util;

import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.dto.ApplicationSearchResult;
import org.exoplatform.appcenter.dto.ApplicationSearchResultEntity;

public class RestEntityBuilder {
    private RestEntityBuilder() {
    }
    public static final ApplicationSearchResultEntity fromSearchApplication(ApplicationSearchResult applicationSearchResult) {
        ApplicationSearchResultEntity applicationSearchResultEntity = (ApplicationSearchResultEntity) fromApplication(applicationSearchResult);
        applicationSearchResultEntity.setExcerpts(applicationSearchResult.getExcerpts());
        return applicationSearchResultEntity;
    }

    private static final Application fromApplication(Application application) {
            return new ApplicationSearchResultEntity(application.getId(),
                                                     application.getTitle(),
                                                     application.getUrl(),
                                                     application.getHelpPageURL(),
                                                     application.getImageFileId(),
                                                     application.getImageFileBody(),
                                                     application.getImageFileName(),
                                                     application.getDescription(),
                                                     false,
                                                     false,
                                                     false,
                                                     false,
                                                     false,
                                                     null,
                                                     null);
    }
}
