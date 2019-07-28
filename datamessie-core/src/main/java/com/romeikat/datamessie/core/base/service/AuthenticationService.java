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
import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.romeikat.datamessie.core.base.dao.impl.UserDao;
import com.romeikat.datamessie.core.base.util.hibernate.HibernateSessionProvider;
import com.romeikat.datamessie.core.domain.entity.impl.User;

@Service
public class AuthenticationService {

  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationService.class);

  @Autowired
  private UserDao userDao;

  @Autowired
  private SessionFactory sessionFactory;

  public Long authenticate(final String username, final String password) {
    final HibernateSessionProvider sessionProvider = new HibernateSessionProvider(sessionFactory);

    if (StringUtils.isBlank(username)) {
      return null;
    }

    // Find user
    final User user = userDao.get(sessionProvider.getStatelessSession(), username);
    sessionProvider.closeStatelessSession();
    if (user == null) {
      return null;
    }

    // Verify user
    final byte[] passwordSalt = user.getPasswordSalt();
    final byte[] passwordHash = user.getPasswordHash();
    final boolean authenticated = authenticate(password, passwordSalt, passwordHash);
    if (authenticated) {
      return user.getId();
    } else {
      return null;
    }
  }

  public boolean authenticate(final String password, final byte[] passwordSalt,
      final byte[] passwordHash) {
    final byte[] expectedPasswordHash = calculateHash(password, passwordSalt);
    final boolean passwordHashEquals = Arrays.equals(expectedPasswordHash, passwordHash);
    return passwordHashEquals;
  }

  public DataMessieRoles getRoles(final Long userId) {
    if (userId == null) {
      return DataMessieRoles.getEmptyRoles();
    } else {
      return DataMessieRoles.getAllRoles();
    }
  }

  public byte[] createSalt() {
    SecureRandom sr;
    try {
      sr = SecureRandom.getInstance("SHA1PRNG");
      final byte[] salt = new byte[512 / 8];
      sr.nextBytes(salt);
      return salt;
    } catch (final NoSuchAlgorithmException e) {
      LOG.error("Could not create salt", e);
      return null;
    }
  }

  public byte[] calculateHash(final String password, final byte[] salt) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-512");
      md.update(salt);
      final byte[] hash = md.digest(password.getBytes());
      return hash;
    } catch (final NoSuchAlgorithmException e) {
      LOG.error("Could not calculate hash", e);
      return null;
    }
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
