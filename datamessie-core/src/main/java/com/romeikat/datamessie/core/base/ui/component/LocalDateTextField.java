package com.romeikat.datamessie.core.base.ui.component;

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

import java.time.LocalDate;
import java.util.Date;

import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.romeikat.datamessie.core.base.util.DateUtil;

public class LocalDateTextField extends DateTextField {

  private static final long serialVersionUID = 1L;

  private IModel<Date> dateModel;

  public LocalDateTextField(final String id, final LocalDate localDate, final DateConverter converter) {
    this(id, new Model<LocalDate>(), converter);
    setModelObject(localDate);
  }

  public LocalDateTextField(final String id, final IModel<LocalDate> localDateModel, final DateConverter converter) {
    super(id, converter);

    // Converting model
    dateModel = new Model<Date>() {

      private static final long serialVersionUID = 1L;

      @Override
      public Date getObject() {
        final LocalDate localDate = localDateModel.getObject();
        final Date date = localDate == null ? null : DateUtil.fromLocalDate(localDate);
        return date;
      }

      @Override
      public void setObject(final Date date) {
        final LocalDate localDate = date == null ? null : DateUtil.toLocalDate(date);
        localDateModel.setObject(localDate);
      }

    };
    setModel(dateModel);
  }

  public void setModelObject(final LocalDate localDate) {
    final Date date = localDate == null ? null : DateUtil.fromLocalDate(localDate);
    setModelObject(date);
  }

}
