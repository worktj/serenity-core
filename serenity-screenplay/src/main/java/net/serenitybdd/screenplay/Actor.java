package net.serenitybdd.screenplay;

import net.serenitybdd.core.PendingStepException;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.core.SkipNested;
import net.serenitybdd.core.eventbus.Broadcaster;
import net.serenitybdd.markers.IsHidden;
import net.serenitybdd.screenplay.events.*;
import net.serenitybdd.screenplay.exceptions.IgnoreStepException;
import net.serenitybdd.screenplay.facts.Fact;
import net.serenitybdd.screenplay.facts.FactLifecycleListener;
import net.thucydides.core.annotations.Pending;
import net.thucydides.core.annotations.Step;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.core.steps.StepListener;
import net.thucydides.core.util.EnvironmentVariables;
import org.openqa.selenium.Keys;

import java.lang.reflect.Method;
import java.util.*;

import static net.serenitybdd.screenplay.SilentTasks.isNestedInSilentTask;
import static net.serenitybdd.screenplay.SilentTasks.isSilent;
import static net.thucydides.core.ThucydidesSystemProperty.MANUAL_TASK_INSTRUMENTATION;

/**
 * An actor represents the person or system using the application under test.
 * Actors can have Abilities, which allows them to perform Tasks and Interactions.
 */
public class Actor implements PerformsTasks, SkipNested {

    private String name;

    private final PerformedTaskTally taskTally = new PerformedTaskTally();
    private EventBusInterface eventBusInterface = new EventBusInterface();
    private ConsequenceListener consequenceListener = new ConsequenceListener(eventBusInterface);

    private String description;
    private Map<String, Object> notepad = new HashMap<>();
    private Map<Class, Ability> abilities = new HashMap<>();

    private String preferredPronoun;

    public Actor(String name) {
        this.name = name;
    }


    public String toString() {
        return getNameOrPronoun();
    }

    public static Actor named(String name) {
        EventBusInterface.castActor(name);
        return new Actor(name);
    }

    public Actor describedAs(String description) {
        this.description = description;
        assignDescriptionToActor(description);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getNameOrPronoun() {
        return (preferredPronoun != null) ? preferredPronoun : name;
    }

    public <T extends Ability> Actor can(T doSomething) {
        if (doSomething instanceof RefersToActor) {
            ((RefersToActor)doSomething).asActor(this);
        }
        abilities.put(doSomething.getClass(), doSomething);
        eventBusInterface.assignAbilityToActor(this, doSomething.toString());
        return this;
    }

    public <T extends Ability> Actor whoCan(T doSomething) {
        return can(doSomething);
    }

    @SuppressWarnings("unchecked")
    public <T extends Ability> T abilityTo(Class<? extends T> doSomething) {
        T ability = (T) abilities.get(doSomething);

        if (ability == null) {
            ability = this.getAbilityThatExtends(doSomething);
        }

        return ability;
    }

    /**
     * Return an ability that extends the given class. Can be a Superclass or an Interface. If there are multiple
     * candidate Abilities, the first one found will be returned.
     * @param extendedClass the Interface class that we expect to find
     * @param <C> the matching Ability cast to extendedClass or null if none match
     */
    @SuppressWarnings("unchecked")
    public <C> C getAbilityThatExtends(Class<C> extendedClass) {
        // See if any ability extends doSomething
        for (Map.Entry<Class, Ability> entry: abilities.entrySet()) {
            // Return the first matching Ability we find
            if (extendedClass.isAssignableFrom(entry.getKey())) {
                return  (C) entry.getValue();
            }
        }
        return null;
    }

    /**
     * Return a list of all {@link Ability}s which implement {@link HasTeardown}
     */
    public List<HasTeardown> getTeardowns() {
        List<HasTeardown> teardowns = new ArrayList<>();
        for (Ability a : abilities.values()) {
            if (a instanceof HasTeardown) {
                teardowns.add((HasTeardown) a);
            }
        }
        return teardowns;
    }

    /**
     * A more readable way to access an actor's abilities.
     */
    public <T extends Ability> T usingAbilityTo(Class<? extends T> doSomething) {
        return abilityTo(doSomething);
    }

    /**
     * A method used to declare that an actor is now the actor in the spotlight, without having them perform any tasks.
     */
    public final void entersTheScene() {}

    public final void has(Performable... todos) {
        attemptsTo(todos);
    }

    private List<FactLifecycleListener> factListeners = new ArrayList<>();

    public final void has(Fact... facts) {
        Arrays.stream(facts).forEach(
                fact -> {
                    fact.setup(this);
                    eventBusInterface.assignFactToActor(this, fact.toString());
                    FactLifecycleListener listener = new FactLifecycleListener(this, fact);
                    factListeners.add(listener);
                    StepEventBus.getEventBus().registerListener(listener);
                }
        );
    }

    /**
     * A tense-neutral synonym for addFact() for use with given() clauses
     */
    public final void wasAbleTo(Performable... todos) {
        attemptsTo(todos);
    }

    public final void attemptsTo(Performable... tasks) {
        beginPerformance();
        for (Performable task : tasks) {
            if (isNestedInSilentTask()) {
                performSilently(task);
            } else if (isSilent(task)) {
                performSilently(task);
            } else if (isHidden(task) || shouldNotReport(task)) {
                performWithoutReporting(task);
            } else {
                perform(InstrumentedTask.of(task));
            }
        }
        endPerformance();
    }

    private boolean isHidden(Performable task) {
        return task instanceof IsHidden;
    }

    private boolean shouldNotReport(Performable task) {
        if (manualTaskInstrumentation() && noStepAnnotationIsPresentIn(task)) {
            return true;
        }
        return !InstrumentedTask.isInstrumented(task) && !InstrumentedTask.shouldInstrument(task);
    }

    private boolean noStepAnnotationIsPresentIn(Performable task) {
        try {
            return task.getClass().getMethod("performAs").getAnnotation(Step.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public <ANSWER> ANSWER asksFor(Question<ANSWER> question) {
        beginPerformance();
        ANSWER answer = question.answeredBy(this);
        endPerformance();

        return answer;
    }

    private <T extends Performable> void performSilently(T todo) {
        perform(todo);
    }

    private <T extends Performable> void performWithoutReporting(T todo) {
        perform(todo);
    }

    private <T extends Performable> void performConditionally(T todo) {
        perform(todo);
    }

    private <T extends Performable> void perform(T todo) {
        if (isPending(todo)) {
            StepEventBus.getEventBus().stepPending();
        }

        try {
            notifyPerformanceOf(todo);
            taskTally.newTask();

            performTask(todo);

            if (anOutOfStepErrorOccurred()) {
                eventBusInterface.mergePreviousStep();
            }
        } catch (Throwable exception) {
            if (!pendingOrIgnore(exception)) {
                eventBusInterface.reportStepFailureFor(todo, exception);
            }
            if (Serenity.shouldThrowErrorsImmediately() || isAnAssumptionFailure(exception)) {
                throw exception;
            }
        } finally {
            eventBusInterface.updateOverallResult();
        }
    }

    private <T extends Performable> void performTask(T todo) {
        if (!StepEventBus.getEventBus().currentTestIsSuspended()) {
            todo.performAs(this);
        }
    }

    private <T extends Performable> void notifyPerformanceOf(T todo) {
        Broadcaster.getEventBus().post(new ActorPerforms(todo, getName()));
    }

    private <T extends Performable> boolean isPending(T todo) {
            Method performAs = getPerformAsForClass(todo.getClass().getSuperclass()).orElse(getPerformAsForClass(todo.getClass()).orElse(null));

            return (performAs != null) && (performAs.getAnnotation(Pending.class) != null);
    }

    private Optional<Method> getPerformAsForClass(Class taskClass) {
        try {
            return Optional.of(taskClass.getMethod("performAs", Actor.class));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private boolean pendingOrIgnore(Throwable exception) {
        return exception instanceof IgnoreStepException ||
                exception instanceof PendingStepException;
    }

    private boolean isAnAssumptionFailure(Throwable e) {
        return e.getClass().getSimpleName().contains("Assumption");
    }

    public final void can(Consequence... consequences) {
        should(consequences);
    }


    public final void should(String groupStepName, Consequence... consequences) {

        try {
            String groupTitle = injectActorInto(groupStepName);
            StepEventBus.getEventBus().stepStarted(ExecutedStepDescription.withTitle(groupTitle));
            should(consequences);

        } catch (Throwable error) {
            throw error;
        } finally {
            StepEventBus.getEventBus().stepFinished();
        }
    }

    private String injectActorInto(String groupStepName) {
        return groupStepName.replaceAll("\\{0\\}", this.toString());
    }

    public final void should(Consequence... consequences) {

        //if (StepEventBus.getEventBus().isDryRun()) { return; }

        ErrorTally errorTally = new ErrorTally(eventBusInterface);

        startConsequenceCheck();

        for (Consequence consequence : consequences) {
            check(consequence, errorTally);
        }

        endConsequenceCheck();

        errorTally.reportAnyErrors();

    }

    private boolean anOutOfStepErrorOccurred() {
        if (eventBusInterface.aStepHasFailedInTheCurrentExample()) {
            return (eventBusInterface.getRunningStepCount()) > taskTally.getPerformedTaskCount();
        } else {
            return false;
        }
    }

    private <T> void check(Consequence<T> consequence, ErrorTally errorTally) {

        ConsequenceCheckReporter reporter = new ConsequenceCheckReporter(eventBusInterface, consequence);
        try {
            reporter.startQuestion(this);
            if (eventBusInterface.shouldIgnoreConsequences()) {
                reporter.reportStepIgnored();
            } else {
                consequence.evaluateFor(this);
                reporter.reportStepFinished();
            }
        } catch (IgnoreStepException e) {
            reporter.reportStepIgnored();
        } catch (Throwable e) {
            errorTally.recordError(consequence, e);
        }
    }

    public <ANSWER> void remember(String key, Question<ANSWER> question) {
        beginPerformance();
        ANSWER answer = this.asksFor(question);
        notepad.put(key, answer);
        endPerformance();
    }

    public void remember(String key, Object value) {
        notepad.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T recall(String key) {
        return (T) notepad.get(key);
    }

    public Map<String, Object> recallAll() {
        return new HashMap<>(notepad);
    }

    @SuppressWarnings("unchecked")
    public <T> T forget(String key) { return (T) notepad.remove(key);}

    public <T> T sawAsThe(String key) {
        return recall(key);
    }

    public <T> T gaveAsThe(String key) {
        return recall(key);
    }

    private void beginPerformance() {
        Broadcaster.getEventBus().post(new ActorBeginsPerformanceEvent(name));
    }

    private void endPerformance() {
        Broadcaster.getEventBus().post(new ActorEndsPerformanceEvent(name));
    }


    private void startConsequenceCheck() {
        beginPerformance();
        consequenceListener.beginConsequenceCheck();
        Broadcaster.getEventBus().post(new ActorBeginsConsequenceCheckEvent(name));
    }

    private void endConsequenceCheck() {
        consequenceListener.endConsequenceCheck();
        Broadcaster.getEventBus().post(new ActorEndsConsequenceCheckEvent(name));
        endPerformance();
    }


    public Actor usingPronoun(String pronoun) {
        this.preferredPronoun = pronoun;
        return this;
    }

    public Actor withNoPronoun() {
        this.preferredPronoun = null;
        return this;
    }


    public void assignDescriptionToActor(String description) {
        StepEventBus.getEventBus().getBaseStepListener().latestTestOutcome().ifPresent(
                testOutcome -> testOutcome.assignDescriptionToActor(getName(), description)
        );
    }

    public void assignName(String name) {
        this.name = name;
    }

    private boolean manualTaskInstrumentation() {
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        return (MANUAL_TASK_INSTRUMENTATION.booleanFrom(environmentVariables, false));
    }

    public void wrapUp() {
        getTeardowns().forEach(HasTeardown::tearDown);
        factListeners.forEach(
                factLifecycleListener -> StepEventBus.getEventBus().dropListener(factLifecycleListener)
        );

    }
}
