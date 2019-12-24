package org.exoplatform.appCenter.services.mock;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfileHandler;

public class MockOrganizationService implements OrganizationService {

  @Override
  public UserHandler getUserHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public UserProfileHandler getUserProfileHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public GroupHandler getGroupHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MembershipTypeHandler getMembershipTypeHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MembershipHandler getMembershipHandler() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addListenerPlugin(ComponentPlugin listener) throws Exception {
    // TODO Auto-generated method stub
  }
}
