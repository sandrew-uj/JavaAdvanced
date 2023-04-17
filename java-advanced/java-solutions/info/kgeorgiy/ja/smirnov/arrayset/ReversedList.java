package info.kgeorgiy.ja.smirnov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedList<E> extends AbstractList<E> {

    List<E> data;
    boolean isReversed;

    public ReversedList(List<E> data) {
        this.data = data;
        this.isReversed = true;
        if (data instanceof ReversedList<E> reversedData) {
            this.isReversed = !reversedData.isReversed;
            this.data = reversedData.data;
        }
    }

    @Override
    public E get(int index) {
        return data.get(isReversed ? size() - index - 1 : index);
    }

    @Override
    public int size() {
        return data.size();
    }
}
