package com.romeikat.datamessie.core.statistics.task;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
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

import org.springframework.context.ApplicationContext;
import com.romeikat.datamessie.core.base.task.management.TaskCancelledException;
import com.romeikat.datamessie.core.base.task.management.TaskExecution;
import com.romeikat.datamessie.core.base.task.management.TaskExecutionWork;
import com.romeikat.datamessie.core.base.util.SpringUtil;
import com.romeikat.datamessie.core.statistics.service.SmsService;
import com.romeikat.datamessie.core.statistics.service.StatisticsStatusService;

public class StatisticsChecker {

  private final StatisticsStatusService statisticsStatusService;
  private final SmsService smsService;
  private final boolean statisticsCheckingSmsEnabled;

  private TaskExecutionWork work;

  public StatisticsChecker(final ApplicationContext ctx) {
    statisticsStatusService = ctx.getBean(StatisticsStatusService.class);
    smsService = ctx.getBean(SmsService.class);
    statisticsCheckingSmsEnabled =
        Boolean.parseBoolean(SpringUtil.getPropertyValue(ctx, "statistics.checking.sms.enabled"));
  }

  public void performChecking(final TaskExecution taskExecution) throws TaskCancelledException {
    // Create report
    work = taskExecution.reportWorkStart("Creating statistics status report");
    final String report = statisticsStatusService.createStatisticsStatusReport();
    taskExecution.reportWorkEnd(work);
    taskExecution.checkpoint();

    // Send report
    if (statisticsCheckingSmsEnabled) {
      work = taskExecution.reportWorkStart("Reporting critical sources");
      smsService.sendSms(report);
      taskExecution.reportWorkEnd(work);
      taskExecution.checkpoint();
    }

    taskExecution.reportWorkEnd(work);
  }

}
