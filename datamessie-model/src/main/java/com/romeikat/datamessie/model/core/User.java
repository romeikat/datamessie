package com.romeikat.datamessie.model.core;

import com.romeikat.datamessie.model.EntityWithIdAndVersion;

/*-
 * ============================LICENSE_START============================
 * data.messie (model)
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

public interface User extends EntityWithIdAndVersion {

  String getUsername();

  User setUsername(String username);

  byte[] getPasswordSalt();

  void setPasswordSalt(byte[] passwordSalt);

  byte[] getPasswordHash();

  void setPasswordHash(byte[] passwordHash);

}
