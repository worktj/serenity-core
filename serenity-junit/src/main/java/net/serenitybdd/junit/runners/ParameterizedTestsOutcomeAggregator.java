package net.serenitybdd.junit.runners;

import net.thucydides.core.model.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.Runner;

import java.util.*;
import java.util.stream.Collectors;

public class ParameterizedTestsOutcomeAggregator {
    private final SerenityParameterizedRunner serenityParameterizedRunner;

    private ParameterizedTestsOutcomeAggregator(SerenityParameterizedRunner serenityParameterizedRunner) {
        this.serenityParameterizedRunner = serenityParameterizedRunner;
    }

    public static ParameterizedTestsOutcomeAggregator from(SerenityParameterizedRunner serenityParameterizedRunner) {
        return new ParameterizedTestsOutcomeAggregator(serenityParameterizedRunner);
    }


    public List<TestOutcome> aggregateTestOutcomesByTestMethods() {
        List<TestOutcome> allOutcomes = getTestOutcomesForAllParameterSets();

        if (allOutcomes.isEmpty()) {
            return new ArrayList<>();
        } else {
            return  aggregatedScenarioOutcomes(allOutcomes);
        }
    }

    private List<TestOutcome> aggregatedScenarioOutcomes(List<TestOutcome> allOutcomes) {
        Map<String, TestOutcome> scenarioOutcomes = new HashMap();

        for (TestOutcome testOutcome : allOutcomes) {
            final String normalizedMethodName = baseMethodName(testOutcome);

            TestOutcome scenarioOutcome = scenarioOutcomeFor(normalizedMethodName, testOutcome, scenarioOutcomes);
            recordTestOutcomeAsSteps(testOutcome, scenarioOutcome);

            if (testOutcome.isManual()) {
                scenarioOutcome = scenarioOutcome.setToManual();
            }

            if (testOutcome.isDataDriven()) {
                scenarioOutcome.addDataFrom(testOutcome.getDataTable());
            }
        }

        List<TestOutcome> aggregatedScenarioOutcomes = new ArrayList<>();
        aggregatedScenarioOutcomes.addAll(scenarioOutcomes.values());
        return aggregatedScenarioOutcomes;

    }

    private void recordTestOutcomeAsSteps(TestOutcome testOutcome, TestOutcome scenarioOutcome) {
        final String name = alternativeMethodName(testOutcome);
        TestStep nestedStep = TestStep.forStepCalled(name).withResult(testOutcome.getResult());
        List<TestStep> testSteps = testOutcome.getTestSteps();

        if (testOutcome.getTestFailureCause() != null) {
            nestedStep.failedWith(testOutcome.getTestFailureCause().toException());
        }

        if (!testSteps.isEmpty()) {
            for (TestStep nextStep : testSteps) {
                nextStep.setDescription(normalizeTestStepDescription(nextStep.getDescription(),
                        scenarioOutcome.getTestSteps().size() + 1));
                nestedStep.addChildStep(nextStep);
                nestedStep.setDuration(nextStep.getDuration() + nestedStep.getDuration());
            }
        }

        if (nestedStep.getDuration() == 0) {
            nestedStep.setDuration(testOutcome.getDuration());
        }
        scenarioOutcome.recordStep(nestedStep);
    }

    private TestOutcome scenarioOutcomeFor(String normalizedMethodName, TestOutcome testOutcome, Map<String, TestOutcome> scenarioOutcomes) {
        if (!scenarioOutcomes.containsKey(normalizedMethodName)) {
            TestOutcome scenarioOutcome = createScenarioOutcome(testOutcome);
            scenarioOutcomes.put(normalizedMethodName, scenarioOutcome);
        }
        return scenarioOutcomes.get(normalizedMethodName);
    }

    private boolean rowResultsAreInconsistantWithOverallResult(TestOutcome testOutcome) {
        TestResult overallRowResult = overallResultFrom(testOutcome.getDataTable().getRows());
        return (testOutcome.isError() || testOutcome.isFailure() || testOutcome.isCompromised())
                && (!testOutcome.getDataTable().getRows().isEmpty())
                && (testOutcome.getResult() != overallRowResult);
    }

    private TestResult overallResultFrom(List<DataTableRow> rows) {

        List<TestResult> resultsOfEachRow = rows.stream()
                .map(DataTableRow::getResult)
                .collect(Collectors.toList());

        return TestResultList.overallResultFrom(resultsOfEachRow);
    }

    private String normalizeTestStepDescription(String description, int index) {
        return StringUtils.replace(description, "[1]", "[" + index + "]");
    }

    private TestOutcome createScenarioOutcome(TestOutcome parameterizedOutcome) {
        TestOutcome testOutcome = TestOutcome.forTest(baseMethodName(parameterizedOutcome),
                parameterizedOutcome.getTestCase());

        return testOutcome;
    }

    private String baseMethodName(TestOutcome testOutcome) {
        return testOutcome.getName().replaceAll("\\[\\d+\\]", "");
    }

    private String alternativeMethodName(TestOutcome testOutcome) {
        Optional<String> qualifier = testOutcome.getQualifier();
        if (qualifier.isPresent()) {
            return testOutcome.getTitle(false) + " " +
                    testOutcome.getQualifier().get();
        } else {
            return testOutcome.getTitle();
        }
    }

    public List<TestOutcome> getTestOutcomesForAllParameterSets() {
        List<TestOutcome> testOutcomes = new ArrayList<>();
        for (Runner runner : serenityParameterizedRunner.getRunners()) {
            for (TestOutcome testOutcome : ((SerenityRunner) runner).getTestOutcomes()) {
                if (!testOutcomes.contains(testOutcome)) {
                    testOutcomes.add(testOutcome);
                }
            }
        }
        return testOutcomes;
    }
}
