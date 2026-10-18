
package ur_os.memory.paging;

import ur_os.memory.MemoryAddress;
import ur_os.memory.MemoryManagerType;
import ur_os.memory.ProcessMemoryManager;
import ur_os.memory.SystemMemoryManager;
import ur_os.system.InterruptType;
import ur_os.system.OS;

public class SMM_Paging extends SystemMemoryManager {

    public SMM_Paging(OS os) {
        super(os);
        type = MemoryManagerType.PAGING;
    }

    @Override
    public int getPhysicalAddress(int logicalAddress,
                                  ProcessMemoryManager pmm,
                                  boolean store) {

        if (pmm.getType() == MemoryManagerType.PAGING) {

            PMM_Paging pmmp = (PMM_Paging) pmm;

            // Obtener el numero de pagina y el desplazamiento.
            MemoryAddress la =
                    pmmp.getPageMemoryAddressFromLocalAddress(logicalAddress);

            // Rechazar direcciones logicas invalidas.
            if (la == null
                    || la.getDivision() < 0
                    || la.getOffset() < 0) {

                System.out.println(
                        "Error: direccion logica invalida: "
                        + logicalAddress
                );

                return -1;
            }

            // Buscar la direccion fisica de la pagina.
            MemoryAddress pa =
                    pmmp.getFrameMemoryAddressFromLogicalMemoryAddress(la);

            // Si la pagina no esta en RAM, ocurre un fallo de pagina.
            if (pa == null) {

                int pageVictim = -1;
                int frameVictimInSwap = -1;

                
                int frameVictim = getOS().getFreeFrame();
                if (frameVictim < 0) {
                    pageVictim = pmmp.getVictim();

                    if (pageVictim < 0) {
                        System.out.println(
                                "Error: no hay marcos fisicos disponibles "
                                + "ni paginas para reemplazar."
                        );

                        return -1;
                    }

                    // Obtener el marco fisico de la pagina victima.
                    frameVictim =
                            pmmp.getFrameMemoryAddressFromLogicalMemoryAddress(
                                    pageVictim
                            );

                    // Obtener la ubicacion de la victima en swap.
                    frameVictimInSwap =
                            pmmp.getVFrameMemoryAddressFromLogicalMemoryAddress(
                                    pageVictim
                            );

                    if (frameVictim < 0 || frameVictimInSwap < 0) {
                        System.out.println(
                                "Error: no se pudo obtener el marco "
                                + "de la pagina victima."
                        );

                        return -1;
                    }
                }

                int pageToLoad = la.getDivision();

                // Obtener la ubicacion de la pagina solicitada en swap.
                int frameToLoadInSwap =
                        pmmp.getVFrameMemoryAddressFromLogicalMemoryAddress(
                                pageToLoad
                        );

                if (frameToLoadInSwap < 0) {
                    System.out.println(
                            "Error: no se encontro el marco de la pagina "
                            + "solicitada en swap."
                    );

                    return -1;
                }

                // Preparar la transferencia entre swap y memoria principal.
                MemoryPageExchange mpe = new MemoryPageExchange(
                        pageVictim,
                        frameVictimInSwap,
                        frameVictim,
                        pageToLoad,
                        frameToLoadInSwap
                );

                if (pageVictim != -1) {

                    // Guardar la victima en swap si fue modificada.
                    if (pmmp.isPageDirty(pageVictim)) {

                        mpe.setFullExchange(true);

                        getOS().interrupt(
                                InterruptType.STORE_PAGE,
                                pmmp.getProcess(),
                                mpe
                        );
                    }

                    // La victima deja de estar residente en RAM.
                    pmmp.setPageValid(pageVictim, false);
                }

                // Cargar la pagina solicitada en el marco fisico elegido.
                getOS().interrupt(
                        InterruptType.LOAD_PAGE,
                        pmmp.getProcess(),
                        mpe
                );

                // Registrar la nueva ubicacion de la pagina en RAM.
                pmmp.setFrameID(pageToLoad, frameVictim);

                pmmp.getPT().getList().get(pageToLoad)
                        .setClock(getOS().getClock());

                // Volver a traducir la direccion logica.
                return getPhysicalAddress(logicalAddress, pmm, store);

            } else {

                // Marcar la pagina como sucia cuando se escribe en ella.
                if (store) {
                    pmmp.setPageDirty(la.getDivision(), true);
                }

                return pa.getAddress();
            }
        }

        return -1;
    }
}
