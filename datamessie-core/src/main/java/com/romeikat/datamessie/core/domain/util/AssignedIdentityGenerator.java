package com.romeikat.datamessie.core.domain.util;

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

import java.io.Serializable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;

public class AssignedIdentityGenerator extends IdentityGenerator {

  @Override
  public Serializable generate(final SharedSessionContractImplementor session, final Object obj) {
    if (obj instanceof Identifiable) {
      final Identifiable identifiable = (Identifiable) obj;
      final long id = identifiable.getId();
      if (id != 0) {
        return id;
      }
    }

    return super.generate(session, obj);
  }

}
