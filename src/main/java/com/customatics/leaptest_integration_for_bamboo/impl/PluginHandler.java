package com.customatics.leaptest_integration_for_bamboo.impl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.customatics.leaptest_integration_for_bamboo.model.Case;
import com.customatics.leaptest_integration_for_bamboo.model.Schedule;
import com.customatics.leaptest_integration_for_bamboo.model.ScheduleCollection;
import com.customatics.leaptest_integration_for_bamboo.model.InvalidSchedule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;


public final class PluginHandler {

    private static PluginHandler pluginHandler = null;

    private PluginHandler(){}

    public static PluginHandler getInstance()
    {
        if( pluginHandler == null ) pluginHandler = new PluginHandler();

        return pluginHandler;
    }


    public String getJunitReportFilePath(TaskContext taskContext, String reportFileName)
    {
        if(reportFileName.isEmpty() || "".equals(reportFileName))
        {
            reportFileName = "report.xml";
        }

        if(!reportFileName.contains(".xml"))
        {
            reportFileName +=".xml";
        }

        String bambooWorkspacePath = taskContext.getWorkingDirectory().getAbsolutePath();

        return String.format("%1$s/%2$s",bambooWorkspacePath,reportFileName);
    }

    public ArrayList<String> getRawScheduleList(String rawScheduleIds, String rawScheduleTitles)
    {
        ArrayList<String> rawScheduleList = new ArrayList<>();

        String[] schidsArray = rawScheduleIds.split("\n|, |,");
        String[] testsArray = rawScheduleTitles.split("\n|, |,");

        for(int i = 0; i < schidsArray.length; i++)
        {
            rawScheduleList.add(schidsArray[i]);
        }
        for(int i = 0; i < testsArray.length; i++)
        {
            rawScheduleList.add(testsArray[i]);
        }

        return rawScheduleList;
    }

    public int getTimeDelay(String rawTimeDelay)
    {
        int defaultTimeDelay = 3;
        if(!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay))
            return Integer.parseInt(rawTimeDelay);
        else
            return defaultTimeDelay;
    }


    public HashMap<String, String> getSchedulesIdTitleHashMap(
            String leaptestAddress,
            ArrayList<String> rawScheduleList,
            BuildLogger buildLogger,
            ScheduleCollection buildResult,
            ArrayList<InvalidSchedule> invalidSchedules
    )
    {

        HashMap<String, String> schedulesIdTitleHashMap = new HashMap<>();

        String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, leaptestAddress);

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(scheduleListUri).execute().get();
            client = null;

            switch (response.getStatusCode())
            {
                case 200:
                    JsonParser parser = new JsonParser();
                    JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

                    for (String rawSchedule : rawScheduleList) {
                        boolean successfullyMapped = false;
                        for (JsonElement jsonScheduleElement : jsonScheduleList) {
                            JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

                            String Id = Utils.defaultStringIfNull(jsonSchedule.get("Id"), "null Id");
                            String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");

                            if (Id.contentEquals(rawSchedule)) {
                                if (!schedulesIdTitleHashMap.containsValue(Title)) {
                                    schedulesIdTitleHashMap.put(rawSchedule, Title);
                                    buildResult.Schedules.add(new Schedule(rawSchedule, Title));
                                    buildLogger.addBuildLogEntry(String.format(Messages.SCHEDULE_DETECTED, Title, rawSchedule));
                                }
                                successfullyMapped = true;
                            }

                            if (Title.contentEquals(rawSchedule)) {
                                if (!schedulesIdTitleHashMap.containsKey(Id)) {
                                    schedulesIdTitleHashMap.put(Id, rawSchedule);
                                    buildResult.Schedules.add(new Schedule(Id, rawSchedule));
                                    buildLogger.addBuildLogEntry(String.format(Messages.SCHEDULE_DETECTED, rawSchedule, Title));
                                }
                                successfullyMapped = true;
                            }
                        }

                        if (!successfullyMapped)
                            invalidSchedules.add(new InvalidSchedule(rawSchedule, Messages.NO_SUCH_SCHEDULE));
                    }
                break;

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s",Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (ConnectException e){
            buildLogger.addErrorLogEntry( e.getMessage());
        } catch (InterruptedException e) {
            buildLogger.addErrorLogEntry( e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry( e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry( e.getMessage());
        } catch (Exception e) {
            buildLogger.addErrorLogEntry(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
            buildLogger.addErrorLogEntry(e.getMessage());
            buildLogger.addErrorLogEntry(Messages.PLEASE_CONTACT_SUPPORT);
        } finally {
            return schedulesIdTitleHashMap;
        }
    }

    public boolean runSchedule(
            String leaptestAddress,
            HashMap.Entry<String,String> schedule,
            int currentScheduleIndex,
            BuildLogger buildLogger,
            ScheduleCollection buildResult,
            ArrayList<InvalidSchedule> invalidSchedules
    )
    {
        boolean isSuccessfullyRun = false;

        String uri = String.format(Messages.RUN_SCHEDULE_URI, leaptestAddress, schedule.getKey());

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;

            switch (response.getStatusCode())
            {
                case 204:
                    isSuccessfullyRun = true;
                    String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, schedule.getValue(), schedule.getKey());
                    buildResult.Schedules.get(currentScheduleIndex).setId(currentScheduleIndex);
                    buildLogger.addBuildLogEntry(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
                    buildLogger.addBuildLogEntry(successMessage);
                    break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s",String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage404);

                case 444:
                    String errorMessage444 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage444 += String.format("\n%1$s",String.format(Messages.SCHEDULE_HAS_NO_CASES,schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage444);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s",String.format(Messages.SCHEDULE_IS_RUNNING_NOW, schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (InterruptedException e) {
            buildResult.Schedules.get(currentScheduleIndex).setError(e.getMessage());
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (ExecutionException e) {
            buildResult.Schedules.get(currentScheduleIndex).setError(e.getMessage());
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (IOException e) {
            buildResult.Schedules.get(currentScheduleIndex).setError(e.getMessage());
            buildLogger.addErrorLogEntry(e.getMessage());
        }
        catch (Exception e){
            String errorMessage = String.format(Messages.SCHEDULE_RUN_FAILURE,  schedule.getValue(), schedule.getKey());
            buildLogger.addErrorLogEntry(errorMessage);
            buildLogger.addErrorLogEntry(e.getMessage());
            buildLogger.addErrorLogEntry(Messages.PLEASE_CONTACT_SUPPORT);
            buildResult.Schedules.get(currentScheduleIndex).setError(String.format("%1$s\n%2$s",errorMessage,e.getMessage()));
            buildResult.Schedules.get(currentScheduleIndex).incErrors();
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT,schedule.getValue(),schedule.getKey()),buildResult.Schedules.get(currentScheduleIndex).getError()));

        }
        finally {
            return isSuccessfullyRun;
        }
    }

    public boolean getScheduleState(
            String leaptestAddress,
            HashMap.Entry<String,String> schedule,
            int currentScheduleIndex,
            String doneStatusValue,
            BuildLogger buildLogger,
            ScheduleCollection buildResult,
            ArrayList<InvalidSchedule> invalidSchedules
    )
    {
        boolean isScheduleStillRunning = true;

        String uri = String.format(Messages.GET_SCHEDULE_STATE_URI, leaptestAddress, schedule.getKey());

        try {

            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;

            switch (response.getStatusCode()) {
                case 200:

                    JsonParser parser = new JsonParser();
                    JsonObject jsonState = parser.parse(response.getResponseBody()).getAsJsonObject();
                    parser = null;

                    String ScheduleId = jsonState.get("ScheduleId").getAsString();

                    if (isScheduleStillRunning(jsonState))
                        isScheduleStillRunning = true;
                    else {
                        isScheduleStillRunning = false;

                        /////////Schedule Info
                        JsonElement jsonLastRun = jsonState.get("LastRun");


                            JsonObject lastRun = jsonLastRun.getAsJsonObject();

                            String ScheduleTitle = lastRun.get("ScheduleTitle").getAsString();

                            buildResult.Schedules.get(currentScheduleIndex).setTime(parseExecutionTimeToSeconds(lastRun.get("ExecutionTotalTime")));

                            int passedCount = caseStatusCount("PassedCount", lastRun);
                            int failedCount = caseStatusCount("FailedCount", lastRun);
                            int doneCount = caseStatusCount("DoneCount", lastRun);

                            if (doneStatusValue.contentEquals("Failed"))
                                failedCount += doneCount;
                            else
                                passedCount += doneCount;


                            ///////////AutomationRunItemsInfo
                            JsonArray jsonAutomationRunItems = lastRun.get("AutomationRunItems").getAsJsonArray();

                            ArrayList<String> automationRunId = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                automationRunId.add(jsonAutomationRunItem.getAsJsonObject().get("AutomationRunId").getAsString());
                            ArrayList<String> statuses = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                statuses.add(jsonAutomationRunItem.getAsJsonObject().get("Status").getAsString());
                            ArrayList<String> elapsed = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                elapsed.add(defaultElapsedIfNull(jsonAutomationRunItem.getAsJsonObject().get("Elapsed")));
                            ArrayList<String> environments = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                environments.add(jsonAutomationRunItem.getAsJsonObject().get("Environment").getAsJsonObject().get("Title").getAsString());

                            ArrayList<String> caseTitles = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems) {
                                String caseTitle = Utils.defaultStringIfNull(jsonAutomationRunItem.getAsJsonObject().get("Case").getAsJsonObject().get("Title"), "Null case Title");
                                if (caseTitle.contentEquals("Null case Title"))
                                    caseTitles.add(caseTitles.get(caseTitles.size() - 1));
                                else
                                    caseTitles.add(caseTitle);
                            }

                            makeCaseTitlesNonRepeatable(caseTitles); //this is required because Bamboo junit reporter does not suppose that there can be 2 or more case with the same name but different results

                            for (int i = 0; i < jsonAutomationRunItems.size(); i++) {

                                //double seconds = jsonArray.getJSONObject(i).getDouble("TotalSeconds");
                                double seconds = parseExecutionTimeToSeconds(elapsed.get(i));

                                buildLogger.addBuildLogEntry(Messages.CASE_CONSOLE_LOG_SEPARATOR);

                                if (statuses.get(i).contentEquals("Failed") || (statuses.get(i).contentEquals("Done") && doneStatusValue.contentEquals("Failed")) || statuses.get(i).contentEquals("Error") || statuses.get(i).contentEquals("Cancelled")) {
                                    if(statuses.get(i).contentEquals("Error") || statuses.get(i).contentEquals("Cancelled"))
                                        failedCount++;

                                    JsonArray jsonKeyframes = jsonAutomationRunItems.get(i).getAsJsonObject().get("Keyframes").getAsJsonArray();

                                    //KeyframeInfo
                                    ArrayList<String> keyFrameTimeStamps = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameTimeStamps.add(jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsString());
                                    ArrayList<String> keyFrameLogMessages = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameLogMessages.add(jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString());

                                    buildLogger.addBuildLogEntry(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));

                                    String fullstacktrace = "";
                                    int currentKeyFrameIndex = 0;

                                    for (JsonElement jsonKeyFrame : jsonKeyframes) {
                                        String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "");
                                        if (!level.contentEquals("") && !level.contentEquals("Trace")) {
                                            String stacktrace = String.format(Messages.CASE_STACKTRACE_FORMAT, keyFrameTimeStamps.get(currentKeyFrameIndex), keyFrameLogMessages.get(currentKeyFrameIndex));
                                            buildLogger.addBuildLogEntry(stacktrace);
                                            fullstacktrace += stacktrace;
                                            fullstacktrace += "&#xA;"; //fullstacktrace += '\n';
                                        }

                                        currentKeyFrameIndex++;
                                    }

                                    fullstacktrace += "Environment: " + environments.get(i);
                                    buildLogger.addBuildLogEntry("Environment: " + environments.get(i));
                                    buildResult.Schedules.get(currentScheduleIndex).Cases.add(new Case(caseTitles.get(i), statuses.get(i), seconds, fullstacktrace, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                                } else {
                                    buildLogger.addBuildLogEntry(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));
                                    buildResult.Schedules.get(currentScheduleIndex).Cases.add(new Case(caseTitles.get(i), statuses.get(i), seconds, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                                }
                            }

                            buildResult.Schedules.get(currentScheduleIndex).setPassed(passedCount);
                            buildResult.Schedules.get(currentScheduleIndex).setFailed(failedCount);

                            if (buildResult.Schedules.get(currentScheduleIndex).getFailed() > 0)
                                buildResult.Schedules.get(currentScheduleIndex).setStatus("Failed");
                            else
                                buildResult.Schedules.get(currentScheduleIndex).setStatus("Passed");
                    }
                break;

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s",String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, schedule.getValue(), schedule.getKey()));
                    throw new Exception(errorMessage404);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s",Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }

        } catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(currentScheduleIndex).setError(e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(currentScheduleIndex).setError(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(currentScheduleIndex).setError(e.getMessage());
        } catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_STATE_FAILURE, schedule.getValue(), schedule.getKey());
            buildResult.Schedules.get(currentScheduleIndex).setError(String.format("%1$s\n%2$s",errorMessage,e.getMessage()));
            buildResult.Schedules.get(currentScheduleIndex).incErrors();
            buildLogger.addErrorLogEntry(errorMessage);
            buildLogger.addErrorLogEntry(e.getMessage());
            buildLogger.addErrorLogEntry(Messages.PLEASE_CONTACT_SUPPORT);
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT,schedule.getValue(),schedule.getKey()),buildResult.Schedules.get(currentScheduleIndex).getError()));
        } finally {
            return isScheduleStillRunning;
        }
    }

    public void createJUnitReport(String JUnitReportFilePath, BuildLogger buildLogger, ScheduleCollection buildResult)
    {
        try
        {
            File reportFile = new File(JUnitReportFilePath);
            if(!reportFile.exists()) reportFile.createNewFile();

            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(ScheduleCollection.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(buildResult, writer);

            StringWriter formattedWriter  =  new StringWriter();
            formattedWriter.append(writer.getBuffer().toString().replace("&amp;#xA;","&#xA;"));

            writer = null;

            try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()))) {
                out.print(formattedWriter);
                out.close();
            }

        }
        catch (FileNotFoundException e) {
            buildLogger.addErrorLogEntry(Messages.REPORT_FILE_NOT_FOUND);
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(Messages.REPORT_FILE_CREATION_FAILURE);
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (JAXBException e) {
            buildLogger.addErrorLogEntry(Messages.REPORT_FILE_CREATION_FAILURE);
            buildLogger.addErrorLogEntry(e.getMessage());
        }
    }

    private boolean isScheduleStillRunning(JsonObject jsonState)
    {
        String status = Utils.defaultStringIfNull(jsonState.get("Status"), "Finished");

        if (status.contentEquals("Running") || status.contentEquals("Queued"))
            return true;
        else
            return false;

    }

    private double parseExecutionTimeToSeconds(String rawExecutionTime)
    {
        String ExecutionTotalTime[] = rawExecutionTime.split(":|\\.");

        return  Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 +  //hours
                Double.parseDouble(ExecutionTotalTime[1]) * 60 +        //minutes
                Double.parseDouble(ExecutionTotalTime[2]) +             //seconds
                Double.parseDouble("0." + ExecutionTotalTime[3]);     //milliseconds
    }

    private double parseExecutionTimeToSeconds(JsonElement rawExecutionTime)
    {
        if(rawExecutionTime != null) {
            String ExecutionTotalTime[] = rawExecutionTime.getAsString().split(":|\\.");

            return Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 +  //hours
                    Double.parseDouble(ExecutionTotalTime[1]) * 60 +        //minutes
                    Double.parseDouble(ExecutionTotalTime[2]) +             //seconds
                    Double.parseDouble("0." + ExecutionTotalTime[3]);     //milliseconds
        }
        else
            return 0;
    }

    private int caseStatusCount(String statusName, JsonObject lastRun)
    {
        Integer temp =  Utils.defaultIntIfNull(lastRun.get(statusName), 0);
        return temp.intValue();
    }

    private void makeCaseTitlesNonRepeatable(ArrayList<String> caseTitles)
    {
        HashSet<String> NotRepeatedNamesSet = new HashSet<String>(caseTitles);
        String[] NotRepeatedNames = NotRepeatedNamesSet.toArray(new String[NotRepeatedNamesSet.size()]);
        NotRepeatedNamesSet = null;

        for(int i = 0; i < NotRepeatedNames.length; i++)
        {
            int count = 0;
            for(int j = 0; j < caseTitles.size(); j++)
            {
                if(NotRepeatedNames[i].equals(caseTitles.get(j)))
                {
                    if(count > 0)
                    {
                        caseTitles.set(j,String.format("%1$s(%2$d)",caseTitles.get(j),count));
                    }
                    count++;
                }
            }
        }

        NotRepeatedNames = null;
    }

    private String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
            return rawElapsed.getAsString();
        else
            return "00:00:00.0000000";

    }
}