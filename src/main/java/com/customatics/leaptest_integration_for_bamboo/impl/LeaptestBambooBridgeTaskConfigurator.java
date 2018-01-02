package com.customatics.leaptest_integration_for_bamboo.impl;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.struts.TextProvider;
import com.atlassian.util.concurrent.NotNull;
import com.atlassian.util.concurrent.Nullable;

import java.util.ArrayList;
import java.util.Map;


public class LeaptestBambooBridgeTaskConfigurator extends AbstractTaskConfigurator {

    private TextProvider textProvider;

    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put("address", params.getString("address"));
        config.put("accessKey",params.getString("accessKey"));
        config.put("delay", params.getString("delay"));
        config.put("doneStatusAs", params.getString("doneStatusAs"));
        config.put("report", params.getString("report"));
        config.put("autoReport", Boolean.toString(params.getBoolean("autoReport")));
        config.put("schNames", params.getString("schNames"));
        config.put("schIds", params.getString("schIds"));


        return config;
    }


    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);

        context.put("delay", "3");
        context.put("report","report.xml");
        context.put("autoReport", Boolean.toString(false));


        ArrayList<String> statuses = new ArrayList<String>();
        statuses.add("Failed");
        statuses.add("Success");
        context.put("statuses",statuses);

    }
    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);

        Map<String, String> config = taskDefinition.getConfiguration();
        context.put("address", config.get("address"));
        context.put("accessKey",config.get("accessKey"));
        context.put("delay", config.get("delay"));
        context.put("doneStatusAs", config.get("doneStatusAs"));
        context.put("report", config.get("report"));
        context.put("autoReport", Boolean.valueOf(config.get("autoReport")));
        context.put("schNames", config.get("schNames"));
        context.put("schIds", config.get("schIds")); // for debug only!

        ArrayList<String> statuses = new ArrayList<String>();
        statuses.add("Failed");
        statuses.add("Success");
        context.put("statuses",statuses);

    }
}
