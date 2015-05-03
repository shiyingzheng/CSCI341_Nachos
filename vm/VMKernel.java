package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.vm.SwapFile.Pair;
import java.util.HashMap;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
  /**
   * Allocate a new VM kernel.
   */
  public VMKernel() {
    super();
  }

  /**
   * Initialize this kernel.
   */
  public void initialize(String[] args) {
    super.initialize(args);
    pageTable = new HashMap<Pair, TranslationEntry>();
  }

  /**
   * Test this kernel.
   */	
  public void selfTest() {
    super.selfTest();
  }

  /**
   * Start running user programs.
   */
  public void run() {
    super.run();
  }

  /**
   * Terminate this kernel. Never returns.
   */
  public void terminate() {
    super.terminate();
  }

  public void contextSwitch(){
    // TODO: on context switch, sync tables, invalidate TLB entries
    // TODO: call syncTables() to sync tables
  }

  private void syncTables(){
    // TODO: sync entries in TLB with the page table
  }

  // Swap a page in from disk to physical memory
  public static TranslationEntry swapInPage(){
    // TODO: sync the translation entries in the page table with the ones in TLB 
    TranslationEntry replacedPage = clockReplacement();
    // TODO: swap page into physical memory using SwapFile functions
    // TODO: put new entry in page table and TLB
    return replacedPage;
  }

  private static TranslationEntry clockReplacement(){
    // TODO: implement clock replacement algorithm here
    return null;
  }

  private TranslationEntry TLBEntryReplacement(){
    // randomly pick an entry to replace
    return null;
  }

  // dummy variables to make javac smarter
  private static VMProcess dummy1 = null;

  private static final char dbgVM = 'v';

  /* 
  * A global page table that contains pages that are currently in physical 
  * memory, which can have pages that do not belong to the current process.
  */
  public static HashMap<Pair, TranslationEntry> pageTable; 

  /* A lock for the global page table. */
  public static Lock pageTableLock;

  public static SwapFile swapFile = new SwapFile();
}

