var localhost = "http://localhost:39128/clusters/";

$(function () {
    $('#jobGraphDiv').hide();
    // hide the error messagae tab first
    // $('#myTab li:eq(0)').hide();
    // show the job output tab
    $('#myTable').colResizable({liveDrag:true});
    $('#myTable').dragtable();
    $('#leftDiv').resizable();
    $('#myTab li:eq(4) a').tab('show');
    $('#jobGraphLink').on('shown.bs.tab', function() {
        $('#jobGraphDiv').show();
    });

    $('#jobGraphLink').on('hidden.bs.tab', function() {
        $('#jobGraphDiv').hide();
    });

    $("#tableDIv").resizable();
    $('#tableDIv').resize(function(){
        $('#rightDiv').width($("#parent").width()-$("#tableDIv").width());
    });


    getProjectId();
    $("#JobHistoryTbody").on('click', 'tr', function () {
        isJobGraphGenerated = false;
        currentSelectedJobs = null;
        currentSelectedStages = null;
        appId = null;
        attemptId = null;
        applicationName = null;

        $("#summaryTitle").html("Application details");
        $("#basicInformationTitle").html("Basic Application Information");
        d3.selectAll("#stageSummaryTbody tr").remove();
        d3.selectAll("#taskSummaryTbody tr").remove();
        $("#errorMessage").text("");
        $("#jobOutputTextarea").text("");
        $("#livyJobLog").text("");
        $("#sparkDriverLog").text("");
        var rows = $("#JobHistoryTbody tr");
        rows.removeClass('selected-hight');
        $(this).addClass('selected-hight');

        //get Application Id
        appId = $(this).find('td:eq(1)').text();
        // get last attempt
        attemptId = $(this).find('td:eq(4)').text();
        applicationName = $(this).find('td:eq(2)').text();
        $("#jobName").text("Application: " + applicationName);

        if (appId == null) {
            return;
        }
        // save current Application ID to LocalStorage
        localStorage.setItem("selectedAppID", appId);
        setBasicInformation();
        setAMcontainer();
        setDiagnosticsLog();
        setLivyLog();
        setDebugInfo("end livy log");
        setJobDetail();
        setStoredRDD();
        setStageDetailsWithTaskDetails();
        setExecutorsDetails();
    });

    $("#sparkEventButton").click(function () {
        JobUtils.openSparkEventLog(projectId, typeof appId == 'undefined' ? "" : appId.toString());
    });

    $("#livyLogButton").click(function() {
        JobUtils.openLivyLog(typeof appId == 'undefined' ? "" : appId.toString());
    });

    $("#openSparkUIButton").click(function () {
        var id = typeof appId == 'undefined' ? "" : appId.toString();
        if (id != "") {
            var application = $.grep(applicationList, function (e) {
                return e.id == id;
            });
            if (application != null && application.length == 1) {
                var currentAttemptId = application[0].attempts[0].attemptId;
                if (currentAttemptId != null) {
                    id = id + "/" + currentAttemptId;
                }
            }
        }
        if(sourceType == "intellij") {
            JobUtils.openSparkUIHistory(id);
        } else {
            JobUtils.openSparkUIHistory(clusterName, id);
        }

    });

    $("#openYarnUIButton").click(function () {
        if(sourceType == "intellij"){
            JobUtils.openYarnUIHistory(typeof appId == 'undefined' ? "" : appId.toString());
        } else {
            JobUtils.openYarnUIHistory(clusterName, typeof appId == 'undefined' ? "" : appId.toString());
        }
    });

    $("#refreshButton").click(function () {
        location.reload();
        refreshGetSelectedApplication();
    });

    getJobHistory();
});

function getJobHistory() {
    getMessageAsync(localhost + projectId + "/applications/", function (s) {
        writeToTable(s);
        refreshGetSelectedApplication();
        $("#JobHistoryTbody tr:eq(0)").click();
    });
}


function getProjectId() {
    queriresMap = {};
    var urlinfo = window.location.href;
    var len = urlinfo.length;
    var offset = urlinfo.indexOf("?");

    var additionalInfo = urlinfo.substr(offset + 1, len);
    var infos = additionalInfo.split("&");
    for (var i = 0; i < infos.length; ++i) {
        strs = infos[i].split("=");
        queriresMap[strs[0]] = strs[1];
    }
    projectId = queriresMap["projectid"];
    sourceType = queriresMap["sourcetype"] == null ? "intellij" : "eclipse";
    clusterName = queriresMap["clustername"];
    var webType = queriresMap["engintype"];
}

function refreshGetSelectedApplication() {
    var selectedAppid = localStorage.getItem("selectedAppID");
    if (selectedAppid == null) {
        return;
    }

    var tableRow = $("#myTable tbody tr").filter(function () {
        return $(this).children('td:eq(1)').text() == selectedAppid;
    }).closest("tr");
}


function getFirstAttempt(attempts) {
    return findElement(attempts, function (a) {
        return typeof a.attemptId == 'undefined' || a.attemptId == 1;
    });
}

function getLastAttempt(attempts) {
    return findElement(attempts, function (a) {
        return typeof a.attemptId == 'undefined' || a.attemptId == attemptId;
    });
}

function setBasicInformation() {
    getMessageAsync(localhost + projectId + "/applications/" + appId, function (s) {
        var application = JSON.parse(s);
        $("#startTime").text(formatServerTime(getFirstAttempt(application.attempts).startTime));
        $("#endTime").text(formatServerTime(getLastAttempt(application.attempts).endTime));
    });
}

function setMessageForLable(str) {
    var ss = document.getElementById("demo");
    ss.innerHTML = str;
}

function writeToTable(message) {
    applicationList = JSON.parse(message);
    $('#myTable tbody').html("");
    d3.select("#myTable tbody")
        .selectAll('tr')
        .data(applicationList)
        .enter()
        .append('tr')
        .attr("align","center")
        .attr("class","ui-widget-content")
        .selectAll('td')
        .data(function(d) {
            return appInformationList(d);
        })
        .enter()
        .append('td')
        .attr('class',"ui-widget-content")
        .attr('id',function(d,i) {
            return i;
        })
        .html(function(d, i) {
            return d;
        });
}

function appInformationList(app) {
    var lists = [];
    var status = app.attempts[app.attempts.length - 1].completed;
    lists.push(getTheJobStatusImgLabel(status));
    lists.push(app.id);
    lists.push(app.name);
    lists.push(formatServerTime(app.attempts[0].startTime));
    if(app.attempts.length == 1 && typeof app.attempts[0].attemptId == 'undefined') {
        lists.push(0);
    } else {
        lists.push(app.attempts.length);
    }
    lists.push(app.attempts[0].sparkUser);
    return lists;
}

function getTheJobStatusImgLabel(str) {
    if (str == true) {
        return "<img src=\"resources/icons/Success.png\">";
    } else {
        return "<img src=\"resources/icons/Error.png\">";
    }
}

function setAMcontainer() {
    if (appId.substr(0, 5) == "local") {
        $("#containerNumber").text("Local Task");
    } else {
        getMessageAsync(localhost + projectId + "/cluster/apps/" + appId + "/appattempts?restType=yarn", function (str) {
            var object = JSON.parse(str);
            containerId = object.appAttempts.appAttempt[0].containerId;
            nodeId = object.appAttempts.appAttempt[0].nodeId;
            $("#containerNumber").text(containerId);
            if (appId.substr(0, 5) != "local" && attemptId != 0) {
                getJobResult();
                getSparkDriverLog();
            }
        });
    }
}

function setDiagnosticsLog() {
    if (appId.substr(0, 5) == "local") {
        $("#errorMessage").text("No Yarn Error Message");
    } else {
        getMessageAsync(localhost + projectId + "/cluster/apps/" + appId + "?restType=yarn", function (s) {
            var object = JSON.parse(s);
            var message = object.app.diagnostics;
            if (message == 'undefined' || message == "") {
                message = "No Error Message";
            }
            $("#errorMessage").text(message);
        });
    }
}

function getSparkDriverLog() {
    if (attemptId == 0 || typeof containerId == 'undefined') {
        return;
    }
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors", function (s) {
        executorsObject = JSON.parse(s);
        var hostPort = getDriverPortFromExecutor(executorsObject);
        ipAddress = hostPort.split(":")[0];
        var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/livy/stderr?restType=yarnhistory";
        getResultFromSparkHistory(url, function (result) {
            $("#sparkDriverLog").text(result);
        });
    });
}

function getJobResult() {
    if (attemptId == 0 || typeof containerId == 'undefined') {
        return;
    }

    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors", function (s) {
        executorsObject = JSON.parse(s);
        var hostPort = getDriverPortFromExecutor(executorsObject);
        ipAddress = hostPort.split(":")[0];
        var url = localhost + projectId + "/jobhistory/logs/" + ipAddress + "/port/30050/" + containerId + "/" + containerId + "/livy/stdout?restType=yarnhistory";
        getResultFromSparkHistory(url, function (result) {
            if (result == "") {
                result = "No out put";
            }
            $("#jobOutputTextarea").text(result);
        });
    });
}

function getResultFromSparkHistory(url, callback) {
    getMessageAsync(url, function (s) {
        callback(s);
    });
}

function getDriverPortFromExecutor(executorsObject) {
    for (i = 0; i < executorsObject.length; ++i) {
        if (executorsObject[i].id == "driver") {
            return executorsObject[i].hostPort;
        }
    }
}

function setLivyLog() {
    getMessageAsync(localhost + projectId + "/?restType=livy&applicationId=" + appId, function (s) {
        $("#livyJobLog").text(s);
    });
}

function setJobDetail() {
    var selectedApp = findElement(applicationList, function (d) {
       return d.id == appId;
    });
    if(typeof selectedApp == 'undefined') {
        return;
    }
    setDebugInfo("selectApp " + appId);
    if(selectedApp.attempts[0].sparkUser == 'hive') {
        return;
    }
    var url = localhost + projectId + "/applications/" + appId + "/" +　attemptId ;
    getMessageAsync(url + "/jobs", function (s) {
        currentSelectedJobs = JSON.parse(s);
        renderJobDetails(currentSelectedJobs);
        // setJobGraphOnApplicationLevel(currentSelectedJobs);
        renderJobGraphOnApplicationLevel(currentSelectedJobs);
        // if(currentSelectedStages != null && !isJobGraphGenerated) {
        //     setJobGraph(currentSelectedJobs);
        // }
    });
}
// d3.select("#stored_rdd_details").selectAll("li")
//     .data(myData)
//     .enter()
//     .append("li")
//     .attr("role","presentation")
//     .append("a")
//     .attr("role","menuitem")
//     .attr("tabindex", -1)
//     .text(function(d) {
//         ++counter;
//         return "RDD " + d.id;
//     }).on("click", function(d,i) {
//     d3.select("#stored_rdd_info")
//         .selectAll("tr")
//         .remove();
//     d3.select("#stored_rdd_info")
//         .selectAll("tr")
//         .data(storedRDDDetailsColumn)
//         .enter()
//         .append("tr")
//         .html(function(inner) {
//             return "<td>"+ inner + "</td><td>" + d[inner] + "</td>";
//         });
// });
function setJobGraph(jobs) {
    isJobGraphGenerated = true;
    d3.select("#job-graph-menu")
        .selectAll('li')
        .data(jobs)
        .enter()
        .append('li')
        .attr("role","presentation")
        .append("a")
        .attr("role","menuitem")
        .attr("tabindex", -1)
        .text(function(job) {
            return "Job " + job['jobId'];
        }).on('click', function(job, i) {
        setJobGraphForOneJob(job);
    });
}
function setJobGraphOnApplicationLevel(jobs) {

}

function setJobGraphForOneJob(job) {
    var stageIds = job['stageIds'];
    var selectedStages = [];
    stageIds.forEach(function(stageId) {
       selectedStages.push(currentSelectedStages.find(function(d) {
           return d['stageId'] == stageId;
       }));
    });
    renderJobGraph(selectedStages);
}

function stagesInfo(jobs, url) {
        getMessageAsync(url + "/stages", function (s) {
            var data = new Object();
            var stages = JSON.parse(s);
            data.jobs = jobs;
            data.stages = stages;
            data.stageDetails = [];
            jobs.stageIds.forEach(function(stageNumber) {
                getMessageAsync(url + "/stages" + "/" + stageNumber, function(s) {
                    var detail = JSON.parse(s);
                });
            });
        });
}

function setJobTimeLine() {
    var url = localhost + projectId + "/cluster/apps/" + appId + "?restType=yarn";
    getMessageAsync(url, function(s) {
        var t = s;
    });
}

///applications/[app-id]/storage/rdd
function setStoredRDD() {
    if(attemptId == 0) {
        renderStoredRDD('');
        return;
    }
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/storage/rdd",function(s) {
        var rdds = JSON.parse(s);
        renderStoredRDD(rdds);
    });
}

function setStageDetailsWithTaskDetails() {
    if(attemptId == 0) {
        $("#stage_detail_info_message").text("No Stage Info");
        return;
    }
    $("#stage_detail_info_message").text('');
    getMessageAsync(localhost + projectId + "/applications/" + appId + "/" + attemptId + "/stages", function (s) {
        currentSelectedStages = JSON.parse(s);
        renderStageSummary(currentSelectedStages);
        setTaskDetails();
        if(!isJobGraphGenerated && currentSelectedJobs != null) {
            setJobGraph(currentSelectedJobs);
        }
    });
}

function setTaskDetails() {
    var httpQuery = localhost + projectId + "/applications" + "?applicationId="+appId + "&attemptId=" + attemptId + "&multi-stages=" + currentSelectedStages.length;
    getMessageAsync(httpQuery, function(s){
        var tasks = JSON.parse(s);
        renderTaskSummary(tasks);
    });
}

function setExecutorsDetails() {
    var httpQuery = localhost + projectId + "/applications/" + appId + "/" + attemptId + "/executors";
    getMessageAsync(httpQuery, function (s) {
        try {
            var executors = JSON.parse(s);
            renderExecutors(executors);
        } catch (e) {

        }
    })
}
function setDebugInfo(s) {
    $("#debuginfo").text(s);
}

function filterTaskSummaryTable() {
    var input, filter, table, tr, td, i;
    filter = $("#filterTableInput").val().toLowerCase();
    tr = $("#taskSummaryTable tbody tr");
    tr.each(function () {
        text = $(this).html().toLowerCase();
        if(text.indexOf(filter) > -1) {
            $(this).css("display","");
        } else {
            $(this).css("display","none");
        }
    });
}

function filterStageTaskTableWithStageIds(stageIds) {
    var tr = $("#stageSummaryTable tbody tr");
    tr.each(function (i) {
        var id = $("#stageSummaryTbody>tr>td:nth-child(2)")[i].innerHTML;
        if( $.inArray( parseInt(id), stageIds) > -1 ) {
            $(this).css("display","");
            filterTaskTableWithStageId(id);
        } else {
            $(this).css("display","none");
        }
    });
}

function filterTaskTableWithStageId(stageId) {
    var httpQuery = localhost + projectId + "/applications/" + appId + "/" + attemptId + "/stages/" + stageId;
    getMessageAsync(httpQuery, function (s) {
        var stageDetails = JSON.parse(s);
        if(getJsonLength(stageDetails)) {
            var filteredTaskIds = Object.keys(stageDetails[0].tasks);
            filterTaskTableWithTaskIds(filteredTaskIds);
        }
    })
}
function filterTaskTableWithTaskIds(taskIds) {
    var tr = $("#taskSummaryTable tbody tr");
    tr.each(function (i) {
        var id = $("#taskSummaryTbody>tr>td:nth-child(1)")[i].innerHTML;
        if( $.inArray( id, taskIds) > -1 ) {
            $(this).css("display","");
        } else {
            $(this).css("display","none");
        }
    });
}
function getJsonLength(jsonObject) {
    var length = 0;
    for(item in jsonObject) {
        length++;
    }
    return length;
}
function openLivyLog() {

}