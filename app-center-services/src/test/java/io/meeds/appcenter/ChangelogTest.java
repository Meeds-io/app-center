/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Runs the addon changelog through the real Liquibase engine: a mocked suite
 * stays green with a changeset the engine refuses, and only an executed
 * rollback proves the hand-written one of 1.0.0-exip-51636-02 exists and
 * parses. The last scenario replays an upgrade of a pre-existing deployment:
 * a DRAWER row present before 1.0.0-exip-51636-01/-02 must come out with both
 * placement flags enabled, without touching other application types.
 */
class ChangelogTest {

  private static final String CHANGELOG = "db.changelogs/app-center-changelog-1.0.0.xml";

  @Test
  void applyRollbackAndReapplyChangelog() throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:appcenter-changelog;shutdown=true", "SA", "")) {
      Database database = DatabaseFactory.getInstance()
                                         .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);

      liquibase.update((String) null);
      assertTrue(columnExists(connection, "ALLOW_STICK"));
      assertTrue(columnExists(connection, "ALLOW_DETACH"));

      liquibase.rollback(2, (String) null);
      assertFalse(columnExists(connection, "ALLOW_STICK"));
      assertFalse(columnExists(connection, "ALLOW_DETACH"));

      insertApplication(connection, 1, "Drawer app", "notifications", 1);
      insertApplication(connection, 2, "Portlet app", "35", 2);

      liquibase.update((String) null);
      assertEquals(Boolean.TRUE, placementFlag(connection, 1, "ALLOW_STICK"));
      assertEquals(Boolean.TRUE, placementFlag(connection, 1, "ALLOW_DETACH"));
      assertEquals(Boolean.FALSE, placementFlag(connection, 2, "ALLOW_STICK"));
      assertEquals(Boolean.FALSE, placementFlag(connection, 2, "ALLOW_DETACH"));
    }
  }

  private boolean columnExists(Connection connection, String columnName) throws Exception {
    try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, "AC_APPLICATION", columnName)) {
      return resultSet.next();
    }
  }

  private void insertApplication(Connection connection, int id, String title, String url, int type) throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(String.format("INSERT INTO AC_APPLICATION (ID, TITLE, URL, APP_TYPE, ACTIVE, BY_DEFAULT) VALUES (%d, '%s', '%s', %d, TRUE, FALSE)",
                                            id,
                                            title,
                                            url,
                                            type));
    }
  }

  private Boolean placementFlag(Connection connection, int id, String columnName) throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("SELECT %s FROM AC_APPLICATION WHERE ID = %d",
                                                                   columnName,
                                                                   id))) {
      resultSet.next();
      return resultSet.getBoolean(1);
    }
  }

}
