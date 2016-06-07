import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class dbengineRTP {
	
	public static void main(String args[]) throws IOException {
		int port = -1;
		Student[] students = new Student[7];
		
		if (args.length != 1){
			System.out.println("Error. Must enter the port number in the following format...");
			System.out.println("dbengineTCP port#");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Error. " + args[0] + " is not a valid port.");
			System.exit(1);
		}
		populateDB(students);
		
		//create server socket
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(port);
			RTP rtp = new RTP(socket, 1000);
			System.out.println("Server is listening... (" + InetAddress.getLocalHost().toString() + ", " + port + ")");
			rtp.listen(socket);
			while (true) {
				for (int i = 0; i < rtp.MAX_CONNECTIONS; i++) {
					//receive student ID/queries from client
					byte[] data = rtp.receive(i);
					if (data != null) {
						String queries = new String(data, "UTF-8");
						String[] parsedQueries = queries.split("\\s+");							
						
						//check if ID exists in data base
						Student temp = null;
						for (int j = 0; j < students.length; j++) {
							if (students[j].ID.equals(parsedQueries[0])) {
								temp = students[i];
							}
				    	}
						
						//Retrieve requested information about student
						//send information back to client
						String serverResponse = "From Server: ";
						byte[] sendData; 
						if (temp != null) {
							for (int j = 1; j < parsedQueries.length; j++) {
								if (parsedQueries[j].equals("last_name")) {
									serverResponse += "last_name: " + temp.last_name + ", ";
								} else if (parsedQueries[j].equals("first_name")) {
									serverResponse += "first_name: " + temp.first_name + ", ";
								} else if (parsedQueries[j].equals("gpa")) {
									serverResponse += "gpa: " + temp.gpa + ", ";
								} else if (parsedQueries[j].equals("quality_points")) {
									serverResponse += "quality_points: " + temp.quality_points + ", ";
								} else if (parsedQueries[j].equals("gpa_hours")) {
									serverResponse += "gpa_hours: " + temp.gpa_hours + ", ";
								} else {
									sendData = new String("Invalid Query: " + parsedQueries[i]).getBytes();
									break;
								}		
							}
							sendData = serverResponse.getBytes();
						} else {
							sendData = new String("Student ID does not exist.").getBytes();
						}
						// Send response to client
						int clientPort = rtp.getConnections()[i].getClientPort();
						InetAddress clientIP = rtp.getConnections()[i].getSendIP();
						rtp.send(socket, port, clientPort, clientIP, sendData);
					}
				}			
			} 
		} catch (IOException e) { 
			e.printStackTrace();
	        System.exit(1); 
		}
	}
	private static class Student {
		String ID, first_name, last_name;
		int quality_points, gpa_hours;
		double gpa;
		
		public Student(String ID, String first_name, String last_name, int quality_points, int gpa_hours, double gpa) {
			this.ID = ID; 
			this.first_name = first_name;
			this.last_name = last_name;
			this.quality_points = quality_points;
			this.gpa_hours = gpa_hours;
			this.gpa = gpa;
		}
	}
	public static Student[] populateDB(Student[] students) {
		students[0] = new Student("903076259","Anthony", "Peterson", 231, 63, 3.666667);
		students[1] = new Student("903084074", "Richard", "Harris", 236, 66, 3.575758);
		students[2] = new Student("903077650", "Joe", "Miller", 224, 65, 3.446154);
		students[3] = new Student("903083691", "Todd", "Collins", 218, 56, 3.892857);
		students[4] = new Student("903082265", "Laura", "Stewart", 207, 64, 3.234375);
		students[5] = new Student("903075951", "Marie", "Cox", 246, 63, 3.904762);
		students[6] = new Student("903084336", "Stephen", "Baker", 234, 66, 3.545455);
		return students;
	}
}
