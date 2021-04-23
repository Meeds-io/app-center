package org.exoplatform.appcenter.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.service.ApplicationCenterService;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.print.Doc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationIndexingServiceConnector extends ElasticIndexingServiceConnector {
    private static final long           serialVersionUID = -8250958906231475231L;

    public static final String          TYPE             = "application";

    private static final Log LOG              = ExoLogger.getLogger(ApplicationIndexingServiceConnector.class);

    private final ApplicationCenterService applicationCenterService;

    public ApplicationIndexingServiceConnector(InitParams initParams,
                                               ApplicationCenterService applicationCenterService) {
        super(initParams);
        this.applicationCenterService = applicationCenterService;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Document create(String id) {
        return getDocument(id);
    }

    @Override
    public Document update(String id) {
        return getDocument(id);
    }

    @Override
    public List<String> getAllIds(int offset, int limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canReindex() {
        return false;
    }

    private Document getDocument(String id) {
        Document document = new Document();
        Map<String, String> fields = new HashMap<>();
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("id is mandatory");
        }
        LOG.debug("Index document for application id={}", id);
        try {
            Application application = applicationCenterService.findApplication(Long.parseLong(id));
            fields.put("id", Long.toString(application.getId()));
            if (StringUtils.isNotBlank(application.getTitle())) {
                fields.put("title", application.getTitle());
            }
            if (StringUtils.isNotBlank(application.getUrl())) {
                fields.put("url", application.getUrl());
            }
            if (StringUtils.isNotBlank(application.getHelpPageURL())) {
                fields.put("helpPageURL", application.getHelpPageURL());
            }
            if (StringUtils.isNotBlank(application.getImageFileBody())) {
                fields.put("imageFileBody", application.getImageFileBody());
            }
            if (StringUtils.isNotBlank(application.getImageFileName())) {
                fields.put("imageFileName", application.getImageFileName());
            }
            if (StringUtils.isNotBlank(application.getDescription())) {
                fields.put("description", application.getDescription());
            }
            Set<String> applicationPermissionIds = application.getPermissions().stream().map(String::valueOf).collect(Collectors.toSet());
            document = new Document(TYPE, id, application.getUrl(), null, applicationPermissionIds, fields);
        } catch (Exception e) {
            LOG.error("application with id '" + id + "' not found");
        }
        return document;
    }
}
