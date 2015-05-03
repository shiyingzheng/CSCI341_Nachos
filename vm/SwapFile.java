package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.HashMap;


/* 
* Incomplete implementation of a swap file. It is an actual file on disk.
*/

public class SwapFile{
	private OpenFile file;
	private Lock fileLock;
  private HashMap<Pair,Integer> diskPageMap;
  private LinkedList<Integer> freeDiskPages;
  private int highestDiskPage;
  private final int pageSize = 1024;
  private final String filename = ".swp";
	public SwapFile(){
		file = new OpenFile(ThreadedKernel.fileSystem,filename);
	  fileLock = new Lock();
    diskPageMap = new HashMap<Pair,Integer>();
    freeDiskPages = new LinkedList<Integer>();
    freeDiskPages.push(0);
    highestDiskPage = 1;
  }

	public boolean swapPageIn(int pid, int vpn, int ppn){
		// swap in the page indicated by pid,vpn. Store it in the space ppn in physical memory
    Integer dpn;
    if((dpn = diskPageMap.get(new Pair(pid,vpn))) == null){
      return false;
    }
    byte[] physMemory = Machine.processor().getMemory();
    file.read(dpn*pageSize, physMemory, ppn, pageSize);
    diskPageMap.remove(new Pair(pid,vpn));
    freeDiskPages.push(dpn);
    return true;
	}

	public boolean swapPageOut(int pid, int vpn, int ppn){
		// swap the page out to disk indicated by pid,vpn. Swap it from the physical location indicated by ppn.
    if(freeDiskPages.isEmpty()){
      addDiskPages();
    }
    Integer dpn = freeDiskPages.pop();
    byte[] physMemory = Machine.processor().getMemory();
    file.write(dpn*pageSize, physMemory, ppn, pageSize);
    diskPageMap.add(new Pair(pid,vpn), dpn);
    return true;
	}

	public void deleteSwapFile(){
    file.close();
    ThreadedKernel.fileSystem.remove(filename);
	}
  public void addDiskPages(){
    for(int i=highestDiskPage;i<highestDiskPage*2;i++){
      freeDiskPages.add(i);
    }
    highestDiskPage *= 2;
  }
  private class Pair{
    int pid;
    int vPageNum;
    private Pair(int pid, int vPageNum){
      this.pid = pid;
      this.vPageNum = vPageNum;
    }
    private boolean equals(Pair other){
      return this.pid == other.pid && this.vPageNum == other.vPageNum;
    }
  }
}
