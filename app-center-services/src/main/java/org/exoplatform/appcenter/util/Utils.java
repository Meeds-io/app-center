package org.exoplatform.appcenter.util;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class Utils {
    private static final Log LOG                            = ExoLogger.getLogger(Utils.class);
    public static final String POST_CREATE_APPLICATION_EVENT = "exo.application.created";
    public static void broadcastEvent(ListenerService listenerService, String eventName, Object source, Object data) {
        try {
            listenerService.broadcast(eventName, source, data);
        } catch (Exception e) {
            LOG.warn("Error broadcasting event '" + eventName + "' using source '" + source + "' and data " + data, e);
        }
    }

}
