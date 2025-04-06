import java.util.LinkedList;
import java.util.Queue;

/**
 * This class implements the Hilbert Sort algorithm to sort a list of records. It keeps track of the sorted list and the
 * unsorted list. It also keeps track of the size of the square.
 */
public class HilbertSort {
    //keep track of sorted list
    Queue<Record> sortedList;
    Queue<Record> unsortedList;
    //size of square
    double S;

    /**
     * This is the constructor for the HilbertSort class. It takes a Queue of Record objects (the list to be sorted) and
     * the size of the square. It initializes the unsortedList and S with the input parameters. It also initializes the
     * sortedList as a new LinkedList.
     *
     * @param List        the list of records to be sorted
     * @param sizeOfSpace the size of the square
     */
    public HilbertSort(Queue<Record> List, double sizeOfSpace) {
        this.unsortedList = List;
        S = sizeOfSpace;
        this.sortedList = new LinkedList();
    }

    /**
     * This method calls the hilbertSort method to sort the records. It returns the sorted list of records.
     *
     * @return the sorted list of records
     */
    public Queue<Record> hilbertHelper() {
        hilbertSort(S, unsortedList);
        return sortedList;
    }

    /**
     * This method recursively divides the square into quadrants to sort the records. It takes the size of the square and
     * the list of records to be sorted as parameters. It creates four new queues for the four quadrants. It then iterates
     * over the unsorted list and places each record into the corresponding quadrant. It then checks each quadrant. If a
     * quadrant has only one record, it adds that record to the sorted list. If a quadrant has more than one record, it
     * further divides that quadrant until only one record is left in each quadrant. Finally, it adds the records to the
     * sortedList based on the order of the quadrants.
     *
     * @param S          the size of the square
     * @param listToSort the list of records to be sorted
     */
    private void hilbertSort(double S, Queue<Record> listToSort) {
        //put all unsorted location into proper quadrant
        Queue<Record> quadrant1 = new LinkedList<Record>();
        Queue<Record> quadrant2 = new LinkedList<Record>();
        Queue<Record> quadrant3 = new LinkedList<Record>();
        Queue<Record> quadrant4 = new LinkedList<Record>();
        while (!listToSort.isEmpty()) {
            Record item = listToSort.remove();
            //check the x,y values of each location and placed it into corresponding quadrant
            if (item.getLAT() >= 0 && item.getLAT() <= S / 2 && 0 <= item.getLON() && item.getLON() <= S / 2) {
                quadrant1.add(item);
            } else if (item.getLAT() >= 0 && item.getLAT() <= S / 2 && S / 2 <= item.getLON() && item.getLON() <= S) {
                quadrant2.add(item);
            } else if (item.getLAT() >= S / 2 && item.getLAT() <= S && S / 2 <= item.getLON() && item.getLON() <= S) {
                quadrant3.add(item);
            } else if (item.getLAT() >= S / 2 && item.getLAT() <= S && 0 <= item.getLON() && item.getLON() <= S / 2) {
                quadrant4.add(item);
            }
        }
        //visit the quadrant by order to check if there is only one item in the quadrant
        // if true add that item to sorted list
        //otherwise further divide that quadrant till there is only one item in the quadrant
        //put items to sortedList based on 1,2,3,4 quadrant order
        if (quadrant1.size() > 0) {
            if (quadrant1.size() == 1) {
                sortedList.add(quadrant1.remove());
            } else {
                //iterates through elements to change x, y by rotation;
                for (Record item : quadrant1) {
                    double temp = item.getLAT();
                    item.setLAT(item.getLON());
                    item.setLON(temp);
                }
                hilbertSort(S / 2, quadrant1);
            }
        }
        if (quadrant2.size() > 0) {
            if (quadrant2.size() == 1) {
                sortedList.add(quadrant2.remove());
            } else {
                for (Record item : quadrant2) {
                    item.setLON(item.getLON() - S / 2);
                }
                hilbertSort(S / 2, quadrant2);
            }
        }
        if (quadrant3.size() > 0) {
            if (quadrant3.size() == 1) {
                sortedList.add(quadrant3.remove());
            } else {
                for (Record item : quadrant3) {
                    item.setLAT(item.getLAT() - S / 2);
                    item.setLON(item.getLON() - S / 2);
                }
                hilbertSort(S / 2, quadrant3);
            }
        }
        if (quadrant4.size() > 0) {
            if (quadrant4.size() == 1) {
                sortedList.add(quadrant4.remove());
            } else {
                //iterates through and change x,y by rotation
                for (Record item : quadrant4) {
                    double temp = item.getLON();
                    item.setLON(S - item.getLAT());
                    item.setLAT(S / 2 - temp);
                }
                hilbertSort(S / 2, quadrant4);
            }
        }
    }

}