/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.appcenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.persistence.GenericDAO;

import io.meeds.appcenter.plugin.MyApplicationsPortletInstancePreferencePlugin;
import io.meeds.appcenter.storage.PortletWindowStorage;

/**
 * The Kernel to Spring bridge exposes no GenericDAO as a bean
 * (KernelContainerLifecyclePlugin#isIgnoreKernelComponent, portal): a bean of
 * this module injecting one fails its context at startup, and with it the
 * portal container, while a test setting a mock in its place passes.
 */
class KernelDaoInjectionTest {

  /**
   * An {@link Autowired} field or method, or a constructor parameter, the
   * module's injection forms, of every bean whatever its profiles;
   * {@code @Resource} and {@code @Inject} are not looked at. Fails a bean
   * injecting a kernel DAO, which only a server start detects otherwise
   */
  @Test
  void noBeanInjectsAKernelDao() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    // Every profile active, as on a server: a @Profile bean is scanned too
    scanner.setEnvironment(new StandardEnvironment() {
      @Override
      public boolean matchesProfiles(String... profileExpressions) {
        return true;
      }
    });
    scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
    Set<String> beanClassNames = scanner.findCandidateComponents("io.meeds.appcenter")
                                        .stream()
                                        .map(BeanDefinition::getBeanClassName)
                                        .collect(Collectors.toSet());
    assertTrue(beanClassNames.contains(PortletWindowStorage.class.getName()), "The scan finds no bean of the module");
    assertTrue(beanClassNames.contains(MyApplicationsPortletInstancePreferencePlugin.class.getName()),
               "The scan skips the beans of a profile");

    List<String> kernelDaoInjections = new ArrayList<>();
    for (String beanClassName : beanClassNames) {
      Class<?> beanClass = Class.forName(beanClassName, false, getClass().getClassLoader());
      for (Class<?> type = beanClass; type != null && type != Object.class; type = type.getSuperclass()) {
        for (Field field : type.getDeclaredFields()) {
          if (field.isAnnotationPresent(Autowired.class) && GenericDAO.class.isAssignableFrom(field.getType())) {
            kernelDaoInjections.add(beanClassName + "#" + field.getName());
          }
        }
        for (Method method : type.getDeclaredMethods()) {
          if (method.isAnnotationPresent(Autowired.class)
              && Arrays.stream(method.getParameterTypes()).anyMatch(GenericDAO.class::isAssignableFrom)) {
            kernelDaoInjections.add(beanClassName + "#" + method.getName() + "()");
          }
        }
      }
      for (Constructor<?> constructor : beanClass.getDeclaredConstructors()) {
        for (Class<?> parameterType : constructor.getParameterTypes()) {
          if (GenericDAO.class.isAssignableFrom(parameterType)) {
            kernelDaoInjections.add(beanClassName + "(" + parameterType.getSimpleName() + ")");
          }
        }
      }
    }
    assertEquals(List.of(), kernelDaoInjections);
  }

}
