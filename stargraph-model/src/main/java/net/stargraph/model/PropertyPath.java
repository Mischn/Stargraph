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

import net.stargraph.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyPath extends Entity {
    public enum Direction {
        OUTGOING,
        INCOMING
    }

    private List<PropertyEntity> properties;
    private List<Direction> directions;



    private static String constructId(List<PropertyEntity> properties, List<Direction> directions) {
        StringJoiner sj = new StringJoiner(" | ");
        for (int i = 0; i < properties.size(); i++) {
            String part = String.format("%s%s", (directions.get(i).equals(Direction.INCOMING))? "^" : "", properties.get(i).getId());
            sj.add(part);
        }
        return sj.toString();
    }

    private static String constructValue(List<PropertyEntity> properties, List<Direction> directions) {
        StringJoiner sj = new StringJoiner(" | ");
        for (int i = 0; i < properties.size(); i++) {
            String part = String.format("%s%s", (directions.get(i).equals(Direction.INCOMING))? "^" : "", properties.get(i).getValue());
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
        String[] parts = id.split("\\s\\|\\s");
        return Stream.of(parts)
                .map(s -> new PropertyParse(
                        (s.startsWith("^"))? s.substring(1) : s,
                        (s.startsWith("^"))? Direction.INCOMING : Direction.OUTGOING
                )).collect(Collectors.toList());
    }



    public PropertyPath(List<PropertyEntity> properties, List<Direction> directions) {
        super(constructId(properties, directions));
        if (properties.size() != directions.size()) {
            throw new AssertionError("Properties and directions should have the same size");
        }
        this.properties = Objects.requireNonNull(properties);
        this.directions = Objects.requireNonNull(directions);
    }

    public PropertyPath(PropertyEntity property, Direction direction) {
        this(Arrays.asList(Objects.requireNonNull(property)), Arrays.asList(Objects.requireNonNull(direction)));
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
    public String getValue() {
        return constructValue(properties, directions);
    }

    @Override
    public List<List<String>> getRankableValues() {

        // uses cartesian product of rankable values of properties
        List<List<String>> res = null;
        for (PropertyEntity property : properties) {
            if (res == null) {
                res = property.getRankableValues().stream().map(v -> Arrays.asList(v.get(0))).collect(Collectors.toList());
            } else {
                res = Utils.cartesianProduct(res, property.getRankableValues().stream().map(v -> v.get(0)).collect(Collectors.toList()));
            }
        }
        return res;
    }

//    @Override
//    public String toString() {
//        return "PropertyPath{" +
//                "id='" + getId() + '\'' +
//                '}';
//    }
}
