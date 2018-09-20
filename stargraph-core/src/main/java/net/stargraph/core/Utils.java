package net.stargraph.core;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    // [["a"], ["b"]] * ["x", "y"] => [["a", "x"], ["a", "y"], ["b", "x"], ["b", "y"]]
    public static <T> List<List<T>> cartesianProduct(List<List<T>> x, List<T> y) {
        List<List<T>> res = new ArrayList<>();

        for (List<T> xs : x) {
            for (T e : y) {
                List<T> es = new ArrayList<>();
                es.addAll(xs);
                es.add(e);
                res.add(es);
            }
        }

        return res;
    }
}
