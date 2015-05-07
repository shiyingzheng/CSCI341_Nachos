package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.vm.SwapFile.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
  /**
   * Allocate a new VM kernel.
   */
  public VMKernel() {
    super();
    System.out.println("VM kernel constructor");
  }

  /**
   * Initialize this kernel.
   */
  public void initialize(String[] args) {
    System.out.println("initialize");
    super.initialize(args);
    pageTable = new HashMap<Pair, TranslationEntry>();
    swapFile = new SwapFile();
    pageTableLock = new Lock();

    pageTableLock.acquire();
    for(int i=0; i<Machine.processor().getNumPhysPages(); i++) {
      SwapFile.Pair pageTableKey = swapFile.new Pair(i, 0);
      VMKernel.pageTable.put(pageTableKey, new TranslationEntry(0,i,false,false,false,false));
    }
    pageTableLock.release();

    for(int i=0; i<Machine.processor().getTLBSize(); i++) {
      Machine.processor().writeTLBEntry(i, new TranslationEntry());
    }
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
    System.out.println("run");
    super.run();
  }

  /**
   * Terminate this kernel. Never returns.
   */
  public void terminate() {
    System.out.println("terminate");
    super.terminate();
  }

  public static void contextSwitch(int pid){
    System.out.println("context switch");
    syncTables(pid);
    for(int i=0;i<Machine.processor().getTLBSize();i++){
      TranslationEntry entry = Machine.processor().readTLBEntry(i);
      entry.valid = false;
      Machine.processor().writeTLBEntry(i,entry);
    }
    // TODO: on context switch, sync tables, invalidate TLB entries
    // TODO: call syncTables() to sync tables
  }

  public static void syncTables(int pid){
    System.out.println("synctables");
    // TODO: sync entries in TLB with the page table
    pageTableLock.acquire(); //may have problems
    for(int i=0;i < Machine.processor().getTLBSize(); i++) {
      TranslationEntry entry = Machine.processor().readTLBEntry(i);
      SwapFile.Pair pageTableEntry = swapFile.new Pair(pid, entry.vpn);
      TranslationEntry pEntry = VMKernel.pageTable.get(pageTableEntry);

      if (pEntry != null){
        pEntry.valid = entry.valid;
        pEntry.readOnly = entry.readOnly;
        pEntry.used = entry.used;
        pEntry.dirty = entry.dirty;
        VMKernel.pageTable.put(pageTableEntry, pEntry);
      }

    } 
    pageTableLock.release(); //may have problems
  }

  // Swap a page in from disk to physical memory
  public static TranslationEntry swapInPage(int pid, int vpn){
    System.out.println("swap in page");
    // TODO: sync the translation entries in the page table with the ones in TLB 
    /* System.out.println("pid: "+pid); */
    /* System.out.println("vpn: "+vpn); */
    GenericPair<Integer,TranslationEntry> pair = clockReplacement();
    TranslationEntry replacedPage = pair.val2;
    TranslationEntry replacedTLBPage = null;

    for(int i=0;i<Machine.processor().getTLBSize();i++){
      TranslationEntry page = Machine.processor().readTLBEntry(i);
      /* System.out.println("page: "+page); */
      if(page.valid && page.vpn == replacedPage.vpn && replacedPage.valid){
        /* System.out.println("page already in tlb: "+page); */
        replacedTLBPage = page;
        break;
      }
    }
    if(replacedTLBPage == null){
      replacedTLBPage = TLBEntryReplacement();
    }
    SwapFile.Pair keyToRemove = swapFile.new Pair(pair.val1,pair.val2.vpn);
    //SwapFile.Pair keyToRemove2 = swapFile.new Pair(pair.val1,pair.val2.vpn);
    //System.out.println("HashCodes "+keyToRemove.hashCode() + " " + keyToRemove2.hashCode());
    //System.out.println("Pair to remove: " + keyToRemove);
    TranslationEntry removed = VMKernel.pageTable.remove(keyToRemove);
    //System.out.println("Removed: "+ removed);
    if(replacedPage.valid){
      swapFile.swapPageOut(pair.val1,replacedPage.vpn,replacedPage.ppn);
    }
    

    //pageTableLock.acquire(); // so far we are only using swapInPage in VMProcess.handleTLBMiss, 
    // which already acquires the lock
    swapFile.swapPageIn(pid, vpn, replacedPage.ppn);
    TranslationEntry entry = new TranslationEntry(vpn,replacedPage.ppn,true, false, true, false);

    int tlbIndex = TLBEntryReplacementIndex();
    Machine.processor().writeTLBEntry(tlbIndex, entry);
    VMKernel.pageTable.put(swapFile.new Pair(pid,vpn),entry);

    //pageTableLock.release();

    /* return replacedPage; // probably wrong....*/
    return entry; // maybe not wrong....
  }
  // returns a pair that represents the pid and the translation entry of the page to replace
  public static GenericPair<Integer, TranslationEntry> clockReplacement(){
    //System.out.println(curPid + " clock");
    //System.out.println(VMKernel.pageTable);
    //pageTableLock.acquire();
    if(VMKernel.debug){
      System.out.println("clock replacement");
    }
    Iterator<SwapFile.Pair> itr = VMKernel.pageTable.keySet().iterator();

    while(true) {
      if(!itr.hasNext()) {
        itr = VMKernel.pageTable.keySet().iterator();
      } 

      SwapFile.Pair pidVpn = itr.next();
      TranslationEntry entry = VMKernel.pageTable.get(pidVpn);
      /* System.out.println("entry to replace:"+entry); */
      if(entry.vpn != pidVpn.vPageNum){
        /* System.out.println("Something's wrong in clockReplacement"); */
      }
      //System.out.println("Should replace "+pidVpn);
      //System.out.println("pageTable "+ VMKernel.pageTable);
      //int curPid = VMKernel.currentProcess().pid;

      if(entry.used && entry.valid) {
        entry.used = false;
      } else {
        //pageTableLock.release();
        return new GenericPair<Integer,TranslationEntry>(pidVpn.pid,entry);
      }
    }
  }
  public static class GenericPair<E,T>{
    E val1;
    T val2;
    public GenericPair(E x, T y){
      this.val1 = x;
      this.val2 = y;
    }
  }

  public static void printTLB(){
    System.out.println("print tlb!");
    for(int i=0; i<Machine.processor().getTLBSize(); i++) {
      TranslationEntry entry = Machine.processor().readTLBEntry(i);
      System.out.print("TLB entry: ");
      System.out.println(entry);
    }
  }
  public static TranslationEntry TLBEntryReplacement(){
    // randomly pick an entry to replace
    System.out.println("tlb entry replacement");
    Random r = new Random();
    int randIndex = r.nextInt(Machine.processor().getTLBSize());
    TranslationEntry entry = Machine.processor().readTLBEntry(randIndex);
    return entry;
  }

  public static int TLBEntryReplacementIndex(){
    // randomly pick an entry to replace
    System.out.println("tlb entry replacement index");
    Random r = new Random();
    int randIndex = r.nextInt(Machine.processor().getTLBSize());
    return randIndex;
  }

  private static VMProcess dummy1 = null;

  private static final char dbgVM = 'v';

  private int clockPos = 0;

  /* 
   * A global page table that contains pages that are currently in physical 
   * memory, which can have pages that do not belong to the current process.
   */
  public static HashMap<Pair, TranslationEntry> pageTable; 

  /* A lock for the global page table. */
  public static Lock pageTableLock;
  public static boolean debug = false;
  public static SwapFile swapFile;
}

