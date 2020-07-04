package com.lampa.emotionrecognition.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SortingHelper {
    // Generic function to sort Map in descending order by values
    public static <K, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        List<Map.Entry<K, V>> mappings = new ArrayList<>(map.entrySet());
        Collections.sort(mappings, (o1, o2) ->
                o1.getValue().compareTo(o2.getValue()));

        // Create an empty LinkedHashMap that takes into account the insertion order of new elements
        Map<K, V> linkedHashMap = new LinkedHashMap<>();

        // Add entities in sorted order
        for (Map.Entry<K, V> entry : mappings) {
            linkedHashMap.put(entry.getKey(), entry.getValue());
        }

        return linkedHashMap;
    }
}
