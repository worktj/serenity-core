package net.serenitybdd.screenplay.waits;


import net.serenitybdd.core.pages.WebElementState;
import net.serenitybdd.markers.IsSilent;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Interaction;
import net.serenitybdd.screenplay.SilentInteraction;
import net.serenitybdd.screenplay.questions.WebElementQuestion;
import net.serenitybdd.screenplay.targets.Target;
import org.hamcrest.Matcher;

import java.time.Duration;

import static net.serenitybdd.screenplay.EventualConsequence.eventually;
import static net.serenitybdd.screenplay.GivenWhenThen.seeThat;
import static net.serenitybdd.screenplay.questions.WebElementQuestion.the;

public class WaitUntilTargetIsReady implements Interaction, IsSilent {
    private final Target target;
    private final Matcher<WebElementState> expectedState;

    public WaitUntilTargetIsReady(Target target, Matcher<WebElementState> expectedState) {
        this.target = target;
        this.expectedState = expectedState;
    }

    @Override
    public <A extends Actor> void performAs(A actor) {
        actor.should(eventually(seeThat(WebElementQuestion.the(target), expectedState)).withNoReporting());
    }

    public WaitUntilBuilder forNoMoreThan(long amount) {
        return new WaitUntilBuilder(amount, target, expectedState);
    }

    public Interaction forNoMoreThan(Duration amount) {
        return new SilentInteraction() {
            @Override
            public <T extends Actor> void performAs(T actor) {
                actor.should(eventually(seeThat(the(target), expectedState))
                        .withNoReporting()
                        .waitingForNoLongerThan(amount.toMillis()).milliseconds());
            }
        };
    }

}
