
package ur_os.memory.paging;

import ur_os.memory.MemoryAddress;
import ur_os.memory.ProcessMemoryManager;
import ur_os.memory.MemoryManagerType;
import ur_os.system.OS;

public class PMM_Paging extends ProcessMemoryManager {

    PageTable pt;
    PageTable vpt;

    int assignedPages;
    int loadedPages;

    public PMM_Paging(int processSize, int assignedPages) {
        this(null, processSize, assignedPages);
    }

    public PMM_Paging(
            ur_os.process.Process p,
            int processSize,
            int assignedPages) {

        super(p, MemoryManagerType.PAGING, processSize);

        pt = new PageTable(processSize, assignedPages, true);
        vpt = new PageTable(processSize, assignedPages, true);

        this.assignedPages = assignedPages;
        this.loadedPages = 0;
    }

    public PMM_Paging(int processSize) {
        this(processSize, 3);
    }

    public PMM_Paging(PMM_Paging pmm) {
        super(pmm);

        pt = new PageTable(pmm.pt);
        vpt = new PageTable(pmm.vpt);

        assignedPages = pmm.assignedPages;
        loadedPages = pmm.loadedPages;
    }

    public int getAssignedPages() {
        return assignedPages;
    }

    public void setAssignedPages(int assignedPages) {
        this.assignedPages = assignedPages;
    }

    public int getLoadedPages() {
        return loadedPages;
    }

    public void setLoadedPages(int loadedPages) {
        this.loadedPages = loadedPages;
    }

    public PageTable getVPT() {
        return vpt;
    }

    public PageTable getPT() {
        return pt;
    }

    public void addFrameID(int frame) {
        pt.addFrameID(frame);
    }

    public void addFrameID(int frame, boolean valid) {
        pt.addFrameID(frame, valid);
        if (valid) {
            loadedPages++;
        }
    }

    public void addVFrameID(int frame) {
        vpt.addFrameID(frame);
    }

    public void addVFrameID(int frame, boolean valid) {
        vpt.addFrameID(frame, valid);
    }

    public void setFrameID(int page, int frame) {

        // Revisar el estado anterior antes de actualizar la tabla.
        boolean wasValid = pt.isPageValid(page);

        // Actualizar la tabla de paginas.
        // PageTable puede marcar la entrada como valida aqui.
        pt.setFrameID(page, frame);

        // Contar la pagina solo si antes no estaba residente.
        if (!wasValid) {
            loadedPages++;
        }

        // Una pagina recien cargada se considera limpia.
        pt.setPageDirty(page, false);
    }

    public MemoryAddress getPageMemoryAddressFromLocalAddress(
            int locAdd) {

        if (locAdd < 0 || locAdd >= getSize()) {
            return new MemoryAddress(-1, -1);
        }

        int page = locAdd / OS.PAGE_SIZE;
        int offset = locAdd % OS.PAGE_SIZE;

        return new MemoryAddress(page, offset);
    }

    public int getFrameMemoryAddressFromLogicalMemoryAddress(int page) {
        return pt.getFrameIdFromPage(page);
    }

    public MemoryAddress getFrameMemoryAddressFromLogicalMemoryAddress(
            MemoryAddress m) {

        if (m == null) {
            return null;
        }

        int page = m.getDivision();
        int offset = m.getOffset();

        if (page < 0
                || page >= pt.getList().size()
                || offset < 0
                || offset >= OS.PAGE_SIZE) {

            return null;
        }

        int frame = pt.getFrameIdFromPage(page);

        if (frame < 0) {
            return null;
        }

        // Registrar el acceso a la pagina residente.
        addMemoryAccess(page);

        int physicalBase = frame * OS.PAGE_SIZE;

        return new MemoryAddress(physicalBase, offset);
    }

    public int getVFrameMemoryAddressFromLogicalMemoryAddress(int page) {

        MemoryAddress address =
                getVFrameMemoryAddressFromLogicalMemoryAddress(
                        new MemoryAddress(page, 0));

        return address.getDivision();
    }

    public MemoryAddress getVFrameMemoryAddressFromLogicalMemoryAddress(
            MemoryAddress m) {

        if (m == null) {
            return new MemoryAddress(-1, -1);
        }

        int page = m.getDivision();
        int offset = m.getOffset();

        if (page < 0
                || page >= vpt.getList().size()
                || offset < 0
                || offset >= OS.PAGE_SIZE) {

            return new MemoryAddress(-1, -1);
        }

        int frame = vpt.getFrameIdFromPage(page);

        if (frame < 0) {
            return new MemoryAddress(-1, -1);
        }

        return new MemoryAddress(frame, offset);
    }

    @Override
    public String toString() {
        return pt.toString();
    }

    public int getFrameInSwap(int page) {
        return vpt.getFrameIdFromPage(page);
    }

    public void setPageValid(int page, boolean valid) {

        boolean wasValid = pt.isPageValid(page);

        pt.setPageValid(page, valid);

        // Mantener el contador sincronizado con las paginas residentes.
        if (wasValid != valid) {
            if (valid) {
                loadedPages++;
            } else {
                loadedPages--;
            }
        }
    }

    public boolean isPageDirty(int page) {
        return pt.isPageDirty(page);
    }

    public void setPageDirty(int page, boolean dirty) {
        pt.setPageDirty(page, dirty);
    }

    @Override
    public int getVictim() {

        if (loadedPages > 0 && pvmm != null) {
            return pvmm.getVictim(memoryAccesses, pt);
        }

        return -1;
    }
}
