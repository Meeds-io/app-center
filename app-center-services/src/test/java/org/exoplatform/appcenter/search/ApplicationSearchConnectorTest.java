package org.exoplatform.appcenter.search;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.hibernate.ObjectNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConversationState.class,PortalContainer.class})
@PowerMockIgnore("javax.management.*")
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
    @Mock
    ConversationState state;
    @Mock
    ApplicationCenterService applicationCenterService;

    ApplicationSearchConnector applicationSearchConnector;
    @Mock
    private PortalContainer container;

    private String                       searchQueryFilePath;

    private String                       searchAllQueryFilePath;

    private static final String          SEARCH_QUERY_FILE_PATH_PARAM = "query.file.path";

    private static final String          SEARCH_ALL_QUERY_FILE_PATH_PARAM = "query.all.file.path";

    String                      searchResult    = null;


    boolean                     developingValue = false;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        container = PortalContainer.getInstance();
        this.applicationCenterService = new ApplicationCenterService();
        searchResult = IOUtil.getStreamContentAsString(getClass().getClassLoader()
                                                                 .getResourceAsStream("application-search-result.json"));

        try {
            Mockito.reset(configurationManager);
            when(configurationManager.getInputStream(searchQueryFilePath)).thenReturn(new ByteArrayInputStream(FAKE_ES_QUERY.getBytes()));


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
            assertNotNull(result);
            assertEquals(1, result.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
     @Test
     public void testBuildResult() {
     List<ApplicationSearchResult> results = applicationSearchConnector.buildResult(searchResult);
     assertNotNull(results);
     }

    @Test
    public void testBuildQueryStatement() {
        String searchTerm  = "searchTerm";
        org.exoplatform.services.security.Identity identity = mock(org.exoplatform.services.security.Identity.class);
        PowerMockito.mockStatic(ConversationState.class);
        when(ConversationState.getCurrent()).thenReturn(state);
        when(state.getIdentity()).thenReturn(identity);
        MembershipEntry membershipEntry = new MembershipEntry("*:/platform/users");
        Collection<MembershipEntry> membershipEntries = new ArrayList<>();
        membershipEntries.add(membershipEntry);
        when(identity.getMemberships()).thenReturn(membershipEntries);
        String searchQuery = applicationSearchConnector.buildQueryStatement(searchTerm,0,10,searchQueryFilePath);
        assertNotNull(searchQuery);
    }

     @Test
     public void testRetrieveSearchQuery() {
        String searchQuery = applicationSearchConnector.retrieveSearchQuery(searchQueryFilePath);
        assertNotNull(searchQuery);
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

        ValueParam allValueParam = new ValueParam();
        valueParam.setName("query.all.file.path");
        valueParam.setValue("ALL_FILE_PATH");

        params.addParameter(propertiesParam);
        params.addParameter(valueParam);
        params.addParameter(allValueParam);
        return params;
    }

}
