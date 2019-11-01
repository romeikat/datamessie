package com.romeikat.datamessie.core.domain.entity;

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

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import com.romeikat.datamessie.model.EntityWithIdAndVersion;

@MappedSuperclass
public abstract class AbstractEntityWithIdAndVersion implements EntityWithIdAndVersion {

  private Long version = 0l;

  public AbstractEntityWithIdAndVersion() {}

  public AbstractEntityWithIdAndVersion(final long id) {
    setId(id);
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(getClass().getSimpleName());
    result.append(" [");

    result.append("id=");
    result.append(getId());

    result.append("]");

    return result.toString();
  }

  @Override
  @Version
  public Long getVersion() {
    return version;
  }

  @Override
  public void setVersion(final Long version) {
    this.version = version;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || (this.getClass() != object.getClass())) {
      return false;
    }
    final AbstractEntityWithIdAndVersion other = (AbstractEntityWithIdAndVersion) object;
    // Fallback
    if (version == null || other.version == null) {
      return super.equals(object);
    }
    // Determine according to id and version
    return getId() == other.getId() && version.equals(other.version);
  }

  @Override
  public int hashCode() {
    // Fallback
    if (version == null) {
      return super.hashCode();
    }
    // Calculate according to id and version
    int result = 0;
    result = 31 * result + new Long(getId()).intValue();
    result = 31 * result + version.intValue();
    return result;
  }

}
