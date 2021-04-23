package org.exoplatform.appcenter.search;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.dto.ApplicationSearchResult;
import org.exoplatform.appcenter.service.ApplicationCenterService;
import org.exoplatform.appcenter.service.ApplicationCenterServiceTest;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.hibernate.ObjectNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationSearchConnectorTest {

    private static final String ES_TYPE         = "application";

    private static final String ES_INDEX        = "application_alias";

    public static final String  FAKE_ES_QUERY   =
            "{offset: @offset@, limit: @limit@, term1: @term@, term2: @term@, permissions: @permissions@}";

    @Mock
    IdentityManager identityManager;

    @Mock
    ConfigurationManager configurationManager;

    @Mock
    ElasticSearchingClient client;

    ApplicationCenterService applicationCenterService;

    ApplicationSearchConnector applicationSearchConnector;

    private PortalContainer container;

    String                      searchResult    = null;

    boolean                     developingValue = false;

    @Before
    public void setUp() throws Exception {
        container = PortalContainer.getInstance();
        this.applicationCenterService = container.getComponentInstanceOfType(ApplicationCenterService.class);
        searchResult = IOUtil.getStreamContentAsString(getClass().getClassLoader()
                                                                 .getResourceAsStream("application-search-result.json"));

        try {
            Mockito.reset(configurationManager);
            when(configurationManager.getInputStream("FILE_PATH")).thenReturn(new ByteArrayInputStream(FAKE_ES_QUERY.getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error retrieving ES Query content", e);
        }
        developingValue = PropertyManager.isDevelopping();
        PropertyManager.setProperty(PropertyManager.DEVELOPING, "false");
        PropertyManager.refresh();

        applicationSearchConnector = new ApplicationSearchConnector(configurationManager, identityManager, client, getParams());

    }

    @After
    public void tearDown() throws ObjectNotFoundException {
        PropertyManager.setProperty(PropertyManager.DEVELOPING, String.valueOf(developingValue));
        PropertyManager.refresh();
    }

    @Test
    public void testSearchWithResult() {
        String term = "searchTerm";
        HashSet<Long> permissions = new HashSet<>(Arrays.asList(1L));
        Identity identity = mock(Identity.class);
        when(identity.getId()).thenReturn("1");
        when(identity.getRemoteId()).thenReturn("testuser1");
        String expectedESQuery = FAKE_ES_QUERY.replaceAll("@term@", term)
                                              .replaceAll("@permissions@", StringUtils.join(permissions, ","))
                                              .replaceAll("@offset@", "0")
                                              .replaceAll("@limit@", "10");
        when(client.sendRequest(eq(expectedESQuery), eq(ES_INDEX), eq(ES_TYPE))).thenReturn(searchResult);
        Application application = new Application(null,
                                                  "title",
                                                  "url",
                                                  null,
                                                  0L,
                                                  null,
                                                  null,
                                                  "description",
                                                  false,
                                                  true,
                                                  false,
                                                  false,
                                                  false,
                                                  ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
        try { 
            Application storedApplication = applicationCenterService.createApplication(application);
            List<ApplicationSearchResult> result = applicationSearchConnector.search(identity.getRemoteId(), term, 0, 10);
        } catch (Exception e) {
            //
        }
    }

    private InitParams getParams() {
        InitParams params = new InitParams();
        PropertiesParam propertiesParam = new PropertiesParam();
        propertiesParam.setName("constructor.params");
        propertiesParam.setProperty("index", ES_INDEX);
        propertiesParam.setProperty("searchType", ES_TYPE);

        ValueParam valueParam = new ValueParam();
        valueParam.setName("query.file.path");
        valueParam.setValue("FILE_PATH");

        params.addParameter(propertiesParam);
        params.addParameter(valueParam);
        return params;
    }

}
