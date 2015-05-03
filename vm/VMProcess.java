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

\
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
    // sync the TLB with entries in page table, invalidate all entries in TLB
    // we can call VMKernel contextSwitch() for this
  }

  
  
  /**
   * Restore the state of this process after a context switch. Called by
   * <tt>UThread.restoreState()</tt>.
   */
  public void restoreState() {
    super.restoreState();
  }

  
  
  // This is directly copied from UserProcess but we need to change it
  /**
   * Transfer data from this process's virtual memory to the specified array.
   * This method handles address translation details. This method must
   * <i>not</i> destroy the current process if an error occurs, but instead
   * should return the number of bytes successfully copied (or zero if no
   * data could be copied).
   *
   * @param vaddr the first byte of virtual memory to read.
   * @param data  the array where the data will be stored.
   * @param offset  the first byte to write in the array.
   * @param length  the number of bytes to transfer from virtual memory to
   *      the array.
   * @return  the number of bytes successfully transferred.
   */
  public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    int amount = 0;
    
    byte[] memory = Machine.processor().getMemory();
    
    if (vaddr < 0 || vaddr >= pageSize * numPages){
      System.err.println("ERROR: Memory address out of range");
      return amount;
    }
    
    UserProcess.readWriteLock.acquire();
    amount = Math.min(length, memory.length-vaddr);
    int curLoc = 0;
    int rem = amount;
    
    int pageOffset = offsetFromAddress(vaddr);
    int pageNumber = pageFromAddress(vaddr);
    
    // try to fetch a page from the TLB
    //TranslationEntry page = machine.Processor.readTLBEntry(pageNumber);
    // If a TLB miss happens, fetch the page from the global page table,
    // then bring that page into the TLB
    
    // handleTLBMiss() will be called
    
    ranslationEntry page = fetchPage(pageNumber);
    
    
    for (int i = 0; rem > 0; i++) {
    	// copy Math.min(pageSize, rem) number of bytes from memory at ppn 
    	// to data at offset + curLoc
    	if (pageOffset != 0) {
    		// different from UserProcess
    		TranslationEntry nextPage = machine.Processor.readTLBEntry(pageNumber+i);
    		System.arraycopy(memory, page.ppn * pageSize + pageOffset, data, offset+curLoc,
    			Math.min(pageSize - pageOffset, rem));
    			curLoc += Math.min(pageSize - pageOffset, rem);
    			rem -= Math.min(pageSize - pageOffset, rem);
    			pageOffset = 0;
    	} else {
    		// different from UserProcess
    		System.arraycopy(memory,pageTable[pageNumber+i].ppn * pageSize + pageOffset,
    			data, offset+curLoc, Math.min(pageSize - pageOffset, rem));
    	}
    }
    UserProcess.readWriteLock.release();
    
    return amount;
   
    //for (int i = 0; rem > 0; i++){
      // copy Math.min(pageSize, rem) number of bytes from memory at ppn 
      // to data at offset + curLoc
    //    if(pageOffset != 0){
          // CHANGE HERE!!!
      //    System.arraycopy(memory,pageTable[pageNumber+i].ppn * pageSize + pageOffset,
     //         data, offset+curLoc, Math.min(pageSize - pageOffset, rem));
     //     curLoc += Math.min(pageSize - pageOffset, rem);
    //      rem -= Math.min(pageSize - pageOffset, rem);
    //      pageOffset = 0;
    //    }
    //    else{ 
          // CHANGE HERE!!!
     //     System.arraycopy(memory, pageTable[pageNumber+i].ppn * pageSize, 
     //     data, offset+curLoc, Math.min(pageSize, rem));
      // set used bit
      // CHANGE HERE!!! we need to do it in this lab
      //pageTable[pageNumber+i].used = true; 
      // increment current location in data by page size
   //   curLoc += Math.min(pageSize, rem);
      // decrement remaining number of bytes by page size
     // rem -= Math.min(pageSize,rem);
    //    }
  //  }
  //  UserProcess.readWriteLock.release();

   
  }

  // This is directly copied from UserProcess but we need to change it
  /**
   * Transfer data from the specified array to this process's virtual memory.
   * This method handles address translation details. This method must
   * <i>not</i> destroy the current process if an error occurs, but instead
   * should return the number of bytes successfully copied (or zero if no
   * data could be copied).
   *
   * @param vaddr the first byte of virtual memory to write.
   * @param data  the array containing the data to transfer.
   * @param offset  the first byte to transfer from the array.
   * @param length  the number of bytes to transfer from the array to
   *      virtual memory.
   * @return  the number of bytes successfully transferred.
   */
  public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    int page = pageFromAddress(vaddr);
    int pageOffset = offsetFromAddress(vaddr);

    // CHANGE HERE!!!
    if (!(pageTable[page].valid == true && vaddr+length <= numPages*pageSize && page < numPages && pageTable[page].readOnly == false)){
      System.out.println("MOOSE AND APPLES");
  handleExit(1);
    }    

    byte[] memory = Machine.processor().getMemory();

    if (vaddr < 0 || vaddr >= memory.length)
      return 0;  

    // amount is the number of bytes we need to copy over to memory
    int amount = Math.min(length, memory.length-vaddr);
    // curLoc is the current start location in data that we need to copy from
    int curLoc = 0;
    // rem is the remaining number of bytes we need to copy 
    int rem = amount;
    int pageNumber = pageFromAddress(vaddr);
    UserProcess.readWriteLock.acquire();
    for (int i = 0; rem > 0; i++){
      // copy Math.min(pageSize, rem) number of bytes from data at offset + curLoc
      // to memory at ppn
         if(pageOffset != 0){
          // CHANGE HERE!!!
          System.arraycopy(data, offset + curLoc, memory, pageTable[pageNumber+i].ppn * pageSize + pageOffset,
               Math.min(pageSize - pageOffset, rem));
          curLoc += Math.min(pageSize - pageOffset, rem);
          rem -= Math.min(pageSize - pageOffset, rem);
          pageOffset = 0;
        }
        else{ 
          // CHANGE HERE!!!
          System.arraycopy(data, offset+curLoc, memory,  pageTable[pageNumber+i].ppn * pageSize, Math.min(pageSize, rem));
      // set used bit // CHANGE HERE!!! we need to set used and dirty bit anyway ;)
      /* pageTable[pageNumber+i].used = true; */
      // increment current location in data by page size
      curLoc += Math.min(pageSize,rem);
      // decrement remaining number of bytes by page size
      rem -= Math.min(pageSize,rem);
        }
    }   
    UserProcess.readWriteLock.release();

    return amount;
  }

  // This is directly copied from UserProcess but we need to change it
  /**
   * Allocates memory for this process, and loads the COFF sections into
   * memory. If this returns successfully, the process will definitely be
   * run (this is the last step in process initialization that can fail).
   *
   * @return  <tt>true</tt> if the sections were successfully loaded.
   */
  protected boolean loadSections() {
    if (numPages > Machine.processor().getNumPhysPages()) {
      coff.close();
      Lib.debug(dbgProcess, "\tinsufficient physical memory");
      return false;
    }  

    // CHANGE HERE!!!
    pageTable = new TranslationEntry[numPages];
    UserKernel.pageListLock.acquire();
    for (int i=0; i<numPages; i++){
      if(UserKernel.freePageList.size() == 0) {
    UserKernel.pageListLock.release();
        handleExit(1);
      }
      int pageNumber = UserKernel.freePageList.removeFirst();
      // CHANGE HERE!!!
      pageTable[i] = new TranslationEntry(i,pageNumber,true,false,false,false);
    }

    UserKernel.pageListLock.release();

    // load sections
    for (int s=0; s<coff.getNumSections(); s++) {
      CoffSection section = coff.getSection(s);

      Lib.debug(dbgProcess, "\tinitializing " + section.getName()
          + " section (" + section.getLength() + " pages)");  

      for (int i=0; i<section.getLength(); i++) {
        int vpn = section.getFirstVPN()+i; 
        // CHANGE HERE!!!   
        pageTable[vpn].readOnly = section.isReadOnly();
        section.loadPage(i, pageTable[vpn].ppn);
      }
    }

    return true;
  }

  // This is directly copied from UserProcess but we need to change it
  /**
   * Release any resources allocated by <tt>loadSections()</tt>.
   */
  protected void unloadSections() {
    UserKernel.pageListLock.acquire();
    for (int i=0; i<numPages; i++){
      // CHANGE HERE!!!
      if(pageTable[i] ==null) { 
        /* UserKernel.pageListLock.release(); */
        /* handleExit(1); */
      }
      else{
        //CHANGE HERE!!!
        int paddr = pageTable[i].ppn; 
        UserKernel.freePageList.add(paddr);
      }
    }
    UserKernel.pageListLock.release();
  }  
  
  
  // try to fetch a page from the TLB
    //TranslationEntry page = machine.Processor.readTLBEntry(pageNumber);
    // If a TLB miss happens, fetch the page from the global page table,
    // then bring that page into the TLB
    
    // handleTLBMiss() will be called
  
    // KEEP IN MIND THAT TLB SIZE IS ONLY 4!
    
  private TranslationEntry fetchPage(int pageNumber) {
  	  TranslationEntry page = null;
  	  try {
  	  	 page = machine.Processor.readTLBEntry(pageNumber);
  	  } catch (Exception e) {
  	  	 System.out.println("fetchPage(): " + e); 
  	  	 
  	  } 
  		 
  	  
  }
  
  /*
  nachos 5.0j initializing... config interrupt timer processor console user-check grader
Testing the console device. Typed characters
will be echoed until q is typed.
q
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
HERE I AMUserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Pandas
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Of the red variety
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 3, page offset = 40
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
PANDAS!
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Unable to create file
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
PANDAS!
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Unable to create file
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
PANDAS!
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Unable to create file
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
PANDAS!
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Unable to create file
UserProcess.readVirtualMemory()
Page number = 11, page offset = 1008
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
PANDAS!
UserProcess.readVirtualMemory()
Page number = 4, page offset = 0
Unable to create file
Machine halting!

Ticks: total 10056397, kernel 10050250, user 6147
Disk I/O: reads 0, writes 0
Console I/O: reads 1, writes 186
Paging: page faults 0, TLB misses 0
Network I/O: received 0, sent 0
*/
  

  /**
  * A function to handle TLB misses. 
  */
  private void handleTLBMiss(){
    int badAddress = Machine.processor().readRegister(Machine.processor().regBadVAddr);
    // 1. check the global page table to see if we can find the page; if so,
    //    just use it
    // 2. Otherwise, use the swapfile and find the page, load into physical 
    //    memory, and add to global page table.
    //    maybe we can use swapInPage(TranslationEntry newEntry) function
    //    in VMKernel, which I just made up but has not been implemented
  }

  /**
   * Handle a user exception. Called by
   * <tt>UserKernel.exceptionHandler()</tt>. The
   * <i>cause</i> argument identifies which exception occurred; see the
   * <tt>Processor.exceptionZZZ</tt> constants.
   *
   * @param	cause	the user exception that occurred.
   */
  public void handleException(int cause) {
    Processor processor = Machine.processor();

    switch (cause) {
      case exceptionTLBMiss:
        handleTLBMiss();
      default:
        super.handleException(cause);
        break;
    }
  }

  private static final int exceptionTLBMiss = 2; 
  private static final int pageSize = Processor.pageSize;
  private static final char dbgProcess = 'a';
  private static final char dbgVM = 'v';
}
