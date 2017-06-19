package com.customatics.leaptest_integration_for_bamboo.impl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.customatics.leaptest_integration_for_bamboo.model.Case;
import com.customatics.leaptest_integration_for_bamboo.model.InvalidSchedule;
import com.customatics.leaptest_integration_for_bamboo.model.Schedule;
import com.customatics.leaptest_integration_for_bamboo.model.ScheduleCollection;
import java.util.ArrayList;
import java.util.HashMap;


public class LeaptestBambooBridgeTask implements TaskType {


    TaskResult result;
    private static PluginHandler pluginHandler = PluginHandler.getInstance();

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        HashMap<String, String> schedulesIdTitleHashMap = null; // Id-Title
        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
        ScheduleCollection buildResult = new ScheduleCollection();
        ArrayList<String> rawScheduleList = null;

        //get fields value
        final String address = taskContext.getConfigurationMap().get("address");
        final String delay = taskContext.getConfigurationMap().get("delay");
        final String doneStatusAs = taskContext.getConfigurationMap().get("doneStatusAs");
        final String report = taskContext.getConfigurationMap().get("report");
        final String schIds = taskContext.getConfigurationMap().get("schIds");
        final String schNames = taskContext.getConfigurationMap().get("schNames");

        String junitReportPath = pluginHandler.getJunitReportFilePath(taskContext, report);
        buildLogger.addBuildLogEntry(junitReportPath);

        rawScheduleList = pluginHandler.getRawScheduleList(schIds,schNames);

        int timeDelay = pluginHandler.getTimeDelay(delay);

        try
        {
            //Get schedule titles (or/and ids in case of pipeline)
            schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(address,rawScheduleList,buildLogger,buildResult,invalidSchedules);
            rawScheduleList = null;


            int currentScheduleIndex = 0;
            for (HashMap.Entry<String,String> schedule : schedulesIdTitleHashMap.entrySet())
            {

                if (pluginHandler.runSchedule(address,schedule, currentScheduleIndex, buildLogger, buildResult, invalidSchedules)) // if schedule was successfully run
                {
                    boolean isStillRunning = true;

                    do
                    {
                        Thread.sleep(timeDelay * 1000); //Time delay
                        isStillRunning = pluginHandler.getScheduleState(address,schedule,currentScheduleIndex, doneStatusAs, buildLogger,buildResult, invalidSchedules);
                    }
                    while (isStillRunning);
                }

                currentScheduleIndex++;
            }

            if (invalidSchedules.size() > 0)
            {
                buildLogger.addBuildLogEntry(Messages.INVALID_SCHEDULES);
                buildResult.Schedules.add(new Schedule(Messages.INVALID_SCHEDULES));

                for (InvalidSchedule invalidSchedule : invalidSchedules)
                {
                    buildLogger.addBuildLogEntry(invalidSchedule.getName());
                    buildResult.Schedules.get(buildResult.Schedules.size() - 1).Cases.add(new Case(invalidSchedule.getName(), "Failed", 0, invalidSchedule.getStackTrace(), "INVALID SCHEDULE"));
                }

            }

            for (Schedule schedule : buildResult.Schedules)
            {
                buildResult.addFailedTests(schedule.getFailed());
                buildResult.addPassedTests(schedule.getPassed());
                buildResult.addErrors(schedule.getErrors());
                schedule.setTotal(schedule.getPassed() + schedule.getFailed());
                buildResult.addTotalTime(schedule.getTime());
            }
            buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

            pluginHandler.createJUnitReport(junitReportPath,buildLogger,buildResult);

            if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0 || invalidSchedules.size() > 0)
                result = TaskResultBuilder.create(taskContext).failed().build();
            else
                result = TaskResultBuilder.create(taskContext).success().build();

            buildLogger.addBuildLogEntry(Messages.PLUGIN_SUCCESSFUL_FINISH);
        }
        catch (IndexOutOfBoundsException e)
        {
            buildLogger.addErrorLogEntry(Messages.NO_SCHEDULES_OR_WRONG_URL_ERROR);
            buildLogger.addErrorLogEntry(e.getMessage());
        }

        catch (Exception e)
        {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildLogger.addErrorLogEntry(Messages.PLUGIN_ERROR_FINISH);
            result = TaskResultBuilder.create(taskContext).failed().build();
        }
        return result;
    }

}
