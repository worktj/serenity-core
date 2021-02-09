package net.serenitybdd.screenplay.ensure

import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Performable
import net.thucydides.core.annotations.Step

open class PerformableExpectation<A, E>(private val actual: A?,
                                        private val expectation: Expectation<A?, E>,
                                        private val expected: E?,
                                        private val isNegated: Boolean = false,
                                        private val expectedDescription: String = "a value") : Performable {

    private val description = expectation.describe(expected, isNegated, expectedDescription);

    @Step("{0} should see #description")
    override fun <T : Actor?> performAs(actor: T) {
        BlackBox.reset()
        val result = expectation.apply(actual, expected!!, actor)

        if (isAFailure(result, isNegated)) {
            throw AssertionError(expectation.compareActualWithExpected(actual, expected, isNegated, expectedDescription))
        }
    }

    /**
     * Internal use only - DO NOT USE
     */
    protected constructor() : this(null,
            Expectation<A?, E>(
                    "placeholder",
                    "placeholder",
                    fun(_: Actor?, _: A?, _: E): Boolean = true
            ),
            null) {
    }
}

open class BiPerformableExpectation<A, E>(private val actual: A?,
                                          private val expectation: DoubleValueExpectation<A?, E>,
                                          private val startRange: E?,
                                          private val endRange: E?,
                                          private val isNegated: Boolean = false,
                                          private val expectedDescription: String) : Performable {

    private val description = expectation.describeRange(startRange, endRange, isNegated, expectedDescription);

    @Step("{0} should see #description")
    override fun <T : Actor?> performAs(actor: T) {
        BlackBox.reset()
        val result = expectation.apply(actual, startRange, endRange, actor)

        if (isAFailure(result, isNegated)) {
            throw AssertionError(expectation.compareActualWithExpected(actual, startRange, endRange, isNegated, expectedDescription))
        }
    }

    /**
     * Internal use only - DO NOT USE
     */
    protected constructor() : this(null,
            DoubleValueExpectation<A?, E>(
                    "placeholder",
                    fun(_: Actor?, _: A?, _: E, _: E): Boolean = true
            ),
            null,
            null,
            false,
            "") {
    }
}

open class PerformablePredicate<A>(private val actual: A?,
                                   private val expectation: PredicateExpectation<A?>,
                                   private val isNegated: Boolean = false,
                                   private val expectedDescription: String,
                                   private val exception: Throwable? = null) : Performable {

    private val description = expectation.describe(isNegated, expectedDescription);

    @Step("{0} should see #description")
    override fun <T : Actor?> performAs(actor: T) {
        BlackBox.reset()
        val result = expectation.apply(actual, actor)

        if (isAFailure(result, isNegated)) {
            if (exception != null) {
                throw exception
            } else {
                throw AssertionError(expectation.compareActualWithExpected(actual, isNegated, expectedDescription))
            }
        }
    }

    fun orElseThrow(exception: Throwable): PerformablePredicate<A> {
        return PerformablePredicate(actual, expectation, isNegated, expectedDescription, exception)
    }

    /**
     * Internal use only - DO NOT USE
     */
    protected constructor() : this(null,
            PredicateExpectation<A?>(
                    "placeholder", "placeholder",
                    fun(_: Actor?, _: A?): Boolean = true
            ),
            false,
            "placeholder") {
    }
}

private fun isAFailure(result: Boolean, isNegated: Boolean) = (!isNegated && !result || isNegated && result)