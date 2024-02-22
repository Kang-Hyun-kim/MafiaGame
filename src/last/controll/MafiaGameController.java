package last.controll;

import java.io.*;
import java.net.*;
import java.util.*;

public class MafiaGameController {

	private Set<PrintWriter> clientWriters = new HashSet<>(); // 역할 정보 주소
	private boolean gameStarted = false;
	private boolean isDayTime = false; // 낮과 밤의 상태 값 기본 적으로 밤(false) 상태를 갖는다. 7명의 플레이어가 모여야 게임이 실행된다.
	private int playerCount = 0; // 플레이어 수
	private Map<String, Socket> playerSockets = new HashMap<>(); // 플레이어 이름과 소켓 맵핑
	private Map<String, String> playerVotes = new HashMap<>(); // 플레이어별 투표 정보
	private Map<String, Integer> voteCounts = new HashMap<>(); // 플레이어별 투표 수
	private Map<String, String> playerMap = new HashMap<>(); // 플레이어 역할 정보
	private List<String> roles = new ArrayList<>(); // 역할 정보
	private List<String> userName = new ArrayList();
	private String playerWithMostVotes; // 가장 많은 표를 받은 플레이어
	String SelectUser; // 찾고 싶은 유저 이름
	// 역할 상수 정의
	private static final String CITIZEN = "시민";
	private static final String DOCTOR = "의사";
	private static final String POLICE = "경찰";
	private static final String MAFIA = "마피아";

	private class Handler extends Thread {
		private Socket socket;
		private PrintWriter writer;
		private String userID;

		public Handler(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {

				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream(), true);

				// 클라이언트로부터 사용자 ID 수신
				userID = reader.readLine();
				System.out.println(userID + "님이 연결되었습니다.");
				playerSockets.put(userID, socket);

				playerCount++;

				// 모든 클라이언트에게 새로운 사용자가 연결되었음을 알림
				// 좀 더 자세히 설명하면 다른 클라이언트들의 주소들을 가진 clientWriters 변수에 저장을 했다.
				for (PrintWriter clientWriter : clientWriters) {
					clientWriter.println(userID + "님이 연결되었습니다.");

				}
				// 클라이언트 접속시 clientWriters
				clientWriters.add(writer);
				userName.add(userID); /////////////////////////

				// 7명의 클라이언트가 모이면 게임 시작
				if (playerCount >= 7 && !gameStarted) {

					// 역할 할당 메서드
					assignRolesRandomly();
					// 게임이 시작되었는지 상태값 변경
					gameStarted = true;
					// 낮과 밤의 상태값 변경
					isDayTime = !isDayTime;

				}

//게임이 끝날때까지 무한루프를 돌리는 구간 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
				// 클라이언트로부터 메시지 수신 및 브로드캐스트
				String message;
				String dTarget = null, mTarget = null, pTarget = null;
				if (isDayTime) {
					broadcast("=======================낮입니다======================= [/vote 플레이어이름]으로 투표를 진행해 주세요");
					
				}
				boolean one = false;
				while ((message = reader.readLine()) != null) {

					// 플레이어 메시지가 /인 명령어 호출을 사용했을때 if문 진입

					// Check if it is a voting message
					if (message.startsWith("/vote")) {
						// Process the vote
						vote(userID, message);
					} else if (!(isDayTime) && gameStarted) {
						
						if(!one) {
							broadcast("--------------밤입니다--------------\n의사는 사람을 살리고\n경찰은 마피아를 찾고\n마피아는 추방 할 플레이어 선택 하세요");
							one = !(one);
						}
						
						if (dTarget != null && dTarget.equals(mTarget))
							broadcast("뛰어난 의사가 플레이어를 구하였습니다.");
						
						if (message.startsWith("/role")) {
							String[] parts = message.split(" ");
							String target = "";
							if (parts.length == 2)
								target = parts[1];

							switch (playerMap.get(userID)) {
							case DOCTOR -> {
								dTarget = sendRoleMessage(userID, target);// 플레이어 선택 하고 변수에 저장
								broadcast("dTarget >>>> " + dTarget);
							}
							case MAFIA -> {
								mTarget = sendRoleMessage(userID, target);// 플레이어 선택 저장
								broadcast("mTarget >>>> " + mTarget);
							}
							case POLICE -> {
								pTarget = sendRoleMessage(userID, target);// 플레이어 훔쳐보기
								broadcast("pTarget >>>> " + pTarget);
							}
							default -> sendPlayMessage(userID, "시민은 아무것도 할 수 없습니다.");

							}
						}

						try {
							Thread.sleep(10);//Thread 간섭 방지 코드
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						// Broadcast regular message to all clients
						for (PrintWriter clientWriter : clientWriters) {
							clientWriter.println(userID + ": " + message);
						}
					}

				}
//구간 종료 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
			} catch (IOException e) {
				System.out.println(userID + "님이 나갔습니다.");
			} finally {
				if (userID != null) {
					clientWriters.remove(writer);
					for (PrintWriter clientWriter : clientWriters) {
						clientWriter.println(userID + "님이 나갔습니다.");
					}
				}
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 서버 시작 메서드
	public void startServer() {
		try (ServerSocket serverSocket = new ServerSocket(90)) {
			System.out.println("마피아 게임 서버 시작...");
			while (true) {
				new Handler(serverSocket.accept()).start();
			}
		} catch (IOException e) {
			System.err.println("90 포트에서 서버를 시작할 수 없습니다.");
		}
	}

	// 플레이어에게 무작위 역할 할당
	private void assignRolesRandomly() throws IOException {
		// 역할 목록 생성
		List<String> availableRoles = new ArrayList<>();
		availableRoles.add(CITIZEN);
		availableRoles.add(CITIZEN);
		availableRoles.add(CITIZEN);
		availableRoles.add(DOCTOR);
		availableRoles.add(POLICE);
		availableRoles.add(MAFIA);
		availableRoles.add(MAFIA);

		// 플레이어 수와 역할 수가 일치하는지 확인
		if (playerSockets.size() != availableRoles.size()) {
			System.out.println("플레이어 수와 역할 수가 일치하지 않습니다.");
			return;
		}
		// 플레이어에게 무작위로 역할 할당
		List<String> playerIDs = new ArrayList<>(playerSockets.keySet());
		Collections.shuffle(availableRoles); // 역할 목록을 섞음

		// 플레이어마다 역할을 할당하고 메시지를 전송
		for (int i = 0; i < playerIDs.size(); i++) {
			String playerID = playerIDs.get(i);
			String role = availableRoles.get(i);
			playerMap.put(playerID, role); // 플레이어와 역할 매핑
//			sendRoleMessage(playerID, role); // 플레이어에게 역할 메시지 전송
			PrintWriter writer = new PrintWriter(playerSockets.get(playerID).getOutputStream(), true);
			writer.println("당신의 역할은 " + role + "입니다.");
			// 1번만 사용하기에 메서드 필요가 없음
		}
	}

	// 플레이어 선택 메서드
	// 플레이어에게 역할 메시지를 보내는 메서드
	private String sendRoleMessage(String playerID, String targetID) {
		// 플레이어 ID를 기반으로 선택된 플레이어를 찾음

		try {
			// 선택된 플레이어에게 역할 메시지를 전송
			PrintWriter writer = new PrintWriter(playerSockets.get(playerID).getOutputStream(), true);
			writer.println("[" + targetID + "] 선택");
		} catch (IOException e) {
			System.err.println("플레이어에게 역할 메시지를 보낼 수 없습니다: " + e.getMessage());
			return null;
		}
		return targetID;
	}

	// 클라이언트 > 다른 모든 클라이언트에게 메시지를 출력하는 메서드
	public void broadcastMessage(String userID, String message) {
		// 모든 클라이언트에게 메시지 전송
		for (PrintWriter clientWriter : clientWriters) {
			clientWriter.println(userID + ": " + message);
		}
	}

	// 모든 클라이언트에게 출력 ( 사회자(서버)가 플레이어들에게 공통적으로 보여줄 메시지)
	private void broadcast(String message) {
		for (PrintWriter clientWriter : clientWriters) {
			clientWriter.println(message);
		}
	}

	// 특정 플레이어에게 메시지를 전달하는 메서드
	private void sendPlayMessage(String playerID, String message) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(playerSockets.get(playerID).getOutputStream(), true);
			writer.println(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println();
	}

	// 투표하기
	public void vote(String userID, String voteMessage) {

		// 게임이 낮에 진행 중인지 확인
		if (!isDayTime) {
			// 낮이 아닌 시간에는 투표를 허용하지 않음
			System.out.println("현재는 낮 시간이 아닙니다. 투표는 낮에만 가능합니다.");
			return;
		}

		// 이미 투표한 플레이어인지 확인
		if (playerVotes.containsKey(userID)) {
			broadcast(userID + "님은 이미 투표하셨습니다.");
			return; // 이미 투표한 경우에는 추가적인 투표를 허용하지 않음
		}
		// '/vote playerName'에서 playerName 추출
		String[] parts = voteMessage.split(" ");
		// 투표 가능한 플레이어인지 확인하기 위해서는 투표한 값에서 split으로 유저명의 값이 담긴 parts를 사용
		if (!(userName.contains(parts[1]))) {// false면 실행
			broadcast("없는 유저입니다. 다시 투표해 주세요");
			return;
		}

		if (parts.length == 2) {
			String playerName = parts[1];
			// 플레이어가 투표한 사람 정보 저장
			playerVotes.put(userID, playerName);
			// 투표 결과를 처리하고 클라이언트에게 알림
			updateVoteCounts(playerName);
			broadcastVoteStatus();
			if (playerVotes.size() == playerCount) {
				handleVoteResult();
			}
		} else {

		}
	}

	// 플레이어별 투표 수 업데이트
	private void updateVoteCounts(String votee) {
		voteCounts.put(votee, voteCounts.getOrDefault(votee, 0) + 1);

	}

	// 투표 상태를 클라이언트에게 전송
	private void broadcastVoteStatus() {
//		System.out.println("broadcastVoteStatus 진입");/////////////////////////////////////
		StringBuilder voteStatus = new StringBuilder();
		voteStatus.append("현재 투표 상태:\n");

		for (Map.Entry<String, String> entry : playerVotes.entrySet()) {
//			System.out.println("broadcastVoteStatus > forEach 진입");/////////////////////////////////////
			voteStatus.append(entry.getKey()).append("님이 ").append(entry.getValue()).append("님에게 투표하셨습니다.\n");
//			System.out.println("voteStatus " + voteStatus + "] entry.getKey()" + entry.getKey() + "entry.getValue()"
//					+ entry.getValue());/////////////////////////////////////
		}
		// 모든 클라이언트에게 투표 상태 전송
		for (PrintWriter clientWriter : clientWriters) {

			clientWriter.println(voteStatus.toString());
		}
	}

	// 투표 결과 처리
	private void handleVoteResult() {
		int maxVotes = 0;
		for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
			if (entry.getValue() > maxVotes) {
				maxVotes = entry.getValue();
				playerWithMostVotes = entry.getKey();
			}
		}
		if (isTie(maxVotes)) {
			broadcast("동점이 발생하여 추방을 하지 않습니다.");
			initializeVoteCounts(); // 투표 결과 초기화
		} else {
			broadcast(playerWithMostVotes + "님이 추방되었습니다. (" + maxVotes + "표)");
			terminateClient(playerWithMostVotes);
		}

		// 투표 결과를 처리한 후에는 voteCounts를 초기화해야 함
		initializeVoteCounts();

		// 모든 플레이어의 투표를 초기화
		playerVotes.clear();
		// 투표 종료전 상태 변경
		isDayTime = !(isDayTime);
	}

	// 동점 여부 확인
	private boolean isTie(int maxVotes) {
		int count = 0;
		for (int votes : voteCounts.values()) {
			if (votes == maxVotes) {
				count++;
			}
		}
		return count > 1;
	}

	// 투표 수 초기화
	private void initializeVoteCounts() {
		voteCounts.clear();
	}

	// 60초 Timer(Timer, 500); 추가 할지 말지 고민중

	// @@@@@@@@@@@@@@@@@@@@@@
	// 게임의 현재 상태를 리턴해주는 메서드( 낮, 밤, 투표, 밤이였을때 할 수 있는 플레이어 역할들 ) 필요한지 잘 모르겠음

	// 의사가 플레이어 지목 ( 밤일때)
	private void doctorNightTarget(String player) {
		// 메서드가 시작이 될때 플레이어가 의사여야만 된다. 플레이어 역할을 전달해주어야한다.
		if (!(isDayTime)) {
			if (playerMap.containsKey(DOCTOR))//
				return;
		}

		sendPlayMessage(player, "의사만 사용 가능한 명령어입니다.");

	}
	// 경찰이 플레이어 역할 보기

	// 마피아 플레이어 지목 (1명일때, 2명일때) = 리턴 값으로 유저 이름을 전달 없으면 null이나 공백으로 예외를 발생시킨다.

	// 의사가 고른 플레이어 & 마피아가 고른 플레이어(예외 값 들어있을 수 있음)를 교차 검증하여 추방하거나 추방하지 않는 메서드

	// 추방된 사람들끼리 채팅방을 만들어주는 메서드 해보고 싶다.. 그냥 희망사항

	// 플레이어 클라이언트 종료
	private void terminateClient(String player) {
		broadcast("player -> " + player);
		// 해당 플레이어의 클라이언트 소켓을 찾아서 종료합니다.
		Socket playerSocket = playerSockets.get(player);
		broadcast("playerSocket -> " + playerSocket);
		broadcast("playerSocket.isClosed ->" + playerSocket.isClosed());
		if (playerSocket != null && !playerSocket.isClosed()) {
			broadcast("if문 진입 -> ");
			try {
				playerSocket.close();
				broadcast(player + "님의 클라이언트가 종료되었습니다.");
				isDayTime = !isDayTime; // 낮인지 밤인지 상태값 변경 투표 종료시 밤으로 변경
				playerCount--; // 현재 플레이어 인원 감소
				broadcast("남은 플레이어 수 : " + playerCount);/////////////////////////////////////////////////////////
				playerSockets.remove(player); // hashmap에 저장된 현재 플레이어들의 정보 삭제
				playerVotes.remove(player); // hashmap에 저장된 현재 플레이어들의 정보 삭제
				clientWriters.remove(player); // 플레이어 주소정보 삭제
				userName.removeIf(item -> item.equals(player));
				broadcast("userName : " + userName);
				broadcast("남은 플레이어  : " + playerSockets); ///////////////////////////////////////////////////////
				broadcast("playerVotes.size() : " + playerVotes.size()); ////////////////////////
			} catch (IOException e) {
				System.err.println("클라이언트 종료 중 오류가 발생했습니다: " + e.getMessage());
			}
		}
	}

	// 클라이언트가 메시지를 전송할 때 호출됨 (지금 사용안하고 있음 추후에 완성시 사용안하면 삭제 예정)
	public void processClientMessage(String userID, String message) {
		if (message.startsWith("/vote")) {
			vote(userID, message);

		} else {
			broadcastMessage(userID, message);

		}
	}

}
