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
        config.put("delay", params.getString("delay"));
        config.put("doneStatusAs", params.getString("doneStatusAs"));
        config.put("report", params.getString("report"));
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

        ArrayList<String> statuses = new ArrayList<String>();
        statuses.add("Failed");
        statuses.add("Success");
        context.put("statuses",statuses);

        statuses = null;



    }
    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);


        context.put("address", taskDefinition.getConfiguration().get("address"));
        context.put("delay", taskDefinition.getConfiguration().get("delay"));
        context.put("doneStatusAs", taskDefinition.getConfiguration().get("doneStatusAs"));
        context.put("report", taskDefinition.getConfiguration().get("report"));
        context.put("schNames", taskDefinition.getConfiguration().get("schNames"));
        context.put("schIds", taskDefinition.getConfiguration().get("schIds")); // for debug only!

        ArrayList<String> statuses = new ArrayList<String>();
        statuses.add("Failed");
        statuses.add("Success");
        context.put("statuses",statuses);

        statuses = null;
    }
}
