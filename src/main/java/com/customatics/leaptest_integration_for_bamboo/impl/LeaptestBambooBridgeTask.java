package com.customatics.leaptest_integration_for_bamboo.impl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.build.test.TestCollectionResult;
import com.atlassian.bamboo.build.test.junit.JunitTestReportCollector;
import com.atlassian.bamboo.plugins.testresultparser.task.JUnitResultParserTask;
import com.atlassian.bamboo.task.*;
import com.customatics.leaptest_integration_for_bamboo.model.Case;
import com.customatics.leaptest_integration_for_bamboo.model.InvalidSchedule;
import com.customatics.leaptest_integration_for_bamboo.model.Schedule;
import com.customatics.leaptest_integration_for_bamboo.model.ScheduleCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;


public class LeaptestBambooBridgeTask implements TaskType {


    TaskResult result;

    private static PluginHandler pluginHandler = PluginHandler.getInstance();

    private final TestCollationService testCollationService;

    public LeaptestBambooBridgeTask(TestCollationService testCollationService)
    {
        this.testCollationService = testCollationService;
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException
    {
        final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(taskContext);

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
        final Boolean autoReportEnabled = Boolean.valueOf(taskContext.getConfigurationMap().get("autoReport"));
        buildLogger.addBuildLogEntry(String.format("autoReportEnabled = %1$s",taskContext.getConfigurationMap().get("autoReport")));
        String junitReportPath = pluginHandler.getJunitReportFilePath(taskContext, report);
        buildLogger.addBuildLogEntry(junitReportPath);

        String schId = null;
        String schTitle = null;

        rawScheduleList = pluginHandler.getRawScheduleList(schIds,schNames);

        int timeDelay = pluginHandler.getTimeDelay(delay);

        try
        {
            //Get schedule titles (or/and ids in case of pipeline)
            schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(address,rawScheduleList,buildLogger,buildResult,invalidSchedules);
            rawScheduleList = null;

            if(schedulesIdTitleHashMap.isEmpty())
            {
                throw new Exception(Messages.NO_SCHEDULES);
            }

            List<String> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());

            int currentScheduleIndex = 0;
            boolean needSomeSleep = false;   //this time is required if there are schedules to rerun left

            while(!schIdsList.isEmpty())
            {

                if(needSomeSleep) {
                    Thread.sleep(timeDelay * 1000); //Time delay
                    needSomeSleep = false;
                }

                for(ListIterator<String> iter = schIdsList.listIterator(); iter.hasNext(); )
                {
                    schId = iter.next();
                    schTitle = schedulesIdTitleHashMap.get(schId);
                    RUN_RESULT runResult = pluginHandler.runSchedule(address, schId, schTitle, currentScheduleIndex, buildLogger,  buildResult, invalidSchedules);
                    buildLogger.addBuildLogEntry("Current schedule index: " + currentScheduleIndex);

                    if (runResult.equals(RUN_RESULT.RUN_SUCCESS)) // if schedule was successfully run
                    {
                        boolean isStillRunning = true;

                        do
                        {
                            Thread.sleep(timeDelay * 1000); //Time delay
                            isStillRunning = pluginHandler.getScheduleState(address,schId,schTitle,currentScheduleIndex, doneStatusAs,buildLogger, buildResult, invalidSchedules);
                            if(isStillRunning) buildLogger.addBuildLogEntry(String.format(Messages.SCHEDULE_IS_STILL_RUNNING, schTitle, schId));
                        }
                        while (isStillRunning);

                        iter.remove();
                        currentScheduleIndex++;
                    }
                    else if (runResult.equals(RUN_RESULT.RUN_REPEAT))
                    {
                        needSomeSleep = true;
                    }
                    else
                    {
                        iter.remove();
                        currentScheduleIndex++;
                    }
                }
            }

            schIdsList = null;
            schedulesIdTitleHashMap = null;

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

            File reportFile = pluginHandler.createJUnitReport(junitReportPath,buildLogger,buildResult);



            if(autoReportEnabled)
            {
                testCollationService.collateTestResults(taskContext, report, new JunitTestReportCollector(), true );
                result = taskResultBuilder.checkTestFailures().build();
            }
            else
            {
                if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0 || invalidSchedules.size() > 0)
                {
                    result = taskResultBuilder.failed().build();
                    buildLogger.addBuildLogEntry("FAILURE");
                }
                else
                {
                    result = taskResultBuilder.success().build();

                    buildLogger.addBuildLogEntry("SUCCESS");
                }
            }

            buildLogger.addBuildLogEntry(Messages.PLUGIN_SUCCESSFUL_FINISH);

        }

        catch (InterruptedException e)
        {
            String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
            buildLogger.addErrorLogEntry(interruptedExceptionMessage);
            pluginHandler.stopSchedule(address,schId,schTitle, buildLogger);
            result = taskResultBuilder.failedWithError().build();
            buildLogger.addErrorLogEntry("ABORTED");

        }
        catch (Exception e)
        {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildLogger.addErrorLogEntry(Messages.PLUGIN_ERROR_FINISH);
            buildLogger.addErrorLogEntry("ERROR");
            result = taskResultBuilder.failedWithError().build();
            buildLogger.addErrorLogEntry(Messages.PLEASE_CONTACT_SUPPORT);
        } finally {
            return result;
        }
    }



}
