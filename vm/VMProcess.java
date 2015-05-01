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

  /**
   * Initializes page tables for this process so that the executable can be
   * demand-paged.
   *
   * @return	<tt>true</tt> if successful.
   */
  protected boolean loadSections() {
    return super.loadSections();
  }

  /**
   * Release any resources allocated by <tt>loadSections()</tt>.
   */
  protected void unloadSections() {
    super.unloadSections();
  }    

  /**
  * A function to handle TLB misses. 
  */
  private void handleTLBMiss(){
    int badAddress = Machine.Processor().readRegister(Processor.regBadVaddr);
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
      case Machine.Processor.exceptionTLBMiss:
        handleTLBMiss();
      default:
        super.handleException(cause);
        break;
    }
  }

  private static final int exceptionTLBMiss = 10; //hopefully this number works
  private static final int pageSize = Processor.pageSize;
  private static final char dbgProcess = 'a';
  private static final char dbgVM = 'v';
}
