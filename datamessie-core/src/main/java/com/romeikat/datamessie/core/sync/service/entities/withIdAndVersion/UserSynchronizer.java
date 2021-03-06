package com.romeikat.datamessie.core.sync.service.entities.withIdAndVersion;

import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

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

import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.dao.EntityWithIdAndVersionDao;
import com.romeikat.datamessie.core.base.dao.impl.UserDao;
import com.romeikat.datamessie.core.domain.entity.impl.User;
import com.romeikat.datamessie.core.sync.service.entities.withoutIdAndVersion.Project2UserSynchronizer;
import com.romeikat.datamessie.core.sync.service.template.withIdAndVersion.EntityWithIdAndVersionSynchronizer;
import com.romeikat.datamessie.core.sync.util.SyncData;

public class UserSynchronizer extends EntityWithIdAndVersionSynchronizer<User> {

  // Input
  private final Project2UserSynchronizer project2UserSynchronizer;

  public UserSynchronizer(final Project2UserSynchronizer project2UserSynchronizer,
      final ApplicationContext ctx) {
    super(User.class, ctx);
    this.project2UserSynchronizer = project2UserSynchronizer;
  }

  @Override
  protected boolean appliesFor(final SyncData syncData) {
    return syncData.shouldUpdateOriginalData();
  }

  @Override
  protected Predicate<Pair<Long, Long>> getLhsIdFilter() {
    return idAndVersion -> project2UserSynchronizer.getUserIds().contains(idAndVersion.getKey());
  }

  @Override
  protected void copyProperties(final User source, final User target) {
    target.setUsername(source.getUsername());
    target.setPasswordSalt(source.getPasswordSalt());
    target.setPasswordHash(source.getPasswordHash());
  }

  @Override
  protected EntityWithIdAndVersionDao<User> getDao(final ApplicationContext ctx) {
    return ctx.getBean(UserDao.class);
  }

}
