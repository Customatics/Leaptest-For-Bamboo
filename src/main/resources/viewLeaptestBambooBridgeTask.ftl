
[@ww.textfield labelKey="leapwork.hostname.label" name="leapworkHostname" id="leapworkHostname" required='true' style="max-width: 350px; "/]
[@ww.textfield labelKey="leapwork.port.label" name="leapworkPort" id="leapworkPort" required='true' style="max-width: 80px; "/]
[@ww.textfield labelKey="leapwork.accessKey.label" name="leapworkAccessKey" id="leapworkAccessKey" required='true' style="max-width: 350px; "/]
[@ww.textfield labelKey="leapwork.delay.label" name="leapworkDelay" id="leapworkDelay" required='false' style="width: 80px;"/]
[@ww.select cssClass="builderSelectWidget" labelKey='leapwork.doneStatusAs.label' name='leapworkDoneStatusAs' list="statuses"  id="leapworkDoneStatusAs" emptyOption='false' style="width: 80px;"/]
[@ww.textfield labelKey="leapwork.report.label" name="leapworkReport" id="leapworkReport" required='true' style="max-width: 350px;"/]
[@ww.textarea labelKey="leapwork.scheduleVariables.label" name="leapworkScheduleVariables" id="leapworkScheduleVariables" required=false style="max-width: 350px; height:80px;"/]
[@ww.checkbox labelKey="leapwork.autoReport.label" name="leapworkAutoReport" toggle="true" descriptionKey="leapwork.autoReport.description" /]
[@ww.checkbox labelKey="leapwork.writePassedFlowKeyFrames.label" name="leapworkWritePassedFlowKeyFrames" toggle="false" /]
[@ww.textarea labelKey="leapwork.schNames.label" name="leapworkSchNames" id="schNames" readonly='false' required='true' style="max-width: 350px;  height:80px;"  emptyOption='false'/]
[@ww.textarea labelKey="leapwork.schIds.label" name="leapworkSchIds" id="schIds" required='false' style="max-width: 350px;" readonly='true'/]

