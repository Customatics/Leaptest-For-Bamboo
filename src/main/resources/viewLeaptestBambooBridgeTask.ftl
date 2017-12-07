
[@ww.label labelKey="leaptest.address.label" name="address" id="address" required='true'/]
[@ww.label labelKey="leaptest.delay.label" name="delay" id="delay" required='false'/]
[#assign statuslist=context.get("statuses") /]
[@ww.select cssClass="builderSelectWidget" labelKey='leaptest.doneStatusAs.label' name='doneStatusAs' list=statuslist  id="doneStatusAs" emptyOption='false'/]
[@ww.label labelKey="leaptest.report.label" name="report" id="report" required='true'/]
[@ww.checkbox labelKey="leaptest.autoReport.label" name="autoReport" toggle="true" descriptionKey="leaptest.autoReport.description" disabled="true"/]
[@ww.label labelKey="leaptest.schNames.label" name="schNames" id="schNames" required='true'/]
[@ww.label labelKey="leaptest.schIds.label" name="schIds" id="schIds" required='false'/]
