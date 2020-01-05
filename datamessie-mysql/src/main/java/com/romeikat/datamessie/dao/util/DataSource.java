package com.romeikat.datamessie.dao.util;

/*-
 * ============================LICENSE_START============================
 * data.messie (mysql)
 * =====================================================================
 * Copyright (C) 2013 - 2019 Dr. Raphael Romeikat
 * =====================================================================
 * This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public
License along with this program.  If not, see
<http://www.gnu.org/licenses/gpl-3.0.html>.
 * =============================LICENSE_END=============================
 */

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSource {

  private static HikariDataSource HIKARI_DATA_SOURCE;

  private DataSource() {}

  private static void initDataSource() {
    if (HIKARI_DATA_SOURCE == null) {
      final HikariConfig config = new HikariConfig();

      config.setJdbcUrl(SpringContext.getPropertyValue("jdbc.url"));
      config.setUsername(SpringContext.getPropertyValue("jdbc.username"));
      config.setPassword(SpringContext.getPropertyValue("jdbc.password"));
      config.setConnectionTimeout(5000);
      config.setMaxLifetime(28800);
      config.setMaximumPoolSize(
          Integer.parseInt(SpringContext.getPropertyValue("connections.maxPoolSize")));

      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");

      HIKARI_DATA_SOURCE = new HikariDataSource(config);
    }
  }

  public static Connection getConnection() throws SQLException {
    initDataSource();
    return HIKARI_DATA_SOURCE.getConnection();
  }

}
