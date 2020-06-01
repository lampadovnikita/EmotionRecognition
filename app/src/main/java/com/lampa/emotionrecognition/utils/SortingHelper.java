package com.lampa.emotionrecognition.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SortingHelper {
    // Generic функция для сортировки Map по значениям по убыванию
    public static <K, V extends Comparable> Map<K, V> sortByValues(Map<K, V> map) {
        // Создаём List, состоящий из сущностей Map b сортируем List
        List<Map.Entry<K, V>> mappings = new ArrayList<>(map.entrySet());
        Collections.sort(mappings, (o1, o2) ->
                o1.getValue().compareTo(o2.getValue()));

        // Создаём пустой LinkedHashMap, который учитывает поряд вставки новых элементов
        Map<K, V> linkedHashMap = new LinkedHashMap<>();

        // Добавляем сущности в отсортированном порядке
        for (Map.Entry<K, V> entry : mappings) {
            linkedHashMap.put(entry.getKey(), entry.getValue());
        }

        return linkedHashMap;
    }
}
