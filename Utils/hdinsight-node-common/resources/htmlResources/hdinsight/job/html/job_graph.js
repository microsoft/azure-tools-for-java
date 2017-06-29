function renderJobGraphOnApplicationLevel(jobs) {
    $('#applicationGraphDiv').removeClass('graph-disabled');
    $('#jobGraphDiv').addClass('graph-disabled');
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });

    var counters = jobs.length, i = 0;
    g.setNode(0, {label :"Driver",   class : "type-TOP"});
    for(i = 1; i <= counters; ++i) {
        var s = "Job " + i;
        var currentClass = jobs[i - 1]["status"] === "SUCCEEDED" ? "sparkJob-success" : "sparkJob-error";
        g.setNode(i, {label: s,  class : currentClass});
    }

    for(i = 1; i <= counters; ++i) {
        g.setEdge(0, i);
    }

    // Create the renderer
    var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
    var svg = d3.select("#applicationGraphSvg");

    // remove all graph first
    d3.selectAll("#applicationGraphSvg g").remove();

    var inner = svg.append("g");

// Run the renderer. This is what draws the final graph.
    render(d3.select("#applicationGraphSvg g"), g);

    var g_width = g.graph().width;
    var g_height = g.graph().height;
    var viewBoxValue = "0 0 " + 1.3 * g_width + " " +  g_height;
    svg.attr("viewBox", viewBoxValue);
    svg.attr("preserveAspectRatio", "xMidYMid meet");

    render(d3.select("#applicationGraphSvg g"), g);
    // Center the graph
    var applicationSvg = $('#applicationGraphSvg');
    if (!spark.graphsize) {
        spark.graphsize = {
            'width': applicationSvg.width(),
            'height': applicationSvg.height()
        };
    } else {
        applicationSvg.width(spark.graphsize.width);
        applicationSvg.height(spark.graphsize.height);
    }

    var zoom = d3.behavior.zoom().on("zoom", function() {
        inner.attr("transform", "translate(" + d3.event.translate + ")" +
            "scale(" + d3.event.scale + ")");
    });
    svg.call(zoom);

    // Simple function to style the tooltip for the given node.
    inner.selectAll("g.node")
        .attr("title", function(v) {
            return setToolTips(jobs, v)
        })
        .each(function(v) {
            $(this).tipsy({
                gravity: "w",
                opacity: 1,
                html: true });
        })
        .on('click',function(d) {
            if ( d === '0') {
                return;
            }
            renderJobGraphForSelectedJob(d);
        });
}

function renderJobGraphForSelectedJob(d) {
    var job = spark.jobStartEvents[d - 1];
    renderJobGraph(job);
}

function setToolTips(jobs, v) {
    if(v !== "0") {
        var counter = parseInt(v) - 1;
        var job = jobs[counter];
        return getFormattedTipsForJob(job);
    } else {
        // driver
        return getFormattedTipsForDriver();
    }
}

function getFormattedTipsForDriver() {
    var containerLogs = spark.selectedYarnApp.amContainerLogs;
    var paths = containerLogs.split('/');
    var amContainer = paths[paths.length - 2];

    return "<p class='name'>Application Details:</p>"
            + "<p class='description jobtips' align='left'>AM Container: {0}<br>".format(amContainer)
            + "<hr class='jobview-hr'/>"
            + "Start time: {0}<br>End Time: {1} <br>Duration(mins): {2}<br> Memory Seconds: {3}<br>Core Seconds: {4}".format( formatServerTime(spark.selectedApp.startTime),
                formatServerTime(spark.selectedApp.endTime),
                ((spark.selectedYarnApp.finishedTime - spark.selectedYarnApp.startedTime)/(1000 * 60)).toFixed(2),
                spark.selectedYarnApp.memorySeconds,
                spark.selectedYarnApp.vcoreSeconds)
            + "<hr class='jobview-hr'/>"
            + "</p>";
}

function getFormattedTipsForJob(job) {
    var timeDuration = getTimeIntervalByMins(job.completionTime, job.submissionTime);
    return "<p class='name jobtips' align='left'>Job ID: {0}<br> {1}</p>".format(job.jobId , job.name) +
        "<p class='description jobtips' align='left'>Time Duration(Mins): {0}<br>".format(timeDuration) +
        "Completed Tasks: {0} &nbsp;&nbsp;&nbsp;Failed Tasks: {1}<br>".format(job.numTasks, job.numFailedTasks) +
        "Completed Stages: {0} &nbsp;&nbsp;&nbsp;Failed Stages:{1}</p>".format(job.numCompletedStages, job.numFailedStages);
}

function renderJobGraph(job) {
    $('#applicationGraphDiv').addClass('graph-disabled');
    $('#jobGraphDiv').removeClass('graph-disabled');
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });

    var id = job['Job ID'];
    var stageIds = job['Stage IDs'];
    var stageInfos = job['Stage Infos'];
    stageInfos.sort(function(left, right) {
        return left['Stage ID'] > right['Stage ID'];
    });
    spark.stageMap = {};
    stageInfos.forEach(function(stage) {
        var id = stage['Stage ID'];
        var parentNodes = stage['Parent IDs'];
        spark.stageMap[id] = stage;
        if (parentNodes.length === 0) {
            g.setNode(id, {'label' : stage['Stage Name'], class: 'type-TOP'});
        } else {
            g.setNode(id, {'label' : stage['Stage Name']});
            parentNodes.forEach(function(parentId) {
                g.setEdge(parentId, id);
            });
        }
    });

    // Create the renderer
    var render = new dagreD3.render();

    // Set up an SVG group so that we can translate the final graph.
    var svg = d3.select('#jobGraphSvg');

    // remove all graph first
    svg.selectAll('g').remove();

    var inner = svg.append("g");

    // Run the renderer. This is what draws the final graph.
    render(inner, g);

    var g_width = g.graph().width;
    var g_height = g.graph().height;
    var viewBoxValue = '0 0 ' + 1.2 * g_width + ' ' + g_height;
    svg.attr('viewBox', viewBoxValue);
    svg.attr('preserveAspectRatio', 'xMidYMid meet');

    render(inner, g);

    // Center the graph
    var zoom = d3.behavior.zoom().on('zoom', function() {
        inner.attr('transform', 'translate(' + d3.event.translate + ')' +
            'scale(' + d3.event.scale + ')');
    });
    svg.call(zoom);

    inner.selectAll('g.node')
        .attr('title', function(d, v) {
            return setToolTipForStage(d);
        })
        .each(function(v) {
            $(this).tipsy({
                gravity: "w",
                opacity: 1,
                html: true });
        })
}

function setToolTipForStage(stageId) {
    var stageInfo = spark.stageMap[stageId];
    if (stageInfo) {
        var filterStages = spark.currentSelectedStages.filter(function(s) {
           return s['stageId'] === parseInt(stageId);
        });
        if (filterStages.length === 1) {
            var selectedStage = filterStages[0];
            return "<p class='name'>Stage Details:</p>"
                + "<p class='description jobtips' align='left'>Input Bytes: {0}<br>".format(selectedStage['inputBytes'])
                +  "Output Bytes: {0}<br>".format(selectedStage['outputBytes'])
                + "Shuffle Read Bytes: {0}<br>".format(selectedStage['shuffleReadBytes'])
                + "Shuffle Write Bytes: {0}<br>".format(selectedStage['shuffleWriteBytes'])
                + "executorRunTime: {0}<br>".format(selectedStage['executorRunTime'])
                + "<hr class='jobview-hr'/>"
                + "Complete Tasks: {0}<br>".format(selectedStage['numCompleteTasks'])
                + "Failed Tasks: {0}<br>".format(selectedStage['numFailedTasks'])
                + "</p>";
        }

    }
}