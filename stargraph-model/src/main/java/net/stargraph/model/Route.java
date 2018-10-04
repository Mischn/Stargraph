package net.stargraph.model;

/*-
 * ==========================License-Start=============================
 * stargraph-model
 * --------------------------------------------------------------------
 * Copyright (C) 2017 Lambda^3
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ==========================License-End===============================
 */

import net.stargraph.data.processor.Hashable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Route implements Hashable {
    private PropertyPath propertyPath;
    private List<NodeEntity> waypoints;

    private Route(PropertyPath propertyPath, List<NodeEntity> waypoints) {
        this.propertyPath = propertyPath;
        this.waypoints = waypoints;
    }

    public Route(NodeEntity waypoint) {
        this(null, Arrays.asList(waypoint));
    }

    public Route extend(PropertyEntity property, PropertyPath.Direction direction, NodeEntity waypoint) {
        PropertyPath newPropertyPath;
        if (this.propertyPath == null) {
            newPropertyPath = new PropertyPath(property, direction);
        } else {
            newPropertyPath = propertyPath.extend(property, direction);
        }

        List<NodeEntity> newWaypoints = new ArrayList<>(waypoints);
        newWaypoints.add(waypoint);

        return new Route(newPropertyPath, newWaypoints);
    }

    public PropertyPath getPropertyPath() {
        return propertyPath;
    }

    public List<NodeEntity> getWaypoints() {
        return waypoints;
    }

    public NodeEntity getFirstWaypoint() {
        return waypoints.get(0);
    }

    public NodeEntity getLastWaypoint() {
        return waypoints.get(waypoints.size() - 1);
    }

    @Override
    public String toString() {
        return "Route{" +
                "propertyPath=" + propertyPath +
                ", waypoints=" + waypoints +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(propertyPath, route.propertyPath) &&
                Objects.equals(waypoints, route.waypoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyPath, waypoints);
    }
}
