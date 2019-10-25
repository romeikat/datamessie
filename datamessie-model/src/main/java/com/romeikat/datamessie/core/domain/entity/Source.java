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

import com.romeikat.datamessie.core.domain.enums.Language;

public interface Source extends EntityWithIdAndVersion {

  public String getName();

  public Source setName(String name);

  public Language getLanguage();

  public Source setLanguage(Language language);

  public String getUrl();

  public Source setUrl(String url);

  public boolean getVisible();

  public Source setVisible(boolean visible);

  public boolean getStatisticsChecking();

  public void setStatisticsChecking(boolean statisticsChecking);

}
