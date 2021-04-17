package trashsoftware.solitaire.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Heap<T> {
    private final List<T> heapList;
    private final Comparator<T> comparator;

    /**
     * Constructor
     *
     * @param comparator comparator, {@link Heap#Heap(Comparator, List)}
     */
    public Heap(Comparator<T> comparator) {
        this(comparator, null);
    }

    /**
     * Constructor.
     * <p>
     * To make a max heap, the comparator should return positive value if o1 < o2.
     * For instance, {@link Integer#compare(int, int)} builds a min heap on integers.
     *
     * @param comparator      the comparator.
     * @param initialElements initial elements
     */
    public Heap(Comparator<T> comparator, List<T> initialElements) {
        this.comparator = comparator;
        if (initialElements == null) this.heapList = new ArrayList<>();
        else {
            this.heapList = new ArrayList<>(initialElements);
            buildHeap();
        }
    }

    public static void main(String[] args) {
        Heap<Integer> heap = new Heap<>(
                (o1, o2) -> Integer.compare(o1, o2),
                List.of(8, 78, -12, 432, 55, 76, 13, 98, 44, 131, 35, 72, 11, 33, 556, 92, 17));
        heap.printHeap();
    }

    public int size() {
        return heapList.size();
    }

    public void insert(T element) {
        heapList.add(element);
        riseNode(heapList.size() - 1);
    }

    public T getPeek() {
        if (heapList.isEmpty()) return null;
        return heapList.get(0);
    }

    public T removePeek() {
        if (heapList.isEmpty()) return null;
        swap(0, heapList.size() - 1);
        T element = heapList.remove(heapList.size() - 1);
        if (!heapList.isEmpty()) heapify(0);
        return element;
    }

    public void printHeap() {
        int index = 0;
        int size = size();
        for (int rowLength = 1; ; rowLength *= 2) {
            for (int i = 0; i < rowLength; ++i) {
                if (index == size) return;
                System.out.print(heapList.get(index) + ", ");
                index++;
            }
            System.out.println();
        }
    }

    private void buildHeap() {
        for (int i = heapList.size() / 2 - 1; i >= 0; --i) {
            heapify(i);
        }
    }

    private void heapify(int index) {
        int leftIndex = (index + 1) * 2 - 1;
        int rightIndex = leftIndex + 1;

        T extreme = heapList.get(index);
        boolean isLeft = true;
        if (leftIndex < heapList.size()) {
            if (comparator.compare(extreme, heapList.get(leftIndex)) > 0) {
                extreme = heapList.get(leftIndex);
            }
        }
        if (rightIndex < heapList.size()) {
            if (comparator.compare(extreme, heapList.get(rightIndex)) > 0) {
                extreme = heapList.get(rightIndex);
                isLeft = false;
            }
        }
        if (comparator.compare(heapList.get(index), extreme) > 0) {
            if (isLeft) {
                swap(index, leftIndex);
                heapify(leftIndex);
            } else {
                swap(index, rightIndex);
                heapify(rightIndex);
            }
        }
    }

    private void riseNode(int index) {
        int parentIndex = (index + 1) / 2 - 1;
        if (parentIndex >= 0) {
            T parent = heapList.get(parentIndex);
            if (comparator.compare(parent, heapList.get(index)) > 0) {
                swap(index, parentIndex);
                riseNode(parentIndex);
            }
        }
    }

    private void swap(int i, int j) {
        T temp = heapList.get(i);
        heapList.set(i, heapList.get(j));
        heapList.set(j, temp);
    }
}
