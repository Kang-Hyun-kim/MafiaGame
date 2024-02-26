package last.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class MafiaGameClient {
	public static void main(String[] args) {
		try (Socket socket = new Socket("localhost", 90)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

			// 사용자 ID 설정
			//방어로직으로 띄워쓰기 금지해야함. /vote를 띄워쓰기로 스플릿을해서 가져오기때문에 금지하는 방향으
			Scanner scanner = new Scanner(System.in);
			System.out.print("사용자 ID를 입력하세요: ");
			String userID = scanner.nextLine();

			// 서버로 사용자 ID 전송
			writer.println(userID);

			// 서버로부터 메시지 수신 및 출력
			Thread receiverThread = new Thread(new Receiver(reader));
			receiverThread.start();

			// 사용자로부터 메시지 입력 및 서버로 전송
			String message;
			while (true) {
				message = scanner.nextLine();
				writer.println(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class Receiver implements Runnable {
		private BufferedReader reader;

		public Receiver(BufferedReader reader) {
			this.reader = reader;
		}

		public void run() {
			try {
				String message;
				while ((message = reader.readLine()) != null) {
						System.out.println(message);
				}
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}

	}
}
