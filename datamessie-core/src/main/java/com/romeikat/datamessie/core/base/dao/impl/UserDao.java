package com.romeikat.datamessie.core.base.dao.impl;

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

import org.hibernate.SharedSessionContract;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import com.romeikat.datamessie.core.base.query.entity.EntityWithIdQuery;
import com.romeikat.datamessie.core.domain.entity.impl.User;

@Repository
public class UserDao extends AbstractEntityWithIdAndVersionDao<User> {

  public UserDao() {
    super(User.class);
  }

  @Override
  protected String defaultSortingProperty() {
    return "username";
  }

  public User get(final SharedSessionContract ssc, final String username) {
    // Query: User
    final EntityWithIdQuery<User> userQuery = new EntityWithIdQuery<>(User.class);
    userQuery.addRestriction(Restrictions.eq("username", username));

    // Done
    final User user = userQuery.uniqueObject(ssc);
    return user;
  }

}
