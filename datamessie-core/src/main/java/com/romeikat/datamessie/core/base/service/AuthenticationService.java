package com.romeikat.datamessie.core.base.service;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2017 Dr. Raphael Romeikat
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

  public boolean authenticate(final String username, final String password) {
    if (username == null) {
      return false;
    }
    // admin / Pastinake
    if (checkUsername(username, "admin") && checkPassword(password, "Pastinake")) {
      return true;
    }
    // admin / Zd2tsd3ig
    if (checkUsername(username, "admin") && checkPassword(password, "Zd2tsd3ig")) {
      return true;
    }
    // Others
    return false;
  }

  public DataMessieRoles getRoles(final String username, final String passwordHash,
      final boolean signedIn) {
    if (username == null || !signedIn) {
      return DataMessieRoles.getEmptyRoles();
    }
    // admin / Pastinake
    if (checkUsername(username, "admin") && checkPasswordHash(passwordHash, "Pastinake")) {
      return DataMessieRoles.getAllRoles();
    }
    // admin / Zd2tsd3ig
    if (checkUsername(username, "admin") && checkPasswordHash(passwordHash, "Zd2tsd3ig")) {
      return DataMessieRoles.getAllRolesExcept(DataMessieRoles.ANALYSIS_PAGE);
    }
    // Others
    return DataMessieRoles.getEmptyRoles();
  }

  private boolean checkUsername(final String providedUsername, final String expectedUsername) {
    if (providedUsername == null || expectedUsername == null) {
      return false;
    }
    return providedUsername.equalsIgnoreCase(expectedUsername);
  }

  private boolean checkPassword(final String providedPassword, final String expectedPassword) {
    if (providedPassword == null || expectedPassword == null) {
      return false;
    }
    return providedPassword.equals(expectedPassword);
  }

  private boolean checkPasswordHash(final String providedPasswordHash,
      final String expectedPassword) {
    if (providedPasswordHash == null || expectedPassword == null) {
      return false;
    }
    final String expectedPasswordHash = getHash(expectedPassword);
    return providedPasswordHash.equals(expectedPasswordHash);
  }

  public String getHash(final String password) {
    String passwordHash = null;
    try {
      // Create MessageDigest instance for SHA-512
      final MessageDigest md = MessageDigest.getInstance("SHA-512");
      // Add password bytes to digest
      md.update(password.getBytes());
      // Get the hash's bytes
      final byte[] bytes = md.digest();
      // Convert bytes in decimal format to hexadecimal format
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      // Get hashed password in hex format
      passwordHash = sb.toString();
    } catch (final NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return passwordHash;
  }

  public static class DataMessieRoles extends Roles {

    private static final long serialVersionUID = 1L;

    public static final String PROJECT_PAGE = "PROJECT_PAGE";

    public static final String STATISTICS_PAGE = "STATISTICS_PAGE";

    public static final String SOURCES_PAGE = "SOURCES_PAGE";

    public static final String SOURCE_PAGE = "SOURCE_PAGE";

    public static final String CRAWLINGS_PAGE = "CRAWLINGS_PAGE";

    public static final String DOCUMENTS_PAGE = "DOCUMENTS_PAGE";

    public static final String DOCUMENT_PAGE = "DOCUMENT_PAGE";

    public static final String ANALYSIS_PAGE = "ANALYSIS_PAGE";

    private DataMessieRoles() {
      super();
    }

    public static DataMessieRoles getRoles(final String... roles) {
      final DataMessieRoles dataMessieRoles = new DataMessieRoles();
      for (final String role : roles) {
        dataMessieRoles.add(role);
      }
      return dataMessieRoles;
    }

    public static DataMessieRoles getEmptyRoles() {
      final DataMessieRoles dataMessieRoles = new DataMessieRoles();
      return dataMessieRoles;
    }

    public static DataMessieRoles getAllRoles() {
      final DataMessieRoles dataMessieRoles = new DataMessieRoles();
      dataMessieRoles.add(PROJECT_PAGE);
      dataMessieRoles.add(STATISTICS_PAGE);
      dataMessieRoles.add(SOURCES_PAGE);
      dataMessieRoles.add(SOURCE_PAGE);
      dataMessieRoles.add(CRAWLINGS_PAGE);
      dataMessieRoles.add(DOCUMENTS_PAGE);
      dataMessieRoles.add(DOCUMENT_PAGE);
      dataMessieRoles.add(ANALYSIS_PAGE);
      return dataMessieRoles;
    }

    public static DataMessieRoles getAllRolesExcept(final String... roles) {
      final DataMessieRoles dataMessieRoles = getAllRoles();
      for (final String role : roles) {
        dataMessieRoles.remove(role);
      }
      return dataMessieRoles;
    }

  }

}
