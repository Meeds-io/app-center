package org.exoplatform.appcenter.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.appcenter.dto.*;
import org.exoplatform.appcenter.service.ApplicationCenterService;
import org.exoplatform.container.PortalContainer;

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
    resultList.add(new Application(1L, "titre1", "url1", "", 0L, 0L, "", "",
                                   "description1", false, true, false, true, false,""));
    resultList.add(new Application(2L, "titre2", "url2", "", 0L, 0L, "", "",
                                   "description2", false, true, false, true, false,""));
    resultList.add(new Application(3L, "titre3", "url3", "", 0L, 0L, "", "",
                                   "description3", false, true, false, true, false,""));
    resultList.add(new Application(4L, "titre4", "url4", "", 0L, 0L, "", "",
                                   "description4", false, true, false, true, false,""));
    resultList.add(new Application(5L, "titre5", "url5", "", 0L, 0L, "", "",
                                   "description5", false, true, false, true, false,""));
    result.setApplications(resultList);
    Mockito.when(applicationCenterService.getApplicationsList(0,10,"")).thenReturn(result);
  
    ApplicationCenterREST applicationCenterREST= new ApplicationCenterREST(applicationCenterService,portalContainer);
    Response response = applicationCenterREST.getApplicationsList(0, 10, "");
    assertEquals(200,response.getStatus());
    assertEquals(resultList.size(),((ApplicationList)response.getEntity()).getApplications().size());
  
  }
  
  @Test
  public void testGetAuthorizedApplicationsList() throws Exception {
    
    ApplicationList result = new ApplicationList();
    List<Application> resultList = new ArrayList<Application>();
    resultList.add(new Application(1L, "titre1", "url1", "", 0L, 0L, "", "",
                                   "description1", false, true, false, true, false,""));
    resultList.add(new Application(2L, "titre2", "url2", "", 0L, 0L, "", "",
                                   "description2", false, true, false, true, false,""));
    resultList.add(new Application(3L, "titre3", "url3", "", 0L, 0L, "", "",
                                   "description3", false, true, false, true, false,""));
    resultList.add(new Application(4L, "titre4", "url4", "", 0L, 0L, "", "",
                                   "description4", false, true, false, true, false,""));
    resultList.add(new Application(5L, "titre5", "url5", "", 0L, 0L, "", "",
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
    resultList.add(new Application(1L, "titre1", "url1", "", 0L, 0L, "", "",
                                   "description1", false, true, false, true, false,""));
    resultList.add(new Application(2L, "titre2", "url2", "", 0L, 0L, "", "",
                                   "description2", false, true, false, true, false,""));
    resultList.add(new Application(3L, "titre3", "url3", "", 0L, 0L, "", "",
                                   "description3", false, true, false, true, false,""));
    resultList.add(new Application(4L, "titre4", "url4", "", 0L, 0L, "", "",
                                   "description4", false, true, false, true, false,""));
    resultList.add(new Application(5L, "titre5", "url5", "", 0L, 0L, "", "",
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
  
    Application application = new Application(1L, "titre1", "url1", "", 0L, 0L, "", "",
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
