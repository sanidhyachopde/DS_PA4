package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {

	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	static final int SERVER_PORT = 10000;
	Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledynamo.provider");
	String portStr = "empty";
	String myPort = "empty";
	String genHash_Port = "empty";
	String node1 = "empty";
	String node2 = "empty";
	String node3 = "empty";
	String nextNode = "empty";
	String previousNode1 = "empty";
	String previousNode2 = "empty";

	SortedMap<String, String> hashedNodes = new TreeMap<String, String>();
	ArrayList<String> messageValues = new ArrayList<String>();
	ArrayList<String> messageKeys = new ArrayList<String>();
	ArrayList<String> insertedKeys = new ArrayList<String>();

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		if(selection.equals("@"))
		{
			for(String key: insertedKeys)
			{
				getContext().deleteFile(key);
				insertedKeys.remove(key);
			}
			System.out.println("Multiple node @ case size: "+getContext().fileList().length);
		}
		else if(selection.equals("*"))
		{
			System.out.println("Deleting all now");
			System.out.println("Multiple node * case size before: "+getContext().fileList().length);
			if(insertedKeys.size()>0) {
				for (String key : insertedKeys) {
					getContext().deleteFile(key);
					insertedKeys.remove(key);
				}
			}
			System.out.println("Multiple node * case size after: "+getContext().fileList().length);

			try {
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						Integer.parseInt(nextNode));
				DataOutputStream output = new DataOutputStream(socket.getOutputStream());
				output.writeUTF("DeleteAll" + "//" + nextNode + "//" + myPort);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			//Multiple nodes, single key case.
			if(insertedKeys.contains(selection)) {
				getContext().deleteFile(selection);
				insertedKeys.remove(selection);
			}
			else
			{
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(nextNode));
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("DeleteOne" + "//" + selection + "//" + myPort);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub

		Object key = null;
		Object value = null;
		for (String k : values.keySet())
		{
			key = values.get("key");
			value = values.get("value");
		}

		System.out.println("Key before hashing is: "+key);

		String hashedKey = "";

		try {
			hashedKey = genHash(key.toString());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		System.out.println("Key after hashing is: "+hashedKey);
		messageKeys.add(hashedKey);
		Collections.sort(messageKeys);
		int keyIndex = messageKeys.indexOf(hashedKey);

		if(keyIndex == 3) {
			System.out.println("Key index of "+key+" is 3");
			node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
			node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
			node3 = hashedNodes.get(messageKeys.get(0));
		}
		else if(keyIndex == 4) {
			System.out.println("Key index of "+key+" is 4");
			node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
			node2 = hashedNodes.get(messageKeys.get(0));
			node3 = hashedNodes.get(messageKeys.get(1));
		}
		else if(keyIndex == 5) {
			System.out.println("Key index of "+key+" is 5");
			node1 = hashedNodes.get(messageKeys.get(0));
			node2 = hashedNodes.get(messageKeys.get(1));
			node3 = hashedNodes.get(messageKeys.get(2));
		}
		else {
			System.out.println("Key index is : "+keyIndex);
			node1 = hashedNodes.get(messageKeys.get(keyIndex+1));
			node2 = hashedNodes.get(messageKeys.get(keyIndex+2));
			node3 = hashedNodes.get(messageKeys.get(keyIndex+3));
		}
		messageKeys.remove(keyIndex);

		System.out.println("For port: "+myPort+", Node 1 is: "+ node1+", Node 2 is: "+ node2+", Node 3 is: "+ node3+" & key is: "+key);

		String packetToSend = "insertmsg" + "//" + node1 + "//" + node2 + "//" + node3 + "//" + key + "//" + value;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, packetToSend);

		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		try
		{
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
			genHash_Port = genHash(portStr);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		try {
			hashedNodes.put(genHash("5554"), REMOTE_PORT0);
			hashedNodes.put(genHash("5556"), REMOTE_PORT1);
			hashedNodes.put(genHash("5558"), REMOTE_PORT2);
			hashedNodes.put(genHash("5560"), REMOTE_PORT3);
			hashedNodes.put(genHash("5562"), REMOTE_PORT4);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		for (String print : hashedNodes.values())
			System.out.println(print);

		for (String key : hashedNodes.keySet())
		{
			messageKeys.add(key);
		}

		for (String value : hashedNodes.values())
		{
			messageValues.add(value);
		}
		int currIndex = messageKeys.indexOf(genHash_Port);

		if(currIndex == messageKeys.size()-1) {
			nextNode = hashedNodes.get(messageKeys.get(0));
		}
		else
		{
			nextNode = hashedNodes.get(messageKeys.get(currIndex+1));
		}

		if(currIndex == 0)
		{
			previousNode1 = hashedNodes.get(messageKeys.get(messageKeys.size()-1));
			previousNode2 = hashedNodes.get(messageKeys.get(messageKeys.size()-2));
		}
		else if(currIndex == 1)
		{
			previousNode1 = hashedNodes.get(messageKeys.get(0));
			previousNode2 = hashedNodes.get(messageKeys.get(messageKeys.size()-1));
		}
		else
		{
			previousNode1 = hashedNodes.get(messageKeys.get(currIndex-1));
			previousNode2 = hashedNodes.get(messageKeys.get(currIndex-2));
		}

		String packet = "recovery" +"//"+ previousNode1 +"//"+ previousNode2 + "//"+ nextNode;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, packet);

		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		String value = "";
		MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
		if(selection.equals("@"))
		{
			System.out.println("Multiple nodes @");
			for (String key : insertedKeys) {
				try {
					System.out.println("File to fetch is: " + key);
					FileInputStream input = getContext().openFileInput(key);
					InputStreamReader isr = new InputStreamReader(input);
					BufferedReader br = new BufferedReader(isr);
					String packet = br.readLine();
					String splitPacket[] = packet.split("@@");
					System.out.println("The value in fetched @ file is: " + splitPacket[0]);
					cursor.addRow(new String[]{key, splitPacket[0]});
					input.close();
				} catch (FileNotFoundException e) {
					System.out.println("File does not exist");
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return cursor;
		}
		else if(selection.equals("*"))
		{
			System.out.println("Multiple nodes *");
			try {
				if(!nextNode.equals(myPort)) {

					System.out.println("AVD no is: "+ nextNode);
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(nextNode));
					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("ReturnAll" + "//" + nextNode+"//"+myPort);

					DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					String packet = input.readUTF();
					System.out.println("The final returned message for * is: "+ packet);
					String keyvalue[] = packet.split("_");
					for(String parts : keyvalue)
					{
						String part[] = parts.split("##");
						String splitPacket[] = part[1].split("@@");
						cursor.addRow(new String[] {part[0],splitPacket[0]});
					}
					return cursor;
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Multiple avds, single key");
			MatrixCursor cursor1 = new MatrixCursor(new String[]{"key", "value"});
			if(insertedKeys.contains(selection)) {
				System.out.println("Key found in current avd");
				try {
					System.out.println("Fetch local file: " + selection);
					FileInputStream input = getContext().openFileInput(selection);
					InputStreamReader isr = new InputStreamReader(input);
					BufferedReader br = new BufferedReader(isr);
					String packet = br.readLine();
					String splitPacket[] = packet.split("@@");
					System.out.println("The value in fetched local file is: " + splitPacket[0]);
					cursor1.addRow(new String[]{selection, splitPacket[0]});
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
				return cursor1;
			}
			else
			{
				System.out.println("Key not found in current avd, next is: "+nextNode);
				try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(nextNode));

					System.out.println("Searching for key in: "+ nextNode);

					DataOutputStream output = new DataOutputStream(socket.getOutputStream());
					output.writeUTF("ReturnOne" + "//" + nextNode + "//" + selection);

					DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					String packet = input.readUTF();
					String keyvalue[] = packet.split("_");
					String splitPacket[] = keyvalue[1].split("@@");
					cursor1.addRow(new String[]{keyvalue[0], splitPacket[0]});
					return cursor1;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected Void doInBackground(ServerSocket... serverSockets) {
			ServerSocket serverSocket = serverSockets[0];
			try {
				while (true) {
					Socket socket = serverSocket.accept();

					DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					String packet = input.readUTF();

					String splitPacket[] = packet.split("//");

					if(splitPacket[0].equals("insertmsg"))
					{
						FileOutputStream outputStream;
						try {
							outputStream = getContext().openFileOutput(splitPacket[2], Context.MODE_PRIVATE);
							String valueToInsert = splitPacket[3] + "@@" + splitPacket[1];
							outputStream.write(valueToInsert.getBytes());
							insertedKeys.add(splitPacket[2]);
							System.out.println("Written file in port: " + splitPacket[1] + ", key: " + splitPacket[2] + " & value: " + splitPacket[3]);
							outputStream.close();
						} catch (Exception e) {
							Log.e(TAG, "File write failed");
							e.printStackTrace();
						}
					}
					else if(splitPacket[0].equals("recovermsgs"))
					{
						String finalMessages = "";
						if(splitPacket[1].equals(myPort))
						{
							System.out.println("In previous port");
							for (String key : insertedKeys)
							{
								FileInputStream in = getContext().openFileInput(key);
								InputStreamReader isr = new InputStreamReader(in);
								BufferedReader br = new BufferedReader(isr);
								String value = br.readLine();
								String keyvalue = key + "##" + value;
								finalMessages += keyvalue + "_";
							}
							System.out.println("In previous, final is: "+finalMessages);
						}
						else
						{
							System.out.println("In next port");
							for (String key : insertedKeys)
							{
								FileInputStream in = getContext().openFileInput(key);
								InputStreamReader isr = new InputStreamReader(in);
								BufferedReader br = new BufferedReader(isr);
								String value = br.readLine();
								String splitValue[] = value.split("@@");
								if(splitValue[1].equals(splitPacket[1]))
								{
									String keyvalue = key + "##" + value;
									finalMessages += keyvalue + "_";
								}
							}
							System.out.println("In next, final is: "+finalMessages);
						}
						DataOutputStream finalOut = new DataOutputStream(socket.getOutputStream());
						finalOut.writeUTF(finalMessages);
					}
					if (splitPacket.length == 3) {
						String finalMessages = "";

						if (splitPacket[0].equals("ReturnAll")) {
							if (!myPort.equals(splitPacket[2])) {
								for (String key : insertedKeys) {
									FileInputStream in = getContext().openFileInput(key);
									InputStreamReader isr = new InputStreamReader(in);
									BufferedReader br = new BufferedReader(isr);
									String value = br.readLine();
									String keyvalue = key + "##" + value;
									finalMessages += keyvalue + "_";
								}

								System.out.println("AVD no is: " + nextNode);
								System.out.println("I am port: " + myPort + " and I am sending to: " + nextNode);
								Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
										Integer.parseInt(nextNode));
								DataOutputStream output = new DataOutputStream(socket1.getOutputStream());
								output.writeUTF("ReturnAll" + "//" + nextNode + "//" + splitPacket[2]);

								DataInputStream msgs = new DataInputStream(socket1.getInputStream());
								String msg = msgs.readUTF();
								finalMessages += msg;

								DataOutputStream finalOut = new DataOutputStream(socket.getOutputStream());
								finalOut.writeUTF(finalMessages);
							} else {
								//Base case to stop
								for (String key : insertedKeys) {
									FileInputStream in = getContext().openFileInput(key);
									InputStreamReader isr = new InputStreamReader(in);
									BufferedReader br = new BufferedReader(isr);
									String value = br.readLine();
									String keyvalue = key + "##" + value;
									finalMessages += keyvalue + "_";
								}
								DataOutputStream out = new DataOutputStream(socket.getOutputStream());
								out.writeUTF(finalMessages);
							}
						}
						else if (splitPacket[0].equals("ReturnOne")) {
							System.out.println("Inside return one case in server");
							if (insertedKeys.contains(splitPacket[2])) {

								System.out.println("Searching for key in: " + myPort);
								try {
									System.out.println("Fetch local file: " + splitPacket[2]);
									FileInputStream inputOne = getContext().openFileInput(splitPacket[2]);
									InputStreamReader isr = new InputStreamReader(inputOne);
									BufferedReader br = new BufferedReader(isr);
									String value = br.readLine();
									System.out.println("The value in fetched local file is: " + value);

									DataOutputStream outOne = new DataOutputStream(socket.getOutputStream());
									outOne.writeUTF(splitPacket[2] + "_" + value);
								} catch (IOException e) {
									e.printStackTrace();
								} catch (RuntimeException e) {
									e.printStackTrace();
								}
							} else {
								try {
									System.out.println("AVD no ** is: " + nextNode);
									Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
											Integer.parseInt(nextNode));

									System.out.println("Searching for key in: " + nextNode);

									DataOutputStream output = new DataOutputStream(socket1.getOutputStream());
									output.writeUTF("ReturnOne" + "//" + nextNode + "//" + splitPacket[2]);

									DataInputStream input1 = new DataInputStream(new BufferedInputStream(socket1.getInputStream()));
									String packet1 = input1.readUTF();

									DataOutputStream outOne = new DataOutputStream(socket.getOutputStream());
									outOne.writeUTF(packet1);
								} catch (UnknownHostException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						else if (splitPacket[0].equals("DeleteAll")) {
							if (!nextNode.equals(splitPacket[2])) {
								System.out.println("Inserted key list size for node before delete is: " + myPort + " is: " + getContext().fileList().length);
								for (String key : insertedKeys) {
									getContext().deleteFile(key);
									insertedKeys.remove(key);
								}
								System.out.println("Inserted key list size for node after delete is: " + myPort + " is: " + getContext().fileList().length);

								try {
									Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
											Integer.parseInt(nextNode));
									DataOutputStream output = new DataOutputStream(socket1.getOutputStream());
									output.writeUTF("DeleteAll" + "//" + nextNode + "//" + splitPacket[2]);

								} catch (NumberFormatException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							} else {
								System.out.println("Deleting in base case node: " + myPort);

								if (insertedKeys.size() > 0) {
									for (String key : insertedKeys) {
										getContext().deleteFile(key);
										insertedKeys.remove(key);
									}
								}
							}
						} else if (splitPacket[0].equals("DeleteOne")) {
							if (insertedKeys.contains(splitPacket[1])) {
								getContext().deleteFile(splitPacket[1]);
								insertedKeys.remove(splitPacket[1]);
							} else {
								if (!nextNode.equals(splitPacket[2])) {
									try {
										Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
												Integer.parseInt(nextNode));
										DataOutputStream output = new DataOutputStream(socket1.getOutputStream());
										output.writeUTF("DeleteOne" + "//" + splitPacket[1] + "//" + splitPacket[2]);

									} catch (NumberFormatException e) {
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	private class ClientTask extends AsyncTask<String, String, String> {

		protected String doInBackground(String... msgs)
		{
			System.out.println("Inside Client Task");
			String packet[] = msgs[0].split("//");


			if(packet[0].equals("insertmsg"))
			{
			String emulatorNo = packet[1];
			String next = packet[2];
			String nextToNext = packet[3];
			String key = packet[4];
			String value = packet[5];

			ArrayList<String> nodesToInsert = new ArrayList<String>();
			nodesToInsert.add(emulatorNo);
			nodesToInsert.add(next);
			nodesToInsert.add(nextToNext);

				try {
					for(String node : nodesToInsert) {
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
								Integer.parseInt(node));

						String packageToSend = "insertmsg" + "//" + node + "//" + key + "//" + value;
						DataOutputStream output = new DataOutputStream(socket.getOutputStream());
						output.writeUTF(packageToSend);
						output.flush();
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
//			else if(packet[0].equals("recovery"))
//			{
//				ArrayList<String> recoveryNodes = new ArrayList<String>();
//				recoveryNodes.add(packet[1]);
//				recoveryNodes.add(packet[2]);
//				recoveryNodes.add(packet[3]);
//
//				for(String node : recoveryNodes) {
//					try {
//						if(node.equals(nextNode)) {
//							Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//									Integer.parseInt(node));
//							String packageToSend = "recovermsgs" + "//" + myPort;
//							DataOutputStream output = new DataOutputStream(socket.getOutputStream());
//							output.writeUTF(packageToSend);
//							output.flush();
//
//							Thread.sleep(2000);
//
//							DataInputStream input = new DataInputStream(socket.getInputStream());
//							String packet1 = input.readUTF();
//							String keyvalue[] = packet1.split("_");
//							Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//									Integer.parseInt(myPort));
//
//							for(String parts : keyvalue)
//							{
//								System.out.println("Recovery pair is: "+parts);
//								String part[] = parts.split("##");
//								String splitPacket[] = part[1].split("@@");
//								String key = part[0];
//								String value = splitPacket[0];
//
//								String toSend = "insertmsg" + "//" + myPort + "//" + key + "//" + value;
//								DataOutputStream outputMsg = new DataOutputStream(socket1.getOutputStream());
//								outputMsg.writeUTF(toSend);
//								outputMsg.flush();
//							}
//						}
//						else
//						{
//							Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//									Integer.parseInt(node));
//							String packageToSend = "recovermsgs" + "//" + node;
//							DataOutputStream output = new DataOutputStream(socket.getOutputStream());
//							output.writeUTF(packageToSend);
//							output.flush();
//
//							Thread.sleep(2000);
//
//							DataInputStream input = new DataInputStream(socket.getInputStream());
//							String packet1 = input.readUTF();
//							String keyvalue[] = packet1.split("_");
//							Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
//									Integer.parseInt(myPort));
//							System.out.println("Recovery packet is: "+packet1);
//
//							for(String parts : keyvalue)
//							{
//								System.out.println("Recovery pair is: "+parts);
//								String part[] = parts.split("##");
//								String splitPacket[] = part[1].split("@@");
//								String key = part[0];
//								String value = splitPacket[0];
//
//								String toSend = "insertmsg" + "//" + myPort + "//" + key + "//" + value;
//								DataOutputStream outputMsg = new DataOutputStream(socket1.getOutputStream());
//								outputMsg.writeUTF(toSend);
//								outputMsg.flush();
//							}
//						}
//					} catch (UnknownHostException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}

			return null;
		}
	}



    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
