package com.mesosphere.sdk.offer.evaluate;

import com.mesosphere.sdk.offer.MesosResource;
import com.mesosphere.sdk.offer.OfferRecommendation;

import java.util.*;

/**
 * The outcome of invoking an {@link OfferEvaluationStage}. Describes whether the evaluation passed or failed, and the
 * reason(s) why. Supports a nested tree of outcomes which describe any sub-evaluations which may have been performed
 * within the {@link OfferEvaluationStage}.
 */
public class EvaluationOutcome {

    /**
     * The outcome value.
     */
    private enum Type {
        PASS,
        FAIL
    }

    private final Type type;
    private final String source;
    private final MesosResource mesosResource;
    private final Collection<OfferRecommendation> offerRecommendations;
    private final Collection<EvaluationOutcome> children;
    private final String reason;

    /**
     * Returns a new passing outcome object with the provided descriptive reason.
     *
     * @param source the object which produced this outcome, whose class name will be labeled as the origin
     * @param reasonFormat {@link String#format(String, Object...)} compatible format string describing the pass reason
     * @param reasonArgs format arguments, if any, to apply against {@code reasonFormat}
     */
    public static EvaluationOutcome.Builder pass(
            Object source,
            String reasonFormat,
            Object... reasonArgs) {
        return pass(source, Collections.emptyList(), reasonFormat, reasonArgs);
    }

    /**
     * Returns a new passing outcome object with the provided descriptive reason.
     *
     * @param source the object which produced this outcome, whose class name will be labeled as the origin
     * @param offerRecommendations the offer recommendations generated by the source, if any
     * @param reasonFormat {@link String#format(String, Object...)} compatible format string describing the pass reason
     * @param reasonArgs format arguments, if any, to apply against {@code reasonFormat}
     */
    public static EvaluationOutcome.Builder pass(
            Object source,
            Collection<OfferRecommendation> offerRecommendations,
            String reasonFormat,
            Object... reasonArgs) {
        return new EvaluationOutcome.Builder(
                Type.PASS,
                source,
                offerRecommendations,
                reasonFormat,
                reasonArgs);
    }

    /**
     * Returns a new failing outcome object with the provided descriptive reason.
     *
     * @param source the object which produced this outcome, whose class name will be labeled as the origin
     * @param reasonFormat {@link String#format(String, Object...)} compatible format string describing the fail reason
     * @param reasonArgs format arguments, if any, to apply against {@code reasonFormat}
     */
    public static EvaluationOutcome.Builder fail(
            Object source,
            String reasonFormat,
            Object... reasonArgs) {
        return new EvaluationOutcome.Builder(
                Type.FAIL,
                source,
                Collections.emptyList(),
                reasonFormat,
                reasonArgs);
    }

    private EvaluationOutcome(
            Type type,
            Object source,
            MesosResource mesosResource,
            Collection<OfferRecommendation> offerRecommendations,
            Collection<EvaluationOutcome> children,
            String reason) {
        this.type = type;
        this.source = source.getClass().getSimpleName();
        this.mesosResource = mesosResource;
        this.offerRecommendations = offerRecommendations;
        this.children = children;
        this.reason = reason;
    }

    /**
     * Returns whether this outcome was passing ({@code true}) or failing ({@code false}).
     */
    public boolean isPassing() {
        return type == Type.PASS;
    }

    /**
     * Returns the name of the object which produced this response.
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the reason that this response is passing or failing.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns any nested outcomes which resulted in this decision.
     */
    public Collection<EvaluationOutcome> getChildren() {
        return children;
    }

    public Optional<MesosResource> getMesosResource() {
        return Optional.ofNullable(mesosResource);
    }

    public List<OfferRecommendation> getOfferRecommendations() {
        List<OfferRecommendation> recommendations = new ArrayList<>();
        recommendations.addAll(offerRecommendations);
        for (EvaluationOutcome outcome : getChildren()) {
            recommendations.addAll(outcome.getOfferRecommendations());
        }
        return recommendations;
    }

    @Override
    public String toString() {
        return String.format("%s(%s): %s", isPassing() ? "PASS" : "FAIL", getSource(), getReason());
    }

    /**
     * Builder for constructor {@link EvaluationOutcome} instances.
     */
    public static class Builder {
        private final Type type;
        private final Object source;
        private final Collection<OfferRecommendation> offerRecommendations;
        private final Collection<EvaluationOutcome> children;
        private final String reason;
        private MesosResource mesosResource;

        public Builder(
                Type type,
                Object source,
                Collection<OfferRecommendation> offerRecommendations,
                String reasonFormat,
                Object... reasonArgs) {
            this.type = type;
            this.source = source;
            this.offerRecommendations = offerRecommendations;
            this.children = new ArrayList<>();
            this.reason = String.format(reasonFormat, reasonArgs);
        }

        public Builder mesosResource(MesosResource mesosResource) {
            this.mesosResource = mesosResource;
            return this;
        }

        public Builder addChild(EvaluationOutcome child) {
            children.add(child);
            return this;
        }

        public Builder addAllChildren(Collection<EvaluationOutcome> children) {
            this.children.addAll(children);
            return this;
        }

        public EvaluationOutcome build() {
            return new EvaluationOutcome(type, source, mesosResource, offerRecommendations, children, reason);
        }
    }
}
