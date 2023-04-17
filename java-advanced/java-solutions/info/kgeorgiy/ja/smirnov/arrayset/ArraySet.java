package info.kgeorgiy.ja.smirnov.arrayset;

import java.util.*;

public class ArraySet<E> implements NavigableSet<E> {

    private final List<E> data;
    private final Comparator<? super E> comparator;

    private ArraySet(List<E> data, Comparator<? super E> comparator) {
        this.data = data;
        this.comparator = nullNaturalComparator(comparator);
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends E> c, Comparator<? super E> comparator) {
        this.data = getCopiedList(c, comparator);
        this.comparator = nullNaturalComparator(comparator);
    }

    public ArraySet(Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    private Comparator<? super E> nullNaturalComparator(Comparator<? super E> comparator) {
        return comparator != Comparator.naturalOrder() ? comparator : null;
    }

    private List<E> getCopiedList(Collection<? extends E> c, Comparator<? super E> comparator) {
        var treeSet = new TreeSet<E>(comparator);
        treeSet.addAll(c);
        return List.copyOf(treeSet);
    }

    private int search(E e) {
        return Collections.binarySearch(data, e, comparator);
    }

    private E getElement(int index) {
        return index >= 0 && index < size() ? data.get(index) : null;
    }

    private int getNeighbourIndex(E e, int shift, boolean withElement) {
        int index = search(e);
        if (index >= 0) {
            return withElement ? index : index + shift;
        }
        return shift > 0 ? -index - 1 : -index - 2;
    }

    @Override
    public E lower(E e) {
        return getElement(getNeighbourIndex(e, -1, false));
    }

    @Override
    public E floor(E e) {
        return getElement(getNeighbourIndex(e, -1, true));
    }

    @Override
    public E ceiling(E e) {
        return getElement(getNeighbourIndex(e, +1, true));
    }

    @Override
    public E higher(E e) {
        return getElement(getNeighbourIndex(e, +1, false));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (E) o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return data.iterator();
    }

    @Override
    public Object[] toArray() {
        return data.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return data.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        boolean result = true;
        for (var e : c) {
            if (!contains(e)) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    private int compareElements(E a, E b) {
        return comparator == null ? ((Comparable<? super E>) a).compareTo(b)
                : comparator.compare(a, b);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compareElements(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }

        int left = getNeighbourIndex(fromElement, 1, fromInclusive);
        int right = getNeighbourIndex(toElement, -1, toInclusive);
        if (left > right) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(data.subList(left, right + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return data.isEmpty() ? this
                : subSet(compareElements(first(), toElement) < 0 ? first() : toElement, true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return data.isEmpty() ? this
                : subSet(fromElement, inclusive, compareElements(last(), fromElement) > 0 ? last() : fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(data.size() - 1);
    }
}
