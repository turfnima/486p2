//
//  Console.java
//  
//
//  Created by Patrick McSweeney on 12/27/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package jnachos.machine;

import jnachos.kern.VoidFunctionPtr;

public class Console {

	// console.h
	// Data structures to simulate the behavior of a terminal
	// I/O device. A terminal has two parts -- a keyboard input,
	// and a display output, each of which produces/accepts
	// characters sequentially.
	//
	// The console hardware device is asynchronous. When a character is
	// written to the device, the routine returns immediately, and an
	// interrupt handler is called later when the I/O completes.
	// For reads, an interrupt handler is called when a character arrives.
	//
	// The user of the device can specify the routines to be called when
	// the read/write interrupts occur. There is a separate interrupt
	// for read and write, and the device is "duplex" -- a character
	// can be outgoing and incoming at the same time.
	//
	// DO NOT CHANGE -- part of the machine emulation
	//
	// Copyright (c) 1992-1993 The Regents of the University of California.
	// All rights reserved. See copyright.h for copyright notice and limitation
	// of liability and disclaimer of warranty provisions.

	// The following class defines a hardware console device.
	// Input and output to the device is simulated by reading
	// and writing to UNIX files ("readFile" and "writeFile").
	//
	// Since the device is asynchronous, the interrupt handler "readAvail"
	// is called when a character has arrived, ready to be read in.
	// The interrupt handler "writeDone" is called when an output character
	// has been "put", so that the next character can be written.

	private static final char EOF = 0;
	private int mReadFileNo; // UNIX file emulating the keyboard
	private int mWriteFileNo; // UNIX file emulating the display
	private VoidFunctionPtr mWriteHandler; // Interrupt handler to call when
	// the PutChar I/O completes
	private VoidFunctionPtr mReadHandler; // Interrupt handler to call when
	// a character arrives from the keyboard
	private Object mHandlerArg; // argument to be passed to the
	// interrupt handlers
	private boolean mPutBusy; // Is a PutChar operation in progress?
	// If so, you can't do another one!
	private char mIncoming; // Contains the character to be read,
	// if there is one available.

	// console.cc
	// Routines to simulate a serial port to a console device.
	// A console has input (a keyboard) and output (a display).
	// These are each simulated by operations on UNIX files.
	// The simulated device is asynchronous,
	// so we have to invoke the interrupt handler (after a simulated
	// delay), to signal that a byte has arrived and/or that a written
	// byte has departed.
	//
	// DO NOT CHANGE -- part of the machine emulation
	//
	// Copyright (c) 1992-1993 The Regents of the University of California.
	// All rights reserved. See copyright.h for copyright notice and limitation
	// of liability and disclaimer of warranty provisions.

	// Dummy functions because C++ is weird about pointers to member functions
	/*
	 * static void ConsoleReadPoll(int c) { Console *console = (Console *)c;
	 * console->CheckCharAvail(); } static void ConsoleWriteDone(int c) {
	 * Console *console = (Console *)c; console->WriteDone(); }
	 */
	// ----------------------------------------------------------------------
	// Console::Console
	// Initialize the simulation of a hardware console device.
	//
	// "readFile" -- UNIX file simulating the keyboard (null -> use stdin)
	// "writeFile" -- UNIX file simulating the display (null -> use stdout)
	// "readAvail" is the interrupt handler called when a character arrives
	// from the keyboard
	// "writeDone" is the interrupt handler called when a character has
	// been output, so that it is ok to request the next char be
	// output
	// ----------------------------------------------------------------------

	public Console(char[] readFile, char[] writeFile, VoidFunctionPtr readAvail, VoidFunctionPtr writeDone,
			Object callArg) {
		if (readFile == null) {
			mReadFileNo = 0; // keyboard = stdin
		} else {
			mReadFileNo = JavaSys.openForReadWrite(new String(readFile), true); // should be
																	           // read-only
		}

		if (writeFile == null) {
			mWriteFileNo = 1; // display = stdout
		} else {
			mWriteFileNo = JavaSys.openForWrite(new String(writeFile));
		}

		// set up the stuff to emulate asynchronous interrupts
		mWriteHandler = writeDone;
		mReadHandler = readAvail;
		mHandlerArg = callArg;
		mPutBusy = false;
		mIncoming = EOF;

		// start polling for incoming packets
		Interrupt.Schedule(ConsoleReadPoll, (int) this, ConsoleTime, ConsoleReadInt);
	}

	// ----------------------------------------------------------------------
	// Console::~Console
	// Clean up console emulation
	// ----------------------------------------------------------------------
	public void close() {
		if (mReadFileNo != 0) {
			JavaSys.close(mReadFileNo);
		}
		if (mWriteFileNo != 1) {
			JavaSys.close(mWriteFileNo);
		}
	}

	// ----------------------------------------------------------------------
	// Console::CheckCharAvail()
	// Periodically called to check if a character is available for
	// input from the simulated keyboard (eg, has it been typed?).
	//
	// Only read it in if there is buffer space for it (if the previous
	// character has been grabbed out of the buffer by the Nachos kernel).
	// Invoke the "read" interrupt handler, once the character has been
	// put into the buffer.
	// ----------------------------------------------------------------------

	public void checkCharAvail() {
		char c;

		// schedule the next time to poll for a packet
		Interrupt.schedule(ConsoleReadPoll, (int) this, ConsoleTime, ConsoleReadInt);

		// do nothing if character is already buffered, or none to be read
		if ((mIncoming != EOF) || !pollFile(mReadFileNo))
			return;

		// otherwise, read character and tell user about it
		byte[] buffer = new byte[2];
		JavaSys.read(mReadFileNo, buffer, 2);
		mIncoming = c;

		Statistics.numConsoleCharsRead++;
		mReadHandler.call(mHandlerArg);
	}

	// ----------------------------------------------------------------------
	// Console::WriteDone()
	// Internal routine called when it is time to invoke the interrupt
	// handler to tell the Nachos kernel that the output character has
	// completed.
	// ----------------------------------------------------------------------

	public void WriteDone() {
		mPutBusy = false;
		Statistics.numConsoleCharsWritten++;
		mWriteHandler.call(mHandlerArg);
	}

	// ----------------------------------------------------------------------
	// Console::GetChar()
	// Read a character from the input buffer, if there is any there.
	// Either return the character, or EOF if none buffered.
	// ----------------------------------------------------------------------

	public char GetChar() {
		char ch = mIncoming;
		mIncoming = EOF;
		return ch;
	}

	// ----------------------------------------------------------------------
	// Console::PutChar()
	// Write a character to the simulated display, schedule an interrupt
	// to occur in the future, and return.
	// ----------------------------------------------------------------------

	public void putChar(char ch) {
		assert (mPutBusy == false);
		byte[] buffer = new byte[2];
		WriteFile(mWriteFileNo, buffer, 2);
		mPutBusy = true;
		Interrupt.schedule(ConsoleWriteDone, (int) this, ConsoleTime, ConsoleWriteInt);
	}

}
