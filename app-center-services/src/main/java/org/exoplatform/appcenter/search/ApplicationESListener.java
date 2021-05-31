package org.exoplatform.appcenter.search;

import org.exoplatform.appcenter.service.ApplicationCenterService;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ApplicationESListener extends Listener<Long, Object> {
    private static final Log LOG = ExoLogger.getExoLogger(ApplicationESListener.class);

    private final IndexingService indexingService;

    public ApplicationESListener(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Override
    public void onEvent(Event<Long, Object> event) throws Exception {
        if (indexingService != null) {
            String applicationId = Long.toString(event.getSource());
            if (ApplicationCenterService.POST_CREATE_APPLICATION.equals(event.getEventName())) {
                reindexApplication(applicationId, "create application");
            }
            if (ApplicationCenterService.POST_UPDATE_APPLICATION.equals(event.getEventName())) {
                reindexApplication(applicationId, "update application");
            }
            if (ApplicationCenterService.POST_DELETE_APPLICATION.equals(event.getEventName())) {
                unindexApplication(applicationId, "delete application");
            }
        }
    }

    private void reindexApplication(String applicationId, String cause) {
        IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
        LOG.debug("Notifying indexing service for application with id={}. Cause: {}", applicationId, cause);
        indexingService.reindex(ApplicationIndexingServiceConnector.TYPE, applicationId);
    }
    private void unindexApplication(String applicationId, String cause) {
        LOG.debug("Notifying unindexing service for application with id={}. Cause: {}", applicationId, cause);
        indexingService.unindex(ApplicationIndexingServiceConnector.TYPE, applicationId);
    }
}
