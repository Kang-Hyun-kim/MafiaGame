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
	private List<String> userName = new ArrayList(); // 유저 이름 배열
	private String MostVotesPlayer; // 가장 많은 표를 받은 플레이어
	private int mafia_DoctorCount=3; // 의사랑 마피아 인원

	// test용으로 사용할 상태 값
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	private static boolean 아침 = false;
	private static boolean 저녁 = false;
	// @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

	// 역할 상수 정의
	private static final String CITIZEN1 = "시민1";
	private static final String CITIZEN2 = "시민2";
	private static final String CITIZEN3 = "시민3";
	private static final String DOCTOR = "의사";
	private static final String POLICE = "경찰";
	private static final String MAFIA1 = "마피아1";
	private static final String MAFIA2 = "마피아2";

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

//				// 7명의 클라이언트가 모이면 게임 시작
//				if (playerCount >= 7 && !gameStarted) {
//
//					// 역할 할당 메서드
//					assignRolesRandomly();
//					// 게임이 시작되었는지 상태값 변경
//					gameStarted = true;
//					// 낮과 밤의 상태값 변경
//					isDayTime = !isDayTime;
//
//				}

				// 플레이어가 7명 그리고 아침이 f고 저녁도 f일때 실행 = 최초 7명이라면 실행 , 게임이 진행되고 있는 상태에서는 아침이나 저녁의
				// 상태값이 하나라도 t이기 때문
				if (playerCount >= 7 && !(아침) && !(저녁)) {
					// 역할 배정은 최초 1회만 실행하면 된다.
					// 역할();
//						ㄴ> 역할 배정이 끝나면 플레이어들 아이디와 역할을 저장해야한다.
					assignRolesRandomly();
					// 아침상태값 변경
					아침 = true;
					// 저녁상태값 다시변경 초기화 작업이라고 생각해줌
					저녁 = false;
				}

				// 클라이언트의 입력값이 null이 아닐경우 무한반복 [스페이스&엔터도 null이 아님]
				// reader.readLine() 을 만나면 대기상태로 바뀐다. 실제로 while문의 조건에서 멈춰 있는다. message를 대입하기 전에서
				// 대기중
				String message;
				while ((message = reader.readLine()) != null) {
					try {
						Thread.sleep(99); // 0.99초 동안 잠들게 만든다 쓰레드 간섭을 최소화 시키려고 만든 방어로직인데 잘은 모르겠다. 찾아볼것
					} catch (Exception e) {
						System.out.println("Handle>while>Thread.sleep>>>>" + e.getMessage());
					}
					// 아침은 참, 저녁은 거짓
					if ((아침) && !(저녁)) {
						낮(userID, message);

					}

					// 아침은 거짓, 저녁은 참
					if (!(아침) && (저녁)) {
						밤(userID, message);

					}

				}

//게임이 끝날때까지 무한루프를 돌리는 구간 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
				// 클라이언트로부터 메시지 수신 및 브로드캐스트
//				String message;
//				String dTarget = null, mTarget = null, pTarget = null;
//				if (isDayTime) {
//					broadcast("=======================낮입니다======================= [/vote 플레이어이름]으로 투표를 진행해 주세요");
//
//				}
//				boolean one = false;
//				while ((message = reader.readLine()) != null) {
//
//					// 플레이어 메시지가 /인 명령어 호출을 사용했을때 if문 진입
//
//					// Check if it is a voting message
//					if (message.startsWith("/vote")) {
//						// Process the vote
//						vote(userID, message);
//					} else if (!(isDayTime) && gameStarted) {
//
//						if (!one) {
//							broadcast(
//									"--------------밤입니다--------------\n의사는 사람을 살리고\n경찰은 마피아를 찾고\n마피아는 추방 할 플레이어 선택 하세요");
//							one = !(one);
//						}
//
//						if (dTarget != null && dTarget.equals(mTarget))
//							broadcast("뛰어난 의사가 플레이어를 구하였습니다.");
//
//						if (message.startsWith("/role")) {
//							String[] parts = message.split(" ");
//							String target = "";
//							if (parts.length == 2)
//								target = parts[1];
//
//							switch (playerMap.get(userID)) {
//							case DOCTOR -> {
//								dTarget = sendRoleMessage(userID, target);// 플레이어 선택 하고 변수에 저장
//								broadcast("dTarget >>>> " + dTarget);
//							}
//							case MAFIA -> {
//								mTarget = sendRoleMessage(userID, target);// 플레이어 선택 저장
//								broadcast("mTarget >>>> " + mTarget);
//							}
//							case POLICE -> {
//								pTarget = sendRoleMessage(userID, target);// 플레이어 훔쳐보기
//								broadcast("pTarget >>>> " + pTarget);
//							}
//							default -> sendPlayMessage(userID, "시민은 아무것도 할 수 없습니다.");
//
//							}
//						}
//
//						try {
//							Thread.sleep(10);// Thread 간섭 방지 코드
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					} else {
//						// Broadcast regular message to all clients
//						for (PrintWriter clientWriter : clientWriters) {
//							clientWriter.println(userID + ": " + message);
//						}
//					}
//
//				}
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

	// 낮메서드 > 낮에 필요한 메서드를 하위 메서드들로 넣음
	private void 낮(String userID, String message) throws IOException {
		// 아침은 참. 저녁은 거짓
		아침 = true;
		저녁 = false;
		// 유저선택() -> null일수 있다. 조건문으로 검사해야함
		String target = 유저선택(userID, message);
		System.out.println("낮 TARGET >>>>>> " + target);
		// 유저선택한정보공개(투표라면 투표한 상황, 능력사용이라면 능력을 사용한 후 상황)
		target = 유저선택한정보공개(target);

		// 추방
//		deleteUser(target);
		// 추방에 사용한 정보 초기화
		// 게임종료체크(여기에서 밤 낮의 상태값 변경)

	}

	// 밤메서드 > 밤에 필요한 메서드를 하위 메서드들로 넣음
	private void 밤(String userID, String message) throws IOException {
		// 저녁은 참. 아침은 거짓
		아침 = false;
		저녁 = true;
		// 유저선택() -> null일수 있다. 조건문으로 검사해야함
		String target = 유저선택(userID, message);
		System.out.println("밤 TARGET >>>>>> " + target);
		// 유저선택한정보공개(투표라면 투표한 상황, 능력사용이라면 능력을 사용한 후 상황)
		// 추방
		// 게임종료 체크
	}

	// 투표할때 사용할수있고, 밤에는 죽일 유저를 선택할수 있어 공통기능으로 사용될 유저를 선택하는 기능
	private String 유저선택(String myID, String message) throws IOException {
		// 내 아이디에게 어떤 유저를 선택했는지 보여주려면 나의 아이디와 원하는 유저의 아이디가 필요하다

		// 플레이어들의 클라이언트의정보가 담긴 map에서 내 소켓정보를 가져온다.
		PrintWriter writer = new PrintWriter(playerSockets.get(myID).getOutputStream(), true);

		// 아침이 참이고 저녁이 거짓 && message가 /vote로 시작할때
		if (아침 && !(저녁) && message.startsWith("/vote")) {

			// [/vote 유저명]으로 입력받았을때 " "공백을 기준으로 문자배열에 저장 = ["/vote","유저명"]
			String[] wantUserID = message.split(" ");

			// 닉네임이 정상적으로 저장되지 않았을때
			if (wantUserID.length < 2 || 2 > wantUserID.length) {
				writer.println("정상적으로 등록된 유저가 아닙니다.");
				return null;

			}
			// 나에게 출력을 해준다.
			writer.println("지금은 투표중인 아침()>> 낮()>> 조건문");
			writer.println("아침: " + 아침 + "\n저녁: " + 저녁);
			writer.println("playerVotes.containsKey(wantUserID[1]): " + playerVotes.containsKey(myID));

			// 투표한 사람검증 = Map(내아이디,타겟아이디)의 키값이 참인지 거짓인지 있다면 참으로 리턴받는다.
			if (playerVotes.containsKey(myID)) {
				writer.println("투표한사람 검증 조건문 >>>>>>>>");
				writer.println("당신은 이미 투표를 마쳤습니다.");
				return null;

				// 배열의 길이가 2이고 투표MAP에 나의 아이디가 false라면 (투표를 하면 put으로 값을 넣었다)
			} else if (wantUserID.length == 2 && !(playerVotes.containsKey(myID))) {
				// 플레이어별 투표 정보
				playerVotes.put(myID, wantUserID[1]);
				// 플레이어별 투표 수
				voteCounts.put(wantUserID[1], voteCounts.getOrDefault(wantUserID[1], 0) + 1);
				writer.println("내가 선택한 유저닉네임은 [ " + wantUserID[1] + " ] 입니다.");
				return wantUserID[1];
			}
			// 추방하기 위해서 유저이름을 리턴해준다. 비정상일 경우 null을 리턴한다. 그리고 null의 대한 처리는 낮()에서 처리

			// 저녁일때 = 역할에 따라 플레이어를 선택 아이디를 리턴, 시민은 안리턴, 경찰은 직업리턴을 해준다.
//		} else if (!(아침) && 저녁 && message.startsWith("/role")) {
		} else if ((아침) && !저녁) { // 테스트하기위해서 강제로 만듬
//			writer.println("아침 & 저녁 참,거짓 조건문>>>>>>>> 저녁 상태");

			if (message.startsWith("/role")) {

				// [/role 유저명]으로 입력받았을때 " "공백을 기준으로 문자배열에 저장 = ["/role","유저명"]
				String[] wantUserID = message.split(" ");

				// 닉네임이 정상적으로 저장되지 않았을때
				if (wantUserID.length < 2 || 2 > wantUserID.length) {
					writer.println("정상적으로 등록된 유저가 아닙니다.");
					return null;

				}
				// 나에게 출력을 해준다.
//			writer.println("지금은 밤능력사용 저녁()>> 밤()>> 조건문");
//			writer.println("아침: " + 아침 + "\n저녁: " + 저녁);
//			writer.println("playerMap 플레이어 역할 : " + playerMap);
//			writer.println("voteCounts 투표 카운트: " + voteCounts);
//			writer.println("playerVotes 투표 선택: " + playerVotes);
//			writer.println("나의 능력 : " + playerMap.get(myID));
//			writer.println("선택한 유저 능력 : " + playerMap.get(wantUserID[1]));
//
//			writer.println("유저명 : " + myID);
//			writer.println("내가 선택한 유저닉네임은 [ " + wantUserID[1] + " ] 입니다.");

//			writer.println("============================");
//			writer.println("playerMap.get(myID).contains(MAFIA)" + playerMap.get(myID).contains(MAFIA));
//			writer.println("playerMap.get(myID).contains(DOCTOR)" +playerMap.get(myID).contains(DOCTOR));
//			writer.println("playerMap.get(myID).contains(POLICE)"+ playerMap.get(myID).contains(POLICE));
//			writer.println("playerMap.get(myID).contains(CITIZEN)"+ playerMap.get(myID).contains(CITIZEN));
//			writer.println("============================");
//			writer.println("playerVotes.containsKey(myID) : "+ playerVotes.containsKey(myID) );
//			writer.println("( wantUserID.length == 2 && !(playerVotes.containsKey(myID) )&&(\r\n"
//					+ "					playerMap.get(myID).contains(DOCTOR)||\r\n"
//					+ "					playerMap.get(myID).contains(MAFIA) ||\r\n"
//					+ "					playerMap.get(myID).contains(POLICE))) : "+
//					( wantUserID.length == 2 && !(playerVotes.containsKey(myID) )&&(
//							playerMap.get(myID).contains(DOCTOR)||
//							playerMap.get(myID).contains(MAFIA) ||
//							playerMap.get(myID).contains(POLICE))) );
//			writer.println("playerVotes.get(myID).contains(MAFIA)" + playerVotes.get(myID).contains(MAFIA));
//			writer.println("playerVotes.get(myID).contains(DOCTOR)" +playerVotes.get(myID).contains(DOCTOR));
//			writer.println("playerVotes.get(myID).contains(POLICE)"+ playerVotes.get(myID).contains(POLICE));
//			writer.println("playerVotes.get(myID).contains(CITIZEN)"+ playerVotes.get(myID).contains(CITIZEN));
//			writer.println("============================");
				// 마피아,의사 일때

				// 능력사용한 사람검증 = Map(내아이디,타겟아이디)의 키값이 참인지 거짓인지 있다면 참으로 리턴받는다.
				if (playerVotes.containsKey(myID)) {
					writer.println("밤 역할 검증 조건문 >>>>>>>>"); // @@@@@@@@@@@@@@@@@@@@@@
					writer.println("당신은 이미 역할을 마쳤습니다.");
					return null;

					// 배열의 길이가 2이고 투표MAP에 나의 아이디가 false라면 (투표를 하면 put으로 값을 넣었다)
					// 나의 직업이 마피아,경찰,의사인 경우
//			} 
//			else if(playerVotes.size()==0 && (playerMap.get(myID).contains(MAFIA)||playerMap.get(myID).contains(DOCTOR))) {
//				writer.println("사이즈가 0입니다");
//				// 플레이어별 투표 수
//				voteCounts.put(wantUserID[1], voteCounts.getOrDefault(wantUserID[1], 0) + 1);
//				playerVotes.put(myID, wantUserID[1]);
//				writer.println("3내가 선택한 유저닉네임은 [ " + wantUserID[1] + " ] 입니다.");
//				return wantUserID[1];

				} else if ((wantUserID.length == 2 && !(playerVotes.containsKey(myID))
						&& (playerMap.get(myID).contains(DOCTOR) || playerMap.get(myID).contains(MAFIA1)
								|| playerMap.get(myID).contains(MAFIA2) || playerMap.get(myID).contains(POLICE)))) {
					// 플레이어별 투표 정보 ( 투표를 하고 난 후에 초기화 작업이 이루어져야 한다. 게임체크할때 초기화를 해주면 좋을것같다)
					writer.println("나의 직업이 마피아,경찰,의사인 경우 >>>>");
					writer.println("playerMap.get(myID).contains(DOCTOR)>>>>" + playerMap.get(myID).contains(DOCTOR));
					writer.println("playerMap.get(myID).contains(MAFIA1)>>>>" + playerMap.get(myID).contains(MAFIA1));
					writer.println("playerMap.get(myID).contains(MAFIA2)>>>>" + playerMap.get(myID).contains(MAFIA2));
					// 마피아,의사 일때
					if (playerMap.get(myID).contains(DOCTOR) || playerMap.get(myID).contains(MAFIA1)
							|| playerMap.get(myID).contains(MAFIA2)) {
						if (playerMap.get(myID).contains(MAFIA1) || playerMap.get(myID).contains(MAFIA2)) {
							// 플레이어별 투표 수
							voteCounts.put(wantUserID[1], voteCounts.getOrDefault(wantUserID[1], 0) + 1);
						}
						writer.println("[ " + wantUserID[1] + " ]를 선택했습니다.");
						// 마피아와 의사의 선택 Map<직업,타겟유저명>
						playerVotes.put(playerMap.get(myID), wantUserID[1]);
//					playerVotes.put(myID, wantUserID[1]);						
						System.out.println("playerVotes : " + playerVotes);
						return wantUserID[1];
						//// 경찰일때
					} else if (playerMap.get(myID).contains(POLICE)) {
						// 경찰이니까 카운트에 올릴 필요 없음
//					voteCounts.put(wantUserID[1], voteCounts.getOrDefault(wantUserID[1], 0) + 1);
						writer.println("선택한 유저직업은 [ " + playerMap.get(wantUserID[1]) + " ] 입니다.");
						return playerMap.get(wantUserID[1]);
						// 시민일때
					}
				} else if (playerMap.get(myID).contains(CITIZEN1) || playerMap.get(myID).contains(CITIZEN2)
						|| playerMap.get(myID).contains(CITIZEN3)) {
					writer.println("시민은 능력이 없답니다");
					return null;
				}
				// 추방하기 위해서 유저이름을 리턴해준다. 비정상일 경우 null을 리턴한다. 그리고 null의 대한 처리는 낮()에서 처리
				return null;
				// 아침=거짓, 저녁=거짓일때 방어코드
			}
		} else {
			writer.println("명령어를 정확히 입력해주세요");
			return "";
		}

		return message;

		// 만약 경찰이 밤에 유저를 선택했다면? 경찰의 능력을 사용할때이므로 밤일때의 조건에서 유저가 경찰일때 조건으로 유저의 아이디가 아닌 유저의
		// 역할을 리턴해준다.

	}

	private void 추방(String myId, String message) {
//		//가장 많은 표를 받은 플레이어 (추방메서드에서 사용
//		MostVotesPlayer
	}

	private String 유저선택한정보공개(String target) {
		if (저녁 == true) {
			if (playerVotes.size() != playerCount) {
				return null;
			}
			// 아침이라면 투표 최다 득표자를 출력
			int maxVotes = 0;
			for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
				if (entry.getValue() > maxVotes) {
					maxVotes = entry.getValue();
					MostVotesPlayer = entry.getKey();
				}
			}
			if (isTie(maxVotes)) {
				broadcast("동점이 발생하여 추방을 하지 않습니다.");
				return null;
			} else {
				broadcast(MostVotesPlayer + "님이 추방결정되었습니다. (" + maxVotes + "표)");
				return MostVotesPlayer;
			}
			
		} else if (아침 == true && (playerVotes.size() == mafia_DoctorCount)) {
			System.out.println("밤 능력 시작");
			// 저녁이라면 의사가 마피아 선택과 같은지,마피아가 같은인원을 선택해 마피아의 대상을 출력, 마피아가 다른인원을뽑아 추방이안되는출력
			int maxVotes = 0;
			for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
				if (entry.getValue() > maxVotes) {
					maxVotes = entry.getValue();
					MostVotesPlayer = entry.getKey();
				}
			}
			if (playerVotes.size() == mafia_DoctorCount) {
				String mafia1 = playerVotes.get(MAFIA1);
				String mafia2 = playerVotes.get(MAFIA2);
				String doctor = playerVotes.get(DOCTOR);
				if(mafia1.equals(mafia2)) {
					if(mafia1.equals(doctor)) {
						MostVotesPlayer = mafia1;
						System.out.println("의사랑 마피아가 같은 유저를 선택했어요");
						return MostVotesPlayer+"@";//의사와 마피아가 같다는 의미로 특수문자를 넣음
					}
					deleteUser(MostVotesPlayer);; //의사와 값이 다르므로 추방할 유저 아이디 전달					
				}
//				MostVotesPlayer = playerVotes.get(MAFIA1); // 유저명으로 바뀸
//				System.out.println("MostVotesPlayer >>" + playerVotes.get(MAFIA));
//				System.out.println("MostVotesPlayer >>" + MostVotesPlayer);
//				if (MostVotesPlayer.equals(playerVotes.get(DOCTOR))) {
//					MostVotesPlayer = playerVotes.get(DOCTOR);
//					System.out.println("MostVotesPlayer_doctor >>" + MostVotesPlayer);
//					deleteUser(MostVotesPlayer);
//				}
			} else if (voteCounts.size() != mafia_DoctorCount) {
				System.out.println("마피아가 서로 다른 사람 고름");
				MostVotesPlayer = null;
				System.out.println("MostVotesPlyer : " + MostVotesPlayer);
				return null;

			}

		}
		return null;

	}

	private void deleteUser(String dUser) {
		// 해당 플레이어의 클라이언트 소켓을 찾아서 종료합니다.
		System.out.println("dUser = "+dUser);
		
		
		Socket playerSocket = playerSockets.get(dUser);
		if (playerSocket != null && !playerSocket.isClosed()) {
			try {
				playerSocket.close();
				broadcast(dUser + "님의 클라이언트가 종료되었습니다.");
				playerCount--; // 현재 플레이어 인원 감소
				playerSockets.remove(dUser); // hashmap에 저장된 현재 플레이어들의 정보 삭제
				if(!playerVotes.containsKey(DOCTOR)) {mafia_DoctorCount--;}
				if(!playerVotes.containsKey(MAFIA1)) {mafia_DoctorCount--;}
				if(!playerVotes.containsKey(MAFIA2)) {mafia_DoctorCount--;}
				playerVotes.clear(); // 투표정보 초기화
				broadcast("남은 플레이어  : " + playerSockets); ///////////////////////////////////////////////////////
				broadcast("playerVotes.size() : " + playerVotes.size()); ////////////////////////
				System.out.println(mafia_DoctorCount);
			} catch (IOException e) {
				System.err.println("클라이언트 종료 중 오류가 발생했습니다: " + e.getMessage());
			}
		}
//		if(dUser.indexOf() == @)
	}
// 만들어야 할 메서드
	// 유저선택한정보공개(투표라면 투표한 상황, 능력사용이라면 능력을 사용한 후 상황)
	// 추방에 사용한 정보 초기화
	// 게임종료체크(여기에서 밤 낮의 상태값 변경)

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
		availableRoles.add(CITIZEN1);
		availableRoles.add(CITIZEN2);
		availableRoles.add(CITIZEN3);
		availableRoles.add(DOCTOR);
		availableRoles.add(POLICE);
		availableRoles.add(MAFIA1);
		availableRoles.add(MAFIA2);

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
				MostVotesPlayer = entry.getKey();
			}
		}
		if (isTie(maxVotes)) {
			broadcast("동점이 발생하여 추방을 하지 않습니다.");
			initializeVoteCounts(); // 투표 결과 초기화
		} else {
			broadcast(MostVotesPlayer + "님이 추방되었습니다. (" + maxVotes + "표)");
			terminateClient(MostVotesPlayer);
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
