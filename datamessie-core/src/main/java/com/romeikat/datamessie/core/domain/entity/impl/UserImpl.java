package com.romeikat.datamessie.core.domain.entity.impl;

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import com.romeikat.datamessie.core.domain.entity.AbstractEntityWithGeneratedIdAndVersion;
import com.romeikat.datamessie.core.domain.entity.User;

@Entity
@Table(name = UserImpl.TABLE_NAME,
    uniqueConstraints = {
        @UniqueConstraint(name = "user_id_version", columnNames = {"id", "version"}),
        @UniqueConstraint(name = "user_username", columnNames = {"username"})})
public class UserImpl extends AbstractEntityWithGeneratedIdAndVersion implements User {

  public static final String TABLE_NAME = "user";

  private String username;

  private byte[] passwordSalt;

  private byte[] passwordHash;

  public UserImpl() {}

  public UserImpl(final long id, final String username, final byte[] passwordSalt,
      final byte[] passwordHash) {
    super(id);
    this.username = username;
    this.passwordSalt = passwordSalt;
    this.passwordHash = passwordHash;
  }

  @Override
  @Column(nullable = false)
  public String getUsername() {
    return username;
  }

  @Override
  public User setUsername(final String username) {
    this.username = username;
    return this;
  }

  @Override
  @Column(nullable = false)
  public byte[] getPasswordSalt() {
    return passwordSalt;
  }

  @Override
  public void setPasswordSalt(final byte[] passwordSalt) {
    this.passwordSalt = passwordSalt;
  }

  @Override
  @Column(nullable = false)
  public byte[] getPasswordHash() {
    return passwordHash;
  }

  @Override
  public void setPasswordHash(final byte[] passwordHash) {
    this.passwordHash = passwordHash;
  }


}
