package com.customatics.leaptest_integration_for_bamboo.impl;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;


public class LeaptestBambooBridgeTask implements TaskType {


    TaskResult result;
    private static LogMessages MESSAGES = LogMessages.getInstance();

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        //get fields value
        final String address = taskContext.getConfigurationMap().get("address");
        final String delay = taskContext.getConfigurationMap().get("delay");
        final String doneStatusAs = taskContext.getConfigurationMap().get("doneStatusAs");
        String report = taskContext.getConfigurationMap().get("report");
        final String schIds = taskContext.getConfigurationMap().get("schIds");
        final String schNames = taskContext.getConfigurationMap().get("schNames");

        if(report.isEmpty() || "".equals(report))
        {
            report = "report.xml";
        }

        if(!report.contains(".xml"))
        {
            report+=".xml";
        }


        String[] schidsArray = schIds.split("\n|, |,");//was "\n"
        String[] testsArray = schNames.split("\n|, |,");//was "\n"

        ArrayList<String> scheduleInfo = new ArrayList<String>();

        for(int i = 0; i < schidsArray.length; i++)
        {
            scheduleInfo.add(schidsArray[i]);
        }
        for(int i = 0; i < testsArray.length; i++)
        {
            scheduleInfo.add(testsArray[i]);
        }
        schidsArray = null;
        testsArray = null;


        HashMap<String, String> InValidSchedules = new HashMap<String, String>();//Id-Stack trace
        ScheduleCollection buildResult = new ScheduleCollection();

        String uri =  String.format(MESSAGES.GET_ALL_AVAILABLE_SCHEDULES_URI, address);

        int timeDelay = 1;
        if(!delay.isEmpty() || !"".equals(delay))
        {timeDelay = Integer.parseInt(delay);}

        String junitReportPath =  report;

        MutableBoolean isRunning = new MutableBoolean(false);
        MutableBoolean successfullyLaunchedSchedule = new MutableBoolean(false);
        HashMap<String, String> schedules = new HashMap<String, String>(); // Id-Title



        try
        {

            GetSchTitlesOrIds(uri, scheduleInfo, buildLogger,  schedules, buildResult, InValidSchedules); //Get schedule titles (or/and ids in case of pipeline)
            scheduleInfo = null;                                        //don't need that anymore
            int index = 0;

            for (HashMap.Entry<String,String> schedule : schedules.entrySet())
            {

                String runUri = String.format(MESSAGES.RUN_SCHEDULE_URI, uri, schedule.getKey());
                String stateUri = String.format(MESSAGES.GET_SCHEDULE_STATE_URI, uri, schedule.getKey());



                RunSchedule(runUri, schedule.getKey(), schedule.getValue(), index, buildLogger, successfullyLaunchedSchedule, buildResult, InValidSchedules); // Run schedule. In case of unsuccessfull run throws exception

                if (successfullyLaunchedSchedule.getValue()) // if schedule was successfully run
                {
                    do
                    {

                        Thread.sleep(timeDelay * 1000); //Time delay
                        GetScheduleState(stateUri, schedule.getKey(), schedule.getValue(), index, buildLogger, isRunning, doneStatusAs, buildResult, InValidSchedules); //Get schedule state info

                    }
                    while (isRunning.getValue());
                }

                index++;
            }

            if (InValidSchedules.size() > 0)
            {
                buildLogger.addBuildLogEntry(MESSAGES.INVALID_SCHEDULES);
                for (String invalidsch : InValidSchedules.keySet())
                { buildLogger.addBuildLogEntry(invalidsch); }

                buildResult.Schedules.add(new Schedule(MESSAGES.INVALID_SCHEDULES));

                ArrayList<String> invSch = new ArrayList<>(InValidSchedules.keySet());
                ArrayList<String> invSchStackTrace = new ArrayList<>(InValidSchedules.values());

                for(int i = 0; i < InValidSchedules.size();i++)
                {
                    buildResult.Schedules.get(buildResult.Schedules.size() - 1).Cases.add(new Case(invSch.get(i), "Failed", 0, invSchStackTrace.get(i), "INVALID SCHEDULE"));
                    //buildResult.Schedules.get(buildResult.Schedules.size()- 1).incErrors();
                }

                invSch = null;
                invSchStackTrace = null;
            }



            for (Schedule sch : buildResult.Schedules)
            {
                buildResult.addFailedTests(sch.getFailed());
                buildResult.addPassedTests(sch.getPassed());
                buildResult.addErrors(sch.getErrors());
                sch.setTotal(sch.getPassed() + sch.getFailed());
                buildResult.addTotalTime(sch.getTime());
            }
            buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

            CreateJunitReport(junitReportPath, buildLogger, taskContext, buildResult);

            if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0)
            {
                result = TaskResultBuilder.create(taskContext).failed().build();

            }
            else
            {
                result = TaskResultBuilder.create(taskContext).success().build();
            }

            InValidSchedules = null;
            buildResult = null;

            buildLogger.addBuildLogEntry(MESSAGES.PLUGIN_SUCCESSFUL_FINISH);
        }

        catch (IndexOutOfBoundsException e)
        {
            buildLogger.addErrorLogEntry(MESSAGES.NO_SCHEDULES_OR_WRONG_URL_ERROR);
            buildLogger.addErrorLogEntry(e.getMessage());
        }

        catch (Exception e)
        {
            buildLogger.addErrorLogEntry(MESSAGES.PLUGIN_ERROR_FINISH);
            result = TaskResultBuilder.create(taskContext).failed().build();
        }
        return result;
    }


    //////////////////////////////////////////////////////

    private static String JsonToBamboo(String str, int current, BuildLogger buildLogger, MutableBoolean isRunning, String doneStatus, ScheduleCollection buildResult, HashMap<String, String> InValidSchedules)
    {

        String BambooMessage = "";

        JSONObject json = new JSONObject(str);

        String ScheduleId = json.getString("ScheduleId");

        if (json.optString("Status").equals("Running") || json.optString("Status").equals("Queued"))
        {
            isRunning.setValue(true);
        }
        else
        {
            isRunning.setValue(false);
            /////////Schedule Info

            JSONObject LastRun = json.optJSONObject("LastRun");

            if (LastRun != null)
            {
                String ScheduleTitle = json.getJSONObject("LastRun").getString("ScheduleTitle");
                String ExecutionTotalTime[] = json.getJSONObject("LastRun").getString("ExecutionTotalTime").split(":|\\.");
                buildResult.Schedules.get(current).setTime(Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 + Double.parseDouble(ExecutionTotalTime[1]) * 60 + Double.parseDouble(ExecutionTotalTime[2]) + Double.parseDouble("0." + ExecutionTotalTime[3]));

                Integer temp = null;

                int PassedCount = 0;
                int FailedCount = 0;
                temp = json.getJSONObject("LastRun").optInt("FailedCount",0);
                if (!temp.equals(null)) {FailedCount = temp.intValue();}
                temp = json.getJSONObject("LastRun").optInt("PassedCount",0);
                if (!temp.equals(null)) {PassedCount = temp.intValue();}
                buildResult.Schedules.get(current).setPassed(PassedCount);
                buildResult.Schedules.get(current).setFailed(FailedCount);

                int DoneCount = 0;
                temp = json.getJSONObject("LastRun").optInt("DoneCount",0);
                if (temp > 0) {DoneCount = temp.intValue();}
                temp = null;

                if (doneStatus.contains("Failed"))
                {
                    buildResult.Schedules.get(current).addFailed(DoneCount);
                }
                else
                {
                    buildResult.Schedules.get(current).addPassed(DoneCount);
                }

                ///////////AutomationRunItemInfo
                JSONArray jsonArray = json.getJSONObject("LastRun").getJSONArray("AutomationRunItems");

                ArrayList<String> AutomationRunId = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) AutomationRunId.add(jsonArray.getJSONObject(i).getString("AutomationRunId"));
                ArrayList<String> Status = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Status.add(jsonArray.getJSONObject(i).getString("Status"));
                ArrayList<String> Elapsed = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Elapsed.add(jsonArray.getJSONObject(i).getString("Elapsed"));

                ArrayList<String> Environment = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Environment.add(jsonArray.getJSONObject(i).getJSONObject("Environment").getString("Title"));

                //CaseInfo
                ArrayList<String> CaseName = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++)
                {
                    String caseTitle = jsonArray.getJSONObject(i).getJSONObject("Case").optString("Title","null");
                    if(!caseTitle.contains("null"))
                    {
                        CaseName.add(caseTitle);
                    }
                    else {
                        CaseName.add(CaseName.get(CaseName.size() - 1)); //.replaceAll("\\(.*\\)","")
                    }
                }

                HashSet<String> NotRepeatedNamesSet = new HashSet<String>(CaseName);
                String[] NotRepeatedNames = NotRepeatedNamesSet.toArray(new String[NotRepeatedNamesSet.size()]);
                NotRepeatedNamesSet = null;

                for(int i = 0; i < NotRepeatedNames.length; i++)
                {
                    int count = 0;
                    for(int j = 0; j < CaseName.size(); j++)
                    {
                        if(NotRepeatedNames[i].equals(CaseName.get(j)))
                        {
                            if(count > 0)
                            {
                                CaseName.set(j,String.format("%1$s(%2$d)",CaseName.get(j),count));
                            }
                            count++;
                        }
                    }
                }

                NotRepeatedNames = null;

                for (int i = 0; i < jsonArray.length(); i++)
                {

                   // double seconds = jsonArray.getJSONObject(i).getDouble("TotalSeconds");
                    String ElapsedTime[] = Elapsed.get(i).split(":|\\.");
                    double seconds  = Double.parseDouble(ElapsedTime[0]) * 60 * 60 + Double.parseDouble(ElapsedTime[1]) * 60 + Double.parseDouble(ElapsedTime[2]) + Double.parseDouble("0." + ElapsedTime[3]);
                    ElapsedTime = null;

                    if (Status.get(i).contains("Failed") || (Status.get(i).contains("Done") && doneStatus.contains("Failed")))
                    {
                        JSONArray keyframes = jsonArray.getJSONObject(i).getJSONArray("Keyframes");
                        //KeyframeInfo
                        ArrayList<String> KeyFrameTimeStamp = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameTimeStamp.add(keyframes.getJSONObject(j).getString("Timestamp"));
                        ArrayList<String> KeyFrameLogMessage = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameLogMessage.add(keyframes.getJSONObject(j).getString("LogMessage"));

                        buildLogger.addBuildLogEntry(String.format(MESSAGES.CASE_INFORMATION, CaseName.get(i), Status.get(i), Elapsed.get(i)));

                        String fullstacktrace = "&#xA;";
                        fullstacktrace += String.format("Environment: %1$s",Environment.get(i));
                        buildLogger.addBuildLogEntry(String.format("Environment: %1$s",Environment.get(i)));
                        fullstacktrace += "&#xA;";

                        for (int j = 0; j < keyframes.length(); j++)
                        {
                            String level =  ObjectUtils.firstNonNull(keyframes.getJSONObject(j).optString("Level"));
                            if (level.equals("") || level.contains("Trace")) { }
                            else
                            {
                                String stacktrace = String.format(MESSAGES.CASE_STACKTRACE_FORMAT, KeyFrameTimeStamp.get(j), KeyFrameLogMessage.get(j));
                                buildLogger.addBuildLogEntry(stacktrace);
                                fullstacktrace += stacktrace;
                                fullstacktrace += "&#xA;"; // fullstacktrace += '\n';
                            }
                        }

                        buildResult.Schedules.get(current).Cases.add(new Case(CaseName.get(i), Status.get(i), seconds, fullstacktrace, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                        keyframes = null;
                    }
                    else
                    {
                        buildLogger.addBuildLogEntry(String.format(MESSAGES.CASE_INFORMATION, CaseName.get(i), Status.get(i), Elapsed.get(i)));
                        buildResult.Schedules.get(current).Cases.add(new Case(CaseName.get(i), Status.get(i), seconds, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                    }
                }

                if (buildResult.Schedules.get(current).getFailed() > 0)
                {
                    buildResult.Schedules.get(current).setStatus("Failed");
                }
                else
                {
                    buildResult.Schedules.get(current).setStatus("Passed");
                }
                jsonArray = null;
            }
            else
            {
                String errorMessage = String.format(MESSAGES.SCHEDULE_HAS_NO_CASES_XML, ScheduleId, str);
                buildResult.Schedules.get(current).setError(errorMessage);
                buildResult.Schedules.get(current).incErrors();
                buildLogger.addErrorLogEntry(String.format(MESSAGES.SCHEDULE_HAS_NO_CASES,  ScheduleId,str));
                InValidSchedules.put(ScheduleId,errorMessage);
            }
        }

        return BambooMessage;
    }

    private static  void GetSchTitlesOrIds(String uri, ArrayList<String> scheduleInfo, BuildLogger buildLogger, HashMap<String, String> schedules, ScheduleCollection buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();

            client = null;

            JSONArray jsonArray =  new JSONArray(response.getResponseBody()); response = null;

            for (int i = 0; i < scheduleInfo.size(); i++)
            {
                boolean success = false;
                for (int j = 0; j < jsonArray.length(); j++)
                {
                    if ( ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Id")).contentEquals(scheduleInfo.get(i)))
                    {
                        String title = ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Title"));


                        if (!schedules.containsValue(title))
                        {
                            schedules.put(scheduleInfo.get(i), title);
                            buildResult.Schedules.add(new Schedule(scheduleInfo.get(i), title));
                        }
                        success = true;
                    }
                    else if (ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Title")).contentEquals(scheduleInfo.get(i)))
                    {
                        String id = ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Id"));

                        if (!schedules.containsKey(id))
                        {
                            schedules.put(id, scheduleInfo.get(i));
                            buildResult.Schedules.add(new Schedule(id, scheduleInfo.get(i)));
                        }
                        success = true;
                    }
                    else
                    {
                    }
                }

                if (!success)
                { InValidSchedules.put(scheduleInfo.get(i),MESSAGES.NO_SUCH_SCHEDULE); }
            }
            return ;
        }
        catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
        }
        catch (Exception e)
        {
            buildLogger.addErrorLogEntry(MESSAGES.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
        }
    }

    private static void RunSchedule(String uri, String schId, String schTitle, int current, BuildLogger buildLogger, MutableBoolean successfullyLaunchedSchedule, ScheduleCollection buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;

            if (response.getStatusCode() != 204)          // 204 Response means correct schedule launching
            {

                String errormessage = String.format(MESSAGES.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                buildLogger.addErrorLogEntry(errormessage);
                buildResult.Schedules.get(current).setError(errormessage);
                throw new Exception();

            }
            else
            {

                successfullyLaunchedSchedule.setValue(true);
                String successmessage = String.format(MESSAGES.SCHEDULE_RUN_SUCCESS, schTitle, schId);
                buildResult.Schedules.get(current).setId(current);
                buildLogger.addBuildLogEntry(successmessage);
            }

            return;
        }
          catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).setError(e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).setError(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).setError(e.getMessage());
        }
        catch (Exception e){
            String errormessage = String.format(MESSAGES.SCHEDULE_RUN_FAILURE,  schTitle, schId);
            buildLogger.addErrorLogEntry(errormessage);
            buildResult.Schedules.get(current).setError(errormessage);
            buildResult.Schedules.get(current).incErrors();
            InValidSchedules.put(schId,buildResult.Schedules.get(current).getError());
            successfullyLaunchedSchedule.setValue(false);
        }

    }

    private static void GetScheduleState(String uri, String schId, String schTitle, int current, BuildLogger buildLogger, MutableBoolean isRunning, String doneStatus, ScheduleCollection buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;
            String result;

            if(response.getStatusCode() != 200)
            {
                String errormessage = String.format(MESSAGES.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                buildLogger.addErrorLogEntry(errormessage);
                buildResult.Schedules.get(current).setError(errormessage);
                throw new Exception();
            }
            else
            {
                result = JsonToBamboo( response.getResponseBody(), current, buildLogger,isRunning,doneStatus, buildResult, InValidSchedules);
            }
        }
          catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).setError(e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).setError(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).setError(e.getMessage());
        }catch (Exception e)
        {
            String errorMessage = String.format(MESSAGES.SCHEDULE_STATE_FAILURE, schTitle, schId);
            buildResult.Schedules.get(current).setError(errorMessage);
            buildResult.Schedules.get(current).incErrors();
            buildLogger.addErrorLogEntry(errorMessage);
            InValidSchedules.put(schId,buildResult.Schedules.get(current).getError());
        }
    }

    private static void CreateJunitReport(String reportPath, BuildLogger buildLogger, TaskContext taskContext, ScheduleCollection buildResult)
    {
        try
        {
            File reportFile = new File(taskContext.getWorkingDirectory().getAbsolutePath()+ "/" + reportPath);
            if(!reportFile.exists()) reportFile.createNewFile();

            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(ScheduleCollection.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(buildResult, writer);

            StringWriter formattedWriter  =  new StringWriter();
            formattedWriter.append(writer.getBuffer().toString().replace("&amp;","&"));

            writer = null;

            try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()))) {
                out.print(formattedWriter);
                out.close();
            }
        }
        catch (FileNotFoundException e) {
            buildLogger.addErrorLogEntry(MESSAGES.REPORT_FILE_NOT_FOUND);
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(MESSAGES.REPORT_FILE_CREATION_FAILURE);
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (PropertyException e) {
            buildLogger.addErrorLogEntry(MESSAGES.REPORT_FILE_CREATION_FAILURE);
            e.printStackTrace();
        } catch (JAXBException e) {
            buildLogger.addErrorLogEntry(MESSAGES.REPORT_FILE_CREATION_FAILURE);
            e.printStackTrace();
        }

    }
}
