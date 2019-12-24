package org.exoplatform.appCenter.services.mock;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockSettingServiceImpl implements SettingService {
  
  Map<String, SettingValue<?>> settingMap = new HashMap<String, SettingValue<?>>();

  @Override
  public void set(Context context, Scope scope, String key, SettingValue<?> value) {
    settingMap.put(key, value);

  }

  @Override
  public void remove(Context context, Scope scope, String key) {

  }

  @Override
  public void remove(Context context, Scope scope) {

  }

  @Override
  public void remove(Context context) {

  }

  @Override
  public SettingValue<?> get(Context context, Scope scope, String key) {
    return settingMap.get(key);
  }

  @Override
  public long countContextsByType(String contextType) {
    return 0;
  }

  @Override
  public List<String> getContextNamesByType(String contextType, int offset, int limit) {
    return null;
  }

  @Override
  public Map<Scope, Map<String, SettingValue<String>>> getSettingsByContext(Context context) {
    return null;
  }

  @Override
  public List<Context> getContextsByTypeAndScopeAndSettingName(String contextType,
                                                               String scopeType,
                                                               String scopeName,
                                                               String settingName,
                                                               int offset,
                                                               int limit) {
    return null;
  }

  @Override
  public Set<String> getEmptyContextsByTypeAndScopeAndSettingName(String contextType,
                                                                  String scopeType,
                                                                  String scopeName,
                                                                  String settingName,
                                                                  int offset,
                                                                  int limit) {
    return null;
  }

  @Override
  public void save(Context context) {
    
  }

  @Override
  public Map<String, SettingValue> getSettingsByContextAndScope(String s, String s1, String s2, String s3) {
    return null;
  }

}
