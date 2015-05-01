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
	public SwapFile(){
		file = new OpenFile();
	}

	public TranslationEntry getPage(int ppn){
		// get the page at the ppn and put in phys mem
		return null;
	}

	public int writePage(TranslationEntry entry){
		// write to page indicated by translation entry
		return 0;
	}

	public void deleteSwapFile(){
		//TODO: close the file, and remove it from the stub file system
	}

	private OpenFile file;
	private Lock fileLock;
	//need some sort of map to store 
}