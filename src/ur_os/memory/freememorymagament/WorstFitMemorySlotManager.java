/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.memory.freememorymagament;

/**
 *
 * @author super
 */
public class WorstFitMemorySlotManager extends FreeMemorySlotManager{

    public WorstFitMemorySlotManager(int memSize){
        super(memSize);
    }

    @Override
    public MemorySlot getSlot(int size) {
        /*Worst Fit: find the largest slot that can contain the request, that
          is, the one with the largest remainder. The idea is that the leftover
          stays big enough for future requests - the opposite strategy to
          Best Fit. The whole list must be traversed to find it.
        */
        MemorySlot worst = null;

        for (MemorySlot slot : list) {
            if (slot.canContain(size)) {
                //Keep track of the candidate with the largest remainder.
                //Strict > keeps the first one (lowest base) in case of a tie.
                if (worst == null || slot.getRemainder(size) > worst.getRemainder(size)) {
                    worst = slot;
                }
            }
        }

        //If there is no slot big enough to contain the requested memory, it will return null
        if (worst == null) {
            System.out.println("Error - Memory cannot allocate a slot big enough for the requested memory");
            return null;
        }

        /*If the requested amount is the slot's size, then the slot
          is removed from the list, and the original one is sent to
          the process
        */
        if (worst.getSize() == size) {
            list.remove(worst);
            return worst;
        }

        /*If the requested amount is not the slot's size, then a new memory
          slot is created from the front of the chosen slot, and assignMemory()
          updates the existing one (base moves up, size shrinks).
        */
        return worst.assignMemory(size);
    }

}
