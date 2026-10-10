/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.memory.freememorymagament;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author super
 */
public class MedianFitMemorySlotManager extends FreeMemorySlotManager{

    public MedianFitMemorySlotManager(int memSize){
        super(memSize);
    }

    @Override
    public MemorySlot getSlot(int size) {
        /*Median Fit: a balance between Best Fit and Worst Fit. Instead of the
          smallest remainder (Best Fit) or the largest one (Worst Fit), it
          chooses the slot whose remainder is the closest to the MEDIAN of all
          the possible remainders. It avoids leaving tiny unusable holes, and
          also avoids consuming the biggest slots.
        */

        /*Step 1 and 2: Filter the valid slots (the ones that can contain the
          request) and calculate the remainder that each one would leave.
          Both lists keep the original order of the free slot list, so
          candidates.get(i) leaves remainders.get(i) bytes.
        */
        ArrayList<MemorySlot> candidates = new ArrayList<>();
        ArrayList<Integer> remainders = new ArrayList<>();

        for (MemorySlot slot : list) {
            if (slot.canContain(size)) {
                candidates.add(slot);
                remainders.add(slot.getRemainder(size));
            }
        }

        //If there is no slot big enough to contain the requested memory, it will return null
        if (candidates.isEmpty()) {
            System.out.println("Error - Memory cannot allocate a slot big enough for the requested memory");
            return null;
        }

        /*Step 3: Calculate the median of the remainders. A temporary copy is
          sorted, so the original order (needed for the tie rule) is kept.
          - Odd number of remainders: the median is the middle value.
          - Even number of remainders: the median is the average of the two
            middle values (it may have decimals, so a double is used).
        */
        ArrayList<Integer> sorted = new ArrayList<>(remainders);
        Collections.sort(sorted);

        int n = sorted.size();
        double median;
        if (n % 2 == 1) {
            median = sorted.get(n / 2);
        } else {
            median = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        }

        /*Step 4: Choose the slot whose remainder has the minimum absolute
          distance to the median: |remainder - median|.
          Step 5: In case of a tie, the strict < keeps the first slot found in
          the original list (lowest base address).
        */
        MemorySlot chosen = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < candidates.size(); i++) {
            double distance = Math.abs(remainders.get(i) - median);
            if (distance < minDistance) {
                minDistance = distance;
                chosen = candidates.get(i);
            }
        }

        /*If the requested amount is the slot's size, then the slot
          is removed from the list, and the original one is sent to
          the process
        */
        if (chosen.getSize() == size) {
            list.remove(chosen);
            return chosen;
        }

        /*If the requested amount is not the slot's size, then a new memory
          slot is created from the front of the chosen slot, and assignMemory()
          updates the existing one (base moves up, size shrinks).
        */
        return chosen.assignMemory(size);
    }

}
