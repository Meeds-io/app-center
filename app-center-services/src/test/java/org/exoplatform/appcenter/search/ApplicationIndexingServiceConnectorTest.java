package org.exoplatform.appcenter.search;

import static org.junit.Assert.*;

import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.service.ApplicationCenterService;
import org.exoplatform.appcenter.service.ApplicationCenterServiceTest;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.junit.Test;

public class ApplicationIndexingServiceConnectorTest extends ApplicationCenterServiceTest {
    ApplicationIndexingServiceConnector applicationIndexingServiceConnector = null;

    ApplicationCenterService applicationCenterService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        this.applicationCenterService = container.getComponentInstanceOfType(ApplicationCenterService.class);

        applicationIndexingServiceConnector = new ApplicationIndexingServiceConnector(getParams(), applicationCenterService);
    }
    @Test
    public void testGetAllIds() {
        applicationIndexingServiceConnector = new ApplicationIndexingServiceConnector(getParams(),applicationCenterService);
        try {
            applicationIndexingServiceConnector.getAllIds(0, 10);
            fail("getAllIds shouldn't be supported");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testCanReindex() {
        applicationIndexingServiceConnector = new ApplicationIndexingServiceConnector(getParams(), applicationCenterService);
        assertFalse(applicationIndexingServiceConnector.canReindex());
    }

    @Test
    public void testGetType() {
        applicationIndexingServiceConnector = new ApplicationIndexingServiceConnector(getParams(), applicationCenterService);
        assertEquals(applicationIndexingServiceConnector.TYPE, applicationIndexingServiceConnector.getType());
    }

    @Test
    public void testCreate() throws Exception {
        applicationIndexingServiceConnector = new ApplicationIndexingServiceConnector(getParams(), applicationCenterService);
        try {
            applicationIndexingServiceConnector.create(null);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            applicationIndexingServiceConnector.create("1");
        } catch (Exception e) {
            // Expected
        }

        Application application = new Application(null,
                                                  "title",
                                                  null,
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
            Document document = applicationIndexingServiceConnector.create(String.valueOf(storedApplication.getId()));
            assertNotNull(document);
            assertEquals(String.valueOf(storedApplication.getId()), document.getId());
            assertEquals(String.valueOf(storedApplication.getId()), document.getFields().get("id"));
            assertEquals(storedApplication.getDescription(), document.getFields().get("description"));
            assertEquals(storedApplication.getTitle(), document.getFields().get("title"));
        }
        catch (Exception e){

        }

    }

    @Test
    public void testUpdate() throws Exception {
        applicationIndexingServiceConnector = new ApplicationIndexingServiceConnector(getParams(), applicationCenterService);
        try {
            applicationIndexingServiceConnector.update(null);
            fail("IllegalArgumentException should be thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            applicationIndexingServiceConnector.update("1");
        } catch (Exception e) {
            // Expected
        }

        Application application = new Application(null,
                                                  "title1",
                                                  "url1",
                                                  null,
                                                  0L,
                                                  null,
                                                  null,
                                                  "description1",
                                                  false,
                                                  true,
                                                  false,
                                                  false,
                                                  false,
                                                  ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
        Application storedApplication  = applicationCenterService.createApplication(application);
        Document document = applicationIndexingServiceConnector.update(String.valueOf(storedApplication.getId()));
        assertNotNull(document);
        assertEquals(String.valueOf(storedApplication.getId()), document.getId());
        assertEquals(String.valueOf(storedApplication.getId()), document.getFields().get("id"));
        assertEquals(storedApplication.getDescription(), document.getFields().get("description"));
        assertEquals(storedApplication.getTitle(), document.getFields().get("title"));

    }

    private InitParams getParams() {
        InitParams params = new InitParams();
        PropertiesParam propertiesParam = new PropertiesParam();
        propertiesParam.setName("constructor.params");
        params.addParameter(propertiesParam);
        return params;
    }
}
