package trashsoftware.solitaire.util;

import java.util.*;

public class SortedList<T> implements Iterable<T> {

    private final LinkedList<T> list;
    private final Comparator<T> comparator;

    public SortedList(Comparator<T> comparator) {
        this(comparator, List.of());
    }

    public SortedList(Comparator<T> comparator, List<T> initialElements) {
        this.comparator = comparator;
        this.list = new LinkedList<>(initialElements);
        list.sort(comparator);
    }

    public static void main(String[] args) {
        SortedList<Integer> sl =
                new SortedList<>(Integer::compare,
                List.of(8, 78, -12, 432, 55, 76, 13, 98, 44, 131, 35, 72, 11, 33, 556, 92, 17));
        System.out.println(sl);
        System.out.println(sl.insert(33));
        System.out.println(sl);
    }

    public T getFirst() {
        return list.isEmpty() ? null : list.getFirst();
    }

    /**
     * Inserts an element and returns the index of the newly inserted element in this list.
     *
     * @param element the element to be inserted
     * @return the index of the newly inserted element in this list
     */
    public int insert(T element) {
        int index = 0;
        ListIterator<T> iterator = list.listIterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (comparator.compare(element, next) < 0) {
                iterator.previous();
                iterator.add(element);
                return index;
            }
            index++;
        }
        list.addLast(element);
        return size() - 1;
    }

    public int rank(T element) {
        int index = 0;
        for (T next : list) {
            if (comparator.compare(element, next) < 0) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public int size() {
        return list.size();
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }
}
