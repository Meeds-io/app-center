package org.exoplatform.appcenter.rest;

import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.dto.ApplicationList;
import org.exoplatform.appcenter.dto.GeneralSettings;
import org.exoplatform.appcenter.service.ApplicationCenterService;
import org.exoplatform.container.PortalContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationCenterRestServiceTest {
  
  @Mock
  private ApplicationCenterService applicationCenterService;


  @Mock
  private PortalContainer portalContainer;
  
  @Before
  public void setup() throws Exception {
    Mockito.when(portalContainer.getName()).thenReturn("portal");
    Mockito.when(portalContainer.getRestContextName()).thenReturn("rest");

  }
  
  @Test
  public void testGetApplicationsList() throws Exception {
    
    ApplicationList result = new ApplicationList();
    List<Application> resultList = new ArrayList<Application>();
    resultList.add(new Application(1L, "titre1", "url1", "", 0L, "", "",
                                   "description1", false, true, false, true, false,""));
    resultList.add(new Application(2L, "titre2", "url2", "", 0L, "", "",
                                   "description2", false, true, false, true, false,""));
    resultList.add(new Application(3L, "titre3", "url3", "", 0L, "", "",
                                   "description3", false, true, false, true, false,""));
    resultList.add(new Application(4L, "titre4", "url4", "", 0L, "", "",
                                   "description4", false, true, false, true, false,""));
    resultList.add(new Application(5L, "titre5", "url5", "", 0L, "", "",
                                   "description5", false, true, false, true, false,""));
    result.setApplications(resultList);
    Mockito.when(applicationCenterService.getApplicationsList(0,10,"")).thenReturn(result);
  
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.getApplicationsList(0, 10, "");
    assertEquals(200,response.getStatus());
    assertEquals(resultList.size(),((ApplicationList)response.getEntity()).getApplications().size());
  
  }
  @Test
  public void testCreateApplication() throws Exception {
    Application application = new Application(1L, "titre1", "url1", "", 0L, "", "",
            "description1", false, true, false, true, false, "");
    ApplicationCenterREST applicationCenterREST = new ApplicationCenterREST(applicationCenterService, portalContainer);
    Response response = applicationCenterREST.createApplication(application);
    assertEquals(204, response.getStatus());
   }
     @Test
     public void testUpdateApplication() throws Exception {
    Application application = new Application(1L, "titre1", "url1", "", 0L, "", "",
            "description1", false, true, false, true, false,"");
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.createApplication(application);
    assertEquals(204, response.getStatus());
    application.setTitle("titre2");
    application.setUrl("url2");
    application.setDescription("description2");

    Response updateResponse = applicationCenterREST.updateApplication(application);
    assertEquals(204, updateResponse.getStatus());
     }
  @Test
  public void testDeleteApplication() throws Exception {
    // ApplicationList result = new ApplicationList();
    Application application = new Application(1L, "titre1", "url1", "", 0L, "", "",
            "description1", false, true, false, true, false,"");
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.createApplication(application);
    assertEquals(204, response.getStatus());
    Response deleteResponse = applicationCenterREST.deleteApplication(application.getId());
    assertEquals(204, deleteResponse.getStatus());

  }
  @Test
  public void testAddFavouriteApplication() throws Exception {
    Application application = new Application(1L, "titre1", "url1", "", 0L, "", "",
            "description1", false, true, false, true, false,"");
    when(applicationCenterService.findApplication(application.getId())).thenReturn(application);
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.createApplication(application);
    assertEquals(204, response.getStatus());
    Response addFavouriteResponse = applicationCenterREST.addFavoriteApplication(application.getId());
    assertEquals(204, addFavouriteResponse.getStatus());

  }
  @Test
  public void testNotAddFavouriteApplication() throws Exception {

    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);

    Response addFavouriteResponse = applicationCenterREST.addFavoriteApplication(2L);
    assertEquals(500, addFavouriteResponse.getStatus());

  }

  @Test
  public void testSearchApplication() throws Exception {
    // ApplicationList result = new ApplicationList();
    Application Application = new Application(1L, "titre1", "url1", "", 0L, "", "",
            "description1", false, true, false, true, false,"");
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    applicationCenterREST.createApplication(Application);
    Response response = applicationCenterREST.search("titre1",0,20);
    assertEquals(200,response.getStatus());
    response = applicationCenterREST.search("titre1",-1,20);
    assertEquals(400,response.getStatus());
    response = applicationCenterREST.search("titre1",0,-1);
    assertEquals(400,response.getStatus());

  }
  @Test
  public void testGetAuthorizedApplicationsList() throws Exception {
    
    ApplicationList result = new ApplicationList();
    List<Application> resultList = new ArrayList<Application>();
    resultList.add(new Application(1L, "titre1", "url1", "", 0L, "", "",
                                   "description1", false, true, false, true, false,""));
    resultList.add(new Application(2L, "titre2", "url2", "", 0L, "", "",
                                   "description2", false, true, false, true, false,""));
    resultList.add(new Application(3L, "titre3", "url3", "", 0L, "", "",
                                   "description3", false, true, false, true, false,""));
    resultList.add(new Application(4L, "titre4", "url4", "", 0L, "", "",
                                   "description4", false, true, false, true, false,""));
    resultList.add(new Application(5L, "titre5", "url5", "", 0L, "", "",
                                   "description5", false, true, false, true, false,""));
    result.setApplications(resultList);
    Mockito.when(applicationCenterService.getAuthorizedApplicationsList(0,10,"",null)).thenReturn(result);
    
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.getAuthorizedApplicationsList(0, 10, "");
    assertEquals(200,response.getStatus());
    assertEquals(resultList.size(),((ApplicationList)response.getEntity()).getApplications().size());
    
  }
  
  @Test
  public void testGetFavoriteApplicationsList() throws Exception {
    
    ApplicationList result = new ApplicationList();
    List<Application> resultList = new ArrayList<Application>();
    resultList.add(new Application(1L, "titre1", "url1", "", 0L, "", "",
                                   "description1", false, true, false, true, false,""));
    resultList.add(new Application(2L, "titre2", "url2", "", 0L, "", "",
                                   "description2", false, true, false, true, false,""));
    resultList.add(new Application(3L, "titre3", "url3", "", 0L, "", "",
                                   "description3", false, true, false, true, false,""));
    resultList.add(new Application(4L, "titre4", "url4", "", 0L, "", "",
                                   "description4", false, true, false, true, false,""));
    resultList.add(new Application(5L, "titre5", "url5", "", 0L, "", "",
                                   "description5", false, true, false, true, false,""));
    result.setApplications(resultList);
    Mockito.when(applicationCenterService.getMandatoryAndFavoriteApplicationsList(null)).thenReturn(result);
    
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.getFavoriteApplicationsList();
    assertEquals(200,response.getStatus());
    assertEquals(resultList.size(),((ApplicationList)response.getEntity()).getApplications().size());
    
  }
  
  @Test
  public void testLogFunctions() throws Exception {
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response =applicationCenterREST.logClickAllApplications();
    assertEquals(200,response.getStatus());
  
    Application application = new Application(1L, "titre1", "url1", "", 0L, "", "",
                                              "description1", false, true, false, true, false,"");
    Mockito.when(applicationCenterService.findApplication(1L)).thenReturn(application);
    response =applicationCenterREST.logClickOneApplications(1L);
    assertEquals(200,response.getStatus());
  
    response =applicationCenterREST.logOpenDrawer();
    assertEquals(200,response.getStatus());
  }
  
  @Test
  public void testGeneralSettings() throws Exception {
  
    GeneralSettings generalSettings = new GeneralSettings();
    generalSettings.setMaxFavoriteApps(12L);
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Mockito.when(applicationCenterService.getAppGeneralSettings()).thenReturn(generalSettings);
  
    Response response = applicationCenterREST.getAppGeneralSettings();
    assertEquals(200,response.getStatus());
    assertEquals(generalSettings.getMaxFavoriteApps(),((GeneralSettings)response.getEntity()).getMaxFavoriteApps());
  
  }
}
