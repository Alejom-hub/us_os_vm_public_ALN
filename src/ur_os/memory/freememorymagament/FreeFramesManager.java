
package ur_os.memory.freememorymagament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import ur_os.memory.paging.PMM_Paging;
import ur_os.memory.paging.PageTableEntry;
import ur_os.process.Process;
import ur_os.system.OS;
import ur_os.system.SystemOS;

/**
 * Administra los marcos de memoria fisica que estan libres.
 */
public class FreeFramesManager extends FreeMemoryManager {

    LinkedList<Integer> freeFrames;

    public FreeFramesManager() {
        this(SystemOS.MEMORY_SIZE);
    }

    public FreeFramesManager(int size) {
        freeFrames = new LinkedList<>();
        int numFrames = size / OS.PAGE_SIZE;

        for (int i = 0; i < numFrames; i++) {
            freeFrames.add(i);
        }
    }

    /**
     * Obtiene un marco libre.
     * Retorna -1 si no hay marcos disponibles.
     */
    public int getFrame() {
        if (freeFrames.isEmpty()) {
            return -1;
        }

        return freeFrames.pop();
    }

    /**
     * Agrega un marco a la lista de marcos libres.
     */
    public void addFrame(int frame) {
        reclaimFrame(frame);
    }

    public int getSize() {
        return freeFrames.size();
    }

    /**
     * Devuelve un marco a la lista de marcos libres.
     * Evita agregar marcos duplicados.
     */
    public void reclaimFrame(int frameID) {
        int totalFrames = SystemOS.MEMORY_SIZE / OS.PAGE_SIZE;

        if (frameID < 0 || frameID >= totalFrames) {
            System.out.println("Error: numero de marco invalido: " + frameID);
            return;
        }

        if (!freeFrames.contains(frameID)) {
            freeFrames.add(frameID);
            Collections.sort(freeFrames);
        }
    }

    @Override
    public void reclaimMemory(Process p) {
        if (p == null || !(p.getPMM() instanceof PMM_Paging)) {
            return;
        }

        PMM_Paging pmm = (PMM_Paging) p.getPMM();
        ArrayList<PageTableEntry> pteList = pmm.getPT().getList();

        for (PageTableEntry pte : pteList) {
            if (pte.isValid() && pte.getFrameId() >= 0) {
                reclaimFrame(pte.getFrameId());
                pte.setValid(false);
            }
        }

        Collections.sort(freeFrames);
    }
}
