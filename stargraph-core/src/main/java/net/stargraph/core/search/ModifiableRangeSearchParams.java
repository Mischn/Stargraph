package net.stargraph.core.search;

import net.stargraph.model.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModifiableRangeSearchParams {
    private boolean incomingEdges;
    private boolean outgoingEdges;
    private List<SearchQueryGenerator.PropertyType> propertyTypes;
    private List<PruningStrategy> pruningStrategies;
    private boolean limitToRepresentatives; // This will drastically limit the search space for some linked data graphs

    private ModifiableRangeSearchParams() {
        // defaults
        this.incomingEdges = true;
        this.outgoingEdges = true;
        this.propertyTypes = Arrays.asList(SearchQueryGenerator.PropertyType.TYPE, SearchQueryGenerator.PropertyType.NON_TYPE);
        this.pruningStrategies = Arrays.asList();
        this.limitToRepresentatives = false;
    }

    public final ModifiableRangeSearchParams incomingEdges(boolean incomingEdges) {
        this.incomingEdges = incomingEdges;
        return this;
    }

    public final ModifiableRangeSearchParams outgoingEdges(boolean outgoingEdges) {
        this.outgoingEdges = outgoingEdges;
        return this;
    }

    public final ModifiableRangeSearchParams propertyTypes(List<SearchQueryGenerator.PropertyType> propertyTypes) {
        this.propertyTypes = propertyTypes;
        return this;
    }

    public final ModifiableRangeSearchParams pruningStrategies(List<PruningStrategy> pruningStrategies) {
        this.pruningStrategies = pruningStrategies;
        return this;
    }

    public final ModifiableRangeSearchParams limitToRepresentatives(boolean limitToRepresentatives) {
        this.limitToRepresentatives = limitToRepresentatives;
        return this;
    }

    public boolean isIncomingEdges() {
        return incomingEdges;
    }

    public boolean isOutgoingEdges() {
        return outgoingEdges;
    }

    public List<SearchQueryGenerator.PropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    public List<PruningStrategy> getPruningStrategies() {
        return pruningStrategies;
    }

    public boolean isLimitToRepresentatives() {
        return limitToRepresentatives;
    }


    public static ModifiableRangeSearchParams create() {
        return new ModifiableRangeSearchParams();
    }

    public ModifiableRangeSearchParams clone() {
        ModifiableRangeSearchParams res = new ModifiableRangeSearchParams();
        res.incomingEdges = incomingEdges;
        res.outgoingEdges = outgoingEdges;
        res.propertyTypes = new ArrayList<>(propertyTypes);
        res.pruningStrategies = new ArrayList<>(pruningStrategies);
        res.limitToRepresentatives = limitToRepresentatives;
        return res;
    }


    // PRUNING STRATEGIES

    public interface PruningStrategy {
        class Result {
            private List<Route> rejectRoutes;
            private List<Route> notTraverseRoutes;
            private ModifiableRangeSearchParams nextRangeSearchParams;

            public Result(List<Route> rejectRoutes, List<Route> notTraverseRoutes, ModifiableRangeSearchParams nextRangeSearchParams) {
                this.rejectRoutes = rejectRoutes;
                this.notTraverseRoutes = notTraverseRoutes;
                this.nextRangeSearchParams = nextRangeSearchParams;
            }

            public List<Route> getRejectRoutes() {
                return rejectRoutes;
            }

            public List<Route> getNotTraverseRoutes() {
                return notTraverseRoutes;
            }

            public ModifiableRangeSearchParams getNextRangeSearchParams() {
                return nextRangeSearchParams;
            }
        }

        Result prune(List<Route> newRoutes, ModifiableRangeSearchParams rangeSearchParams);
    }


    /**
     * Doesn't allow links back to any previously visited waypoints (=cycles)
     */
    public static class NoCyclesPruning implements PruningStrategy {

        @Override
        public Result prune(List<Route> newRoutes, ModifiableRangeSearchParams rangeSearchParams) {
            List<Route> rejectRoutes = new ArrayList<>();
            List<Route> notTraverseRoutes = new ArrayList<>();

            for (Route route : newRoutes) {
                if (route.getWaypoints().size() > 1 && route.getWaypoints().subList(0, route.getWaypoints().size()-1).contains(route.getLastWaypoint())) {
                    rejectRoutes.add(route);
                    notTraverseRoutes.add(route);
                }
            }

            return new Result(rejectRoutes, notTraverseRoutes, rangeSearchParams.clone());
        }
    }

    /**
     * Will disable to use incoming edges after range 1
     */
    public static class EdgeDirectionPruning implements PruningStrategy {

        @Override
        public Result prune(List<Route> newRoutes, ModifiableRangeSearchParams rangeSearchParams) {
            boolean incomingEdges = rangeSearchParams.isIncomingEdges();
            if (newRoutes.size() > 0 && newRoutes.get(0).getWaypoints().size() >= 1) {
                incomingEdges = false;
            }

            return new Result(new ArrayList<>(), new ArrayList<>(), rangeSearchParams.clone().incomingEdges(incomingEdges));
        }
    }

}
