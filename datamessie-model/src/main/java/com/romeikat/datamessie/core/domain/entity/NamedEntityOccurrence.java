package com.romeikat.datamessie.core.domain.entity;

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

import com.romeikat.datamessie.core.domain.enums.NamedEntityType;

public interface NamedEntityOccurrence extends EntityWithIdAndVersion {

  public long getNamedEntityId();

  public NamedEntityOccurrence setNamedEntityId(final long namedEntityId);

  public long getParentNamedEntityId();

  public NamedEntityOccurrence setParentNamedEntityId(final long parentNamedEntityId);

  public NamedEntityType getType();

  public NamedEntityOccurrence setType(final NamedEntityType type);

  public int getQuantity();

  public NamedEntityOccurrence setQuantity(final int quantity);

  public long getDocumentId();

  public NamedEntityOccurrence setDocumentId(final long documentId);

  public boolean hasDifferentParent();

}
