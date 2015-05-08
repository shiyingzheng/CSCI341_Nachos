package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Set;

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
    System.out.println("START VMProcess init, pid " + pid);
    lockOnBeforeSwitch = false;
    System.out.println("END VMProcess init, pid " + pid);
  }

  /**
   * Save the state of this process in preparation for a context switch.
   * Called by <tt>UThread.saveState()</tt>.
   */
  public void saveState() {
    System.out.println("START save state, pid " + pid);
    /* super.saveState(); */
    if (VMKernel.pageTableLock.isHeldByCurrentThread()){
      System.out.println("release lock in save state");
      VMKernel.pageTableLock.release();
      lockOnBeforeSwitch = true;
    }
    //VMKernel.contextSwitch(pid);
    System.out.println("END save state, pid " + pid);
    // sync the TLB with entries in page table, invalidate all entries in TLB
    // we can call VMKernel contextSwitch() for this
  }



  /**
   * Restore the state of this process after a context switch. Called by
   * <tt>UThread.restoreState()</tt>.
   */
  public void restoreState() {
    System.out.println("START restore state, pid " +pid);
    System.out.println("cur thread: "+(UThread) KThread.currentThread());
    /* super.restoreState(); */
    VMKernel.contextSwitch(pid);
    if (lockOnBeforeSwitch){
      System.out.println("acquire lock in restore state");
      lockOnBeforeSwitch = false;
      VMKernel.pageTableLock.acquire();
    }
    System.out.println("END restore state, pid " + pid);
  }

  private TranslationEntry findEntryInTLB(int pid, int pageNumber){
    System.out.println("START find entry in tlb for pid " + pid + " for page " + pageNumber);
    VMKernel.printTLB();
    TranslationEntry page = null;
    for(int i=0; i<Machine.processor().getTLBSize(); i++) {
      TranslationEntry entry = Machine.processor().readTLBEntry(i);
      //System.out.println("TLB entry "+i+":"+entry);
      if(entry.valid && pageNumber == entry.vpn) {
        page = entry;
        break;
      }
    }
    if (page == null){
      System.out.println("did not find page " + pageNumber);
    }
    else{
      System.out.println("found page " + pageNumber);
    }
    System.out.println("END find entry in tlb for pid " + pid + " for page " + pageNumber);
    return page;
  }

  // try to fetch a page from the TLB
  // If a TLB miss happens, fetch the page from the global page table,
  // then bring that page into the TLB
  // handleTLBMiss() will be called
  public TranslationEntry getPage(int pid, int pageNumber, boolean write) {
    System.out.println("START get page pid " + pid + " page number " + pageNumber + " write? " + write);
    TranslationEntry page = findEntryInTLB(pid, pageNumber);
    if(page == null) {
      page = handleTLBMiss(pageNumber);
      //page = findEntryInTLB(pid, pageNumber);
    }

    /* System.out.println("fetchPage(): " + e);  */

    page.used = true;
    if(write){
      page.dirty = true; // ;)
    }
    System.out.println("END get page pid " + pid + " page number " + pageNumber + " write? " + write);
    return page;
  }

  protected boolean loadSections() {
    System.out.println("START load sections, pid " + pid);
    for (int i=0; i<numPages; i++){
      /* System.out.println("i is "+i); */
      VMKernel.pageTableLock.acquire();
      VMKernel.GenericPair<Integer, TranslationEntry> replaced = VMKernel.clockReplacement();
      Pair key = new Pair(pid, i);
      /*
      SwapFile.Pair key2 = swapFile.new Pair(pid, i);
      SwapFile.Pair key3 = swapFile.new Pair(pid, i+1);
      System.out.println("equals:" + key.equals(key3) + " hashcode:" + (key.hashCode()== key3.hashCode()));
      */
      System.out.println("AH: "+replaced.val2);
      Pair replacedEntry = new Pair(replaced.val1,replaced.val2.vpn);
      /* System.out.println("Inserting "+key); */
      /* System.out.println("Replacing "+replacedEntry); */
      //System.out.println("equals:" + key.equals(replacedEntry) + " hashcode:" + (key.hashCode()== replacedEntry.hashCode()));
      System.out.println("Entry to be replaced: "+replacedEntry);
      TranslationEntry removed = VMKernel.pageTable.remove(replacedEntry);

      if(removed == null){
        System.out.println("THIS IS SO BAD");
        /* System.out.println("Page table is "+ VMKernel.pageTable); */
      }
      VMKernel.pageTable.put(key, new TranslationEntry(i,replaced.val2.ppn,true,false,true,false)); 
      
      System.out.println(VMKernel.pageTable);
      VMKernel.pageTableLock.release();
    }

    for (int s=0; s<coff.getNumSections(); s++) {
      CoffSection section = coff.getSection(s);

      Lib.debug(dbgProcess, "\tinitializing " + section.getName()
          + " section (" + section.getLength() + " pages)");  

      for (int i=0; i<section.getLength(); i++) {
        /* System.out.println("i is "+i); */
        int vpn = section.getFirstVPN()+i;    
        Pair key = new Pair(pid, i);
        //System.out.println(VMKernel.pageTable); //BUG we have way too many things in page table
        TranslationEntry page = getPage(pid,vpn,false);
        VMKernel.pageTableLock.acquire();
        page.readOnly = section.isReadOnly();
        section.loadPage(i, page.ppn);
        VMKernel.pageTableLock.release();
      }
    }
    System.out.println("END load sections, pid " + pid);
    return true;
  }

  private TranslationEntry handleTLBMiss(int vpageNum){
    System.out.println("START handle tlb miss pid " + pid + " vaddr " + vpageNum);
    VMKernel.printTLB();
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
    //System.out.println("hi");
    // we need to know if the page is coming from memory or disk
    /* int pageNumber = pageFromAddress(vaddr); */
    TranslationEntry maybeEntryIsThere = findEntryInTLB(pid, vpageNum);
    if (maybeEntryIsThere != null){
      return maybeEntryIsThere;
    }

    boolean fromDisk = false;
    VMKernel.pageTableLock.acquire();

    TranslationEntry page = null;
    Pair pageTableKey = new Pair(pid, vpageNum);

    page = VMKernel.pageTable.get(pageTableKey);
    //System.out.println("This should be null: "+page);

    /* System.out.println(VMKernel.pageTable); */
    if(page == null || !page.valid) {
      VMKernel.GenericPair pair = VMKernel.clockReplacement();
      TranslationEntry entry = (TranslationEntry) pair.val2;
      System.out.println("replacement: "+entry);
      invalidateTLBEntry(entry);
      int pid = (Integer) pair.val1;
      Pair key = new Pair(pid, entry.vpn);
      System.out.println("replaced pid: "+pid+ " replaced vpn: "+entry.vpn);
      TranslationEntry removed = (TranslationEntry) VMKernel.pageTable.remove(key);
      System.out.println("remove key: "+key);
      System.out.println("removed: "+removed);
      page = new TranslationEntry(vpageNum, entry.ppn, true, false, true, false);
      VMKernel.pageTable.put(new Pair(pid, vpageNum), page);
      int tlbIndex = VMKernel.TLBEntryReplacementIndex();
      Machine.processor().writeTLBEntry(tlbIndex, page);
    } else {
      System.out.println("from the page table");
      System.out.println(VMKernel.pageTable);
      int tlbIndex = VMKernel.TLBEntryReplacementIndex();
      Machine.processor().writeTLBEntry(tlbIndex, page);
    }

    // may need to do this in swapInPage?
    page.used = true;

    VMKernel.pageTableLock.release();
    /* if(!fromDisk) { */
    /*   int index = VMKernel.TLBEntryReplacementIndex(); */
    /*   Machine.processor().writeTLBEntry(index, page); */
      /* VMKernel.syncTables(pid); */
    /* } */

    /* System.out.println("fetchPage(): " + e);  */

    /* System.out.println(VMKernel.pageTable); */
    /* System.out.println("TLB MISS"); */
    /* System.out.println("page: "+page); */
    VMKernel.syncTables(this.pid);
    VMKernel.printTLB();
    // TODO:
    // 1. check the global page table to see if we can find the page; if so,
    //    just use it
    // 2. Otherwise, use the swapfile and find the page, load into physical 
    //    memory, and add to global page table.
    //    maybe we can use swapInPage(TranslationEntry newEntry) function
    //    in VMKernel, which I just made up but has not been implemented
    //System.out.println("lock release in handle tlb miss");
    //System.out.println("bye");
    System.out.println("END handle tlb miss pid " + pid + " vpage " + vpageNum);
    return page;
  }

  private void invalidateTLBEntry(TranslationEntry page) {
    for(int i=0;i<Machine.processor().getTLBSize();i++){
      TranslationEntry entry = Machine.processor().readTLBEntry(i);
      /* System.out.println("page: "+page); */
      if(entry.valid && entry.vpn == page.vpn && page.valid){
        /* System.out.println("page already in tlb: "+page); */
        entry.valid = false;
        Machine.processor().writeTLBEntry(i, entry);
        break;
      }
    }
  }

  /**
   * A function to handle TLB misses. 
   */
  private TranslationEntry handleTLBMiss(){
    System.out.println("START handle tlb miss no arg, pid " + pid);
    int badAddress = pageFromAddress(Machine.processor().readRegister(Machine.processor().regBadVAddr));
    System.out.println("END handle tlb miss no arg, pid addr" + Machine.processor().readRegister(Machine.processor().regBadVAddr));
    System.out.println("END handle tlb miss no arg, pid addr" + badAddress);
    System.out.println("END handle tlb miss no arg, pid " + pid);
    return handleTLBMiss(badAddress);
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
    System.out.println("START handle exception, pid " + pid + " cause " + cause);
    Processor processor = Machine.processor();

    switch (cause) {
      case exceptionTLBMiss:
        handleTLBMiss();
        break;
      default:
        super.handleException(cause);
        break;
    }
    System.out.println("END handle exception, pid " + pid + " cause " + cause);
  }

  protected void unloadSections() {
    System.out.println("START unload sections, pid " + pid);
    VMKernel.pageTableLock.acquire();
    Set<Pair> keySet = VMKernel.pageTable.keySet();
    Object[] keys = keySet.toArray();
    for(int i=0; i<keys.length; i++) {
      Pair key =  (Pair) keys[i];
      if(key.pid == this.pid) {
        TranslationEntry entry = VMKernel.pageTable.get(key);
        VMKernel.pageTable.put(new Pair(0,0), new TranslationEntry(SILLY, entry.ppn, false, false, false, false));
        //TODO Swap
      }
    }
    VMKernel.pageTableLock.release();
    System.out.println("END unload sections, pid " + pid);
  }    

  private static final int exceptionTLBMiss = 2; 
  private static final int pageSize = Processor.pageSize;
  private static final char dbgProcess = 'a';
  private static final char dbgVM = 'v';
  private boolean lockOnBeforeSwitch;
  private final int SILLY = 69;
}
