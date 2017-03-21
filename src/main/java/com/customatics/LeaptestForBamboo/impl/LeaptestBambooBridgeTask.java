package com.customatics.LeaptestForBamboo.impl;

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
import java.util.concurrent.ExecutionException;

/**
 * Created by Роман on 24.02.2017.
 */
public class LeaptestBambooBridgeTask implements TaskType {


    TaskResult result;
  //  static ArrayList<String> InValidSchedules = new ArrayList<String>();
  //  static testsuites buildResult = new testsuites();


    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        //get fields value
        final String version = taskContext.getConfigurationMap().get("version");
        final String address = taskContext.getConfigurationMap().get("address");
        final String delay = taskContext.getConfigurationMap().get("delay");
        final String doneStatusAs = taskContext.getConfigurationMap().get("doneStatusAs");
        String report = taskContext.getConfigurationMap().get("report");
        final String schIds = taskContext.getConfigurationMap().get("schIds");
        final String schNames = taskContext.getConfigurationMap().get("schNames");

        if(report.isEmpty())
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
        testsuites buildResult = new testsuites();


        String uri = address + "/api/v1/runSchedules";
        int timeDelay = 1;
        if(ObjectUtils.firstNonNull(delay) != null)
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

                String runUri = uri + "/" + schedule.getKey() + "/runNow";
                String stateUri = uri + "/state/" + schedule.getKey();



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
                buildLogger.addBuildLogEntry("INVALID SCHEDULES:");
                for (String invalidsch : InValidSchedules.keySet())
                { buildLogger.addBuildLogEntry(invalidsch); }

                buildResult.Schedules.add(new testsuite("INVALID SCHEDULES"));

                ArrayList<String> invSch = new ArrayList<>(InValidSchedules.keySet());
                ArrayList<String> invSchStackTrace = new ArrayList<>(InValidSchedules.values());

                for(int i = 0; i < InValidSchedules.size();i++)
                {
                    buildResult.Schedules.get(buildResult.Schedules.size() - 1).Cases.add(new testcase(invSch.get(i), "Failed", 0, invSchStackTrace.get(i), "INVALID SCHEDULE"));
                    buildResult.Schedules.get(buildResult.Schedules.size()- 1).incErrors();
                }

                invSch = null;
                invSchStackTrace = null;
            }



            for (testsuite sch : buildResult.Schedules)
            {
                buildResult.addFailedTests(sch.Failed());
                buildResult.addPassedTests(sch.Passed());
                buildResult.addErrors(sch.Errors());
                sch.Total(sch.Passed() + sch.Failed());
                buildResult.addTotalTime(sch.Time());
            }
            buildResult.TotalTests(buildResult.FailedTests() + buildResult.PassedTests());

            CreateJunitReport(junitReportPath, buildLogger, taskContext, buildResult);

            if (buildResult.Errors() != 0 || buildResult.FailedTests() != 0)
            {
                result = TaskResultBuilder.create(taskContext).success().build();

            }
            else
            {
                result = TaskResultBuilder.create(taskContext).failed().build();
            }

            InValidSchedules = null;
            buildResult = null;

            buildLogger.addBuildLogEntry("Leaptest for Bamboo  plugin  successfully finished!");
        }

        catch (IndexOutOfBoundsException e)
        {
            buildLogger.addErrorLogEntry("No Schedules or wrong url! Check connection to your server or schedules and try again!");
            buildLogger.addErrorLogEntry(e.getMessage());
        }
        /*catch (InterruptedException e) {
            buildLogger.addErrorLogEntry("")
            buildLogger.addErrorLogEntry(e.getMessage());
        }*/
        catch (Exception e)
        {
            buildLogger.addErrorLogEntry("Leaptest for Bamboo plugin finished with errors!");
            result = TaskResultBuilder.create(taskContext).failed().build();
        }
        return result;
    }


    //////////////////////////////////////////////////////

    private static String JsonToBamboo(String str, int current, BuildLogger buildLogger, MutableBoolean isRunning, String doneStatus, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {

        String BambooMessage = "";


        JSONObject json = new JSONObject(str);

        String ScheduleId = json.getString("ScheduleId");


        if (!json.optString("Status").equals(""))
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
                buildResult.Schedules.get(current).Time(Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 + Double.parseDouble(ExecutionTotalTime[1]) * 60 + Double.parseDouble(ExecutionTotalTime[2]) + Double.parseDouble("0." + ExecutionTotalTime[3]));


                Integer temp;

                int PassedCount = 0;
                int FailedCount = 0;
                temp = json.getJSONObject("LastRun").optInt("FailedCount",0);
                if (!temp.equals(null)) {FailedCount = temp.intValue();}
                temp = json.getJSONObject("LastRun").optInt("PassedCount",0);
                if (!temp.equals(null)) {PassedCount = temp.intValue();}
                buildResult.Schedules.get(current).Passed(PassedCount);
                buildResult.Schedules.get(current).Failed(FailedCount);

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

                //CaseInfo
                ArrayList<String> CaseName = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) CaseName.add(jsonArray.getJSONObject(i).getJSONObject("Case").getString("Title"));




                for (int i = 0; i < jsonArray.length(); i++)
                {

                    double seconds = jsonArray.getJSONObject(i).getDouble("TotalSeconds");

                    if (Status.get(i).contains("Failed") || (Status.get(i).contains("Done") && doneStatus.contains("Failed")))
                    {
                        JSONArray keyframes = jsonArray.getJSONObject(i).getJSONArray("Keyframes");
                        //KeyframeInfo
                        ArrayList<String> KeyFrameTimeStamp = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameTimeStamp.add(keyframes.getJSONObject(j).getString("Timestamp"));
                        ArrayList<String> KeyFrameStatus = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameStatus.add(keyframes.getJSONObject(j).getString("Status"));
                        ArrayList<String> KeyFrameLogMessage = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameLogMessage.add(keyframes.getJSONObject(j).getString("LogMessage"));

                        buildLogger.addBuildLogEntry(String.format("Case: %1$s | Status: %2$s | Elapsed: %3$s", CaseName.get(i), Status.get(i), Elapsed.get(i)));

                        String fullstacktrace = "&#xA;";

                        for (int j = 0; j < keyframes.length(); j++)
                        {
                            String level =  ObjectUtils.firstNonNull(keyframes.getJSONObject(j).optString("Level"));
                            if (level.equals("") || level.contains("Trace")) { }
                            else
                            {

                                String stacktrace = String.format(/*"%1$s | %2$s | %3$s"*/"%1$s - %3$s", KeyFrameTimeStamp.get(j), KeyFrameStatus.get(j), KeyFrameLogMessage.get(j));
                                buildLogger.addBuildLogEntry(stacktrace);
                                fullstacktrace += stacktrace;
                                fullstacktrace += "&#xA;";
                               // fullstacktrace += '\n';
                            }
                        }
                        buildResult.Schedules.get(current).Cases.add(new testcase(CaseName.get(i), Status.get(i), seconds, fullstacktrace, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                        //buildResult.Schedules.get(current).incFailed();
                        keyframes = null;
                    }
                    else
                    {
                        buildLogger.addBuildLogEntry(String.format("Case: %1$s | Status: %2$s | Elapsed: %3$s", CaseName.get(i), Status.get(i), Elapsed.get(i)));
                        buildResult.Schedules.get(current).Cases.add(new testcase(CaseName.get(i), Status.get(i), seconds, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                       // buildResult.Schedules.get(current).incPassed();
                    }
                }

                if (buildResult.Schedules.get(current).Failed() > 0)
                {
                    buildResult.Schedules.get(current).Status("Failed");
                }
                else
                {
                    buildResult.Schedules.get(current).Status("Passed");
                }
                jsonArray = null;
            }
            else
            {
                String errorMessage = String.format("Schedule [%1$s] returns bad JSON: &#xA; %2$s", ScheduleId, str);
                buildResult.Schedules.get(current).Error(errorMessage);
                buildResult.Schedules.get(current).Cases.add(new testcase("Bad Json:", "Failed", 0,str, ScheduleId));
                buildResult.Schedules.get(current).incErrors();
                buildLogger.addErrorLogEntry(String.format("Schedule[%1$s] returns bad JSON:\n %2$s",  ScheduleId,str));
                InValidSchedules.put(ScheduleId,errorMessage);
            }
        }

        return BambooMessage;
    }

    private static  void GetSchTitlesOrIds(String uri, ArrayList<String> scheduleInfo, BuildLogger buildLogger, HashMap<String, String> schedules, testsuites buildResult, HashMap<String, String> InValidSchedules)
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
                            buildResult.Schedules.add(new testsuite(scheduleInfo.get(i), title));
                        }
                        success = true;
                    }
                    else if (ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Title")).contentEquals(scheduleInfo.get(i)))
                    {
                        String id = ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Id"));

                        if (!schedules.containsKey(id))
                        {
                            schedules.put(id, scheduleInfo.get(i));
                            buildResult.Schedules.add(new testsuite(id, scheduleInfo.get(i)));
                        }
                        success = true;
                    }
                    else
                    {

                    }
                }

                if (!success)
                { InValidSchedules.put(scheduleInfo.get(i),"Tried to get schedule title or id! Check connection to your server and try again!"); }
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
            buildLogger.addErrorLogEntry(String.format("Tried to get schedule titles or id! Check connection to your server and try again!"));
        }
    }

    private static void RunSchedule(String uri, String schId, String schTitle, int current, BuildLogger buildLogger, MutableBoolean successfullyLaunchedSchedule, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;



            if (response.getStatusCode() != 204)          // 204 Response means correct schedule launching
            {

                String errormessage = String.format("Code: %1$s Status: %2$s!", response.getStatusCode(), response.getStatusText());
                buildLogger.addErrorLogEntry(errormessage);
                buildResult.Schedules.get(current).Error(errormessage);
                throw new Exception();

            }
            else
            {

                successfullyLaunchedSchedule.setValue(true);
                String successmessage = String.format("Schedule: %1$s | Schedule Id: %2$s | Launched: SUCCESSFULLY", schTitle, schId);
                buildResult.Schedules.get(current).Id(current);
                buildLogger.addBuildLogEntry(successmessage);
            }

            return;
        }
          catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        }
        catch (Exception e){
            String errormessage = String.format("Failed to run %1$s[%2$s]! Check it at your Leaptest server or connection to your server and try again!",  schTitle, schId);
            buildLogger.addErrorLogEntry(errormessage);
            buildResult.Schedules.get(current).Error(errormessage);
            buildResult.Schedules.get(current).incErrors();
            InValidSchedules.put(schId,buildResult.Schedules.get(current).Error());
            successfullyLaunchedSchedule.setValue(false);
        }

    }

    private static void GetScheduleState(String uri, String schId, String schTitle, int current, BuildLogger buildLogger, MutableBoolean isRunning, String doneStatus, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;
            String result;

            if(response.getStatusCode() != 200)
            {
                String errormessage = String.format("Code: %1$s Status: %2$s!", response.getStatusCode(), response.getStatusText());
                buildLogger.addErrorLogEntry(errormessage);
                buildResult.Schedules.get(current).Error(errormessage);
                throw new Exception();
            }
            else
            {

                result = JsonToBamboo( response.getResponseBody(), current, buildLogger,isRunning,doneStatus, buildResult, InValidSchedules);

            }
        }
          catch (InterruptedException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        } catch (ExecutionException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        }catch (Exception e)
        {
            String errorMessage = String.format("Tried to get %1$s[%2$s] state! Check connection to your server and try again!", schTitle, schId);
            buildResult.Schedules.get(current).Error(errorMessage);
            buildResult.Schedules.get(current).incErrors();
            buildLogger.addErrorLogEntry(errorMessage);
            InValidSchedules.put(schId,buildResult.Schedules.get(current).Error());
        }
    }

    private static void CreateJunitReport(String reportPath, BuildLogger buildLogger, TaskContext taskContext, testsuites buildResult)
    {
        try
        {
            File reportFile = new File(taskContext.getWorkingDirectory().getAbsolutePath()+ "/" + reportPath);
            if(!reportFile.exists()) reportFile.createNewFile();

            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(testsuites.class);

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
            buildLogger.addErrorLogEntry("Couldn't find report file! Wrong path! Press \"help\" button nearby \"report\" textbox! ");
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (IOException e) {
            buildLogger.addErrorLogEntry(e.getMessage());
        } catch (PropertyException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }
}
