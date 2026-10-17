/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.system;

import java.util.ArrayList;
import ur_os.memory.Memory;
import ur_os.memory.MemoryInstruction;
import ur_os.memory.SystemMemoryManager;
import ur_os.memory.freememorymagament.MemorySlot;
import ur_os.process.ProcessState;
import ur_os.process.Process;
import ur_os.virtualmemory.SwapMemory;

/**
 *
 * @author super
 */
public class MemoryUnit {

    Memory memory;
    SwapMemory swap;
    protected ArrayList<ur_os.process.Process> processes;
    SystemMemoryManager smm;
    CPU cpu;

    public MemoryUnit() {
        this(
                new Memory(SystemOS.MEMORY_SIZE),
                new SwapMemory(SystemOS.SWAP_MEMORY_SIZE),
                null,
                null
        );
    }

    public MemoryUnit(
            Memory m,
            SwapMemory s,
            CPU cpu,
            SystemMemoryManager smm) {

        memory = m;
        swap = s;
        processes = new ArrayList();
        this.cpu = cpu;
        this.smm = smm;
    }

    public void addProcess(ur_os.process.Process p) {
        p.setState(ProcessState.NEW_MEMORY);
        processes.add(p);
    }

    public void update() {
        Process temp;
        MemoryInstruction mop;

        for (int i = 0; i < processes.size(); i++) {

            if (processes.get(i).getState() != ProcessState.NEW_MEMORY) {

                mop = (MemoryInstruction) processes.get(i).getCurrentInstruction();

                executeMemoryOperation(processes.get(i), mop);

                if (processes.get(i).advanceInstruction()) {
                    // If the process finishes the current burst.
                    System.out.println("MEMORY OPERATION EXECUTION DONE");

                    temp = processes.get(i);
                    processes.remove(processes.get(i));

                    cpu.os.interrupt(InterruptType.MEMORY_DONE, temp);
                    i--;

                } else {
                    System.out.println(
                            "MEMORY OPERATION EXECUTION - "
                            + mop.getRemainingCycles()
                            + " cycles left"
                    );
                }

            } else {
                processes.get(i).setState(ProcessState.MEMORY);
            }
        }
    }

    public void executeMemoryOperation(Process p, MemoryInstruction mop) {

        int logAdd;
        int phyAdd;

        if (mop != null) {

            System.out.println(
                    "Process " + p.getPid()
                    + " is executing " + mop
            );

            switch (mop.getMType()) {

                case LOAD:
                    logAdd = mop.getLogicalAddress();

                    phyAdd = smm.getPhysicalAddress(
                            logAdd,
                            p.getPMM()
                    );

                    // Verificar que la direccion fisica sea valida.
                    if (isValidPhysicalAddress(phyAdd)) {
                        load(phyAdd);
                    } else {
                        System.out.println(
                                "Error: no se puede leer la direccion fisica "
                                + phyAdd
                        );
                    }

                    break;

                case STORE:
                    logAdd = mop.getLogicalAddress();

                    phyAdd = smm.getPhysicalAddress(
                            logAdd,
                            p.getPMM(),
                            true
                    );

                    // Verificar que la direccion fisica sea valida.
                    if (isValidPhysicalAddress(phyAdd)) {
                        store(phyAdd, mop.getContent());
                    } else {
                        System.out.println(
                                "Error: no se puede escribir en la direccion fisica "
                                + phyAdd
                        );
                    }

                    break;
            }
        }
    }

    private boolean isValidPhysicalAddress(int physicalAddress) {
        return physicalAddress >= 0
                && physicalAddress < memory.getSize();
    }

    public void removeProcess(ur_os.process.Process p) {
        processes.remove(p); // The process is found thanks to the equals() method.
    }

    @Override
    public String toString() {

        if (!processes.isEmpty()) {

            StringBuilder sb = new StringBuilder();
            sb.append("MU ");

            for (ur_os.process.Process p : processes) {
                sb.append(p);
                sb.append("\n");
            }

            return sb.toString();

        } else {
            return "MU: Empty";
        }
    }

    public byte load(int physicalAddress) {
        byte b = memory.get(physicalAddress);
        System.out.println("The obtained data is: " + b);
        return b;
    }

    public void store(int physicalAddress, byte content) {
        memory.set(physicalAddress, content);

        System.out.println(
                "The data " + memory.get(physicalAddress)
                + " is stored in: " + physicalAddress
        );
    }

    void loadSlot(MemorySlot m, MemorySlot vm) {
        int base = m.getBase();
        int vbase = vm.getBase();

        for (int i = 0; i < vm.getSize(); i++) {
            memory.set(base + i, swap.get(vbase + i));
        }
    }

    void storeSlot(MemorySlot m, MemorySlot vm) {
        int base = m.getBase();
        int vbase = vm.getBase();

        for (int i = 0; i < vm.getSize(); i++) {
            swap.set(vbase + i, memory.get(base + i));
        }
    }

    void loadPage(int frameMem, int frameSwap) {
        int base = frameMem * OS.PAGE_SIZE;
        int vbase = frameSwap * OS.PAGE_SIZE;

        for (int i = 0; i < OS.PAGE_SIZE; i++) {
            memory.set(base + i, swap.get(vbase + i));
        }
    }

    void storePage(int frameMem, int frameSwap) {
        int base = frameMem * OS.PAGE_SIZE;
        int vbase = frameSwap * OS.PAGE_SIZE;

        for (int i = 0; i < OS.PAGE_SIZE; i++) {
            swap.set(vbase + i, memory.get(base + i));
        }
    }

    void setSMM(SystemMemoryManager smm) {
        this.smm = smm;
    }
}