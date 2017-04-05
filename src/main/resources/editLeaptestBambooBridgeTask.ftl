[#assign versionlist=context.get("versions") /]
[@ww.select cssClass="builderSelectWidget" labelKey='leaptest.version.label' name='version' list=versionlist  id="version" emptyOption='false' style="width: 80px;"/]
[@ww.textfield labelKey="leaptest.address.label" name="address" id="address" required='true' style="max-width: 350px; "/]
[@ww.textfield labelKey="leaptest.delay.label" name="delay" id="delay" required='false' style="width: 80px;"/]
[#assign statuslist=context.get("statuses") /]
[@ww.select cssClass="builderSelectWidget" labelKey='leaptest.doneStatusAs.label' name='doneStatusAs' list=statuslist  id="doneStatusAs" emptyOption='false' style="width: 80px;"/]
[@ww.textfield labelKey="leaptest.report.label" name="report" id="report" required='true' style="max-width: 350px;"/]
[@ww.textarea labelKey="leaptest.schNames.label" name="schNames" id="schNames" required='true' style="max-width: 350px;  height:80px;" readonly='true' emptyOption='false'/]
[@ww.textarea labelKey="leaptest.schIds.label" name="schIds" id="schIds" required='false' style="max-width: 350px;" readonly='true'/]

<input type="button" class="aui-button aui-button-primary" id="mainButton" value="Select schedules" onclick="GetSch()" />
<div id="MyContainer" class="popupDiv" style="position:static; width:334px;"></div>

<style type="text/css">

ul.ul-treefree { padding-left:25px; font-weight: bold;}
ul.ul-treefree ul { margin:0; padding-left:6px; }
ul.ul-treefree li { position:relative; list-style:none outside none; border-left:solid 1px #999; margin:0; padding:0 0 0 19px; line-height:23px; }
ul.ul-treefree li:before { content:""; display:block; border-bottom:solid 1px #999; position:absolute; width:18px; height:11px; left:0; top:0; }
ul.ul-treefree li:last-child { border-left:0 none; }
ul.ul-treefree li:last-child:before { border-left:solid 1px #999; }
ul.ul-dropfree div.drop { width:11px; height:11px; position:absolute; z-index:10; top:6px; left:-6px; background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABYAAAALCAIAAAD0nuopAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAE1JREFUeNpinDlzJgNlgAWI09LScEnPmjWLoAImrHpIAkwMFAMqGMGC6X44GzkIsHoQooAFTTVQKdbAwxOigyMsmIh3MC7ASHnqBAgwAD4CGeOiDhXRAAAAAElFTkSuQmCC"); background-position:-11px 0; background-repeat:no-repeat; cursor:pointer;}

 .popupDiv { background-color: white; background-clip:padding-box; overflow:hidden; position:absolute; display:none; padding:3px 0.5em; border: 1px solid; border-color: #ccc #666 #666 #ccc; border-color: rgba(0, 0, 0, 0.2); box-shadow: 0 5px 10px rgba(0, 0, 0, 0.2); text-align: left; z-index: 100;}
  html:not(.ua-mac) .popupDiv { -webkit-transform: translateZ(0); transform: none;}
 .popupDiv.quickLinksMenuPopup {width: auto; min-width: 25em;}
 .popupDiv.popupLoading {background-color: #FFF; padding: 5px !important;}
  #warningPopup {background-color: #ffc;}
  a.actionLinkNew {background-color: #fff; padding: 3px 10px 3px;}
  a.actionLinkNew:hover, a.actionLinkNewHover {background-color: #e3e9ef;}
  #changesPopup {width: 42em; padding: 0; z-index: 80;}
  #changesPopup .changesContainer { margin: 0; padding: 5px 10px;}
  #changesPopup .userChangesHeader { background-color: #F0F5FB; padding: 2px 5px; font-size: 95%; font-weight: bold; border: none;}
  #changesPopup .userChangesHeader.highlightChanges { background-color: #FFC;}
  #changesPopup .userChangeComment {overflow-x: hidden; margin-right: .5em; font-size: 90%; line-height: 1.5em;}
  #changesPopup .userChangeFiles { float: right; min-width: 6em; font-size: 85%; text-align: right;}
  #changesPopup .userChangeFiles span.highlightChanges{ left: 0;}
  #changesPopup .userChanges {border: none;}
  #changesPopup .userChange {clear: both; padding: 5px 0 5px 5px; border-bottom: 1px solid #E3E9EF;}
  #changesPopup .userChange:last-of-type {border: none;}
  #changesPopup .artifactCommentTable {width: 100%;}
  #changesPopup .artifactCommentBuildType {width: 30%; vertical-align: top;}
  #changesPopup .buildChangesHeader {font-weight: normal;}
  #changesPopup .buildStartDate {float: right;font-size: 90%;}
  #changesPopup .buildChanges {width: 100%;}
  #changesPopup .buildChanges td.username {width: 30%; font-weight: bold; font-size: 90%;}
  #changesPopup .buildChanges td.userChange {width: 70%;}
  #changesPopup .changesPopupTable {width: 100%; }
  #changesPopup .changesPopupTable th {padding-right: 1em;}
  #changesPopup .changesPopupTable td.files {text-align: right;}
  #changesPopup .changesPopupTable td.user { padding-right: 1.5em; text-align: right;}
  #changesPopup .changesPopupTable .date {padding-right: 1.5em;}
  #changesPopup .changesPopupTable td, #changesPopup .simpleTabs .tabs {margin: 5px 0 0 10px; overflow: auto; }
  #changesPopup table.buildChanges { border-collapse: collapse;}
  #changesPopup table.buildChanges td.username, #changesPopup table.buildChanges td.userChangeTD { border-bottom: 1px solid #E3E9EF;}
  #changesPopup table.buildChanges div.userChange {border: none;}
  #changesPopup .ellipsis, span.textExpandArrow { display: inline-block; width: 22px; height: 1em;background: #F3F3F3;border: 1px solid #C8C8C8;border-radius: 2px;color: #C8C8C8;text-align: center;}
  #changesPopup .ellipsis:hover, span.textExpandArrow:hover {cursor: pointer; background: #E3E3E3;}
  #changesPopup .historyBuildNote { margin-bottom: 4px;}
  #changesPopup .changePopupHeader, #changesPopup .changesPopupFooter {background-color: #f5f5f5;padding: 4px;font-size: 90%;}
  #changesPopup .changePopupHeader {margin-bottom: 6px; border-bottom: 1px solid #E3E9EF;}
  #changesPopup .changePopupFooter {margin-top: 6px;}
  #changesPopup .userChangeComment td.status, #changesPopup .userChangeComment td.buildNumber {vertical-align: top;}
  #changesPopup .userChangeComment td.buildNumber {width: 10%;}
  #changesPopup .artifactsChangeHeader {background-position: 5px 3px; padding-left: 25px;}
  span.changeName {font-size: 90%;}
  div.dependencyRelationIcon {float: right;}
  div.subrepoIcon {float: right;}

</style>

<script type="text/javascript">

fieldArea_schIds.style.display='none';

         function GetSch() {

               if(!address.value)
               {
                alert('"Address field is empty! Cannot connect to server!"');
               }
               else
               {
                    if((jQuery)("#MyContainer").html() == "")
                    {
                        var json;

                        var url = document.getElementById("address").value + "/api/v1/runSchedules";

                        var XHR = ("onload" in new XMLHttpRequest()) ? XMLHttpRequest : XDomainRequest;
                        var xhr = new XHR();
                        xhr.open('GET', url, true);
                        xhr.onload = function ()
                        {


                            json = JSON.parse(this.responseText);

                            var container = document.getElementById("MyContainer");


                            (jQuery)(document).click(function (event) {
                                if ((jQuery)(event.target).closest('#MyContainer').length == 0 && (jQuery)(event.target).attr('id') != 'mainButton') {
                                    (jQuery)("#MyContainer input:checkbox").remove();
                                    (jQuery)("#MyContainer li").remove();
                                    (jQuery)("#MyContainer ul").remove();
                                    (jQuery)("#MyContainer br").remove();
                                    container.style.display = 'none';


                                }
                            });


                            var schName = new Array();
                            var schId = new Array();
                            var schProjectId = new Array();

                            for (var i = 0; i < json.length; i++) {
                                if (json[i].IsDisplayedInScheduleList == true) {
                                    schId.push(json[i].Id);
                                    schName.push(json[i].Title);
                                    schProjectId.push(json[i].ProjectId);
                                }
                            }

                            var projects = new Array();
                            var projurl = document.getElementById("address").value + "/api/v1/Projects";
                            var XHRPr = ("onload" in new XMLHttpRequest()) ? XMLHttpRequest : XDomainRequest;
                            var xhrPr = new XHRPr();
                            xhrPr.open('GET', projurl, true);
                            xhrPr.onload = function ()
                            {

                                var projJson = JSON.parse(this.responseText);

                                for(var i = 0; i < projJson.length; i++)
                                {
                                projects.push(projJson[i].Title);
                                }

                                for(var i = 0; i < schProjectId.length; i++)
                                {
                                    for(var j = 0; j < projJson.length; j++)
                                    {

                                        if(schProjectId[i] == projJson[j].Id)
                                        {
                                            schProjectId[i] = projJson[j].Title;
                                        }
                                    }
                                }
                                projJson = null;

                                container.innerHTML += '<br>';

                                var drpdwn = document.createElement('ul');
                                drpdwn.className = 'ul-treefree ul-dropfree';

                                for(var i = 0; i < projects.length; i++)
                                {
                                    var projectli = document.createElement('li');

                                    var drop = document.createElement('div');
                                    drop.class = 'drop';
                                    drop.style = 'background-position: 0px 0px;';
                                    projectli.appendChild(drop);
                                    projectli.innerHTML+=projects[i];

                                    var schul = document.createElement('ul');
                                    schul.style = 'display:none; font-weight: normal';

                                    for(var j = 0; j < schProjectId.length; j++)
                                    {
                                        if(projects[i] == schProjectId[j])
                                        {
                                            var schli = document.createElement('li');
                                            var chbx = document.createElement('input');
                                            chbx.type = 'checkbox';
                                            chbx.name = schName[j];
                                            chbx.id = i;
                                            chbx.value = schId[j];

                                            schli.appendChild(chbx);
                                            schli.innerHTML+=schName[j];
                                            schul.appendChild(schli);
                                        }
                                    }

                                    projectli.appendChild(schul);
                                    drpdwn.appendChild(projectli);
                                }

                                container.appendChild(drpdwn);



                                 container.innerHTML += '<br>';

                                 container.style.display='block';

                                 (jQuery)(".ul-dropfree").find("li:has(ul)").prepend('<div class="drop"></div>');
                                 	(jQuery)(".ul-dropfree div.drop").click(function() {
                                 		if ((jQuery)(this).nextAll("ul").css('display')=='none') {
                                 			(jQuery)(this).nextAll("ul").slideDown(400);
                                 			(jQuery)(this).css({'background-position':"-11px 0"});
                                 		} else {
                                 			(jQuery)(this).nextAll("ul").slideUp(400);
                                 			(jQuery)(this).css({'background-position':"0 0"});
                                 		}
                                 	});
                                 	(jQuery)(".ul-dropfree").find("ul").slideUp(400).parents("li").children("div.drop").css({'background-position':"0 0"});

                                 var TestNames = document.getElementById("schNames");
                                 var TestIds = document.getElementById("schIds");

                                 var boxes = (jQuery)("#MyContainer input:checkbox");
                                 var existingTests = new Array();
                                 existingTests = TestNames.value.split("\n");

                                    if (TestNames.value != null && TestIds.value != null) {
                                        for (var i = 0; i < existingTests.length; i++) {
                                            for (j = 0; j < boxes.length; j++)
                                            {
                                                console.log(boxes[j].getAttributeNode('name').value)
                                                if (existingTests[i] == boxes[j].getAttributeNode('name').value)
                                                 {
                                               (jQuery)(boxes[j]).prop('checked', 'checked');

                                                }
                                            }
                                        }

                                    }

                                 (jQuery)("#MyContainer input:checkbox").on("change", function ()
                                 {
                                     var NamesArray = new Array();
                                     var IdsArray = new Array();
                                     for (var i = 0; i < boxes.length; i++)
                                     {
                                          var box = boxes[i];
                                          if ((jQuery)(box).prop('checked'))
                                          {
                                                NamesArray[NamesArray.length] = (jQuery)(box).attr('name');
                                                IdsArray[IdsArray.length] = (jQuery)(box).val();
                                          }
                                     }
                                     TestNames.value = NamesArray.join("\n");
                                     TestIds.value = IdsArray.join("\n");
                                     console.log(TestIds.value);
                                 });


                            }
                            xhrPr.onerror = function ()
                            {
                             alert('"Error occured! Cannot get the list of Projects! Check connection to your server!"' + this.status);
                            }
                             xhrPr.send();

                        }
                        xhr.onerror = function ()
                        {
                            alert('"Error occured! Cannot get the list of schedules! Check connection to your server!"' + this.status);
                        }
                        xhr.send();
                    }
                    else
                    {
                          (jQuery)("#MyContainer input:checkbox").remove();
                          (jQuery)("#MyContainer li").remove();
                          (jQuery)("#MyContainer ul").remove();
                          (jQuery)("#MyContainer br").remove();
                          GetSch();
                    }
               }
         }
</script>