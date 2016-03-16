package ru.azaz.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by AZAZ on 16.03.2016.
 */
public class Intersection {
    public static<T> Collection<T> intersect(Collection<? extends T>a,Collection<? extends T>b){
        return a.stream().filter(o1 -> b.contains(o1)).collect(Collectors.toSet());
    }
}
