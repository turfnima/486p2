/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *
 *  Created by Patrick McSweeney on 12/13/08.
 *
 */
package jnachos.kern;

import jnachos.machine.*;

/**
 * The ExceptionHanlder class handles all exceptions raised by the simulated
 * machine. This class is abstract and should not be instantiated.
 */
public abstract class ExceptionHandler {

	/**
	 * This class does all of the work for handling exceptions raised by the
	 * simulated machine. This is the only funciton in this class.
	 *
	 * @param pException
	 *            The type of exception that was raised.
	 * @see ExceptionType.java
	 */
	public static void handleException(ExceptionType pException) {
		switch (pException) {
		// If this type was a system call
		case SyscallException:

			// Get what type of system call was made
			int type = Machine.readRegister(2);

			// Invoke the System call handler
			SystemCallHandler.handleSystemCall(type);
			break;

		case PageFaultException:

			// See if there is a free page in RAM.
			int ppn = AddrSpace.mFreeMap.find();

			// If there is not choose a victim page and handle its logic.
			if (ppn < 0) {
				// Runt the Page Replacement Algorithm.
				ppn = JNachos.getPageReplacementAlgorithm().chooseVictimPage();

				if (ppn < 0) {
					System.out.println("Page Replacement Algorithm Failed to find victim page.  Shutting Down");
					Interrupt.halt();
				}
				int victimPid = JNachos.getPageFrameMap()[ppn];
				if (victimPid < 0) {
					System.out.println("Victim Process not found.  Shutting Down");
					Interrupt.halt();
				}

				NachosProcess victim = NachosProcess.mProcessHash.get(victimPid);

				if (victim == null) {
					System.out.println("Victim Process not found in table.  Shutting Down");
					Interrupt.halt();
				}
				TranslationEntry toEvict = null;
				for (int i = 0; i < victim.getSpace().mPageTable.length; i++) {
					if (victim.getSpace().mPageTable[i].physicalPage == ppn) {
						toEvict = victim.getSpace().mPageTable[i];
						break;
					}
				}
				if (toEvict == null) {
					System.out.println("Page not found in  victim process.  Shutting Down");
					Interrupt.halt();
				}

				if (toEvict.dirty) {
					// bytes to store the page to be evicted
					byte[] toEvictBytes = new byte[Machine.PageSize];

					// copy the page info from RAM to toEvictBytes
					System.arraycopy(Machine.mMainMemory, toEvict.physicalPage * Machine.PageSize, toEvictBytes, 0,
							Machine.PageSize);
					// copy the toEvictBytes to swapspace
					JNachos.mSwapSpace.writeAt(toEvictBytes, Machine.PageSize,
							toEvict.trans_diskmap * Machine.PageSize);
				}
				// Update the Victim's page table entry.
				toEvict.physicalPage = -1;
				toEvict.valid = false;
				toEvict.use = false;
				toEvict.dirty = false;
			}

			// numPageFaults to keep track of page fault numbers
			Statistics.numPageFaults = Statistics.numPageFaults + 1;
			int pagefaultVA = Machine.readRegister(39);
			int faultVPN = pagefaultVA / Machine.PageSize;
			Debug.print('v', "PAGE FAULT: VPN " + faultVPN);
			Debug.print('v', "Page fault virtual address "
					+ JNachos.getCurrentProcess().getSpace().diskmap[faultVPN] * Machine.PageSize);
			Debug.print('v', "Number of pageFaults " + Statistics.numPageFaults);

			// Copy the page into main memory.
			byte[] bytes = new byte[Machine.PageSize];
			JNachos.mSwapSpace.readAt(bytes, Machine.PageSize,
					JNachos.getCurrentProcess().getSpace().diskmap[faultVPN] * Machine.PageSize);
			System.arraycopy(bytes, 0, Machine.mMainMemory, ppn * Machine.PageSize, Machine.PageSize);

			// Update current process's page table
			JNachos.getCurrentProcess().getSpace().mPageTable[faultVPN].physicalPage = ppn;
			JNachos.getCurrentProcess().getSpace().mPageTable[faultVPN].valid = true;
			JNachos.getPageFrameMap()[ppn] = JNachos.getCurrentProcess().getid();

			// Update global map between pageframe and PID.
			JNachos.getCurrentProcess().getSpace().mPageTable[faultVPN].use = true;
			break;

		// All other exceptions shut down for now
		default:
			System.exit(0);
		}
	}
}
