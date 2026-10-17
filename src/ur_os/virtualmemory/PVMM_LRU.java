/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.virtualmemory;

import java.util.LinkedList;
import ur_os.memory.paging.PageTable;
import ur_os.memory.paging.PageTableEntry;

/**
 *
 * @author user
 */
public class PVMM_LRU extends ProcessVirtualMemoryManager {

    public PVMM_LRU() {
        type = ProcessVirtualMemoryManagerType.LRU;
    }

    @Override
    public int getVictim(
            LinkedList<Integer> memoryAccesses,
            PageTable pt) {

        LinkedList<Integer> validPages = new LinkedList<>();
        LinkedList<Integer> accessedPages = new LinkedList<>();

        // Obtener las paginas que estan cargadas en memoria.
        int pageNumber = 0;

        for (PageTableEntry pte : pt.getList()) {

            if (pte.isValid()) {
                validPages.add(pageNumber);
            }

            pageNumber++;
        }

        // Si no hay paginas validas, no existe una victima.
        if (validPages.isEmpty()) {
            return -1;
        }

        // Recorrer los accesos desde el mas reciente al mas antiguo.
        for (int i = memoryAccesses.size() - 1; i >= 0; i--) {

            int page = memoryAccesses.get(i);

            if (validPages.contains(page)
                    && !accessedPages.contains(page)) {

                accessedPages.add(page);
            }
        }

        // Considerar paginas validas que no aparecen en el historial.
        for (int page : validPages) {

            if (!accessedPages.contains(page)) {
                return page;
            }
        }

        // La ultima pagina de esta lista es la menos recientemente usada.
        if (!accessedPages.isEmpty()) {
            return accessedPages.getLast();
        }

        // Proteccion adicional: no se encontro una victima.
        return -1;
    }
}