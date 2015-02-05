package evans.dave.AndrotagServer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import evans.dave.AndrotagCommon.TCPMessage;
import evans.dave.duinotag.*;

public class TCPServer {

	static HashTable<byte,Game> games = new HashTable<byte,Game>();

	// (ArrayList<Game>) Collections.synchronizedList(
	static class ServerThread implements Runnable {
		Socket client = null;

		public ServerThread(Socket c) {
			this.client = c;
		}

		public void run() {
			try {
				System.out.println("Connected to client : "
						+ client.getInetAddress().getHostName());

				ObjectOutputStream oos = new ObjectOutputStream(
						client.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(
						client.getInputStream());
				
				byte code;
				byte gid;
				byte tid;
				Game game;
				
				// Try to read client message
				read_stream: while (true) {
					// Check first character
					code = TCPMessage.CLOSE;
					try {
						code = ois.readByte();
					} catch (EOFException e) {
						break read_stream;
					}
					;

					System.out.println("Read " + code);

					switch (code) {
					case TCPMessage.CLOSE:
						break read_stream;

					case TCPMessage.STRING_MSG:
						String str = (String) ois.readObject();
						System.out.println("Received: " + str);
						break;
						
					case TCPMessage.GET_GAME_LIST:
						oos.writeByte(TCPMessage.GET_GAME_LIST);
						oos.writeObject(getGameList());
						oos.flush();
						break;
					
					case TCPMessage.GET_GAME:
						// Get the game id that they want
						gid = ois.readByte();
						// Send the data back
						oos.writeByte(TCPMessage.GET_GAME);
						// Automatically returns NO_GAME if game isn't available
						oos.writeObject(findGame(gid)); 
						oos.flush();
						break;
					
					case TCPMessage.REQ_JOIN_GAME:
						// Get the game id that they want
						gid = ois.readByte();
						oos.writeByte(TCPMessage.GET_GAME);
						// Check if we can add a player to the game
						
						// Check game slots
						Game game = findGame(gid);
						if (game == Game.NO_GAME || game.isFull()){
							oos.writeByte(0); // Deny join
						}else {
							oos.writeByte(1); // Confirm join
							oos.writeObject(game); // Return game info
						}
						oos.flush();
						break;
						
					case TCPMessage.REQ_JOIN_TEAM:
						// Get the game id, and team id, and user who's requesting
						gid = ois.readByte();
						tid = ois.readByte();
						User user = (User) ois.readObject();
						
						// Check if team is full
						game = findGame(gid);
						
						// Try and add to game
						boolean success = game.addUserToTeam(user, tid);
						
						if (!success){
							oos.writeByte(0); // Deny join
						} else {
							oos.writeByte(1); // Allow join
							oos.writeObject(game);
						}
						oos.flush();
						break;
						
					case TCPMessage.REQ_CREATE_GAME:
						
						// Get the game they want to create
						Game game = (GameSettings) ois.readObject();
						
						// Check if it conflicts with a game already made
						
						
						
					}
				}
				System.out.println("Closed client : "
						+ client.getInetAddress().getHostName());
				client.close();

			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	/** Main program **/
	public static void main(String args[]) {

		// Set up some example games
		GeneralPlayer davek = new GeneralPlayer(new User("Davek", 0),
				Team.NO_TEAM);
		GeneralPlayer studley = new GeneralPlayer(new User("Studley Hungwell",
				6969), Team.NO_TEAM);
		GeneralPlayer dfarce = new GeneralPlayer(new User("DFarce", 001),
				Team.NO_TEAM);

		games.put(1, new Game(1, 2, 10, System.currentTimeMillis(), System
				.currentTimeMillis() + 60 * 1000 * 5, new Team[] {
				new Team(0, 0xFF0000, "Red"), new Team(1, 0x0000FF, "Blue") },
				25, dfarce.user));

		games.put(2, new Game(2, 3, 255,
				System.currentTimeMillis() + 60 * 1000 * 5, System
						.currentTimeMillis() + 60 * 1000 * 15, new Team[] {
						new Team(4, 0xFFFF00, "Blazers Lasers"),
						new Team(1, 0x00FFFF, "Blu dabadeee"),
						new Team(3, 0xFFFFFF, "White Power") }, 100,
				studley.user));

		games.put(4, new Game(4, 4, 255,
				System.currentTimeMillis() + 60 * 1000 * 5, System
						.currentTimeMillis() + 60 * 1000 * 15, new Team[] {
						new Team(100, 0xFFFF00, "Just me"),
						new Team(2, 0x00FFFF, "Team 2") }, 5, new User(
						"Nick knack paddy whack", 2)));

		games.get(1).teams[0].add(davek);
		games.get(1).teams[0].add(studley);
		games.get(4).teams[1].add(dfarce);

		try {
			ServerSocket server = new ServerSocket(7000);
			while (true) {
				Socket p = server.accept();
				new Thread(new ServerThread(p)).start();
			}
		} catch (Exception ex) {
			System.err.println("Error : " + ex.getMessage());
		}

	}

	private static HashTable<GameInfo> getGameList() {
		HashTable<GameInfo> gameList = new HashTable<GameInfo>();
		for (GameInfo g : games) {
			gameList.put(g.id, g.getSuper());
		}
		return gameList;
	}
	
	private static Game findGame(byte gid){
		if (games.containsKey(gid))
		for (Game g : games)
			if (g.id == gid)
				return g;
		return Game.NO_GAME; // TODO: Make Game.NO_GAME class
	}
}