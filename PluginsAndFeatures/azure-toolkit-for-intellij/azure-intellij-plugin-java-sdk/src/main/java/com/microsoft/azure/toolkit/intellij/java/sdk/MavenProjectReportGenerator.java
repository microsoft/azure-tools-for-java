package com.microsoft.azure.toolkit.intellij.java.sdk;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.Error;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.*;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.AzureSearchScope;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.DeprecatedDependencyUtil;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.MavenRootProjectScope;
import com.microsoft.azure.toolkit.intellij.java.sdk.utils.MavenUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class creates a scheduled executor that periodically generates a project report that contains information about
 * Azure SDK dependency, client method usage and sends the report to Application Insights.
 * This report generation is enabled, by default and can be disabled at anytime by the end user by turning off the
 * "Azure SDK Report Enabled" registry toggle. This report is only generated for Maven projects.
 */
@Slf4j
public final class MavenProjectReportGenerator implements ProjectActivity, DumbAware, ProjectManagerListener {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String AZURE_SDK_REPORT_ENABLED = "Azure SDK Report Enabled";
    private static final String AZURE_GROUP_ID = "com.azure";
    private static final String APP_INSIGHTS_CONNECTION_STRING = "InstrumentationKey=1d377c0e-44f8-4d56-bee7-7f13a3fef594;" +
            "IngestionEndpoint=https://centralus-2.in.applicationinsights.azure.com/;" +
            "LiveEndpoint=https://centralus.livediagnostics.monitor.azure.com/";

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final int INITIAL_DELAY_IN_MINUTES = 1;
    private static final String AZURE_SDK_BOM = "azure-sdk-bom";
    private static final String SERVICE_METHOD_ANNOTATION = "com.azure.core.annotation.ServiceMethod";
    private static final String BETA_METHOD_ANNOTATION = "com.azure.cosmos.util.Beta";

    private final TelemetryClient telemetryClient;
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public MavenProjectReportGenerator() {
        final TelemetryConfiguration configuration = TelemetryConfiguration.createDefault();
        configuration.setConnectionString(APP_INSIGHTS_CONNECTION_STRING);
        telemetryClient = new TelemetryClient(configuration);
    }

    //@TODO recheck if generated report is idempotent (still the same)
    @Nullable
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        ReadAction.nonBlocking(() -> {
            generateReport(project);
            return Unit.INSTANCE;
        }).inSmartMode(project)
          .coalesceBy(project, MavenProjectReportGenerator.class)
          .expireWhen(project::isDisposed)
          .submit(scheduledExecutor);

        return null;
    }

    @Override
    public void projectClosed(@Nonnull Project project) {
        scheduledExecutor.shutdown();
    }

    private void generateReport(@Nonnull Project project) {
        final AzureConfiguration config = Azure.az().config();
        if (config.getTelemetryEnabled() == null || config.getTelemetryEnabled()) {
            try {
                final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstanceIfCreated(project);
                if (mavenProjectsManager == null || !mavenProjectsManager.isMavenizedProject()) {
                    return;
                }
                // A single IntelliJ project can contain multiple root projects. Create
                // separate reports for each root Maven project. A root Maven project can consist of multiple
                // Maven modules.
                final Map<String, MavenProjectReport> projectReports = createProjectReports(project);

                for (final Map.Entry<String, MavenProjectReport> entry : projectReports.entrySet()) {
                    final String key = entry.getKey();
                    final MavenProjectReport value = entry.getValue();
                    log.debug("Report for " + key + ": " + OBJECT_MAPPER.writeValueAsString(value));
                    sendReportToAppInsights(value);
                }
            } catch (final ProcessCanceledException e) {
                throw e;
            } catch (final Exception e) {
                log.error("Unable to send the Azure SDK report ", e);
            }
        } else {
            log.debug("Azure telemetry is disabled");
        }
    }

    private Map<String, MavenProjectReport> createProjectReports(Project project) {
        final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);

        final Map<String, MavenProjectReport> projectReports = new HashMap<>();
        mavenProjectsManager.getRootProjects()
                .forEach(mavenProject -> inspectRootMavenProject(mavenProjectsManager, mavenProject, projectReports));
        return projectReports;
    }

    private void inspectRootMavenProject(MavenProjectsManager mavenProjectsManager, MavenProject mavenProject,
                                         Map<String, MavenProjectReport> projectReports) {
        final MavenProjectReport report = new MavenProjectReport();
        projectReports.put(mavenProject.getMavenId().getDisplayString(), report);

        report.setGroupId(getMd5(mavenProject.getMavenId().getGroupId()));
        report.setArtifactId(getMd5(mavenProject.getMavenId().getArtifactId()));
        report.setVersion(getMd5(mavenProject.getMavenId().getVersion()));

        if (mavenProject.isAggregator()) {
            mavenProjectsManager.getModules(mavenProject)
                    .forEach(mavenModule -> analyzeMavenModule(mavenProjectsManager, mavenModule, report));
        } else {
            analyzeMavenModule(mavenProjectsManager, mavenProject, report);
        }
        // code analysis doesn't have to be per module and can be done on the entire Maven Project
        analyzeCode(mavenProjectsManager, mavenProject, report);
    }

    private void analyzeMavenModule(MavenProjectsManager mavenProjectsManager, MavenProject mavenProject, MavenProjectReport report) {
        analyzePom(mavenProjectsManager, mavenProject, report);
    }

    private void analyzePom(MavenProjectsManager mavenProjectsManager, MavenProject mavenProject, MavenProjectReport report) {
        final List<String> azureDependencies = new ArrayList<>();
        final List<DeprecatedDependency> deprecatedDependencies = new ArrayList<>();

        checkDependencyManagement(mavenProjectsManager, mavenProject, report);
        mavenProject.getDependencyTree()
                .forEach(mavenArtifactNode -> {
                    final MavenArtifact dependency = mavenArtifactNode.getArtifact();
                    final String groupId = dependency.getGroupId();
                    final String artifactId = dependency.getArtifactId();
                    final String versionId = dependency.getVersion();

                    final Optional<DeprecatedDependency> deprecatedDependency = DeprecatedDependencyUtil.lookupReplacement(groupId, artifactId);
                    deprecatedDependency.ifPresent(deprecatedDependencies::add);

                    final boolean isAzureGroupId = isAzureGroupId(groupId);
                    if (isAzureGroupId) {
                        azureDependencies.add(dependency.getMavenId().getDisplayString());
                    }

                    if (report.getBomVersion() == null && isAzureGroupId && versionId != null && !versionId.contains("beta")) {
                        report.addError(new Error("Azure SDK BOM not used",
                                ErrorCode.BOM_NOT_USED, ErrorLevel.WARNING, List.of(dependency.getMavenId().getDisplayString())));
                    }

                    if (isAzureGroupId && versionId != null && versionId.contains("beta")) {
                        report.addError(new Error("Beta version of the library used",
                                ErrorCode.BETA_DEPENDENCY_USED, ErrorLevel.WARNING, List.of(dependency.getMavenId().getDisplayString())));
                    }
                });
        report.addAllAzureDependencies(azureDependencies);
        report.addAllDeprecatedDependencies(deprecatedDependencies);
    }

    private static boolean isAzureGroupId(String groupId) {
        return groupId != null && groupId.startsWith(AZURE_GROUP_ID);
    }

    private void analyzeCode(MavenProjectsManager mavenProjectsManager, MavenProject mavenProject, MavenProjectReport report) {
        final Module module = ApplicationManager.getApplication()
                .runReadAction((Computable<Module>) () -> mavenProjectsManager.findModule(mavenProject));
        if (module == null) {
            return;
        }
        final Project project = mavenProjectsManager.getProject();
        // Search for all methods in the library with the target annotation
        final AzureSearchScope azureSearchScope = new AzureSearchScope(project);
        AllClassesSearch.search(azureSearchScope, project).forEach(psiClass -> {
            ProgressManager.checkCanceled();
            for (final PsiMethod method : psiClass.getMethods()) {
                ProgressManager.checkCanceled();
                final MethodCallDetails serviceMethodCall = findAnnotatedMethodCalls(mavenProject, method, project, SERVICE_METHOD_ANNOTATION);
                if (serviceMethodCall != null) {
                    report.addServiceMethodCall(serviceMethodCall);
                }
                final MethodCallDetails betaMethodCall = findAnnotatedMethodCalls(mavenProject, method, project, BETA_METHOD_ANNOTATION);
                if (betaMethodCall != null) {
                    report.addBetaMethodCall(betaMethodCall);
                }
            }
            return true;
        });
    }

    @Nullable
    private MethodCallDetails findAnnotatedMethodCalls(MavenProject mavenProject,
                                                       PsiMethod method, Project project, String annotation) {
        final PsiAnnotation psiAnnotation = method.getAnnotation(annotation);
        if (psiAnnotation != null) {
            ProgressManager.checkCanceled();
            // Annotated method found; search for references
            final Collection<PsiReference> references = ReferencesSearch.search(method, new MavenRootProjectScope(project, mavenProject)).findAll();
            if (!references.isEmpty()) {
                final PsiClass containingClass = method.getContainingClass();
                final PsiType returnType = method.getReturnType();
                if (containingClass == null || returnType == null) {
                    return null;
                }
                final String className = containingClass.getQualifiedName();
                if (className == null) {
                    return null;
                }
                final String methodName = className + "." + method.getName();
                final String params = Arrays.stream(method.getParameterList().getParameters())
                        .map(parameter -> parameter.getType().getCanonicalText())
                        .collect(Collectors.joining(","));
                final String methodSignature = returnType.getCanonicalText() + " " + methodName + "(" + params + ")";
                return new MethodCallDetails().setCallFrequency(references.size()).setMethodName(methodSignature);
            }
        }
        return null;
    }

    private void sendReportToAppInsights(MavenProjectReport report) {
        try {
            final EventTelemetry telemetry = new EventTelemetry("azure-sdk-java-intellij-telemetry");
            telemetry.getProperties().putAll(getCustomEventProperties(report));
            telemetryClient.track(telemetry);
            telemetryClient.flush();
            log.info("Successfully sent the report to Application Insights");
        } catch (final Exception ex) {
            log.error("Unable to send report to Application Insights. " + ex.getMessage());
        }
    }

    private Map<String, String> getCustomEventProperties(MavenProjectReport report) {
        final Map<String, Object> properties = OBJECT_MAPPER.convertValue(report, MAP_TYPE_REFERENCE);
        final Map<String, String> customEventProperties = new HashMap<>(properties.size());
        // AppInsights customEvents table does not support nested JSON objects in "properties" field
        // So, we have to convert the nested objects to strings
        properties.forEach((key, value) -> {
            if (value instanceof String) {
                customEventProperties.put(key, (String) value);
            } else {
                customEventProperties.put(key, BinaryData.fromObject(value).toString());
            }
        });
        return customEventProperties;
    }

    private void checkDependencyManagement(MavenProjectsManager mavenProjectsManager, MavenProject mavenProject, MavenProjectReport report) {
        final VirtualFile pomFile = mavenProject.getFile();
        final PsiFile psiFile = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiFile>) () -> PsiManager.getInstance(mavenProjectsManager.getProject()).findFile(pomFile));
        if (psiFile == null) {
            return;
        }
        final FileViewProvider viewProvider = psiFile.getViewProvider();
        final XmlFile xmlFile = (XmlFile) viewProvider.getPsi(StdLanguages.XML);
        final XmlTag rootTag = ApplicationManager.getApplication()
                .runReadAction((Computable<XmlTag>) xmlFile::getRootTag);
        if (rootTag != null && "project".equals(rootTag.getName())) {
            final XmlTag dependencyManagement = ApplicationManager.getApplication()
                    .runReadAction((Computable<XmlTag>) () -> rootTag.findFirstSubTag("dependencyManagement"));
            if (dependencyManagement != null) {
                final XmlTag dependenciesTag = ApplicationManager.getApplication()
                        .runReadAction((Computable<XmlTag>) () -> dependencyManagement.findFirstSubTag("dependencies"));
                if (dependenciesTag != null) {
                    final XmlTag[] dependencyTags = ApplicationManager.getApplication()
                            .runReadAction((Computable<XmlTag[]>) () -> dependenciesTag.findSubTags("dependency"));

                    for (final XmlTag dependencyTag : dependencyTags) {
                        final String groupId = getTextValue(dependencyTag, "groupId");
                        final String artifactId = getTextValue(dependencyTag, "artifactId");
                        final String versionId = getTextValue(dependencyTag, "version");

                        if (isAzureGroupId(groupId) && AZURE_SDK_BOM.equals(artifactId)) {
                            final String latestArtifactVersion = MavenUtils.getLatestArtifactVersion(groupId, artifactId);
                            if (versionId != null && !versionId.equals(latestArtifactVersion)) {
                                report.setBomVersion(versionId);
                                report.addError(new Error("Newer version of azure-sdk-bom available",
                                        ErrorCode.OUTDATED_DEPENDENCY, ErrorLevel.WARNING,
                                        List.of(groupId + ":" + artifactId + ":" + versionId)));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private String getTextValue(XmlTag dependencyTag, String subTagName) {
        final XmlTag subTag = dependencyTag.findFirstSubTag(subTagName);
        if (subTag != null) {
            return subTag.getValue().getText();
        }
        return null;
    }

    private String getMd5(String inputText) {
        return DigestUtils.md5Hex(inputText);
    }
}
