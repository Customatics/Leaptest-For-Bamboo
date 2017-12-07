# Leaptest-For-Bamboo
This is Leaptest plugin for Bamboo

# More Details 
Leaptest is a mighty automation testing system and now it can be used for running [smoke, functional, acceptance] tests, generating reports and a lot more in Atlassian Bamboo. You can easily configure integration directly in Bamboo enjoying UI friendly configuration page with easy connection and test suites selection. 

# Features:
 - Setup and test Leaptest connection in few clicks
 - Run automated tests in your Bamboo build tasks
 - Automatically receive test results
 - Build status based tests results
 - Generate a xml report file in JUnit format
 - Write tests trace to build output log
 - Smart UI
 
# Update 1.2.1:
 - Support older Bamboo versions from 5.9 (tested on 5.9.7)
 - Support new bamboo version 6.2 (tested on 6.2.2)
 - New option added: allows you to pass results automatically to the JUnit parser if you choose such an option. 
Don't require to add JUnit parser task anymore.  
WARNING: in older Bamboo versions this option may automatically finish a job after this task!   
If you have problems with JUnit parser report file detection, this issue may help:  
https://jira.atlassian.com/browse/BAM-12768

# Installing
- Use atlassian-sdk maven 3.2.1.
- Command: atlas-mvn package 
- Or simply install the jar-file from the "target" folder: Bamboo Administration -> Add-ons -> Upload add-on -> Choose that jar-file

# Instruction
1. Add "Leaptest for Bamboo" to your plan. Category "Builder" or "Tests"
2. Enter your Leaptest server address something like "http://win10-agent2:9000" or "http://localhost:9000".
3. Enter time delay in seconds. When schedule is run, plugin will wait this time before trying to get schedule state. If schedule is still running, plugin will wait this time again. By default this value is 1 second.
4. Select how plugin should set "Done" status value: to Success or Failed.
5. Enter JUnit report file name. This file will be created at your job's working directory. If there is an xml file with the same name, it will be overwritten. By default it is "report.xml".
6. Press button "Select Schedules" to get a list of all available schedules grouped by projects. Select schedules you want to run. Press "Save" button.
7. Add "JUnit parser" to your plan. Enter JUnit report file name. It MUST be the same you've entered before!
8. Run your plan and get results. Enjoy!



# Screenshots
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/highlight1.png)
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/highlight2.png)
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/highlight3.png)
![ScreenShot](https://github.com/Customatics/Leaptest-For-Bamboo/blob/master/src/main/resources/images/screen1.png)


