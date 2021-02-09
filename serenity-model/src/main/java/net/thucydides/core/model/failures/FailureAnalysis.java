package net.thucydides.core.model.failures;

import io.cucumber.java.PendingException;
import net.serenitybdd.core.PendingStepException;
import net.serenitybdd.core.environment.ConfiguredEnvironment;
import net.serenitybdd.core.exceptions.CausesAssertionFailure;
import net.serenitybdd.core.exceptions.CausesCompromisedTestFailure;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepFailureException;
import net.thucydides.core.util.EnvironmentVariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.thucydides.core.model.TestResult.*;

/**
 * Determine whether a given type of exception should result in a failure or an error.
 * By default, any exception  that extends AssertionError is a FAILURE.
 * Any exception  that extends WebdriverAssertionError and has a cause that is an AssertionError is also a FAILURE.
 * All other exceptions are an ERROR (except for StepFailureException as described below)
 * <p>
 * Any exception that extends StepFailureException and has a cause that meets the above criteria is classed as above.
 * All other exceptions are an ERROR
 * <p>
 * You can specify your own exceptions that will cause a failure by using the serenity.fail.on property.
 * You can also specify those that will cause an error using serenity.error.on.
 */
public class FailureAnalysis {

    public FailureAnalysis() {
        this(ConfiguredEnvironment.getEnvironmentVariables());
    }

    private final FailureAnalysisConfiguration configured;

    public FailureAnalysis(EnvironmentVariables environmentVariables) {
        this.configured = new FailureAnalysisConfiguration(environmentVariables);
    }

    public TestResult resultFor(Class testFailureCause) {
        if (reportAsPending(testFailureCause)) {
            return PENDING;
        }
        if (reportAsSkipped(testFailureCause)) {
            return SKIPPED;
        }
        if (reportAsCompromised(testFailureCause)) {
            return COMPROMISED;
        }
        if (reportAsFailure(testFailureCause)) {
            return FAILURE;
        }
        return ERROR;
    }

    private static final List<Class<?>> DEFAULT_FAILURE_TYPES = new ArrayList<>();

    static {
        DEFAULT_FAILURE_TYPES.addAll(Arrays.asList(AssertionError.class, CausesAssertionFailure.class));
    }

    private static final List<Class<?>> DEFAULT_COMPROMISED_TYPES = new ArrayList<>();

    static {
        DEFAULT_COMPROMISED_TYPES.addAll(Arrays.asList(CausesCompromisedTestFailure.class));
    }

    private static final List<Class<?>> DEFAULT_PENDING_TYPES = new ArrayList<>();

    static {
        DEFAULT_PENDING_TYPES.addAll(Arrays.asList(PendingStepException.class, PendingException.class));
    }

    private static final List<Class<?>> DEFAULT_ERROR = new ArrayList<>();

    static {
        DEFAULT_ERROR.addAll(Arrays.asList(Error.class));
    }

    public boolean reportAsFailure(Class<?> testFailureCause) {
        if (testFailureCause == null) {
            return false;
        }
        for (Class<?> validFailureType : configured.failureTypes()) {
            if (isA(validFailureType, testFailureCause)) {
                return true;
            }
        }
        return false;
    }

    public boolean reportAsCompromised(Class<?> testFailureCause) {
        if (testFailureCause == null) {
            return false;
        }
        for (Class<?> validCompromisedType : configured.compromisedTypes()) {
            if (isA(validCompromisedType, testFailureCause)) {
                return true;
            }
        }
        return false;
    }

    public boolean reportAsPending(Class<?> testFailureCause) {
        if (testFailureCause == null) {
            return false;
        }
        for (Class<?> validPendingType : configured.pendingTypes()) {
            if (isA(validPendingType, testFailureCause)) {
                return true;
            }
        }
        return false;
    }

    public boolean reportAsSkipped(Class<?> testFailureCause) {
        if (testFailureCause == null) {
            return false;
        }
        for (Class<?> validSkippedType : configured.skippedTypes()) {
            if (isA(validSkippedType, testFailureCause)) {
                return true;
            }
        }
        return false;
    }

    public boolean reportAsError(Class<?> testFailureCause) {
        if (testFailureCause == null) {
            return false;
        }
        for (Class<?> validErrorType : configured.errorTypes()) {
            if (isA(validErrorType, testFailureCause)) {
                return true;
            }
        }
        return false;
    }

    private boolean isA(Class<?> expectedClass, Class testFailureCause) {
        return expectedClass.isAssignableFrom(testFailureCause);
    }

    public TestResult resultFor(Throwable testFailureCause) {
        if (isPendingException(testFailureCause)) {
            return PENDING;
        } else if (isSkippedException(testFailureCause)) {
            return SKIPPED;
        } else if (isFailure(testFailureCause)) {
            return FAILURE;
        } else if (failingStepException(testFailureCause)) {
            return FAILURE;
        } else if (isCompromised(testFailureCause)) {
            return COMPROMISED;
        } else {
            return ERROR;
        }
    }

    public TestResult resultFor(StepFailure stepFailure) {
        if (stepFailure.getExceptionClass() == null) {
            return FAILURE;
        } else {
            return resultFor(stepFailure.getExceptionClass());
        }
    }

    private boolean failingStepException(Throwable testFailureCause) {
        if (testFailureCause == null) {
            return false;
        }

        return ((StepFailureException.class.isAssignableFrom(testFailureCause.getClass()))
                && (testFailureCause.getCause() != null)
                && (isFailure(testFailureCause.getCause())));
    }

    private boolean isFailure(Throwable testFailureCause) {
        return reportAsFailure(RootCause.ofException(testFailureCause));
    }

    private boolean isCompromised(Throwable testFailureCause) {
        return reportAsCompromised(RootCause.ofException(testFailureCause));
    }

    private boolean isPendingException(Throwable testFailureCause) {
        return reportAsPending(RootCause.ofException(testFailureCause));
    }

    private boolean isSkippedException(Throwable testFailureCause) {
        return reportAsSkipped(RootCause.ofException(testFailureCause));
    }
}
