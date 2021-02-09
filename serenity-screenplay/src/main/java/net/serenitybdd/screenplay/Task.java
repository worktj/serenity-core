package net.serenitybdd.screenplay;

import net.serenitybdd.core.steps.Instrumented;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A marker class to indicate that a Performable represents a higher level business task,
 * rather than a system interaction.
 */
public interface Task extends Performable {
    static <T extends Performable> AnonymousTask where(String title, T... steps) {
        return Instrumented.instanceOf(AnonymousTask.class).withProperties(title, Arrays.asList(steps));
    }

    static <T extends Performable> AnonymousPerformableFunction where(String title, Consumer<Actor> performableOperation) {
        return Instrumented.instanceOf(AnonymousPerformableFunction.class).withProperties(title, performableOperation);
    }

    static <T extends Performable> AnonymousPerformableRunnable thatPerforms(String title, Runnable performableOperation) {
        return Instrumented.instanceOf(AnonymousPerformableRunnable.class).withProperties(title, performableOperation);
    }
}
