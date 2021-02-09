package net.thucydides.core.requirements;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.requirements.model.Requirement;
import net.thucydides.core.requirements.model.RequirementsConfiguration;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.Inflector;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;

public abstract class AbstractRequirementsTagProvider {

    protected final EnvironmentVariables environmentVariables;
    protected final String rootDirectory;
    protected final RequirementsConfiguration requirementsConfiguration;
    protected final RequirementsService requirementsService;

    protected AbstractRequirementsTagProvider(EnvironmentVariables environmentVariables, String rootDirectory) {
        this.environmentVariables = environmentVariables;
        this.requirementsConfiguration = new RequirementsConfiguration(environmentVariables);
        this.requirementsService = Injectors.getInjector().getInstance(RequirementsService.class);
        this.rootDirectory = rootDirectory;
    }

    protected AbstractRequirementsTagProvider(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        this.requirementsConfiguration = new RequirementsConfiguration(environmentVariables);
        this.requirementsService = Injectors.getInjector().getInstance(RequirementsService.class);
        this.rootDirectory = getDefaultRootDirectory();
    }


    protected String humanReadableVersionOf(String name) {
        String underscoredName = Inflector.getInstance().underscore(name);
        return Inflector.getInstance().humanize(underscoredName);
    }

    protected String getDefaultType(int level, int maxDepth) {
        List<String> types = getRequirementTypes();

        // Flat structure: maxdepth 0
        //      cap, feature | level 0 => [1]
        //      cap, feature,story | level 0 => [2]

        // 1-layer structure: maxdepth 1
        //      cap, feature | level 0 => [0]
        //      cap, feature | level 1 => [1]
        //      cap, feature,story | level 0 => [1]
        //      cap, feature,story | level 1 => [2]

        // 2-layer structure: maxdepth 2
        //      cap, feature, story | level 0 => [0]
        //      cap, feature, story | level 1 => [1]
        //      cap, feature, story | level 2 => [2]
        int relativeLevel = types.size() - 1 - maxDepth + level;

        if (relativeLevel > types.size() - 1) {
            return types.get(types.size() - 1);
        } else if (relativeLevel >= 0) {
            return types.get(relativeLevel);
        } else {
            return types.get(0);
        }
    }

    public abstract List<Requirement> getRequirements();

    protected String getDefaultType(int level) {
        return getDefaultType(level, getRequirementTypes().size() - 1);
    }

    public List<String> getRequirementTypes(String rootDirectory) {
        return new RequirementsConfiguration(environmentVariables, rootDirectory).getRequirementTypes();
    }

    private List<String> requirementsTypes = null;

    public List<String> getRequirementTypes() {
        if (requirementsTypes == null) {
            requirementsTypes = new RequirementsConfiguration(environmentVariables, rootDirectory).getRequirementTypes();
        }
        return requirementsTypes;
    }

    protected String getDefaultRootDirectory() {
        return requirementsConfiguration.getDefaultRootDirectory();
    }


    protected Optional<Requirement> firstRequirementFoundIn(Optional<Requirement>... requirements) {
        return stream(requirements).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
//        for(Optional<Requirement> requirement : requirements) {
//            if (requirement.isPresent()) {
//                return requirement;
//            }
//        }
//        return Optional.empty();
    }

    public Optional<Requirement> getParentRequirementOf(Requirement requirement) {

        return RequirementsTree.from(getRequirements()).stream().filter(
                candidateParent -> candidateParent.hasChild(requirement)
        ).findFirst();
//
//        for (Requirement candidateParent : RequirementsList.of(getRequirements()).asFlattenedList()) {
//            if (candidateParent.getChildren().contains(requirement)) {
//                return Optional.of(candidateParent);
//            }
//        }
//        return Optional.empty();
    }



}
