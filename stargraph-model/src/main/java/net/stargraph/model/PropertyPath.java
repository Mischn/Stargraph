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
import net.stargraph.rank.Rankable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyPath implements Hashable, Rankable {
    public enum Direction {
        OUTGOING,
        INCOMING
    }

    private List<PropertyEntity> properties;
    private List<Direction> directions;

    public PropertyPath(List<PropertyEntity> properties, List<Direction> directions) {
        if (properties.size() != directions.size()) {
            throw new AssertionError("Properties and directions should have the same size");
        }
        this.properties = properties;
        this.directions = directions;
    }

    public PropertyPath(PropertyEntity property, Direction direction) {
        this(Arrays.asList(property), Arrays.asList(direction));
    }

    public PropertyPath extend(PropertyEntity property, Direction direction) {
        List<PropertyEntity> newProperties = new ArrayList<>(properties);
        List<Direction> newDirections = new ArrayList<>(directions);
        newProperties.add(property);
        newDirections.add(direction);
        return new PropertyPath(newProperties, newDirections);
    }

    public List<PropertyEntity> getProperties() {
        return properties;
    }

    public List<Direction> getDirections() {
        return directions;
    }

    public PropertyEntity getLastProperty() {
        return properties.get(properties.size()-1);
    }

    public Direction getLastDirection() {
        return directions.get(directions.size()-1);
    }

    @Override
    public String getRankableValue() {
        //TODO redefine interface of Rankable to compare multiple strings?
        return properties.stream().map(p -> p.getRankableValue()).collect(Collectors.joining(" "));
    }

    @Override
    public String getId() {
        StringJoiner sj = new StringJoiner(" ");
        for (int i = 0; i < properties.size(); i++) {
            String part = String.format("%s%s", (directions.get(i).equals(Direction.OUTGOING))? ">>" : "<<", properties.get(i).getId());
            sj.add(part);
        }
        return sj.toString();
    }


    public static class PropertyParse {
        public String propertyId;
        public Direction direction;

        public PropertyParse(String propertyId, Direction direction) {
            this.propertyId = propertyId;
            this.direction = direction;
        }
    }
    public static List<PropertyParse> parseId(String id) {
        String[] parts = id.split("\\s+");
        return Stream.of(parts)
                .map(s -> new PropertyParse(
                        s.substring(2),
                        (s.startsWith(">>"))? Direction.OUTGOING : Direction.INCOMING)
                ).collect(Collectors.toList());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyPath that = (PropertyPath) o;

        return getId() != null ? getId().equals(that.getId()) : that.getId() == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "PropertyPath{" +
                "id='" + getId() + '\'' +
                '}';
    }
}
