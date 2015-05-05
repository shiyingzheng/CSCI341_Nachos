package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/*TODO: 
  1. test all of this shit; 
  2. what are pages
  3. i think we are doing a book
  4. it should be about puppies i think
  5. meow meow meow
  */

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
  /**
   * Allocate a new process.
   */
  public VMProcess() {
    super();
  }

  /**
   * Save the state of this process in preparation for a context switch.
   * Called by <tt>UThread.saveState()</tt>.
   */
  public void saveState() {
    super.saveState();
    VMKernel.contextSwitch();
    // sync the TLB with entries in page table, invalidate all entries in TLB
    // we can call VMKernel contextSwitch() for this
  }



  /**
   * Restore the state of this process after a context switch. Called by
   * <tt>UThread.restoreState()</tt>.
   */
  public void restoreState() {
    /* super.restoreState(); */
  }

  private TranslationEntry findEntryInTLB(int pid, int pageNumber){
    TranslationEntry page = null;
    for(int i=0; i<Machine.processor().getTLBSize(); i++) {
      TranslationEntry entry = Machine.processor().readTLBEntry(i);

      if(entry.valid && pageNumber == entry.vpn) {
        page = entry;
        break;
      }
    }
    return page;
  }

  // try to fetch a page from the TLB
  // If a TLB miss happens, fetch the page from the global page table,
  // then bring that page into the TLB
  // handleTLBMiss() will be called
  public TranslationEntry getPage(int pid, int pageNumber, boolean write) {
    TranslationEntry page = findEntryInTLB(pid, pageNumber);
    if(page == null) {
      handleTLBMiss(pageNumber);
      page = findEntryInTLB(pid, pageNumber);
    }

    /* System.out.println("fetchPage(): " + e);  */

    page.used = true;
    if(write){
      page.dirty = true; // ;)
    }
    return page;
  }

  protected boolean loadSections() {
    for (int i=0; i<numPages; i++){
      VMKernel.GenericPair<Integer, TranslationEntry> replaced = VMKernel.clockReplacement();
      SwapFile.Pair key = swapFile.new Pair(pid, i);
      VMKernel.pageTable.put(key, new TranslationEntry(i,replaced.val2.ppn,true,false,false,false));
    }

    for (int s=0; s<coff.getNumSections(); s++) {
      CoffSection section = coff.getSection(s);

      Lib.debug(dbgProcess, "\tinitializing " + section.getName()
          + " section (" + section.getLength() + " pages)");  

      for (int i=0; i<section.getLength(); i++) {
        int vpn = section.getFirstVPN()+i;    
        SwapFile.Pair key = swapFile.new Pair(pid, i);
        VMKernel.pageTable.get(key).readOnly = section.isReadOnly();
        section.loadPage(i, VMKernel.pageTable.get(key).ppn);
      }
    }
    return true;
  }

  private void handleTLBMiss(int vpn){
    //TODO: make getPage always return the correct page.
    /* int badAddress = Machine.processor().readRegister(Machine.processor().regBadVAddr);
       int pid = VMKernel.currentProcess().pid;
       int pageNumber = badAddress / pageSize;
       TranslationEntry page = null;

       SwapFile.Pair entry = swapFile.new Pair(pid, pageNumber);
       page = VMKernel.pageTable.get(entry);

       if(page == null) {
    //TODO:
    page = VMKernel.swapInPage(pid, pageNumber);
       }
       */
    TranslationEntry page = null;
    int pageNumber = pageFromAddress(vpn);
    /* System.out.println(pageNumber); */
    /* System.out.println(VMKernel.pageTable); */
    SwapFile.Pair pageTableKey = swapFile.new Pair(pid, pageNumber);

    page = VMKernel.pageTable.get(pageTableKey);

    if(page == null) {
      page = VMKernel.swapInPage(pid, pageNumber);
    }

    /* System.out.println("fetchPage(): " + e);  */

    page.used = true;
    /* System.out.println(VMKernel.pageTable); */
    System.out.println("TLB MISS");
    // TODO:
    // 1. check the global page table to see if we can find the page; if so,
    //    just use it
    // 2. Otherwise, use the swapfile and find the page, load into physical 
    //    memory, and add to global page table.
    //    maybe we can use swapInPage(TranslationEntry newEntry) function
    //    in VMKernel, which I just made up but has not been implemented
  }

  /**
   * A function to handle TLB misses. 
   */
  private void handleTLBMiss(){
    int badAddress = Machine.processor().readRegister(Machine.processor().regBadVAddr);
    handleTLBMiss(badAddress);
  }

  /**
   * Handle a user exception. Called by
   * <tt>UserKernel.exceptionHandler()</tt>. The
   * <i>cause</i> argument identifies which exception occurred; see the
   * <tt>Processor.exceptionZZZ</tt> constants.
   *
   * @param	cause	the user exception that occurred.
   */
  public void handleException(int cause){
    Processor processor = Machine.processor();

    switch (cause) {
      case exceptionTLBMiss:
        handleTLBMiss();
        break;
      default:
        System.out.println("cause: "+cause);
        super.handleException(cause);
        break;
    }
  }

  private static final int exceptionTLBMiss = 2; 
  private static final int pageSize = Processor.pageSize;
  private static final char dbgProcess = 'a';
  private static final char dbgVM = 'v';
  private static SwapFile swapFile = VMKernel.swapFile;
}
