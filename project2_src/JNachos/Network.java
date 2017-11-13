//
//  Network.java
//  
//
//  Created by Patrick McSweeney on 12/27/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package jnachos.machine;
import jnachos.kern.VoidFunctionPtr;
import jnachos.kern.Debug;
public class Network 
{


    private int mNetworkAddress;	// This machine's network address
    private double mChanceToWork;	// Likelihood packet will be dropped
    private int mSock;			// UNIX socket number for incoming packets
    private String mSockName;		// File name corresponding to UNIX socket
    private VoidFunctionPtr mWriteHandler; // Interrupt handler, signalling next packet 
									//      can be sent.  
    private VoidFunctionPtr mReadHandler;  // Interrupt handler, signalling packet has 
				// 	arrived.
    private Object mHandlerArg;		// Argument to be passed to interrupt handler
				//   (pointer to post office)
    private boolean mSendBusy;		// Packet is being sent.
    private boolean mPacketAvail;		// Packet has arrived, can be pulled off of
				//   network
    private PacketHeader inHdr;		// Information about arrived packet
    byte inbox[];  // Data for arrived packet //MaxPacketSize



	// network.h 
	//	Data structures to emulate a physical network connection.
	//	The network provides the abstraction of ordered, unreliable,
	//	fixed-size packet delivery to other machines on the network.
	//
	//	You may note that the interface to the network is similar to 
	//	the console device -- both are full duplex channels.
	//
	//  DO NOT CHANGE -- part of the machine emulation
	//
	// Copyright (c) 1992-1993 The Regents of the University of California.
	// All rights reserved.  See copyright.h for copyright notice and limitation 
	// of liability and disclaimer of warranty provisions.


	// Network address -- uniquely mNetworkAddressifies a machine.  This machine's ID 
	//  is given on the command line.
	//typedef int NetworkAddress;	 

	// The following class defines the network packet header.
	// The packet header is prepended to the data payload by the Network driver, 
	// before the packet is sent over the wire.  The format on the wire is:  
	//	packet header (PacketHeader)
	//	data (containing MailHeader from the PostOffice!)

	private class PacketHeader {
	//  public:
		public int mToAddress;		// Destination machine ID
		public int mFromAddress;	// source machine ID
		public int mLength;	 	// bytes of packet data, excluding the 
					// packet header (but including the 
					// MailHeader prepended by the post office)
					
		public PacketHeader(byte buffer[])
		{
		
		}
	}

	public static final int MaxWireSize = 64;	// largest packet that can go out on the wire
	public static final int MaxPacketSize =	(MaxWireSize - 16);	
					// data "payload" of the largest packet


	// The following class defines a physical network device.  The network
	// is capable of delivering fixed sized packets, in order but unreliably, 
	// to other machines connected to the network.
	//
	// The "reliability" of the network can be specified to the constructor.
	// This number, between 0 and 1, is the chance that the network will lose 
	// a packet.  Note that you can change the seed for the random number 
	// generator, by changing the arguments to RandomInit() in Initialize().
	// The random number generator is used to choose which packets to drop.




	// Dummy functions because C++ can't call member functions indirectly 
/*	static void NetworkReadPoll(int arg)
	{ Network *network = (Network *)arg; network->CheckPktAvail(); }
	static void NetworkSendDone(int arg)
	{ Network *network = (Network *)arg; network->SendDone(); }
*/
	// Initialize the network emulation
	//   addr is used to generate the socket name
	//   reliability says whether we drop packets to emulate unreliable links
	//   readAvail, writeDone, callArg -- analogous to console
	public Network(int pAddr, double pReliability,
		VoidFunctionPtr readAvail, VoidFunctionPtr writeDone, Object pCallArg)
	{
		mNetworkAddress = pAddr;
		if (pReliability < 0) mChanceToWork = 0;
		else if (pReliability > 1) mChanceToWork = 1;
		else mChanceToWork = pReliability;

		// set up the stuff to emulate asynchronous interrupts
		mWriteHandler = writeDone;
		mReadHandler = readAvail;
		mHandlerArg = callArg;
		mSendBusy = false;
		inHdr.mLength = 0;
		
		mSock = OpenSocket();
		//sprintf(mSockName, "SOCKET_%d", (int)pAddr);
		assignNameToSocket(mSockName,mSock);		 // Bind socket to a filename 
							 // in the current directory.

		// start polling for incoming packets
		Interrupt.Schedule(NetworkReadPoll, (int)this, NetworkTime, NetworkRecvInt);
	}

	public void destroyNetwork()
	{
		closeSocket(mSock);
		deAssignNameToSocket(mSockName);
	}

	// if a packet is already buffered, we simply delay reading 
	// the incoming packet.  In real life, the incoming 
	// packet might be dropped if we can't read it in time.
	public void checkPktAvail()
	{
		// schedule the next time to poll for a packet
		interrupt.schedule(NetworkReadPoll, (int)this, NetworkTime, NetworkRecvInt);

		if (inHdr.mLength != 0) 	// do nothing if packet is already buffered
		{
			return;		
		}
		if (!PollSocket(mSock)) 	// do nothing if no packet to be read
		{
			return;
		}

		// otherwise, read packet in
		char[] buffer = new char[MaxWireSize];
		readFromSocket(mSock, buffer, MaxWireSize);

		// divide packet into header and data
		inHdr = new PacketHeader(buffer);
		assert((inHdr.to == mNetworkAddress) && (inHdr.mLength <= MaxPacketSize));
//		bcopy(buffer + sizeof(PacketHeader), inbox, inHdr.length);


		Debug.print('n', "Network received packet from "+ inHdr.from+ ", length "+ inHdr.mLength+ "...\n");
		Statistics.numPacketsRecvd++;

		// tell post office that the packet has arrived
		mReadHandler.call(mHandlerArg);	
	}

	// notify user that another packet can be sent
	public void sendDone()
	{
		mSendBusy = false;
		Statistics.numPacketsSent++;
		mWriteHandler.call(mHandlerArg);
	}

	// send a packet by concatenating hdr and data, and schedule
	// an interrupt to tell the user when the next packet can be sent 
	//
	// Note we always pad out a packet to MaxWireSize before putting it into
	// the socket, because it's simpler at the receive end.
	public void send(PacketHeader hdr, char[] data)
	{
		char[] toName = new char[32];

		//sprintf(toName, "SOCKET_%d", (int)hdr.to);
		
		assert((mSendBusy == false) && (hdr.mLength > 0) 
			&& (hdr.mLength <= MaxPacketSize) && (hdr.from == mNetworkAddress));
		Debug.print('n', "Sending to addr %d, %d bytes... ", hdr.to, hdr.mLength);

		Interrupt.Schedule(NetworkSendDone, this, NetworkTime, NetworkSendInt);

		if (Random() % 100 >= mChanceToWork * 100) 
		{ // emulate a lost packet
			Debug.print('n', "oops, lost it!\n");
			return;
		}

		// concatenate hdr and data into a single buffer, and send it out
		char[] buffer = new char[MaxWireSize];
		buffer = (char[])hdr;
		//bcopy(data, buffer + sizeof(PacketHeader), hdr.length);
		sendToSocket(mSock, buffer, MaxWireSize, toName);
		
	}

	// read a packet, if one is buffered
	public PacketHeader Receive(char[] data)
	{
		PacketHeader hdr = inHdr;

		inHdr.mLength = 0;
		//if (hdr.length != 0)
		//	bcopy(inbox, data, hdr.length);
		return hdr;
	}
  
}
