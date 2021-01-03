import java.util.*;
import java.io.*;
import java.math.*;

// VIRTUAL MEMORY SIMULATOR 
class vmsim 
{
	// Simulating 'Second Chance' 
	public static void main(String[] args) throws Exception
	{
		// Bytes per KB
		int BpKB = 1024;
		int debg = 0; // debug int for tracking disk writes 
		
		// java vmsim -n <numframes> -p <pagesize KB> -s <memory split> <tracefile>
		///	args	   0      1      2       3         4       5             6
		// Parse arguments 
		int numFrames = Integer.parseInt(args[1]);
		int accesses = 0;
		int writes = 0;
		// initialize the amount of bits needed to find page address
		int addr = 0;
		
		// size in kilobytes
		int sizeInKB = Integer.parseInt(args[3]);
		
		//System.out.println("Size is " + sizeInKB + " Kilobytes.");
		//System.out.println("Translates to " + sizeInKB*BpKB + " Bytes.");
			
		addr = 32-log2(sizeInKB*BpKB);
		//System.out.println("Page address is the first " + addr + " bits.");

		// Retrieve each processes piece of the memory
		String[] memSplit = args[5].split(":");
		
		// Calculate each process' number of frames based on memory split
		int proc0Frames = Integer.parseInt(memSplit[0]);
		int proc1Frames = Integer.parseInt(memSplit[1]);
		
		// find the total int that you will need to divide into
		// the amount of total frames 
		int totalSplit = proc0Frames + proc1Frames;
		
		// partition holds the size of each memory piece that is 
		// given to each process
		int partition = numFrames / totalSplit;
		
		// maximum size for each processes memory frames 
		int p0Size = proc0Frames*partition;
		int p1Size = proc1Frames*partition;
		
		// DEBUG PRINTS
		//System.out.println("Total Frames = " + numFrames + " frames");
		//System.out.println("Page Size = " + pageSize);
		
		//System.out.println("Memory split for Process 0 = " + memSplit[0]);
		//System.out.println("Memory split for Process 1 = " + memSplit[1]);
		
		//System.out.println("Frames given to Process 0 = " + p0Size + " frames ");
		//System.out.println("Frames given to Process 1 = " + p1Size + " frames ");
		
		// DEBUG INT
		int read = 0;
		
		// Try the given file 
		try
		{
			// Parse in file
			BufferedReader BR = new BufferedReader(new FileReader(args[6]));
			
			// proc 0/1 linked lists 
			VMNode proc0Head = null;
			VMNode proc1Head = null;
			
			// used to manage allocated frames 
			// for each process (nodes in LL)
			int proc0ListSize = 0;
			int proc1ListSize = 0;
			
			// initialize each proc's pg faults, disk writes, mem accesses
			int proc0Writes = 0;
			int proc0Faults = 0;
			int proc0Accesses = 0;
			
			int proc1Writes = 0;
			int proc1Faults = 0;
			int proc1Accesses = 0;
			
			// Initialize variables to be taken from each page 
			int procNum;
			int pageAdd;
			String S_L; // load/store val
			String hexVal; // temp hex val 
			
			// Begin at the front of the file
			String page = BR.readLine();
			
			// WHILE READING IN FILE//
	///////////////////////////////////////////////////////////////////////////////////
			while(page != null)
			{
				// split the page line into 3 parts
				String[] pageArr = page.split(" ");
				S_L = pageArr[0];
				//System.out.println("S/L : " + S_L);

				// process number
				procNum = Integer.parseInt(pageArr[2]);
	/////////////////// HANDLE PROC 0 ////////////////////////////////////////////////
				if(procNum == 0)
				{
					proc0Accesses++;
					
					// split the address into its hex form,
					String[] pageAddSplit = pageArr[1].split("x");
					hexVal = pageAddSplit[1];
					
					//System.out.println(hexVal);
					
					// convert the hex value to binary 
					String bitVal = binGenerator(hexVal);
					String address = bitVal.substring(0, addr);
					
					//System.out.println(bitVal);
					
					// if the head is null, create one
					if(proc0Head == null)
					{	
						//System.out.println("*********************************");
						//System.out.println("Passed in L/S value: " + S_L);
						//System.out.println("passing in: write(" + S_L + ")");
						//System.out.println("write(s/l) returned: " + write(S_L));
						//System.out.println("*********************************");
						proc0Head = new VMNode(address, write(S_L), 0, null, null);
						proc0ListSize++;
						proc0Faults++;
					}
					// else traverse this processes list
					else
					{
						//System.out.println("Head was not null, moving onto filling memory.");
						
						if(proc0ListSize < p0Size) // fill memory
						{
							//System.out.println("There is still room in memory: " + proc0ListSize + "/" + p0Size);
							
							// search the list for a hit
							VMNode curr = proc0Head;
							while(curr.next != null)
							{
								// if there is a hit in memory
								if(curr.pgAddr.equals(address)){ break;}
								
								// move to the next node
								else curr = curr.next;
							}
							// hit found 
							if(curr.pgAddr.equals(address))
							{
								//System.out.println();
								//System.out.println("********************");
								//System.out.println("Hit found in memory.");
								//System.out.println("Curr address : " + curr.pgAddr + " matches " + address);
								//System.out.println("********************");
								//System.out.println();
								
								
								curr.RB = 1; // hit
								if(write(S_L) == 1)
								curr.DB = 1;
							}
							// no hit 
							else
							{
								//System.out.println();
								//System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
								//System.out.println("No hit found, adding new page.");
								//System.out.println("Curr address : " + curr.pgAddr + " did not match " + address);
								//System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
								//System.out.println();
								
								proc0Faults++; // page fault
								proc0ListSize++;
								//System.out.println("*********************************");
								//System.out.println("Passed in L/S value: " + S_L);
								//System.out.println("passing in: write(" + S_L + ")");
								//System.out.println("write(s/l) returned: " + write(S_L));
								//System.out.println("*********************************");
								curr.next = new VMNode(address, write(S_L), 0, null, curr);
							}
						}
						//// LIST IS FULL AT THIS POINT ////
						////   FIND A PAGE TO REPLACE	////
						else // implement second chance 
						{
							//System.out.println("LIST FULL");
							// traverse list to find a hit 
							VMNode curr = proc0Head;
							while(curr != null)
							{	
								if(curr.pgAddr.equals(address)){ break;}
								else curr = curr.next;
							}
							// there was a hit in memory 
							if(curr != null && curr.pgAddr.equals(address))
							{
								
								curr.RB = 1; // set reference bit
								if(write(S_L) == 1)
									curr.DB = 1;
							}
							// list has been searched for match, at this point
							// now traverse again and search for a page to evict 
							else // page was not found, must evict a page 
							{ 
								proc0Faults++; // inc page faults 
								
								// traverse the list starting at the head again (FIFO)
								curr = proc0Head;
								while(curr != null)
								{	
									// if a referenced page is found, give it a second chance 
									if(curr.RB == 1)
									{
										if(curr.prev == null) // if its the head 
										{
											curr.RB = 0; // reset reference bit
											VMNode temp = curr; // hold this node temporarily 
											
											proc0Head = curr.next; // move the head pointer
											curr = proc0Head; // move curr 
											curr.prev = null; // remove pointer to old head 
											
											VMNode ptr = proc0Head;
											while(ptr.next != null){ ptr = ptr.next;} // traverse the list 
											
											// place second chance node at the end 
											ptr.next = temp;
											temp.next = null;
											temp.prev = ptr;
											//curr = curr.next;
										}
										else if(curr.next == null) // if its the tail 
										{
											// this node is already at the tail 
											curr.RB = 0; // reset reference bit
										}
										else // if its in the middle 
										{
											curr.RB = 0; // reset reference bit
											VMNode temp = curr; // hold this node temporarily
											
											// remove references to moving node from its adjacent neighbors 
											curr.prev.next = curr.next; 
											curr.next.prev = curr.prev;
											curr = curr.next;
											
											// traverse the list 
											VMNode ptr = proc0Head;
											while(ptr.next != null){ ptr = ptr.next;} // traverse the list 
											
											// place second chance node at the end 
											ptr.next = temp;
											temp.next = null;
											temp.prev = ptr;
										}
									}
									else // page for removal selected 
									{
										// check dirty bit for disk write 
										if(curr.DB == 1)
										{ 
											debg++;
											//System.out.println("Actually wrote to disk");
											proc0Writes++;
										}
										if(curr.next == null) // if this is the tail
										{
											// replace the tail
											//proc0Faults++;
											//System.out.println("*********************************");
											//System.out.println("Passed in L/S value: " + S_L);
											//System.out.println("passing in: write(" + S_L + ")");
											//System.out.println("write(s/l) returned: " + write(S_L));
											//System.out.println("*********************************");
											curr = new VMNode(address, write(S_L), 0, null, curr.prev);
											break;
										}
										else if(curr.prev == null) // if this is the head
										{
											//////////////////////////
											// create new head and dereference old 
											proc0Head = proc0Head.next;
											proc0Head.prev = null;
											
											curr = proc0Head;
											while(curr.next != null){ curr = curr.next;}
											// add page 
											//proc0Faults++;
											//System.out.println("*********************************");
											//System.out.println("Passed in L/S value: " + S_L);
											//System.out.println("passing in: write(" + S_L + ")");
											//System.out.println("write(s/l) returned: " + write(S_L));
											//System.out.println("*********************************");
											curr.next = new VMNode(address, write(S_L), 0, null, curr);
											break;
											
										}
										else // else its in between the head and tail
										{
											curr.prev.next = curr.next;
											curr = proc0Head;
											while(curr.next != null){ curr = curr.next;}
											// add page 
											//proc0Faults++;
											//System.out.println("*********************************");
											//System.out.println("Passed in L/S value: " + S_L);
											//System.out.println("passing in: write(" + S_L + ")");
											//System.out.println("write(s/l) returned: " + write(S_L));
											//System.out.println("*********************************");
											curr.next = new VMNode(address, write(S_L), 0, null, curr);
											break;
										}
									}
								}
							}
						}
					}
				}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				// HANDLE PROC 1 
				else if(procNum == 1)
				{
					proc1Accesses++;
					// store / load 
	
					// split the address into its hex form,
					String[] pageAddSplit = pageArr[1].split("x");
					hexVal = pageAddSplit[1];
					
					//System.out.println(hexVal);
					
					// convert the hex value to binary 
					String bitVal = binGenerator(hexVal);
					String address = bitVal.substring(0, addr);
					
					//System.out.println("Address length: " + address.length());
					//System.out.println(address);
					
					// if the head is null, create one
					if(proc1Head == null)
					{
						//System.out.println("*********************************");
						//System.out.println("Passed in L/S value: " + S_L);
						//System.out.println("passing in: write(" + S_L + ")");
						//System.out.println("write(s/l) returned: " + write(S_L));
						//System.out.println("*********************************");
						proc1Head = new VMNode(address, write(S_L), 0, null, null);
						proc1ListSize++;
						proc1Faults++;
					}
					// else traverse this processes list
					else
					{
						/////////////
						
						//System.out.println("Head was not null, moving onto filling memory.");
						
						if(proc1ListSize < p1Size) // fill memory
						{
							//System.out.println("There is still room in memory: " + proc0ListSize + "/" + p0Size);
							
							// search the list for a hit, otherwise 
							VMNode curr = proc1Head;
							while(curr.next != null) // hwat if its the head and no match?
							{
								// if there is a hit in memory
								if(curr.pgAddr.equals(address)){ break;}
								// move to the next node
								else curr = curr.next;
							}
							// hit found 
							if(curr.pgAddr.equals(address))
							{
								//System.out.println();
								//System.out.println("********************");
								//System.out.println("Hit found in memory.");
								//System.out.println("Curr address : " + curr.pgAddr + " matches " + address);
								//System.out.println("********************");
								//System.out.println();
								curr.RB = 1; // hit
								
								if(write(S_L) == 1)
								curr.DB = 1;
							}
							// no hit 
							else
							{
								//System.out.println();
								//System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
								//System.out.println("No hit found, adding new page.");
								//System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
								//System.out.println();
								proc1Faults++; // page fault
								proc1ListSize++;
								//System.out.println("*********************************");
								//System.out.println("Passed in L/S value: " + S_L);
								//System.out.println("passing in: write(" + S_L + ")");
								//System.out.println("write(s/l) returned: " + write(S_L));
								//System.out.println("*********************************");
								curr.next = new VMNode(address, write(S_L), 0, null, curr);
							}
						}
						//// LIST IS FULL AT THIS POINT ////
						////   FIND A PAGE TO REPLACE	////
						else // implement second chance 
						{
							//System.out.println("LIST FULL");
							// traverse list to find a hit 
							VMNode curr = proc1Head;
							while(curr != null)
							{	
								if(curr.pgAddr.equals(address)){ break;}
								else curr = curr.next;
							}
							// there was a hit in memory 
							if(curr != null && curr.pgAddr.equals(address))
							{
								curr.RB = 1; // set reference bit
								if(write(S_L) == 1)
								curr.DB = 1;
							}
							// list has been searched for match, at this point
							// now traverse again and search for a page to evict 
							else // page was not found, must evict a page 
							{ 
								proc1Faults++; // inc page faults 
								
								// traverse the list starting at the head again
								curr = proc1Head;
								while(curr != null)
								{	
									// if a referenced page is found, give it a second chance 
									if(curr.RB == 1)
									{
										if(curr.prev == null) // if its the head 
										{
											curr.RB = 0; // reset reference bit
											VMNode temp = curr; // hold this node temporarily 
											
											proc1Head = curr.next; // move the head pointer
											curr = proc1Head; // move curr 
											curr.prev = null; // remove pointer to old head 
											
											VMNode ptr = proc1Head;
											while(ptr.next != null){ ptr = ptr.next;} // traverse the list 
											
											// place second chance node at the end 
											ptr.next = temp;
											temp.next = null;
											temp.prev = ptr;
											//curr = curr.next;
										}
										else if(curr.next == null) // if its the tail 
										{
											// this node is already at the tail
											curr.RB = 0; // reset reference bit
										}
										else // if its in the middle 
										{
											curr.RB = 0; // reset reference bit
											VMNode temp = curr; // hold this node temporarily
											
											// remove references to moving node from its adjacent neighbors 
											curr.prev.next = curr.next; 
											curr.next.prev = curr.prev;
											curr = curr.next;
											
											// traverse the list 
											VMNode ptr = proc1Head;
											while(ptr.next != null){ ptr = ptr.next;} // traverse the list 
											
											// place second chance node at the end 
											ptr.next = temp;
											temp.next = null;
											temp.prev = ptr;
										}
									}
									else // page for removal selected 
									{
										// check dirty bit for disk write 
										if(curr.DB == 1)
										{ 
											proc1Writes++;
											debg++;
										}
										//*
										if(curr.next == null) // if this is the tail
										{
											// replace the tail
											//System.out.println("*********************************");
											//System.out.println("Passed in L/S value: " + S_L);
											//System.out.println("passing in: write(" + S_L + ")");
											//System.out.println("write(s/l) returned: " + write(S_L));
											//System.out.println("*********************************");
											curr = new VMNode(address, write(S_L), 0, null, curr.prev);
											break;
											
										}
										else if(curr.prev == null) // if this is the head
										{
											// remove old head
											proc1Head = proc1Head.next;
											proc1Head.prev = null;
											
											curr = proc1Head;
											while(curr.next != null){ curr = curr.next;}
											// add page 
											//System.out.println("*********************************");
											//System.out.println("Passed in L/S value: " + S_L);
											//System.out.println("passing in: write(" + S_L + ")");
											//System.out.println("write(s/l) returned: " + write(S_L));
											//System.out.println("*********************************");
											curr.next = new VMNode(address, write(S_L), 0, null, curr);
											break;
											
										}
										else // else its in between the head and tail
										{
											// replace head
											curr.prev.next = curr.next;
											curr = proc1Head;
											while(curr.next != null){ curr = curr.next;}
											
											// add page 
											//System.out.println("*********************************");
											//System.out.println("Passed in L/S value: " + S_L);
											//System.out.println("passing in: write(" + S_L + ")");
											//System.out.println("write(s/l) returned: " + write(S_L));
											//System.out.println("*********************************");
											curr.next = new VMNode(address, write(S_L), 0, null, curr);
											break;
										}
									}
								}
							}
						}
					}
				}
				// Move to next page
				page = BR.readLine();
			}

			System.out.println("Algorithm: Second Chance");
			System.out.println("Number of frames: " + (p0Size + p1Size));
			System.out.println("Page size: " + sizeInKB + " KB");
			System.out.println("Total memory accesses: " + (proc0Accesses + proc1Accesses));
			System.out.println("Total page faults: " + (proc0Faults + proc1Faults));
			System.out.println("Total writes to disk: " + (proc0Writes + proc1Writes));
			//System.out.println();
			//System.out.println("For process 1.");
			//System.out.println("Number of frames: " + p1Size);
			//System.out.println("Total memory accesses: " + proc1Accesses);
			//System.out.println("Total page faults: " + proc1Faults);
			//System.out.println("Total writes to disk: " + proc1Writes);
			//System.out.println("Should have had : " + debg + " disk writes TOTAL");
		}
		// Catch file not found, etc.  
		catch(Exception x)
		{
			System.out.println(x);
			x.printStackTrace();
		}
		// PUT FINAL PRINT STATEMENTS HERE 
	}
	// FIX THIS ; CAUSING OOB EXCEPTIONS 
	static String binGenerator(String HexVal) 
	{
		// HEX TO BINARY TABLE
		// constructed table helps replace hex values 
		// IN ORDER to avoid wronful replacements and
		// maintaining the 32 bit representation 
		HexVal = HexVal.replaceAll("0", "0000");
        HexVal = HexVal.replaceAll("1", "0001");
		// 0 and 1 must go first to ensure new 
		// binary values are not interpreted as 
		// hexadecimal 
		
        HexVal = HexVal.replaceAll("2", "0010");
        HexVal = HexVal.replaceAll("3", "0011");
        HexVal = HexVal.replaceAll("4", "0100");
        HexVal = HexVal.replaceAll("5", "0101");
        HexVal = HexVal.replaceAll("6", "0110");
        HexVal = HexVal.replaceAll("7", "0111");
        HexVal = HexVal.replaceAll("8", "1000");
        HexVal = HexVal.replaceAll("9", "1001");
        HexVal = HexVal.replaceAll("a", "1010");
        HexVal = HexVal.replaceAll("b", "1011");
        HexVal = HexVal.replaceAll("c", "1100");
        HexVal = HexVal.replaceAll("d", "1101");
        HexVal = HexVal.replaceAll("e", "1110");
        HexVal = HexVal.replaceAll("f", "1111");
        return HexVal;
	}
	static int log2(int pageSize)
	{
		 return (int)(Math.log(pageSize)/Math.log(2));
	}
	// argument passed in is load/store
	// returned int used as Dirty Bit for each page 
	static int write(String S_L)
	{
		if(S_L.equals("l")) // load
			return 0;
		else if(S_L.equals("s")) // STORE
			return 1;
		
		else 
			System.out.println("	***** ERROR *****	");
			return -1;
	}
	public static class VMNode
	{
		String pgAddr;
		int DB;	// dirty bit
		int RB;	// reference bit 
		VMNode next;
		VMNode prev;
		
		public VMNode()
		{
			this.pgAddr = "";
			this.DB = -1;	// -1 used to represent uninitialized 
			this.RB = -1;	// VMNode fields 
			this.next = null;
			this.prev = null;
		}
		public VMNode(String addr, int x, int y, VMNode next, VMNode prev)
		{
			this.pgAddr = addr;
			this.DB = x;
			this.RB = y;
			this.next = next;
			this.prev = prev;
		}
	}
	
	
	
}

