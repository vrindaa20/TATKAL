package com.tatkalx.collections;

import com.tatkalx.model.Passenger;
import com.tatkalx.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Generic repository demonstrating:
 * - Bounded type parameter (<T extends Comparable<T>>)
 * - Diamond operator (<>)
 * - Generic methods (<E>)
 * - Upper-bounded wildcard (? extends User)
 * - Lower-bounded wildcard (? super Passenger)
 * - Collections utility methods (sort, binarySearch, frequency, unmodifiableList)
 */
public class GenericRepository<T extends Comparable<T>> {

    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        if (item != null) {
            items.add(item);
        }
    }

    public List<T> getSortedItems() {
        List<T> copy = new ArrayList<>(items);
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    /**
     * Uses Collections.binarySearch on a sorted copy of items.
     */
    public int binarySearchSorted(T key) {
        List<T> sorted = new ArrayList<>(items);
        Collections.sort(sorted);
        return Collections.binarySearch(sorted, key);
    }

    public int countOccurrences(T target) {
        return Collections.frequency(items, target);
    }

    /**
     * Generic method filtering any List<E> using a Predicate<E> functional interface.
     */
    public static <E> List<E> filterList(List<E> source, Predicate<E> condition) {
        List<E> matched = new ArrayList<>();
        for (E element : source) {
            if (condition.test(element)) {
                matched.add(element);
            }
        }
        return matched;
    }

    /**
     * Upper-bounded wildcard (? extends User): safely reads User subtypes (Passenger, Admin).
     */
    public static List<String> extractRoleDescriptions(List<? extends User> users) {
        List<String> descriptions = new ArrayList<>();
        for (User u : users) {
            descriptions.add(u.getFullName() + " -> " + u.getRoleDescription());
        }
        return descriptions;
    }

    /**
     * Lower-bounded wildcard (? super Passenger): safely writes Passenger objects into List<Passenger>,
     * List<User>, or List<Object>.
     */
    public static void addVerifiedPassengersToTarget(List<Passenger> source,
                                                     List<? super Passenger> destination) {
        for (Passenger p : source) {
            if (p.isVerified()) {
                destination.add(p);
            }
        }
    }
}
