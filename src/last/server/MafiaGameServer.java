package last.server;

import java.io.*;
import java.net.*;
import java.util.*;

import last.controll.MafiaGameController;
import last.controll.MafiaGameControllercopy;

public class MafiaGameServer {

	private Socket socket;
	private PrintWriter writer;
	private String userID;
	private MafiaGameController controller;
	private Set<PrintWriter> clientWriters = new HashSet<>();

	public static void main(String[] args) {

		MafiaGameController controller = new MafiaGameController();
		controller.startServer();
//		MafiaGameControllercopy controller2 = new MafiaGameControllercopy();
//		controller2.startServer();
	}

	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(socket.getOutputStream(), true);

			// 클라이언트로부터 사용자 ID 수신
			userID = reader.readLine();
			System.out.println(userID + "님이 접속하셨습니다.");

			// 모든 클라이언트에게 새 사용자 접속 알림
			for (PrintWriter clientWriter : clientWriters) {
				clientWriter.println(userID + "님이 접속하셨습니다.");
			}
			clientWriters.add(writer);

			// 클라이언트로부터 메시지 수신 및 처리
			String message;
			
			while ((message = reader.readLine()) != null) {
				try {

//					쓰레드를 공유하고 있어 간섭현상나는 것을 방지하기 위한 코드
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.out.println(userID + "님이 나가셨습니다.");
		} finally {
			if (userID != null) {
				clientWriters.remove(writer);
				for (PrintWriter clientWriter : clientWriters) {
					clientWriter.println(userID + "님이 나가셨습니다.");
				}
			}
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
//	}
}
