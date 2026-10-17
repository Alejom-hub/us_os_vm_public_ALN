/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.memory.paging;

import java.util.ArrayList;
import java.util.LinkedList;
import ur_os.system.OS;

/**
 *
 * @author super
 */
public class PageTable {

    ArrayList<PageTableEntry> pageTable;
    public static final int SAMPLE_PROCESS_SIZE = 100;

    int programSize; // Size of the program in bytes.
    int size; // Number of pages that the program uses.
    boolean virtualMemory;
    int assignedPages;

    public PageTable() {
        this((SAMPLE_PROCESS_SIZE / OS.PAGE_SIZE) + 1);
    }

    public PageTable(int programSize) {
        this(programSize, -1, false);
    }

    public PageTable(
            int programSize,
            int assignedPages,
            boolean virtualMemory) {

        this.programSize = programSize;
        this.virtualMemory = virtualMemory;
        this.size = (int) Math.ceil(
                (double) programSize / OS.PAGE_SIZE
        );
        this.assignedPages = assignedPages;
        this.pageTable = new ArrayList(size);
    }

    public PageTable(PageTable pt) {
        this(pt.getProgramSize());
        pageTable = new ArrayList(pt.getTable());
    }

    private ArrayList<PageTableEntry> getTable() {
        return pageTable;
    }

    public int getFrameIdFromPage(int page) {

        // Verificar los limites de la tabla y las entradas existentes.
        if (page < 0
                || page >= size
                || page >= pageTable.size()) {
            return -2;
        }

        if (pageTable.get(page).isValid()) {
            return pageTable.get(page).getFrameId();
        }

        // La pagina existe, pero no esta cargada en memoria.
        return -1;
    }

    public void addFrameID(int frame) {
        addFrameID(frame, false);
    }

    public void addFrameID(int frame, boolean valid) {
        pageTable.add(new PageTableEntry(frame, valid));
    }

    public void setFrameID(int page, int frame) {

        // No permitir indices ni marcos invalidos.
        if (page < 0 || page >= size || frame < 0) {
            System.out.println("Error - Invalid page or frame");
            return;
        }

        if (page == pageTable.size()) {

            // Crear una entrada nueva.
            pageTable.add(new PageTableEntry(frame));

        } else if (page < pageTable.size()) {

            // Actualizar una entrada existente.
            pageTable.get(page).setFrameId(frame);

        } else {
            System.out.println("Error - Including erroneous page number");
            return;
        }

        pageTable.get(page).setValid(true);
        pageTable.get(page).setDirty(false);
    }

    public boolean isPageDirty(int page) {

        if (isValidPageIndex(page)) {
            return pageTable.get(page).isDirty();
        }

        System.out.println("Error - Including erroneous page number");
        return false;
    }

    public void setPageDirty(int page, boolean dirty) {

        if (isValidPageIndex(page)) {
            pageTable.get(page).setDirty(dirty);
        } else {
            System.out.println("Error - Including erroneous page number");
        }
    }

    public boolean isPageValid(int page) {

        if (isValidPageIndex(page)) {
            return pageTable.get(page).isValid();
        }

        System.out.println("Error - Including erroneous page number");
        return false;
    }

    public void setPageValid(int page, boolean valid) {

        if (isValidPageIndex(page)) {
            pageTable.get(page).setValid(valid);
        } else {
            System.out.println("Error - Including erroneous page number");
        }
    }

    private boolean isValidPageIndex(int page) {
        return page >= 0 && page < pageTable.size();
    }

    public LinkedList<PageTableEntry> getValidList() {

        LinkedList<PageTableEntry> validPages = new LinkedList<>();

        for (PageTableEntry p : pageTable) {
            if (p.isValid()) {
                validPages.add(p);
            }
        }

        return validPages;
    }

    public int getSize() {
        return size;
    }

    public int getProgramSize() {
        return programSize;
    }

    public static int getPageSize() {
        return OS.PAGE_SIZE;
    }

    public ArrayList<PageTableEntry> getList() {
        return this.pageTable;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (PageTableEntry pageTableEntry : pageTable) {
            sb.append("Page: ");
            sb.append(count++);
            sb.append(" ");
            sb.append(pageTableEntry.toString());
            sb.append("\n");
        }

        return sb.toString();
    }
}