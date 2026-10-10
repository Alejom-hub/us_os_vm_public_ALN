/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.memory.freememorymagament;

import ur_os.memory.segmentation.SegmentTableEntry;
import ur_os.memory.segmentation.PMM_Segmentation;
import ur_os.memory.contiguous.SMM_Contiguous;
import java.util.ArrayList;
import java.util.LinkedList;
import ur_os.process.Process;
import ur_os.memory.ProcessMemoryManager;
import static ur_os.memory.MemoryManagerType.CONTIGUOUS;
import static ur_os.memory.MemoryManagerType.SEGMENTATION;
import ur_os.memory.contiguous.PMM_Contiguous;

/**
 *
 * @author super
 */
public abstract class FreeMemorySlotManager extends FreeMemoryManager{
    
    LinkedList<MemorySlot> list;
    
    public FreeMemorySlotManager(){
        this(ur_os.system.SystemOS.MEMORY_SIZE);
    }
    
    public FreeMemorySlotManager(int memSize){
        list = new LinkedList();
        list.add(new MemorySlot(0,memSize));
    }
    
    public abstract MemorySlot getSlot(int size);
    
    public void fuseSlots(){
        int tam = list.size();
        for (int i = 0; i < tam-1; i++) {
            if(list.get(i).getEnd()+1 == list.get(i+1).getBase()){
                list.get(i).addSlot(list.get(i+1));
                list.remove(list.get(i+1));
                tam--;
                i--;
            }
        }
        for (int i = 0; i < tam; i++) {
            if(list.get(i).getSize() == 0){
                list.remove(i);
                tam--;
                i--;
            }
        }
        
    }
    
    private void returnMemorySlot(MemorySlot m){

        //A process that never got memory has nothing to return
        if(m == null){
            return;
        }

        int i = 0;
        //Find the first slot with a higher base address than the one inserted
        while(i<list.size() && list.get(i).getBase() < m.getBase()){
            i++;
        }

        /*The returned slot is inserted in order (by base address), as a copy,
          so the process' slot is not modified. fuseSlots() only joins it with
          its neighbors if they are really contiguous; otherwise it stays as a
          separate hole (that is the external fragmentation).
        */
        list.add(i, new MemorySlot(m));

        fuseSlots();

    }

    /*
    ******Fragmentation calculation*********
    */

    //Total free memory: the sum of the sizes of all the holes
    public int getTotalFreeMemory(){
        int total = 0;
        for (MemorySlot memorySlot : list) {
            total += memorySlot.getSize();
        }
        return total;
    }

    //Size of the biggest hole: the largest request that can be satisfied right now
    public int getLargestFreeSlot(){
        int largest = 0;
        for (MemorySlot memorySlot : list) {
            if(memorySlot.getSize() > largest){
                largest = memorySlot.getSize();
            }
        }
        return largest;
    }

    /*External fragmentation: fraction of the free memory that is NOT in the
      largest hole, so it cannot be used for a request of that size.
        - 0.0 -> all the free memory is in one contiguous block
        - close to 1.0 -> the free memory is split in many small holes
    */
    public double getExternalFragmentation(){
        int total = getTotalFreeMemory();
        if(total == 0){
            return 0;
        }
        return 1 - (double)getLargestFreeSlot()/total;
    }

    /*External fragmentation as the professor defines it: there is enough
      total free memory to satisfy the request, but it is not contiguous.
    */
    public boolean isExternalFragmentationFor(int request){
        return getLargestFreeSlot() < request && getTotalFreeMemory() >= request;
    }

    @Override
    public void reclaimMemory(Process p){
        ProcessMemoryManager pmm = p.getPMM();
        switch (pmm.getType()) {
            case SEGMENTATION:
                PMM_Segmentation pmms = (PMM_Segmentation)p.getPMM();
                ArrayList<SegmentTableEntry> list = pmms.getSt().getTable();
                for (SegmentTableEntry ste : list) {
                    this.returnMemorySlot(ste.getMemorySlot());
                }
                
                break;
            default:
            case CONTIGUOUS:
                PMM_Contiguous pmmc = (PMM_Contiguous)p.getPMM();
                this.returnMemorySlot(pmmc.getMemorySlot());
                pmmc.setValid(false);
                break;
        }
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (MemorySlot memorySlot : list) {
            sb.append(memorySlot.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public int getSize() {
        return this.list.size();
    }
    
}
