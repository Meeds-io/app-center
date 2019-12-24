package org.exoplatform.appCenter.services.test;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.mockito.Mockito;

import org.exoplatform.appCenter.services.rest.ApplicationsRestService;
import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.impl.ResourceBinder;

/**
 * AbstractServiceTest.java
 */
@ConfiguredBy({
//    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/component.search.configuration.xml"),
//    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/dgfla-test-configuration.xml"),
//    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/dgfla.component.core.test.configuration.xml"),
//    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.notification.test.configuration.xml"),
//    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-notification-configuration.xml"),
//    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test.configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
})
public abstract class AbstractServiceTest extends BaseExoTestCase {
  protected static Log              LOG = ExoLogger.getLogger(AbstractServiceTest.class.getName());

  protected ResourceBinder          resourceBinder;
  
  protected OrganizationService organizationService;

  protected ApplicationsRestService applicationsRestService;

  protected UriInfo                 mockUriInfo;

  protected SecurityContext         mockSecurityContext;

  protected Principal               mockPrincipal;

  @Override
  protected void setUp() throws Exception {
    begin();
    organizationService = getService(OrganizationService.class);
    applicationsRestService = getService(ApplicationsRestService.class);

    mockUriInfo = Mockito.mock(UriInfo.class);
    Mockito.when(mockUriInfo.getAbsolutePath()).thenReturn(null);
    mockPrincipal = Mockito.mock(Principal.class);
    Mockito.when(mockPrincipal.getName()).thenReturn("root");

    mockSecurityContext = Mockito.mock(SecurityContext.class);
    Mockito.when(mockSecurityContext.getUserPrincipal()).thenReturn(mockPrincipal);
  }

  @Override
  protected void tearDown() throws Exception {
    resourceBinder.clear();
    //
    end();
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }
}
