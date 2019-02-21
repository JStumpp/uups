package com.github.jstumpp.uups;

import org.hamcrest.TypeSafeMatcher;

public class ExceptionMatcher extends TypeSafeMatcher<Exception> {

    private String actual;
    private Class expected;

    private ExceptionMatcher(Class expected) {
        this.expected = expected;
    }

    public static ExceptionMatcher assertException(Class expected) {
        return new ExceptionMatcher(expected);
    }

    @Override
    protected boolean matchesSafely(Exception exception) {
        actual = exception.toString();
        return actual.equals(expected);
    }

    @Override
    public void describeTo(org.hamcrest.Description description) {
        description.appendText("Actual =").appendValue(actual)
                .appendText(" Expected =").appendValue(
                expected);
    }
}