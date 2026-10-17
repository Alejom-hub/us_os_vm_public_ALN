/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ur_os.system;

import ur_os.process.ProcessInstructionType;
import ur_os.memory.contiguous.SMM_Contiguous;
import ur_os.memory.Memory;
import ur_os.memory.MemoryManagerType;
import ur_os.process.Process;
import java.util.ArrayList;
import java.util.Random;
import ur_os.memory.MemoryInstruction;
import ur_os.memory.MemoryOperationType;
import ur_os.memory.freememorymagament.FreeMemorySlotManager;
import ur_os.memory.contiguous.PMM_Contiguous;
import ur_os.memory.paging.PMM_Paging;
import ur_os.memory.segmentation.PMM_Segmentation;
import ur_os.process.ProcessState;
import ur_os.process.EndInstruction;
import ur_os.process.IOInstruction;
import ur_os.process.Instruction;
import ur_os.virtualmemory.SwapMemory;

/**
 *
 * @author super
 */
public class SystemOS implements Runnable{
    
    SimulationType simType;
    private static int clock = 0;
    private static final int MAX_SIM_CYCLES = 1000;
    private static final int MAX_SIM_PROC_CREATION_TIME = 50;
    private static final double PROB_PROC_CREATION = 0.1;
    public static final int MAX_PROC_SIZE = 1000;
    private static Random r = new Random(1235);
    private OS os;
    private CPU cpu;
    private IOQueue ioq;
    
    private Memory memory;
    private SwapMemory swap;
    
    
    public static final int SEED_SEGMENTS = 7401;
    public static final int SEED_PROCESS_SIZE = 9630;
    
    public static final int MEMORY_SIZE = 2048; //2KB (0 - 2047) - Lab BestFit/WorstFit
    //public static final int MEMORY_SIZE = 1_048_576; //1MB
    public static final int SWAP_MEMORY_SIZE = 1_048_576; // 1 MB
    
    protected ArrayList<Process> processes;
    ArrayList<Integer> execution;
    ArrayList<Process> waitingForMemory; //Processes that arrived, but no hole could contain them yet
    double peakExternalFragmentation;

    public SystemOS(SimulationType simType) {
        memory = new Memory(MEMORY_SIZE);
        swap = new SwapMemory(SWAP_MEMORY_SIZE);
        cpu = new CPU(memory,swap);
        ioq = new IOQueue();
        os = new OS(this, cpu, ioq);
        cpu.setOS(os);
        ioq.setOS(os);
        execution = new ArrayList();
        processes = new ArrayList();
        waitingForMemory = new ArrayList();
        peakExternalFragmentation = 0;
        //initSimulationQueue();
        //initSimulationQueueSimple();
        //initSimulationQueueSimpler();
        //initSimulationQueueBestWorstFit();
        //initSimulationQueueClassExample();
        //initSimulationQueueMedianFit();
        //initSimulationQueueFragmentation();
        initSimulationQueuePagingLRU();
        

        showProcesses();
        this.simType = simType;
    }
    
    public int getTime(){
        return clock;
    }
    
    public ArrayList<Process> getProcessAtI(int i){
        ArrayList<Process> ps = new ArrayList();
        
        for (Process process : processes) {
            if(process.getTime_init() == i){
                ps.add(process);
            }
        }
        
        return ps;
    }

    public void initSimulationQueue(){
        double tp;
        Process p;
        for (int i = 0; i < MAX_SIM_PROC_CREATION_TIME; i++) {
            tp = r.nextDouble();
            if(PROB_PROC_CREATION >= tp){
                p = new Process();
                p.setTime_init(clock);
                processes.add(p);
            }
            clock++;
        }
        clock = 0;
    }
    
    public void initSimulationQueueSimple(){
        Process p;
        int cont = 0;
        for (int i = 0; i < MAX_SIM_PROC_CREATION_TIME; i++) {
            if(i % 4 == 0){
                p = new Process(cont++,-1,true);
                p.setTime_init(clock);
                processes.add(p);
            }
            clock++;
        }
        clock = 0;
    }
    
    public void initSimulationQueueSimpler(){
        
        int tempSize;
        Process p = new Process(0,0);
        tempSize = r.nextInt(MAX_PROC_SIZE-1)+1;
        p.setSize(tempSize);
        Instruction temp;  
        p.addCPUInstructions(3);
        temp = new MemoryInstruction(MemoryOperationType.LOAD, r.nextInt(tempSize), (byte)-1, 4); //Load from logical address 5, 4 clock cycles    
        p.addInstruction(temp);
        p.addCPUInstructions(3);
        temp = new EndInstruction();
        p.addInstruction(temp);
        processes.add(p);
        
        
        //Process 1
        p = new Process(1,2);
        tempSize = r.nextInt(MAX_PROC_SIZE-1)+1;
        p.setSize(tempSize);
        p.addCPUInstructions(3);
        //temp = new IOInstruction(5); 
        temp = new MemoryInstruction(MemoryOperationType.STORE, r.nextInt(tempSize), (byte)38, 3); //Store in logical address 10, valir 38, 3 clock cycles
        p.addInstruction(temp);
        p.addCPUInstructions(3);
        temp = new EndInstruction();
        p.addInstruction(temp);
        processes.add(p);
        
        
        //Process 2
        p = new Process(2,6);
        tempSize = r.nextInt(MAX_PROC_SIZE-1)+1;
        p.setSize(tempSize);
        p.addCPUInstructions(7);
        //temp = new IOInstruction(3);    
        temp = new MemoryInstruction(MemoryOperationType.LOAD, r.nextInt(tempSize), (byte)-1, 4); //Load from logical address 62, 4 clock cycles    
        p.addInstruction(temp);
        p.addCPUInstructions(5);
        temp = new EndInstruction();
        p.addInstruction(temp);
        processes.add(p);
        
        //Process 3
        p = new Process(3,8);
        tempSize = r.nextInt(MAX_PROC_SIZE-1)+1;
        p.setSize(tempSize);
        p.addCPUInstructions(4);
        //temp = new IOInstruction(3);    
        temp = new MemoryInstruction(MemoryOperationType.STORE, r.nextInt(tempSize), (byte)42, 4); //Store in logical address 10, valir 38, 3 clock cycles
        p.addInstruction(temp);
        p.addCPUInstructions(7);
        temp = new EndInstruction();
        p.addInstruction(temp);
        processes.add(p);
        
        clock = 0;
    }
    
    /*
    Scenario for the BestFit / WorstFit lab (MEMORY_SIZE = 2048 bytes).
    Each process: CPU bursts, one STORE on a logical address, CPU bursts, END.
    The STORE duration (memory cycles) controls how long each process stays in
    memory, so P1 and P3 leave early and open holes between P0, P2 and P4.
    */
    public void initSimulationQueueBestWorstFit(){
        Process p;
        Instruction temp;
        
        //Process 0 - Size 400 - Long lived
        p = new Process(0,0);
        p.setSize(400);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 100, (byte)10, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 1 - Size 200 - Short lived (opens a 200 byte hole)
        p = new Process(1,1);
        p.setSize(200);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 50, (byte)11, 2);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 2 - Size 500 - Long lived
        p = new Process(2,2);
        p.setSize(500);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 250, (byte)12, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 3 - Size 300 - Short lived (opens a 300 byte hole)
        p = new Process(3,3);
        p.setSize(300);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 150, (byte)13, 2);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 4 - Size 400 - Long lived (leaves a 248 byte hole at the end)
        p = new Process(4,4);
        p.setSize(400);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 300, (byte)14, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 5 - Size 150 - Arrives when there are holes of 200, 300 and 248
        p = new Process(5,25);
        p.setSize(150);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 120, (byte)15, 3);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 6 - Size 40 - Arrives after P5 is placed
        p = new Process(6,27);
        p.setSize(40);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 10, (byte)16, 3);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        clock = 0;
    }
    
    /*
    Example scenario from the class slides (Contiguous allocation, 2KB).
    Process ID | T-ini | T-fin | Size | Log. Add.
        0      |    0  |   15  |  300 |  100
        1      |   10  |   25  |  500 |   50
        2      |   16  |   35  |  200 |  150
        3      |   20  |   30  |   50 |   10   (table says 150, slide drawing uses 50)
        4      |   25  |   37  |  400 |  100
        5      |   28  |   40  |  700 |  350
        6      |   30  |   41  |  200 |   10
    Each process: CPU, one STORE on its logical address, CPU, END.
    The STORE duration is tuned so the process ends at its T-fin.
    */
    public void initSimulationQueueClassExample(){
        Process p;
        Instruction temp;
        
        //Process 0 - T-ini 0 - T-fin 15 - Size 300 - Log. Add. 100
        p = new Process(0,0);
        p.setSize(300);
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 100, (byte)20, 12);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 1 - T-ini 10 - T-fin 25 - Size 500 - Log. Add. 50
        p = new Process(1,10);
        p.setSize(500);
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 50, (byte)21, 12);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 2 - T-ini 16 - T-fin 35 - Size 200 - Log. Add. 150
        p = new Process(2,16);
        p.setSize(200);
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 150, (byte)22, 14);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 3 - T-ini 20 - T-fin 30 - Size 50 - Log. Add. 10
        p = new Process(3,20);
        p.setSize(50); //Table says 150, but the slide draws P3 in 1000-1049 (50 bytes)
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 10, (byte)23, 5);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 4 - T-ini 25 - T-fin 37 - Size 400 - Log. Add. 100
        p = new Process(4,25);
        p.setSize(400);
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 100, (byte)24, 7);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 5 - T-ini 28 - T-fin 40 - Size 700 - Log. Add. 350
        p = new Process(5,28);
        p.setSize(700);
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 350, (byte)25, 7);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 6 - T-ini 30 - T-fin 41 - Size 200 - Log. Add. 10
        p = new Process(6,30);
        p.setSize(200);
        p.addCPUInstructions(1);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 10, (byte)26, 5);
        p.addInstruction(temp);
        p.addCPUInstructions(1);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        clock = 0;
    }
    
    /*
    Scenario for the MedianFit lab (MEMORY_SIZE = 2048 bytes).
    P1 and P3 leave early, so when P6 arrives there are 3 holes of very
    different sizes (100, 300 and 600). BestFit, WorstFit and MedianFit
    choose a different hole for P6 and P7.
    */
    public void initSimulationQueueMedianFit(){
        Process p;
        Instruction temp;
        
        //Process 0 - Size 100 - Long lived
        p = new Process(0,0);
        p.setSize(100);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 50, (byte)30, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 1 - Size 100 - Short lived (opens a 100 byte hole at 100)
        p = new Process(1,1);
        p.setSize(100);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 20, (byte)31, 2);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 2 - Size 200 - Long lived
        p = new Process(2,2);
        p.setSize(200);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 150, (byte)32, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 3 - Size 300 - Short lived (opens a 300 byte hole at 400)
        p = new Process(3,3);
        p.setSize(300);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 250, (byte)33, 2);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 4 - Size 300 - Long lived
        p = new Process(4,4);
        p.setSize(300);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 200, (byte)34, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 5 - Size 448 - Long lived (leaves a 600 byte hole at 1448)
        p = new Process(5,5);
        p.setSize(448);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 400, (byte)35, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 6 - Size 80 - Arrives with holes of 100, 300 and 600
        p = new Process(6,30);
        p.setSize(80);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 60, (byte)36, 3);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        //Process 7 - Size 40 - Arrives after P6 is placed
        p = new Process(7,33);
        p.setSize(40);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 30, (byte)37, 3);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);
        
        clock = 0;
    }
    
    /*
    Scenario for the Fragmentation lab (MEMORY_SIZE = 2048 bytes).
    P0-P5 build the same memory as the MedianFit lab: when P6 arrives there
    are 3 holes of 100 (base 100), 300 (base 400) and 600 (base 1448).
    Then P6-P9 arrive; each algorithm splits the holes differently, so each one
    suffers external fragmentation (enough free memory in total, but not
    contiguous) on a different request:
        - Worst Fit: P7 (550)
        - Median Fit: P8 (280)
        - Best Fit / First Fit: P9 (60)
    The rejected process waits until a long lived process finishes.
    */
    public void initSimulationQueueFragmentation(){
        Process p;
        Instruction temp;

        //Process 0 - Size 100 - Long lived
        p = new Process(0,0);
        p.setSize(100);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 50, (byte)40, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 1 - Size 100 - Short lived (opens a 100 byte hole at 100)
        p = new Process(1,1);
        p.setSize(100);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 20, (byte)41, 2);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 2 - Size 200 - Long lived
        p = new Process(2,2);
        p.setSize(200);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 150, (byte)42, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 3 - Size 300 - Short lived (opens a 300 byte hole at 400)
        p = new Process(3,3);
        p.setSize(300);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 250, (byte)43, 2);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 4 - Size 300 - Long lived
        p = new Process(4,4);
        p.setSize(300);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 200, (byte)44, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 5 - Size 448 - Long lived (leaves a 600 byte hole at 1448)
        p = new Process(5,5);
        p.setSize(448);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 400, (byte)45, 40);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 6 - Size 80 - Arrives with holes of 100, 300 and 600
        p = new Process(6,30);
        p.setSize(80);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 60, (byte)46, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 7 - Size 550 - Only fits if the 600 hole was not split
        p = new Process(7,32);
        p.setSize(550);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 500, (byte)47, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 8 - Size 280
        p = new Process(8,34);
        p.setSize(280);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 200, (byte)48, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        //Process 9 - Size 60
        p = new Process(9,36);
        p.setSize(60);
        p.addCPUInstructions(2);
        temp = new MemoryInstruction(MemoryOperationType.STORE, 40, (byte)49, 30);
        p.addInstruction(temp);
        p.addCPUInstructions(2);
        p.addInstruction(new EndInstruction());
        processes.add(p);

        clock = 0;
    }

    /*
     * Scenario dedicated to testing Paging with LRU.
     * The process has 33 pages, while physical memory has 32 frames.
     * It accesses pages 0 through 31, accesses page 0 again, and then
     * accesses page 32. When page 32 is requested, LRU should select
     * page 1 as the victim, provided the access history is recorded
     * correctly and all page loads succeed.
     */
    public void initSimulationQueuePagingLRU(){
        Process p = new Process(0, 0);

        // 33 pages of 64 bytes = 2112 bytes.
        p.setSize(33 * OS.PAGE_SIZE);

        // ProcessInstructionList requires the first instruction to be CPU.
        // Without this initial CPU instruction, the first STORE is rejected
        // and the process can end up with an empty instruction list.
        p.addCPUInstructions(1);

        // Access pages 0 through 31 to fill all 32 physical frames.
        for (int pagina = 0; pagina < 32; pagina++) {
            int direccionLogica = pagina * OS.PAGE_SIZE;
            byte dato = (byte) (pagina + 1);

            p.addInstruction(
                new MemoryInstruction(
                    MemoryOperationType.STORE,
                    direccionLogica,
                    dato,
                    1
                )
            );
        }

        // Access page 0 again, making it recently used.
        p.addInstruction(
            new MemoryInstruction(
                MemoryOperationType.STORE,
                0,
                (byte) 100,
                1
            )
        );

        // Request page 32 when all physical frames are occupied.
        // LRU should select page 1 as the victim.
        p.addInstruction(
            new MemoryInstruction(
                MemoryOperationType.STORE,
                32 * OS.PAGE_SIZE,
                (byte) 101,
                1
            )
        );

        p.addInstruction(new EndInstruction());
        processes.add(p);

        clock = 0;
    }

    public void initSimulationQueueSimpler3(){
        
        
        Process p = new Process(0,0);
        p.setSize(200);
        Instruction temp;
        p.addCPUInstructions(5);
        temp = new IOInstruction(4);    
        p.addInstruction(temp);
        p.addCPUInstructions(3);
        processes.add(p);
        
        
        
        //Process 1
        p = new Process(1,5);
        p.setSize(500);
        p.addCPUInstructions(13);
        temp = new IOInstruction(5);    
        p.addInstruction(temp);
        p.addCPUInstructions(16);
        processes.add(p);
        
        
        //Process 2
        p = new Process(2,6);
        p.setSize(250);
        p.addCPUInstructions(7);
        temp = new IOInstruction(3);    
        p.addInstruction(temp);
        p.addCPUInstructions(5);
        processes.add(p);
        
        
        //Process 3
        p = new Process(3,24);
        p.setSize(800);
        p.addCPUInstructions(4);
        temp = new IOInstruction(3);    
        p.addInstruction(temp);
        p.addCPUInstructions(7);
        processes.add(p);
        
        
        
        //Process 4
        p = new Process(4,31);
        p.setSize(600);
        p.addCPUInstructions(7);
        temp = new IOInstruction(3);    
        p.addInstruction(temp);
        p.addCPUInstructions(7);
        processes.add(p);
        
        
        
        clock = 0;
    }
    
    
    
    public void initSimulationQueueSimpler2(){
        
        Process p = new Process(false);
        Instruction temp;
        p.addCPUInstructions(15);
        temp = new IOInstruction(12);    
        p.addInstruction(temp);
        p.addCPUInstructions(21);
        p.setTime_init(0);
        p.setPid(0);
        processes.add(p);
        
        
        p = new Process(false);
        p.addCPUInstructions(8);
        temp = new IOInstruction(4);    
        p.addInstruction(temp);
        p.addCPUInstructions(16);
        p.setTime_init(2);
        p.setPid(1);
        processes.add(p);
        
        p = new Process(false);
        p.addCPUInstructions(10);
        temp = new IOInstruction(15);    
        p.addInstruction(temp);
        p.addCPUInstructions(12);
        p.setTime_init(6);
        p.setPid(2);
        processes.add(p);
        
        p = new Process(false);
        p.addCPUInstructions(9);
        temp = new IOInstruction(6);    
        p.addInstruction(temp);
        p.addCPUInstructions(17);
        p.setTime_init(8);
        p.setPid(3);
        processes.add(p);
        
        clock = 0;
    }
    
    
    
    public boolean isSimulationFinished(){
        
        boolean finished = true;
        
        for (Process p : processes) {
            finished = finished && p.isFinished();
        }
        
        return finished;
    
    }

    public SimulationType getSimulationType() {
        return simType;
    }
    
    public int getClock(){
        return clock;
    }
    
    @Override
    public void run() {
        double tp;
        ArrayList<Process> ps;
        
        System.out.println("******SIMULATION START******");
        
        int i=0;
        Process temp_exec;
        int tempID;
        while(!isSimulationFinished() && i < MAX_SIM_CYCLES){//MAX_SIM_CYCLES is the maximum simulation time, to avoid infinite loops
            System.out.println("******Clock: "+i+"******");
            
            if(i == 8){
                i = i;
            }
            
            if(this.getSimulationType() == SimulationType.ALL || this.getSimulationType() == SimulationType.PROCESS_PLANNING){
                System.out.println(cpu);
                System.out.println(ioq);
            }
            
            //Processes waiting for memory are retried first, in arrival order
            ps = new ArrayList(waitingForMemory);
            waitingForMemory.clear();
            for (Process p : ps) {
                if(os.canAllocate(p.getSize(), false)){
                    System.out.println("Process "+p.getPid()+" admitted after waiting "+(i - p.getTime_init())+" cycles for memory");
                    createProcess(p);
                }else{
                    waitingForMemory.add(p);
                }
            }
            
            //Crear procesos, si aplica en el ciclo actual
            ps = getProcessAtI(i);
            for (Process p : ps) {
                if(os.canAllocate(p.getSize(), true)){
                    createProcess(p);
                }else{
                    System.out.println("Process "+p.getPid()+" ("+p.getSize()+" bytes) waits until a hole can contain it");
                    waitingForMemory.add(p);
                }
            } //If the scheduler is preemtive, this action will trigger the extraction from the CPU, is any process is there.
            
            //Actualizar el OS, quien va actualizar el Scheduler            

            os.update();
            //os.update() prepares the system for execution. It runs at the beginning of the cycle.
            
                        
            clock++;
            
            temp_exec = cpu.getProcess();
            if(temp_exec == null){
                tempID = -1;
            }else{
                tempID = temp_exec.getPid();
            }
            execution.add(tempID);
            
            //Actualizar la CPU
            cpu.update();
            
            
            ///Actualizar la IO
            ioq.update();
            
            //Las actualizaciones de CPU y IO pueden generar interrupciones que actualizan a cola de listos, cuando salen los procesos
            
            if(this.getSimulationType() == SimulationType.ALL || this.getSimulationType() == SimulationType.PROCESS_PLANNING){
                System.out.println("After the cycle: ");
                System.out.println(cpu);
                System.out.println(ioq);
            }
            i++;

        }
        System.out.println("******SIMULATION FINISHES******");
        //os.showProcesses();
        
        System.out.println("******Process Execution******");
        for (Integer num : execution) {
            System.out.print(num+" ");
        }
        System.out.println("");
        
        System.out.println("******Performance Indicators******");
        System.out.println("Total execution cycles: "+clock);
        System.out.println("CPU Utilization: "+this.calcCPUUtilization());
        System.out.println("Throughput: "+this.calcThroughput());
        System.out.println("Average Turnaround Time: "+this.calcTurnaroundTime());
        System.out.println("Average Waiting Time: "+this.calcAvgWaitingTime());
        System.out.println("Average Context Switches: "+this.calcAvgContextSwitches());
        System.out.println("Average Response Time: "+this.calcAvgResponseTime());
        
        showProcesses();
        memory.showNotNullBytes();
        
        showFreeMemory();
        
        System.out.println("******Fragmentation Indicators******");
        if(OS.SMM != MemoryManagerType.PAGING){
            System.out.println("Free Memory Slot Manager: "+OS.MSM);
            System.out.println("Requests rejected by external fragmentation: "+os.getExternalFragmentationRejections());
            System.out.printf("Peak external fragmentation: %.2f%%%n", peakExternalFragmentation*100);
        }
        if(!waitingForMemory.isEmpty()){
            System.out.println("Processes still waiting for memory: "+waitingForMemory.size());
        }
    }
    
    private void createProcess(Process p){
        os.create_process(p);
        System.out.println("Process Created: "+p.getPid()+"\n"+p);
        
        showFreeMemory();
    }
    
    public void showFreeMemory(){
        if(OS.SMM == MemoryManagerType.PAGING){
            System.out.println("Free frame number: "+os.fmm.getSize());
        }else{
            System.out.println("Free Memory Slots ("+os.fmm.getSize()+"): ");
            FreeMemorySlotManager msm = (FreeMemorySlotManager)os.fmm;
            System.out.println(msm);
        }
        showFragmentation();
    }
    
    /*
    Fragmentation calculation, following the class definitions:
    - External fragmentation: total free memory exists to satisfy a request,
      but it is not contiguous. Measured as 1 - (largest hole / total free).
    - Internal fragmentation: allocated memory - requested memory, summed for
      every process that is in memory.
    - 50% rule: with N allocated blocks, about 0.5 N blocks (holes) are lost
      to fragmentation (First Fit analysis).
    */
    public void showFragmentation(){
        System.out.println("---Fragmentation---");
        if(OS.SMM != MemoryManagerType.PAGING){
            FreeMemorySlotManager msm = (FreeMemorySlotManager)os.fmm;
            double ext = msm.getExternalFragmentation();
            if(ext > peakExternalFragmentation){
                peakExternalFragmentation = ext;
            }
            int holes = msm.getSize();
            int blocks = countAllocatedBlocks();
            System.out.println("Total free memory: "+msm.getTotalFreeMemory()+" bytes | Largest hole: "+msm.getLargestFreeSlot()+" bytes | Holes: "+holes);
            System.out.printf("External fragmentation: %.2f%% (1 - largest hole / total free memory)%n", ext*100);
            if(blocks > 0){
                System.out.printf("50%% rule: %d holes for N = %d allocated blocks -> holes/N = %.2f (expected ~0.5)%n", holes, blocks, (double)holes/blocks);
            }else{
                System.out.println("50% rule: no allocated blocks");
            }
        }
        System.out.println("Internal fragmentation: "+calcInternalFragmentation()+" bytes");
    }
    
    //A process is in memory if it was created, it got its memory and it has not finished
    private boolean isInMemory(Process p){
        if(p.getState() == ProcessState.FINISHED){
            return false;
        }
        switch(OS.SMM){
            case PAGING:
                return p.getPMM() instanceof PMM_Paging;
            case SEGMENTATION:
                return p.getPMM() instanceof PMM_Segmentation;
            default:
            case CONTIGUOUS:
                return p.getPMM() instanceof PMM_Contiguous && ((PMM_Contiguous)p.getPMM()).isValid();
        }
    }
    
    //Number of allocated memory blocks (N): one slot per process, or one per segment
    public int countAllocatedBlocks(){
        int blocks = 0;
        for (Process p : processes) {
            if(isInMemory(p)){
                if(p.getPMM() instanceof PMM_Segmentation){
                    blocks += ((PMM_Segmentation)p.getPMM()).getSt().getSize();
                }else{
                    blocks++;
                }
            }
        }
        return blocks;
    }
    
    /*Internal fragmentation: allocated memory - requested memory.
      - Contiguous: the slot has exactly the requested size, so it is 0.
      - Segmentation: each segment gets exactly its limit, so it is 0.
      - Paging: the last page of the process is not full:
        pages * PAGE_SIZE - process size.
    */
    public int calcInternalFragmentation(){
        int internal = 0;
        for (Process p : processes) {
            if(isInMemory(p)){
                if(p.getPMM() instanceof PMM_Paging){
                    int pages = (p.getSize() + OS.PAGE_SIZE - 1) / OS.PAGE_SIZE;
                    internal += pages * OS.PAGE_SIZE - p.getSize();
                }else if(p.getPMM() instanceof PMM_Contiguous){
                    internal += ((PMM_Contiguous)p.getPMM()).getLimit() - p.getSize();
                }
            }
        }
        return internal;
    }
    
    public void showProcesses(){
        System.out.println("Process list:");
        StringBuilder sb = new StringBuilder();
        
        for (Process process : processes) {
            sb.append(process);
            sb.append("\n");
        }
        
        System.out.println(sb.toString());
    }
    
    
    public double calcCPUUtilization(){
        int cont=0;
        for (Integer num : execution) {
            if(num == -1)
                cont++;
        }
        
        return (execution.size()-cont)/(double)execution.size();
    }
    
    public double calcTurnaroundTime(){
        
        double tot = 0;
        
        for (Process p : processes) {
            tot = tot + (p.getTime_finished() - p.getTime_init());
        }
        
        
        return tot/processes.size();
    }
    
    public double calcThroughput(){
        return (double)processes.size()/execution.size();
    }
    
    public double calcAvgWaitingTime(){
        double tot = 0;
        
        for (Process p : processes) {
            tot = tot + ((p.getTime_finished() - p.getTime_init()) - p.getTotalExecutionTime());
        }
        
        return tot/processes.size();
    }
    
    public double calcAvgContextSwitches(){
        int cont = 1;
        int prev = execution.get(0);
        for (Integer i : execution) {
            if(prev != i){
                cont++;
                prev = i;
            }
        }
        
        return cont / (double)processes.size();
    }

    public double calcAvgResponseTime(){
        
        double tot = 0;
        int temp = 0;
        for (Process p : processes) {
            temp = execution.indexOf(p.getPid());//On which cycle did the process started execution
            tot = tot + (temp - p.getTime_init());//Difference between execution start and arrival
        }
        
        return tot/processes.size();
    }
    
    
}
