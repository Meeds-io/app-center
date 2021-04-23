package org.exoplatform.appcenter.search;

import org.exoplatform.appcenter.util.Utils;
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
            if (Utils.POST_CREATE_APPLICATION_EVENT.equals(event.getEventName())) {
                reindexAgendaEvent(applicationId, "create application");
            }
        }
    }

    private void reindexAgendaEvent(String applicationId, String cause) {
        IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
        LOG.debug("Notifying indexing service for application with id={}. Cause: {}", applicationId, cause);
        indexingService.reindex(ApplicationIndexingServiceConnector.TYPE, applicationId);
    }
}
