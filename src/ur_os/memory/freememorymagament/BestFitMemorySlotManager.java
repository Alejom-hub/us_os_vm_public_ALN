/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.memory.freememorymagament;

/**
 *
 * @author super
 */
public class BestFitMemorySlotManager extends FreeMemorySlotManager{

    public BestFitMemorySlotManager(int memSize){
        super(memSize);
    }

    @Override
    public MemorySlot getSlot(int size) {
        /*Best Fit: find the smallest slot that can still contain the request,
          that is, the one with the smallest remainder. It keeps the big slots
          for future requests, but may produce many tiny unusable holes
          (external fragmentation).
        */
        MemorySlot best = null;

        for (MemorySlot slot : list) {
            if (slot.canContain(size)) {
                /*If the requested amount is the slot's size, then the slot
                  is removed from the list, and the original one is sent to
                  the process. A perfect fit cannot be improved, so the search
                  stops immediately.
                */
                if (slot.getSize() == size) {
                    list.remove(slot);
                    return slot;
                }
                //Keep track of the candidate with the smallest remainder.
                //Strict < keeps the first one (lowest base) in case of a tie.
                if (best == null || slot.getRemainder(size) < best.getRemainder(size)) {
                    best = slot;
                }
            }
        }

        //If there is no slot big enough to contain the requested memory, it will return null
        if (best == null) {
            System.out.println("Error - Memory cannot allocate a slot big enough for the requested memory");
            return null;
        }

        /*If the requested amount is not the slot's size, then a new memory
          slot is created from the front of the chosen slot, and assignMemory()
          updates the existing one (base moves up, size shrinks).
        */
        return best.assignMemory(size);
    }

}
