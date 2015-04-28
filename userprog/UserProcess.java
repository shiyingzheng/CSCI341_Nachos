package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Iterator;

/* Shiying Zheng, Ben Stern, Joey Gonzales-Dones, Katherine Chan, Jacob Chae */

/*TODO: 1. test all of this shit; 
2. when we exit ??? passing your unwanted kid to your parent i.e. giving it to its granparents. poor grandparents
3. synchronization shit ??? (might conflict with 2)
4. exit not working and page faults
5. maybe other stuff
*/


/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
  /**
   * Allocate a new process.
   */
  public UserProcess() {
    fileOpenTable = new HashMap<Integer, OpenFile>();
    filenameOpenTable = new HashMap<String, Integer>();
    filenameCloseTable = new HashMap<String, Integer>();
    readOffsetTable = new HashMap<Integer, Integer>();
    writeOffsetTable = new HashMap<Integer, Integer>();
    children = new ArrayList<UserProcess>();
    

    UserKernel.pidLock.acquire();
    pid = UserKernel.currPid++;
    //if parent is not root, we should set ppid to actual parent in handleExec
    ppid = 1; 
    UserKernel.pidLock.release();

    UserKernel.processTableLock.acquire();
    UserKernel.processStatusTable.put(pid, null);
    UserKernel.processTableLock.release();

    setup();
    nextFileDescriptor=3;
  }

  private void setup(){
    OpenFile stdin = UserKernel.console.openForReading();
    OpenFile stdout = UserKernel.console.openForWriting();

    // initialize in the openFileTable
    fileOpenTable.put(0, stdin);
    fileOpenTable.put(1, stdout);

    String stdinName = stdin.getName();
    String stdoutName = stdout.getName();

    // initialize in the filenameOpenTable
    filenameOpenTable.put(stdinName, 0);
    filenameOpenTable.put(stdoutName, 1);

    // setup stdin and stdout
    UserKernel.fileListLock.acquire();
    if(!UserKernel.openFileList.containsKey(stdinName)) {
      UserKernel.openFileList.put(stdinName, new ArrayList<Integer>(Arrays.asList(1, 0)));
    } else {
      ArrayList<Integer> fileEntry = UserKernel.openFileList.get(stdinName);
      int openNum = fileEntry.get(0);
      fileEntry.set(0, openNum + 1);
    }

    if(!UserKernel.openFileList.containsKey(stdoutName)) {
      UserKernel.openFileList.put(stdoutName, new ArrayList<Integer>(Arrays.asList(1, 0)));
    } else {
      ArrayList<Integer> fileEntry = UserKernel.openFileList.get(stdoutName);
      int openNum = fileEntry.get(0);
      fileEntry.set(0, openNum + 1);
    }
    UserKernel.fileListLock.release();

    // initialize in the offsetTable
    readOffsetTable.put(0, 0);
    readOffsetTable.put(1, 0);

    // initialize in the writeOffsetTable
    writeOffsetTable.put(0, 0);
    writeOffsetTable.put(1, 0);
  }

  /**
   * Allocate and return a new process of the correct class. The class name
   * is specified by the <tt>nachos.conf</tt> key
   * <tt>Kernel.processClassName</tt>.
   *
   * @return	a new process of the correct class.
   */
  public static UserProcess newUserProcess() {
    return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
  }

  /**
   * Execute the specified program with the specified arguments. Attempts to
   * load the program, and then forks a thread to run it.
   *
   * @param	name	the name of the file containing the executable.
   * @param	args	the arguments to pass to the executable.
   * @return	<tt>true</tt> if the program was successfully executed.
   */
  public boolean execute(String name, String[] args) {
    /* System.out.println(name); */
    /* for(int i=0;i<args.length-1;i++) { */
    /*   System.out.println(args[i]); */
    /* } */
    if (!load(name, args))
      return false;

    new UThread(this).setName(name).fork();    

    return true;
  }

  /**
   * Save the state of this process in preparation for a context switch.
   * Called by <tt>UThread.saveState()</tt>.
   */
  public void saveState() {
  }

  /**
   * Restore the state of this process after a context switch. Called by
   * <tt>UThread.restoreState()</tt>.
   */
  public void restoreState() {
    Machine.processor().setPageTable(pageTable);
  }

  /**
   * Extract the page number component from a 32-bit address.
   *
   * @param address the 32-bit address.
   * @return  the page number component of the address.
   */
  private static int pageFromAddress(int address) {
    return (int) (((long) address & 0xFFFFFFFFL) / pageSize);
  }

  private static int offsetFromAddress(int address) {
    return (int) (((long) address & 0xFFFFFFFFL) % pageSize);
  }

  /**
   * Read a null-terminated string from this process's virtual memory. Read
   * at most <tt>maxLength + 1</tt> bytes from the specified address, search
   * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
   * without including the null terminator. If no null terminator is found,
   * returns <tt>null</tt>.
   *
   * @param	vaddr	the starting virtual address of the null-terminated
   *			string.
   * @param	maxLength	the maximum number of characters in the string,
   *				not including the null terminator.
   * @return	the string read, or <tt>null</tt> if no null terminator was
   *		found.
   */
  public String readVirtualMemoryString(int vaddr, int maxLength) {
    if(!(maxLength >= 0)){
	handleExit(1);
    }    

    byte[] bytes = new byte[maxLength+1]; 
    /* System.out.println("num bytes " + bytes.length);  */

    int bytesRead = readVirtualMemory(vaddr, bytes);   
    /* System.out.println("ytes read: "+bytesRead); */

    for (int length=0; length<bytesRead; length++) {
      /* System.out.println("HERE IAM"); */
      /* System.out.println("length: " +length); */
      /* System.out.println("bytesRead: " +bytesRead); */
      /* System.out.println("I MA A CHAR: "+(char)bytes[length]); */
      if (bytes[length] == 0){
        return new String(bytes, 0, length);
      }
    }  

    return null;
  }

  /**
   * Transfer data from this process's virtual memory to all of the specified
   * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
   *
   * @param	vaddr	the first byte of virtual memory to read.
   * @param	data	the array where the data will be stored.
   * @return	the number of bytes successfully transferred.
   */
  public int readVirtualMemory(int vaddr, byte[] data) {
    return readVirtualMemory(vaddr, data, 0, data.length);
  }

  /**
   * Transfer data from this process's virtual memory to the specified array.
   * This method handles address translation details. This method must
   * <i>not</i> destroy the current process if an error occurs, but instead
   * should return the number of bytes successfully copied (or zero if no
   * data could be copied).
   *
   * @param	vaddr	the first byte of virtual memory to read.
   * @param	data	the array where the data will be stored.
   * @param	offset	the first byte to write in the array.
   * @param	length	the number of bytes to transfer from virtual memory to
   *			the array.
   * @return	the number of bytes successfully transferred.
   */
  public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    int pageOffset = offsetFromAddress(vaddr);
    int page = pageFromAddress(vaddr);
    /* System.out.println("READING: vaddr: " + vaddr + " data: " + data + " offset: " + offset + " length: " + length + " page " + pageFromAddress(vaddr) + " maxPages: " + numPages + "physical page: " + pageTable[page].ppn * pageSize + pageOffset); */
    if (!(pageTable[page].valid == true && vaddr+length <= numPages*pageSize && page < numPages )){
      System.out.println("PANDAS AND APPLES");
	handleExit(1);
    }   

    byte[] memory = Machine.processor().getMemory();

    // for now, just assume that virtual addresses equal physical addresses
    //not anymore!
    if (vaddr < 0 || vaddr >= pageSize * numPages){
      System.out.println("gah");
      handleExit(1);
    }
    int amount = Math.min(length, memory.length-vaddr);

    int curLoc = 0;
    int rem = amount;
    int pageNumber = pageFromAddress(vaddr);
    /* int pageOffset = offsetFromAddress(vaddr); */
    /* UserProcess.readWriteLock.acquire(); */
    for (int i = 0; i < amount/pageSize + 1; i++){
      // copy Math.min(pageSize, rem) number of bytes from memory at ppn 
      // to data at offset + curLoc
	      System.arraycopy(memory, pageTable[pageNumber+i].ppn * pageSize + pageOffset, 
        data, offset+curLoc, Math.min(pageSize, rem));
      // set used bit
      /* pageTable[pageNumber+i].used = true; */
      // increment current location in data by page size
      curLoc += pageSize;
      // decrement remaining number of bytes by page size
      rem -= pageSize;
    }
    /* UserProcess.readWriteLock.release();  */
    
    /*
    for (int i = 0; i < data.length; i++){
      System.out.println(data[i]);
    }
    */

    return amount;
  }

  /**
   * Transfer all data from the specified array to this process's virtual
   * memory.
   * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
   *
   * @param	vaddr	the first byte of virtual memory to write.
   * @param	data	the array containing the data to transfer.
   * @return	the number of bytes successfully transferred.
   */
  public int writeVirtualMemory(int vaddr, byte[] data) {
    return writeVirtualMemory(vaddr, data, 0, data.length);
  }

  /**
   * Transfer data from the specified array to this process's virtual memory.
   * This method handles address translation details. This method must
   * <i>not</i> destroy the current process if an error occurs, but instead
   * should return the number of bytes successfully copied (or zero if no
   * data could be copied).
   *
   * @param	vaddr	the first byte of virtual memory to write.
   * @param	data	the array containing the data to transfer.
   * @param	offset	the first byte to transfer from the array.
   * @param	length	the number of bytes to transfer from the array to
   *			virtual memory.
   * @return	the number of bytes successfully transferred.
   */
  public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    /* System.out.println("address is " + data); */
    /* System.out.println("WRITING: vaddr: " + vaddr + " data: " + data + " offset: " + offset + " length: " + length + " page " + pageFromAddress(vaddr) + " maxPages: " + numPages); */
    /* if (!(offset >= 0 && length >= 0 && offset+length <= data.length && pageFromAddress(vaddr)*pageSize + offsetFromAddress(vaddr) + offset+ length< numPages * pageSize)){ */
    int page = pageFromAddress(vaddr);
    int pageOffset = offsetFromAddress(vaddr);

    /* System.out.println("PAGE: " + (pageOffset+length)); */
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

    /* System.out.println("DATA LEN " +data.length); */
    /* System.out.println("LEN: "+length); */
    /* int pageOffset = offsetFromAddress(vaddr); */
    for (int i = 0; i < amount/pageSize + 1; i++){
      // copy Math.min(pageSize, rem) number of bytes from data at offset + curLoc
      // to memory at ppn

      /* UserProcess.readWriteLock.acquire(); */
      System.arraycopy(data, offset+curLoc, memory, pageTable[pageNumber+i].ppn * pageSize + pageOffset, 
          Math.min(pageSize, rem));
      /* String s = ""; */
      /* for(int j = offset+curLoc;j<offset+curLoc+Math.min(pageSize,rem);j++){ */
      /*   s = s + (char)data[j]; */
      /* } */
      /* System.out.println(s);  */
      //System.out.println("write remaining " + rem + " bytes");
      //System.out.println("write mem, ppn " + pageTable[vaddr/pageSize+i].ppn);
      //System.out.println("write mem, data " + offset+curLoc);
      // set dirty bit and used bit in pageTable entry
      /* pageTable[vaddr/pageSize+i].used = true; */
      /* pageTable[vaddr/pageSize+i].dirty = true; */
      // increment current location in data array by page size
      curLoc += pageSize;
      // decrement remaining amount of bytes by page size
      rem -= pageSize;
    }   
    /* UserProcess.readWriteLock.release(); */
    /*
       System.out.println("write data array ");
       for (int i = 0; i < data.length; i++){
       System.out.println(data[i]);
       }
       */
    return amount;
  }

  /**
   * Load the executable with the specified name into this process, and
   * prepare to pass it the specified arguments. Opens the executable, reads
   * its header information, and copies sections and arguments into this
   * process's virtual memory.
   *
   * @param	name	the name of the file containing the executable.
   * @param	args	the arguments to pass to the executable.
   * @return	<tt>true</tt> if the executable was successfully loaded.
   */
  private boolean load(String name, String[] args) {
    Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
    /* System.out.println(name); */
    OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
    if (executable == null) {
      Lib.debug(dbgProcess, "\topen failed");
      System.out.println("FALSE");
      return false;
    }  

    try {
      coff = new Coff(executable);
    }
    catch (EOFException e) {
      executable.close();
      Lib.debug(dbgProcess, "\tcoff load failed");
      return false;
    }  

    // make sure the sections are contiguous and start at page 0
    numPages = 0;
    for (int s=0; s<coff.getNumSections(); s++) {
      CoffSection section = coff.getSection(s);
      if (section.getFirstVPN() != numPages) {
        coff.close();
        Lib.debug(dbgProcess, "\tfragmented executable");
        return false;
      }
      numPages += section.getLength();
    }  

    // make sure the argv array will fit in one page
    byte[][] argv = new byte[args.length][];
    int argsSize = 0;
    for (int i=0; i<args.length; i++) {
      argv[i] = args[i].getBytes();
      // 4 bytes for argv[] pointer; then string plus one for null byte
      argsSize += 4 + argv[i].length + 1;
    }
    if (argsSize > pageSize) {
      coff.close();
      Lib.debug(dbgProcess, "\targuments too long");
      return false;
    }  

    // program counter initially points at the program entry point
    initialPC = coff.getEntryPoint();	 

    // next comes the stack; stack pointer initially points to top of it
    numPages += stackPages;
    initialSP = numPages*pageSize; 

    // and finally reserve 1 page for arguments
    numPages++;    

    if (!loadSections()){
      unloadSections();
      return false; 
    } 

    // store arguments in last page
    int entryOffset = (numPages-1)*pageSize;
    int stringOffset = entryOffset + args.length*4;    

    this.argc = args.length;
    this.argv = entryOffset;

    for (int i=0; i<argv.length; i++) {
      byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
      Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
      entryOffset += 4;
      Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
          argv[i].length);
      stringOffset += argv[i].length;
      Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
      stringOffset += 1;
    }  

    return true;
  }

  /**
   * Allocates memory for this process, and loads the COFF sections into
   * memory. If this returns successfully, the process will definitely be
   * run (this is the last step in process initialization that can fail).
   *
   * @return	<tt>true</tt> if the sections were successfully loaded.
   */
  protected boolean loadSections() {
    if (numPages > Machine.processor().getNumPhysPages()) {
      coff.close();
      Lib.debug(dbgProcess, "\tinsufficient physical memory");
      return false;
    }  

    pageTable = new TranslationEntry[numPages];
    UserKernel.pageListLock.acquire();
    for (int i=0; i<numPages; i++){
      if(UserKernel.freePageList.size() == 0) {
    UserKernel.pageListLock.release();
        handleExit(1);
      }
      int pageNumber = UserKernel.freePageList.removeFirst();
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
        pageTable[vpn].readOnly = section.isReadOnly();
        section.loadPage(i, pageTable[vpn].ppn);
      }
    }

    return true;
  }

  /**
   * Release any resources allocated by <tt>loadSections()</tt>.
   */
  protected void unloadSections() {
    UserKernel.pageListLock.acquire();
    for (int i=0; i<numPages; i++){
      if(pageTable[i] ==null) {
    UserKernel.pageListLock.release();
        handleExit(1);
      }
      int paddr = pageTable[i].ppn;
      UserKernel.freePageList.add(paddr);
    }
    UserKernel.pageListLock.release();
  }    

  /**
   * Initialize the processor's registers in preparation for running the
   * program loaded into this process. Set the PC register to point at the
   * start function, set the stack pointer register to point at the top of
   * the stack, set the A0 and A1 registers to argc and argv, respectively,
   * and initialize all other registers to 0.
   */
  public void initRegisters() {
    Processor processor = Machine.processor(); 

    // by default, everything's 0
    for (int i=0; i<processor.numUserRegisters; i++)
      processor.writeRegister(i, 0); 

    // initialize PC and SP according
    processor.writeRegister(Processor.regPC, initialPC);
    processor.writeRegister(Processor.regSP, initialSP);   

    // initialize the first two argument registers to argc and argv
    processor.writeRegister(Processor.regA0, argc);
    processor.writeRegister(Processor.regA1, argv);
  }

  /**
   * Handle the halt() system call. 
   */
  private int handleHalt() {
    unloadSections();
    Machine.halt();
    Lib.assertNotReached("Machine.halt() did not halt machine!");
    return 0;
  }

  /*
   * Handle the creat() system call.
   */
  private int handleCreat(int a0){
    String fileName = readVirtualMemoryString(a0, 256);
    /* System.out.println("filename is: " +fileName + "a"); */

    if(fileName == null || fileName.equals("")) {
      /* System.out.println("file name is null in create"); */
      return -1;
    }

    // should not return file descriptor if unlink has been called on it
    UserKernel.fileListLock.acquire();
    if (UserKernel.openFileList.containsKey(fileName)){
      ArrayList<Integer> fileEntry = UserKernel.openFileList.get(fileName);
      if (fileEntry.get(1) == 1){
        //UserKernel.fileListLock.release();
        return -1;
      }
    }
    UserKernel.fileListLock.release();

    if (filenameOpenTable.containsKey(fileName)){
      return filenameOpenTable.get(fileName);
    }

    OpenFile file = ThreadedKernel.fileSystem.open(fileName, true);

    if(file == null) {
      /* System.out.println("file " + fileName + " is null in create"); */
      return -1;
    }

    fileOpenTable.put(nextFileDescriptor, file);
    filenameOpenTable.put(fileName, nextFileDescriptor);
    readOffsetTable.put(nextFileDescriptor, 0); 
    writeOffsetTable.put(nextFileDescriptor, 0); 

    UserKernel.fileListLock.acquire();
    if(UserKernel.openFileList.containsKey(fileName)) {
      ArrayList<Integer> fileEntry = UserKernel.openFileList.get(fileName);
      int numOpen = fileEntry.get(0);
      fileEntry.set(0, numOpen + 1);
    } else {
      UserKernel.openFileList.put(fileName, new ArrayList<Integer>(Arrays.asList(1, 0)));
    }
    UserKernel.fileListLock.release();

    return nextFileDescriptor++;
  }

  private int handleExit(int a0){
    Set<Integer> keys = fileOpenTable.keySet();
    Object[] keyArray = keys.toArray();
    for(int i=0;i<keyArray.length;i++) {
      int fd = (Integer) keyArray[i];
      handleClose(fd);
    }
    unloadSections();

    UserKernel.processTableLock.acquire();
    UserKernel.processStatusTable.put(pid, a0);
    UserKernel.processTableLock.release();

    if(pid != 1){//my pid will be 1 if I don't have a parent
      Semaphore mySem = UserProcess.semaphoreTable.get(pid);
      mySem.V();
    }

    if(pid == 1) {
      handleHalt();
    } else {
      KThread.currentThread().finish();
    }
    return a0;
  }

  private int handleExec(int a0, int a1, int a2){
    String fileName = readVirtualMemoryString(a0, 256);
    if (!fileName.substring(fileName.length()-5).equals(".coff")) {
      System.out.println("POOOPS");
      return -1;
    }

    int argc = a1;
    String[] args = new String[argc]; 

    for (int i = 0; i < argc; i++){
      byte[] argPointer = new byte[4];
      int pointerLength = readVirtualMemory(a2 + i*4, argPointer, 0, 4);
      if (pointerLength == 0){
        return -1;
      }
      String arg = readVirtualMemoryString(Lib.bytesToInt(argPointer, 0), 256);
      args[i]=arg;
    }

    UserProcess process = UserProcess.newUserProcess();

    int childPID = process.pid;
    process.ppid=pid;

    UserKernel.processTableLock.acquire();
    UserKernel.processStatusTable.put(childPID, null);
    UserKernel.processTableLock.release();
    Semaphore childSem = new Semaphore(0);
    UserProcess.semaphoreTable.put(childPID,childSem);

    if (process == null){
      return -1;
    }

    children.add(process);

    boolean exec = process.execute(fileName, args);

    if (exec == false){
      return -1;
    }

    return childPID;
  }

  private int handleJoin(int a0, int a1){
    int childPID = a0;

    if (childPID == pid || childPID <= 0){
      /* System.out.println("PID:"+ pid + " childPID:" + childPID); */
      return -1;
    }

    UserProcess child = null;

    for (int i = 0; i < children.size(); i++){
      UserProcess c = children.get(i);
      if (c.pid == childPID){
        child = c;
      }
    }

    if (child == null){
      System.out.println("case 2");
      return -1;
    }

    Integer status = -1;

    Semaphore childSem = UserProcess.semaphoreTable.get(childPID);
    childSem.P();

    UserKernel.processTableLock.acquire();
    status = UserKernel.processStatusTable.get(child.pid);
    UserKernel.processTableLock.release();

    if (status == null){
      return 0;
    }

    int bytesWritten = writeVirtualMemory(a1, Lib.bytesFromInt(status), 0, 4); 
    if (bytesWritten == 0){
      return 0;
    }

    children.remove(child);

    return 1;
  }

  private int handleOpen(int a0){
    String fileName = readVirtualMemoryString(a0, 256);

    if(fileName == null) {
      /* System.out.println("file name is null in open"); */
      return -1;
    }

    // should not return file descriptor if unlink has been called on it
    UserKernel.fileListLock.acquire();
    if (UserKernel.openFileList.containsKey(fileName)){
      ArrayList<Integer> fileEntry = UserKernel.openFileList.get(fileName);
      if (fileEntry.get(1) == 1){
        //UserKernel.fileListLock.release();
        return -1;
      }
    }
    UserKernel.fileListLock.release();

    if (filenameOpenTable.containsKey(fileName)){
      return filenameOpenTable.get(fileName);
    }

    if(filenameCloseTable.containsKey(fileName)) {
      filenameCloseTable.remove(fileName);
    }

    OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);

    if(file == null) {
      /* System.out.println("file is null in open"); */
      return -1;
    }

    UserKernel.fileListLock.acquire();
    if(UserKernel.openFileList.containsKey(fileName)) {
      ArrayList<Integer> fileEntry = UserKernel.openFileList.get(fileName);
      int numOpen = fileEntry.get(0);
      fileEntry.set(0, numOpen + 1);
    } else {
      UserKernel.openFileList.put(fileName, new ArrayList<Integer>(Arrays.asList(1, 0)));
    }
    UserKernel.fileListLock.release();

    fileOpenTable.put(nextFileDescriptor, file);
    filenameOpenTable.put(fileName, nextFileDescriptor);
    readOffsetTable.put(nextFileDescriptor, 0); 
    writeOffsetTable.put(nextFileDescriptor, 0); 
    return nextFileDescriptor++;
  }

  private int handleRead(int a0, int a1, int a2){
    int fd = a0; //file descriptor
    int length = a2; //how much we want to read from file

    //if file unopened, error
    if (!fileOpenTable.containsKey(fd)){
      //System.out.println("cannot read unopened file");
      return -1;
    }

    //read from file and store in buffer
    OpenFile file = fileOpenTable.get(fd);
    int pos = readOffsetTable.get(fd);
    byte[] bytes = new byte[length];
    int readLength = -1;
    if (fd == 0 || fd == 1){
      readLength = file.read(bytes, 0, length);
    }
    else{
      readLength = file.read(pos, bytes, 0, length);
    }

    //System.out.println("should read " + length + ", read " + readLength + " bytes");

    if (readLength == -1){
      //System.out.println("Read " + fd + " failed");
      return -1;
    }

    //update offset
    if(fd != 1 || fd != 0) {
      readOffsetTable.put(fd, pos+readLength);
    }

    //write to virtual memory
    int transferredLength = writeVirtualMemory(a1, bytes, 0, readLength);
    //System.out.println("transferred " + transferredLength + " bytes to virtual mem");

    return transferredLength;
  }

  private int handleWrite(int a0, int a1, int a2){
    int fd = a0; //file descriptor
    int length = a2; //how much we want to read from buffer

    //if file unopened, error
    if (!fileOpenTable.containsKey(fd)){
      /* System.out.println("cannot write to unopened file"); */
      return -1;
    }

    //read from buffer
    String buffer = readVirtualMemoryString(a1, a2);
    /* System.out.println(buffer); */
    if (buffer == null){
      /* System.out.println("buffer is null in write"); */
      return -1;
    }

    //convert buffer content to byte array
    byte[] bytes = buffer.getBytes();
    //if buffer content is less than specified length, error
    if (bytes.length < length){
      /* System.out.println("bytes.length " + bytes.length); */
      /* System.out.println("specified length of buffer " + length); */
      /* System.out.println("buffer content smaller than specified"); */
      return -1;
    }

    //write to file at offset
    OpenFile file = fileOpenTable.get(fd);

    int writtenLength;
    int pos = writeOffsetTable.get(fd);
    if(fd == 0 || fd == 1){
      writtenLength = file.write(bytes, 0, length);
    } else {
      writtenLength = file.write(pos, bytes, 0, length);
    }


    /* System.out.println("offset " + offset); */
    /* for (int i = 0; i < bytes.length; i++){ */
    /*   System.out.print((char)bytes[i]); */
    /* } */

    if (writtenLength == -1){
      System.out.println("Write to " + fd + " failed");
      return -1;
    }

    //update offset
    if(fd != 1 || fd != 0) {
      writeOffsetTable.put(fd, pos+writtenLength);
    }

    return writtenLength;
  }

  private int handleClose(int a0){
    OpenFile f = fileOpenTable.remove(a0);
    if(f == null) {
      return -1;
    }
    /* System.out.println("closing: " + f.getName()); */
    filenameCloseTable.put(f.getName(), a0);
    filenameOpenTable.remove(f.getName());
    readOffsetTable.remove(a0);
    writeOffsetTable.remove(a0);
    f.close();

    String fileName = f.getName();
    /* String fileName = fileOpenTable.get(a0).getName(); */

    /* System.out.println(UserKernel.openFileList); */
    UserKernel.fileListLock.acquire();
    ArrayList<Integer> fileEntry = UserKernel.openFileList.get(fileName);
    /*
     * If the current process is the only one opening the file and 
     *  we have called unlink on the file, unlink and remove the fileEntry.
     * Otherwise, decrease the number of files opened in the fileEntry.
     */
    if(fileEntry.get(0) == 1 && fileEntry.get(1) == 1) {
      fileEntry.set(0, 0);
      UserKernel.fileListLock.release();
      handleUnlink(a0);
    } else {
      int numOpen = fileEntry.get(0);
      fileEntry.set(0, numOpen - 1);
      UserKernel.fileListLock.release();
    }

    return 0;
  }

  private int handleUnlink(int a0){
    String fileName = readVirtualMemoryString(a0, 256);
    if(filenameOpenTable.get(fileName) != null) {
      return -1;
    }

    /*
       System.out.println(UserKernel.openFileList);
       System.out.println(fileName);
       System.out.println(filenameOpenTable);
       System.out.println(fd);
       */

    //System.out.println("file name in unlink: " + fileName);

    UserKernel.fileListLock.acquire();
    ArrayList<Integer> fileEntry = UserKernel.openFileList.get(fileName);

    if(fileEntry == null) {
      ThreadedKernel.fileSystem.remove(fileName);
      UserKernel.fileListLock.release();
      return 0;
    }

    // we probably don't want this ever
    /* if (fileEntry == null){ */
    /*   System.out.println("PUPPIES"); */
    /*   UserKernel.fileListLock.release(); */
    /*   return -1; */
    /* } */

    if(fileEntry.get(0) == 0) {
      if(ThreadedKernel.fileSystem.remove(fileName)){
        UserKernel.openFileList.remove(fileName);
        UserKernel.fileListLock.release();
        return 0;
      } else {
        UserKernel.fileListLock.release();
        return -1;
      }
    } else {
      fileEntry.set(1, 1);
      UserKernel.fileListLock.release();
      return 0;
    }
  }


  private static final int
    syscallHalt = 0,
                syscallExit = 1,
                syscallExec = 2,
                syscallJoin = 3,
                syscallCreate = 4,
                syscallOpen = 5,
                syscallRead = 6,
                syscallWrite = 7,
                syscallClose = 8,
                syscallUnlink = 9;

  /**
   * Handle a syscall exception. Called by <tt>handleException()</tt>. The
   * <i>syscall</i> argument identifies which syscall the user executed:
   *
   * <table>
   * <tr><td>syscall#</td><td>syscall prototype</td></tr>
   * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
   * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
   * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
   * 								</tt></td></tr>
   * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
   * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
   * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
   * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
   *								</tt></td></tr>
   * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
   *								</tt></td></tr>
   * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
   * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
   * </table>
   * 
   * @param	syscall	the syscall number.
   * @param	a0	the first syscall argument.
   * @param	a1	the second syscall argument.
   * @param	a2	the third syscall argument.
   * @param	a3	the fourth syscall argument.
   * @return	the value to be returned to the user.
   */
  public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    switch (syscall) {
      case syscallHalt: 
        return handleHalt();
      case syscallExit:
        return handleExit(a0); //return handleExit(a0); //TODO
      case syscallExec:
        return handleExec(a0, a1, a2);
      case syscallJoin:
        return handleJoin(a0, a1); 
      case syscallCreate:
        int fd = handleCreat(a0);
        //System.out.println(fd);
        return fd;
      case syscallOpen:
        return handleOpen(a0);
      case syscallRead:
        return handleRead(a0, a1, a2);
      case syscallWrite:
        return handleWrite(a0, a1, a2);
      case syscallClose:
        return handleClose(a0);
      case syscallUnlink:
        return handleUnlink(a0);
      default:
        Lib.debug(dbgProcess, "Unknown syscall " + syscall);
        Lib.assertNotReached("Unknown system call!");
    }
    return 0;
  }

  /**
   * Handle a user exception. Called by
   * <tt>UserKernel.exceptionHandler()</tt>. The
   * <i>cause</i> argument identifies which exception occurred; see the
   * <tt>Processor.exception</tt> constants.
   *
   * @param	cause	the user exception that occurred.
   */
  public void handleException(int cause) {
    Processor processor = Machine.processor(); 

    switch (cause) {
      case Processor.exceptionSyscall:
        int result = handleSyscall(processor.readRegister(Processor.regV0),
            processor.readRegister(Processor.regA0),
            processor.readRegister(Processor.regA1),
            processor.readRegister(Processor.regA2),
            processor.readRegister(Processor.regA3)
            );
        processor.writeRegister(Processor.regV0, result);
        processor.advancePC();
        break;				       

      default:
        Lib.debug(dbgProcess, "Unexpected exception: " +
            Processor.exceptionNames[cause]);
        Lib.assertNotReached("Unexpected exception");
    }
  }

  /** The program being run by this process. */
  protected Coff coff;

  /** This process's page table. */
  protected TranslationEntry[] pageTable;
  /** The number of contiguous pages occupied by the program. */
  protected int numPages;

  /** The number of pages in the program's stack. */
  protected final int stackPages = 8;

  private int initialPC, initialSP;
  private int argc, argv;

  private static final int pageSize = Processor.pageSize;
  private static final char dbgProcess = 'a';
  private static Lock readWriteLock = new Lock();
  private static HashMap<Integer, Semaphore> semaphoreTable = new HashMap<Integer, Semaphore>(); // map pid to a semaphore. 
  //Signal the semaphore for your parent before exiting.
  private HashMap<Integer, OpenFile> fileOpenTable; //fd : OpenFile object
  private HashMap<String, Integer> filenameOpenTable; //fileName : fd
  private HashMap<String, Integer> filenameCloseTable; //fileName : fd
  private HashMap<Integer, Integer> readOffsetTable; //fd : offset
  private HashMap<Integer, Integer> writeOffsetTable; //fd : offset
  private int nextFileDescriptor;

  public int pid;
  public int ppid;
  public ArrayList<UserProcess> children; // an arraylist of children processes
}
