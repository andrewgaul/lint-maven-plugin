package com.lewisd.maven.lint.rules.basic;

import com.lewisd.maven.lint.ResultCollector;
import com.lewisd.maven.lint.rules.AbstractReduntantVersionRule;
import com.lewisd.maven.lint.util.ExpressionEvaluator;
import com.lewisd.maven.lint.util.ModelUtil;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class DuplicateDependenciesRule extends AbstractReduntantVersionRule {

    @Autowired
    public DuplicateDependenciesRule(final ExpressionEvaluator expressionEvaluator, final ModelUtil modelUtil) {
        super(expressionEvaluator, modelUtil);
    }

    @Override
    protected void addRequiredModels(final Set<String> requiredModels) {
    }

    @Override
    public String getIdentifier() {
        return "DuplicateDep";
    }

    @Override
    public String getDescription() {
        return "Multiple dependencies, in <dependencies> or <managedDependencies>, with the same co-ordinates are reduntant, " +
               "and can be confusing.  If they have different versions, they can lead to unexpected behaviour.";
    }

    @Override
    public void invoke(final MavenProject mavenProject, final Map<String, Object> models, final ResultCollector resultCollector) {
        final Model originalModel = mavenProject.getOriginalModel();
        final Collection<Dependency> dependencies = getExpressionEvaluator().getPath(originalModel, "dependencies");
        final Collection<Dependency> managedDependencies = getExpressionEvaluator().getPath(originalModel, "dependencyManagement/dependencies");

        checkForDuplicateDependencies(mavenProject, resultCollector, dependencies, "Dependency");
        checkForDuplicateDependencies(mavenProject, resultCollector, managedDependencies, "Managed dependency");
    }

    private void checkForDuplicateDependencies(final MavenProject mavenProject, final ResultCollector resultCollector,
                                               final Collection<Dependency> dependencies,
                                               final String dependencyDescription) {
        final Collection<Dependency> otherDependencies = new LinkedList<Dependency>(dependencies);
        for (final Dependency dependency : dependencies) {
            checkForDuplicateArtifacts(mavenProject, resultCollector, dependency, otherDependencies, dependencyDescription);
        }
    }

    private void checkForDuplicateArtifacts(final MavenProject mavenProject, final ResultCollector resultCollector,
                                            final Dependency dependency, final Collection<Dependency> otherDependencies, final String dependencyDescription) {
        for (final Iterator<Dependency> i = otherDependencies.iterator(); i.hasNext();) {
            final Dependency otherManagedDependency = i.next();

            if (otherManagedDependency.getManagementKey().equals(dependency.getManagementKey())) {
                i.remove();
                if (otherManagedDependency != dependency) {
                    final String version = getModelUtil().getVersion(dependency);
                    final String otherVersion = getModelUtil().getVersion(otherManagedDependency);
                    final InputLocation location = getModelUtil().getLocation(dependency);
                    final InputLocation otherLocation = getModelUtil().getLocation(otherManagedDependency);
                    if (version != null && otherVersion != null && otherVersion.equals(version) || version.equals(otherVersion)) {
                        resultCollector.addViolation(mavenProject, this, dependencyDescription + " '" + getModelUtil().getKey(dependency) +
                                                                         "' is declared multiple times with the same version: " +
                                                                         otherLocation.getLineNumber() + ":" + otherLocation.getColumnNumber(), location);
                    } else {
                        resultCollector.addViolation(mavenProject, this, dependencyDescription + " '" + getModelUtil().getKey(dependency) +
                                                                         "' is declared multiple times with different versions (" + version + ", "
                                                                         + otherVersion + ")", location);
                    }
                }
            }

        }
    }

}
