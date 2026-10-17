/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ur_os.system;

import ur_os.memory.contiguous.SMM_Contiguous;
import ur_os.memory.freememorymagament.FreeFramesManager;
import ur_os.memory.paging.PMM_Paging;
import ur_os.memory.ProcessMemoryManager;
import ur_os.memory.MemoryManagerType;
import ur_os.process.Process;
import ur_os.process.planning.ReadyQueue;
import ur_os.process.ProcessState;
import java.util.Random;
import ur_os.memory.freememorymagament.BestFitMemorySlotManager;
import ur_os.memory.freememorymagament.FirstFitMemorySlotManager;
import ur_os.memory.freememorymagament.FreeMemoryManager;
import ur_os.memory.freememorymagament.MemorySlot;
import ur_os.memory.freememorymagament.FreeMemorySlotManager;
import ur_os.memory.freememorymagament.WorstFitMemorySlotManager;
import ur_os.memory.freememorymagament.MedianFitMemorySlotManager;
import ur_os.memory.segmentation.PMM_Segmentation;
import static ur_os.memory.MemoryManagerType.CONTIGUOUS;
import ur_os.memory.SystemMemoryManager;
import ur_os.memory.contiguous.PMM_Contiguous;
import ur_os.memory.freememorymagament.FreeMemorySlotManagerType;
import ur_os.memory.paging.MemoryPageExchange;
import ur_os.memory.paging.SMM_Paging;
import ur_os.memory.segmentation.SMM_Segmentation;
import static ur_os.system.InterruptType.SCHEDULER_CPU_TO_RQ;
import static ur_os.system.SystemOS.MAX_PROC_SIZE;
import ur_os.virtualmemory.*;
import ur_os.virtualmemory.ProcessVirtualMemoryManagerType;
import static ur_os.virtualmemory.ProcessVirtualMemoryManagerType.FIFO;
import static ur_os.virtualmemory.ProcessVirtualMemoryManagerType.LRU;


/**
 *
 * @author super
 */
public class OS {
    
    ReadyQueue rq;
    IOQueue ioq;
    private static int process_count = 0;
    SystemOS system;
    CPU cpu;
    SystemMemoryManager smm;
    FreeMemoryManager fmm;
    FreeMemoryManager fvmm;
    Random r;
    boolean lazySwap;
    int externalFragmentationRejections; //Requests rejected because the free memory was not contiguous

    public static final int MAX_PROCESS_PRIORITY = 10; //Page size in bytes
    public static final int PAGE_SIZE = 64; //Page size in bytes
    public static final MemoryManagerType SMM = MemoryManagerType.PAGING; //CONTIGUOUS, SEGMENTATION or PAGING
    public static final FreeMemorySlotManagerType MSM = FreeMemorySlotManagerType.BEST_FIT;
    
    public static final ProcessVirtualMemoryManagerType PVMM = ProcessVirtualMemoryManagerType.LRU;
    public static final int FRAMES_PER_PROCESS = 3; //Maximum number of frames assigned to a process, if virtual memory is on
    public static final boolean VIRTUAL_MEMORY_MODE_ON = true; //Maximum number of frames assigned to a process, if virtual memory is on
    
    
    public OS(SystemOS system, CPU cpu, IOQueue ioq){
        rq = new ReadyQueue(this);
        this.ioq = ioq;
        this.system = system;
        this.cpu = cpu;
        lazySwap = false;//No preloading pages to reduce page faults
        
         if(SMM == MemoryManagerType.PAGING){
            smm = new SMM_Paging(this);
            fmm = new FreeFramesManager(SystemOS.MEMORY_SIZE);
            fvmm = new FreeFramesManager(SystemOS.SWAP_MEMORY_SIZE);
        }else{
            switch(SMM){
                case CONTIGUOUS:
                    smm = new SMM_Contiguous(this);
                    break;
                
                case SEGMENTATION:
                    smm = new SMM_Segmentation(this);
                    break;
            }
             
             
            switch(MSM){
            case FIRST_FIT:
                fmm = new FirstFitMemorySlotManager(SystemOS.MEMORY_SIZE); //Memory
                fvmm= new FirstFitMemorySlotManager(SystemOS.SWAP_MEMORY_SIZE); //Swap memory
                break;
            case BEST_FIT:
                fmm = new BestFitMemorySlotManager(SystemOS.MEMORY_SIZE);
                fvmm = new BestFitMemorySlotManager(SystemOS.SWAP_MEMORY_SIZE);
                
                break;
            case WORST_FIT:
                fmm = new WorstFitMemorySlotManager(SystemOS.MEMORY_SIZE);
                fvmm = new WorstFitMemorySlotManager(SystemOS.SWAP_MEMORY_SIZE);
                break;
            case MEDIAN_FIT:
                fmm = new MedianFitMemorySlotManager(SystemOS.MEMORY_SIZE);
                fvmm = new MedianFitMemorySlotManager(SystemOS.SWAP_MEMORY_SIZE);
                break;
            }
            System.out.println("Free Memory Slot Manager: "+MSM+" ("+fmm.getClass().getSimpleName()+")");
        }
         
        r = new Random();
    }
    
    
    public void update(){
        rq.update();
    }
    
    public boolean isCPUEmpty(){
        return cpu.isEmpty();
    }
    
    public Process getProcessInCPU(){
        return cpu.getProcess();
    }
    
    public void interrupt(InterruptType t, Process p){
        interrupt(t,p,null);
    }
    
    
    
    public void interrupt(InterruptType t, Process p, Object i){
       
        ProcessMemoryManager pmm;
        MemorySlot m;
        MemorySlot vm;
        MemoryPageExchange mpe;
        
        switch(t){
            
            case CPU_TO_MEMORY:
                cpu.addProcessToMemoryUnit(p);
                break;
        
            case CPU_TO_IO:
                ioq.addProcess(p);
                break;
            
            case FINISH_PROCESS:
                p.setState(ProcessState.FINISHED);
                p.setTime_finished(system.getTime());
                System.out.println("Process Terminated: "+p.getPid()+" "+p.getSize());
                fmm.reclaimMemory(p);
                system.showFreeMemory();
                break;
            
            case IO_DONE: //It is assumed that the process in IO is done and it has been removed from the queue
                rq.addProcess(p);
            break;

            case MEMORY_DONE: //It is assumed that the process in Memory is done and it has been removed from the queue
                rq.addProcess(p);
            break;
            
            case SCHEDULER_CPU_TO_RQ:
                //When the scheduler is preemptive and will send the current process in CPU to the Ready Queue
                Process temp = cpu.extractProcess();
                rq.addProcess(temp);
                if(p != null){
                    cpu.addProcess(p);
                }
                
            break;
            
            
            case SCHEDULER_RQ_TO_CPU:
                //When the scheduler defined which process will go to CPU
                cpu.addProcess(p);
                
            break;
            
            
            /*
            ******Virtual memory interruptions*********
            */
            case LOAD_SLOT:
                vm = (MemorySlot)i;
                m = getMemorySlot(vm.getSize());
                pmm = p.getPMM();
                if(pmm instanceof PMM_Contiguous){
                    PMM_Contiguous pmmc = (PMM_Contiguous)pmm;
                    pmmc.setMemorySlot(m); //Set the new allocated slot in memory
                    cpu.loadSlot(m,vm); //Bring data from swap to memory
                    pmmc.setValid(true);
                }else if(pmm instanceof PMM_Segmentation){
                    //TBD
                    //if allocated memory is full and the segment does not fit in the space
                    //  Select a candidate victim segment
                    //  Send it to swap
                    //  Free the space
                    //Bring segment from swap to memory
                }
                
                break;
                
            case STORE_SLOT:
                pmm = p.getPMM();
                if(pmm instanceof PMM_Contiguous){
                    PMM_Contiguous pmmc = (PMM_Contiguous)pmm;
                    if(pmmc.isDirty()){
                        m = pmmc.getMemorySlot(); //Get the allocated slot in memory
                        vm = pmmc.getVMemorySlot(); //Get the allocated slot in swap memory
                        cpu.storeSlot(m,vm); //Send data to swap from memory, if there were changes
                        fmm.reclaimMemory(p); //Take allocated memory from the process
                    }
                    pmmc.setValid(false);
                }else if(pmm instanceof PMM_Segmentation){
                    //TBD
                    //if segment is dirty
                    //  Send it to swap
                    //  Free the space
                    //Mark the segment invalid
                }
                
                break;
            
                
            case LOAD_PAGE:
                mpe = (MemoryPageExchange)i;
                pmm = p.getPMM();
                if(pmm instanceof PMM_Paging){
                    PMM_Paging pmmp = (PMM_Paging)pmm;
                    cpu.loadPage(mpe.getFrameVictim(),mpe.getFrameToLoadFromSwap()); //Bring data from swap to memory
                }
                break;
                
            case STORE_PAGE:
                mpe = (MemoryPageExchange)i;
                pmm = p.getPMM();
                if(pmm instanceof PMM_Paging){
                    PMM_Paging pmmp = (PMM_Paging)pmm;
                    cpu.storePage(mpe.getFrameVictim(),mpe.getFrameVictimInSwap()); //Send data from memory to swap
                }
                if(!mpe.isFullExchange()){ //If it is a fullExchange, then the frame will be used to load another page
                    FreeFramesManager ffmm = (FreeFramesManager)fmm;
                    ffmm.reclaimFrame(mpe.getFrameVictim()); //Get back the frame 
                }
                break;
            
        }

    }
    
    
    
    
    
    public void removeProcessFromCPU(){
        cpu.removeProcess();
    }
    
    public void create_process(){
        create_process(null);
    }
    
    public void create_process(ur_os.process.Process p){
        if(p != null){
            //Keep the pid given in the scenario: a process that waited for memory may be created after later ones
            if(p.getPid() < 0){
                p.setPid(process_count);
            }
            process_count++;
        }else{
            p = new Process(process_count++, system.getTime());
        }
        rq.addProcess(p);
        ProcessMemoryManager pmm;
        switch (SMM) {
            case PAGING:
                if(p.getSize() == 0)
                    pmm = new PMM_Paging(p,r.nextInt(MAX_PROC_SIZE-1)+1,0);
                else
                    pmm = new PMM_Paging(p,p.getSize(),0);
                
                PMM_Paging pmmp = (PMM_Paging)pmm;
               
                if(VIRTUAL_MEMORY_MODE_ON){
                    pmmp.setAssignedPages(FRAMES_PER_PROCESS);
                }else{
                    pmmp.setAssignedPages(-1); //The process will get all the pages needed to store the process
                }
                
                p.setPMM(pmm);
                assignFramesToProcess(p);
                break;
            case SEGMENTATION:
                if(p.getSize() == 0)
                    pmm = new PMM_Segmentation(r.nextInt(MAX_PROC_SIZE-1)+1);
                else
                    pmm = new PMM_Segmentation(p.getSize());
                p.setPMM(pmm);
                assignSegmentsToProcess(p);
                break;
            default:
            case CONTIGUOUS:
                if(VIRTUAL_MEMORY_MODE_ON){
                    if(p.getSize() == 0)
                        pmm = new PMM_Contiguous(p, getVMemorySlot(r.nextInt(MAX_PROC_SIZE-1)+1),this.lazySwap);
                    else
                        pmm = new PMM_Contiguous(p, getVMemorySlot(p.getSize()),this.lazySwap);
                }else{
                    if(p.getSize() == 0){
                        int tsize = r.nextInt(MAX_PROC_SIZE-1)+1;
                        pmm = new PMM_Contiguous(p, getVMemorySlot(tsize),getMemorySlot(tsize),this.lazySwap);
                    }else
                        pmm = new PMM_Contiguous(p, getVMemorySlot(p.getSize()),getMemorySlot(p.getSize()),this.lazySwap);
                }
                p.setPMM(pmm); //Assign the newly created PMM
                
                /*if(!lazySwap){
                    PMM_Contiguous pmmc = (PMM_Contiguous)pmm;
                    pmmc.setMemorySlot(getMemorySlot(p.getSize())); //get free slot and assign it to the process and store the process in memory. 
                }*/
                break;
        }
        
        switch(PVMM){//Assign the Process Virtual Memory Manager, to support the selection of the victim memory division
            case FIFO:
                p.getPMM().setPVMM(new PVMM_FIFO());
            break;

            case LRU:
                p.getPMM().setPVMM(new PVMM_LRU());
                break;
            
            case LFU:
                p.getPMM().setPVMM(new PVMM_LFU());
                break;
                
            case MFU:
                p.getPMM().setPVMM(new PVMM_MFU());
                break;

        }
        
        
    }
    
    public MemorySlot getMemorySlot(int size){
        FreeMemorySlotManager msm = (FreeMemorySlotManager)fmm;
        MemorySlot m = msm.getSlot(size);
        if(m == null){
            reportAllocationFailure(size);
        }
        return m;
    }

    /*
    Admission control for contiguous allocation (without virtual memory): the
    process can only be created if one hole can contain it. Otherwise it waits
    (in SystemOS) until some process finishes and returns its memory.
    The report flag avoids repeating the message every cycle while it waits.
    */
    public boolean canAllocate(int size, boolean report){
        // Paging necesita al menos un marco fisico para que el proceso pueda iniciar.
        if(SMM == MemoryManagerType.PAGING){
            FreeFramesManager freeFrames = (FreeFramesManager)fmm;
            FreeFramesManager freeSwapFrames = (FreeFramesManager)fvmm;

            int pagesRequired = size > 0
                    ? (size + PAGE_SIZE - 1) / PAGE_SIZE
                    : 1; // Si el tamano aun no esta definido, se valida al menos una pagina.

            boolean hasPhysicalFrame = freeFrames.getSize() >= (VIRTUAL_MEMORY_MODE_ON ? 1 : pagesRequired);
            boolean hasSwapFrames = freeSwapFrames.getSize() >= pagesRequired;

            if(hasPhysicalFrame && hasSwapFrames){
                return true;
            }

            if(report){
                if(!hasPhysicalFrame){
                    System.out.println("NOT ENOUGH MEMORY - No hay suficientes marcos fisicos libres para crear el proceso.");
                }
                if(!hasSwapFrames){
                    System.out.println("NOT ENOUGH MEMORY - No hay suficientes marcos libres en swap para crear el proceso.");
                }
            }
            return false;
        }

        // Se conserva la validacion original para asignacion contigua sin memoria virtual.
        if(SMM != MemoryManagerType.CONTIGUOUS || VIRTUAL_MEMORY_MODE_ON){
            return true;
        }
        FreeMemorySlotManager msm = (FreeMemorySlotManager)fmm;
        if(msm.getLargestFreeSlot() >= size){
            return true;
        }
        if(report){
            reportAllocationFailure(size);
        }
        return false;
    }

    private void reportAllocationFailure(int size){
        FreeMemorySlotManager msm = (FreeMemorySlotManager)fmm;
        if(msm.isExternalFragmentationFor(size)){
            //There is enough memory in total, but it is not contiguous
            externalFragmentationRejections++;
            System.out.println("EXTERNAL FRAGMENTATION - Request of "+size+" bytes rejected: there are "
                    +msm.getTotalFreeMemory()+" free bytes, but the largest hole is "+msm.getLargestFreeSlot()+" bytes");
        }else{
            System.out.println("NOT ENOUGH MEMORY - Request of "+size+" bytes rejected: there are only "
                    +msm.getTotalFreeMemory()+" free bytes");
        }
    }

    public int getExternalFragmentationRejections(){
        return externalFragmentationRejections;
    }
    
    public MemorySlot getVMemorySlot(int size){
        FreeMemorySlotManager vmsm = (FreeMemorySlotManager)fvmm;
        return vmsm.getSlot(size);
    }
    
    public void assignSegmentsToProcess(Process p){
        PMM_Segmentation pmm = (PMM_Segmentation)p.getPMM();
        int limit;
        MemorySlot m;
        int ptSize = pmm.getSt().getSize();
        for (int i = 0; i < ptSize; i++) {
            limit = pmm.getSegment(i).getLimit();
            m = this.getMemorySlot(limit);
            pmm.getSegment(i).setMemorySlot(m);
            pmm.getSegment(i).setValid(true); //The segment is loaded in memory
        }
    }
    
    public int getFreeFrame(){
        FreeFramesManager freeFrames = (FreeFramesManager)fmm;
        return freeFrames.getFrame();
    }
    
    public MemorySlot getFreeMemorySlot(int size){
        FreeMemorySlotManager freeSlots = (FreeMemorySlotManager)fmm;
        return freeSlots.getSlot(size);
    }
    
    public void assignFramesToProcess(Process p){
        PMM_Paging pmmp = (PMM_Paging)p.getPMM();
        FreeFramesManager vfreeFrames = (FreeFramesManager)fvmm;
        FreeFramesManager freeFrames = (FreeFramesManager)fmm;

        int ptSize = pmmp.getVPT().getSize(); // Numero de paginas virtuales del proceso.

        // Verificamos todos los recursos antes de retirar marcos, para no dejar
        // la tabla de paginas parcialmente asignada si no hay memoria suficiente.
        int physicalFramesRequired = VIRTUAL_MEMORY_MODE_ON ? 1 : ptSize;
        if(ptSize > vfreeFrames.getSize()){
            System.out.println("Error - No hay suficientes marcos disponibles en swap para el proceso " + p.getPid());
            return;
        }
        if(physicalFramesRequired > freeFrames.getSize()){
            System.out.println("Error - No hay suficientes marcos fisicos para el proceso " + p.getPid());
            return;
        }

        // Reservar los marcos virtuales en swap y construir la tabla de paginas.
        for(int i = 0; i < ptSize; i++){
            int swapFrame = vfreeFrames.getFrame();
            if(swapFrame < 0){
                System.out.println("Error - No se pudo reservar un marco de swap para el proceso " + p.getPid());
                return;
            }
            pmmp.addVFrameID(swapFrame, true);

            if(VIRTUAL_MEMORY_MODE_ON){
                pmmp.addFrameID(-1); // La pagina inicia sin marco fisico residente.
            }else{
                int physicalFrame = freeFrames.getFrame();
                if(physicalFrame < 0){
                    System.out.println("Error - No se pudo reservar un marco fisico para el proceso " + p.getPid());
                    return;
                }
                pmmp.addFrameID(physicalFrame, true);
            }
        }

        // Con memoria virtual, solo la primera pagina se carga inicialmente en RAM.
        if(VIRTUAL_MEMORY_MODE_ON){
            int firstFrame = freeFrames.getFrame();
            if(firstFrame >= 0){
                pmmp.setFrameID(0, firstFrame);
            }else{
                System.out.println("Error - No se pudo asignar el marco fisico inicial al proceso " + p.getPid());
            }
        }
    }
    
    public void showProcesses(){
        System.out.println("Process list:");
        System.out.println(rq.toString());
    }
    
    
    public SimulationType getSimulationType() {
        return system.getSimulationType();
    }
    
    public int getClock(){
        return system.getClock();
    }
}
