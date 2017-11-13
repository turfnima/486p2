/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.kern;

import java.util.Hashtable;

import jnachos.filesystem.OpenFile;
import jnachos.machine.*;

//helper class for fork()
class Helper implements VoidFunctionPtr {
	public void call(Object arg) {
		// restores the currents Process users state
		JNachos.getCurrentProcess().restoreUserState();
		// restores the address space of the current process
		JNachos.getCurrentProcess().getSpace().restoreState();

		Machine.run();

	}
}

/** The class handles System calls made from user programs. */
public class SystemCallHandler {
	/** The System call index for halting. */
	public static final int SC_Halt = 0;

	/** The System call index for exiting a program. */
	public static final int SC_Exit = 1;

	/** The System call index for executing program. */
	public static final int SC_Exec = 2;

	/** The System call index for joining with a process. */
	public static final int SC_Join = 3;

	/** The System call index for creating a file. */
	public static final int SC_Create = 4;

	/** The System call index for opening a file. */
	public static final int SC_Open = 5;

	/** The System call index for reading a file. */
	public static final int SC_Read = 6;

	/** The System call index for writting a file. */
	public static final int SC_Write = 7;

	/** The System call index for closing a file. */
	public static final int SC_Close = 8;

	/** The System call index for forking a forking a new process. */
	public static final int SC_Fork = 9;

	/** The System call index for yielding a program. */
	public static final int SC_Yield = 10;

	public static final int SC_Printf = 11;

	public static final int SC_LinkAndRun = 12;

	// the second hash tables for the relation between parent and child
	// processes
	public static Hashtable childparent_relation = new Hashtable();

	/**
	 * Entry point into the Nachos kernel. Called when a user program is
	 * executing, and either does a syscall, or generates an addressing or
	 * arithmetic exception.
	 * 
	 * For system calls, the following is the calling convention:
	 * 
	 * system call code -- r2 arg1 -- r4 arg2 -- r5 arg3 -- r6 arg4 -- r7
	 * 
	 * The result of the system call, if any, must be put back into r2.
	 * 
	 * And don't forget to increment the pc before returning. (Or else you'll
	 * loop making the same system call forever!
	 * 
	 * @pWhich is the kind of exception. The list of possible exceptions are in
	 *         Machine.java
	 **/
	public static void handleSystemCall(int pWhichSysCall) {
		Machine.writeRegister(Machine.PrevPCReg, Machine.readRegister(Machine.PCReg));
		Machine.writeRegister(Machine.PCReg, Machine.readRegister(Machine.NextPCReg));
		Machine.writeRegister(Machine.NextPCReg, Machine.readRegister(Machine.NextPCReg) + 4);

		// System.out.println("SysCall:" + pWhichSysCall);

		switch (pWhichSysCall) {
		// If halt is received shut down

		

		case SC_Halt:
			Debug.print('a', "Shutdown, initiated by user program.");
			Interrupt.halt();
			break;

		case SC_Fork:
			NachosProcess child = new NachosProcess("child"); // create a new
																// nachoprocess
			child.setSpace(new AddrSpace(JNachos.getCurrentProcess().getSpace())); // copy
																					// the
																					// memory
																					// of
																					// the
																					// parent
																					// to
																					// that
																					// of
																					// the
																					// child
																					// process
			// write to the register of the child process id 0;
			Machine.writeRegister(2, 0);
			// save child's user state;
			child.saveUserState();

			// write to the register of child's process id, return to parent;
			Machine.writeRegister(2, child.getid());
			// call fork to make the child ready
			child.fork(new Helper(), null);

			// Machine.writeRegister(2, 10);

			break;

		case SC_Join:

			// check if there's any parent process for this child process
			int registerfour = Machine.readRegister(4);

			if (NachosProcess.mProcessHash.get(registerfour) == null) // if the
																		// child
																		// has
																		// no
																		// parent
																		// process
			{
				// this is an error case, write -1 to the second register of
				// parent
				Machine.writeRegister(2, -1);

			} else if (registerfour == JNachos.getCurrentProcess().getid()) /// process
																			/// return
																			/// its
																			/// own
																			/// pid
				Machine.writeRegister(2, 0); // return immediately

			else // if the child has a parent
			{

				// associate child with its parent in the second hashtable
				childparent_relation.put(registerfour, JNachos.getCurrentProcess());
				boolean old = Interrupt.setLevel(false);
				JNachos.getCurrentProcess().sleep();
				Interrupt.setLevel(old);
			}
			break;

		case SC_Printf:
			int index = Machine.readRegister(4);
			int value = 1;
			String message = new String();
			while ((char) value != '\0') {
				value = Machine.readMem(index, 1);
				message += (char) value;
				index++;
			}
			System.out.println(message);

			break;

		case SC_Exit:
			// Read in any arguments from the 4th register
			int arg = Machine.readRegister(4);

			System.out
					.println("Current Process " + JNachos.getCurrentProcess().getName() + " exiting with code " + arg);
			if (childparent_relation.get(JNachos.getCurrentProcess().getid()) != null)// There
																						// are
																						// processes
																						// on
																						// the
																						// waiting
																						// list

			{
				// set the next process ready to run, and remove the entry
				Scheduler.readyToRun((NachosProcess) childparent_relation.remove(JNachos.getCurrentProcess().getid()));
			}
			// remove the process from the other hashtable
			NachosProcess.mProcessHash.remove(JNachos.getCurrentProcess().getid());

			// Finish the invoking process
			JNachos.getCurrentProcess().finish();
			break;

		default:
			Interrupt.halt();
			break;
		}

	}
}
