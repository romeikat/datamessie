package com.romeikat.datamessie.core.base.service;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2018 Dr. Raphael Romeikat
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.romeikat.datamessie.core.AbstractTest;

public class AuthenticationServiceTest extends AbstractTest {

  private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceTest.class);

  @Autowired
  private AuthenticationService authenticationService;

  @Test
  public void authenticate_success() throws DecoderException {
    final String password = "test";
    final String passwordSalt =
        "27dc26310555341f18bb1c551550df98dd36fcde224c7fd6ddb25f9f6948c924aad52067ee6e8d82aeff57250cefd5064598c4b753331dfc44550cc982d37292";
    final String passwordHash =
        "e42f3bea3fca8b5c03af1ef5a069994a73445267d52e486eebccce0ba268fc3ae8fb22ca9616ced487f0f777d2b631e4f7078b97e5e2e85e1eea99c0d98cc95f";

    final byte[] passwordSaltBytes = Hex.decodeHex(passwordSalt.toCharArray());
    final byte[] passwordHashBytes = Hex.decodeHex(passwordHash.toCharArray());

    final boolean authenticated =
        authenticationService.authenticate(password, passwordSaltBytes, passwordHashBytes);
    assertTrue(authenticated);
  }

  @Test
  public void authenticate_failSalt() throws DecoderException {
    final String password = "test";
    final String wrongPasswordSalt = "00";
    final String passwordHash =
        "e42f3bea3fca8b5c03af1ef5a069994a73445267d52e486eebccce0ba268fc3ae8fb22ca9616ced487f0f777d2b631e4f7078b97e5e2e85e1eea99c0d98cc95f";

    final byte[] passwordSaltBytes = Hex.decodeHex(wrongPasswordSalt.toCharArray());
    final byte[] passwordHashBytes = Hex.decodeHex(passwordHash.toCharArray());

    final boolean authenticated =
        authenticationService.authenticate(password, passwordSaltBytes, passwordHashBytes);
    assertFalse(authenticated);
  }

  @Test
  public void authenticate_failHash() throws DecoderException {
    final String password = "test";
    final String passwordSalt =
        "27dc26310555341f18bb1c551550df98dd36fcde224c7fd6ddb25f9f6948c924aad52067ee6e8d82aeff57250cefd5064598c4b753331dfc44550cc982d37292";
    final String wrongPasswordHash = "00";

    final byte[] passwordSaltBytes = Hex.decodeHex(passwordSalt.toCharArray());
    final byte[] passwordHashBytes = Hex.decodeHex(wrongPasswordHash.toCharArray());

    final boolean authenticated =
        authenticationService.authenticate(password, passwordSaltBytes, passwordHashBytes);
    assertFalse(authenticated);
  }

  @Test
  @Ignore
  public void createSaltAndHash() {
    final String password = "test";
    final byte[] salt = authenticationService.createSalt();
    final byte[] hash = authenticationService.calculateHash(password, salt);

    LOG.debug("Password: {}", password);
    LOG.debug("Salt: {}", Hex.encodeHexString(salt));
    LOG.debug("Hash: {}", Hex.encodeHexString(hash));
  }

}
