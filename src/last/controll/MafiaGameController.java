package last.controll;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


public class MafiaGameController {

	private Set<PrintWriter> clientWriters = new HashSet<>(); // 역할 정보 주소
	private int playerCount = 0; // 플레이어 수
	private Map<String, Socket> playerSockets = new HashMap<>(); // 플레이어 이름과 소켓 맵핑
	private Map<String, String> playerVotes = new HashMap<>(); // 플레이어별 투표 정보
	private Map<String, Integer> voteCounts = new HashMap<>(); // 플레이어별 투표 수
	private Map<String, String> playerMap = new HashMap<>(); // 플레이어 역할 정보
	private Map<String, String> copyPlayerMap = new HashMap<>(); // 플레이어 역할 정보 복사
	private static final int MAX_CLIENTS = 7;
	
	private boolean mafiaWin = false;
	private boolean civilWin = false;
    
	private List<String> userName = new ArrayList<String>(); // 유저 이름 배열
	private String MostVotesPlayer; // 가장 많은 표를 받은 플레이어
	
	private PreparedStatement statement; // 데이터베이스

	// 데이터베이스 연결 정보
	private static final String JDBC_URL = "jdbc:mysql://localhost:3306/mafia";
	private static final String USERNAME = "java";
	private static final String PASSWORD = "mysql";
	
	private static boolean MORNING = false;
	private static boolean EVENING = false;

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
				Iterator<PrintWriter> iterator = clientWriters.iterator();
				while (iterator.hasNext()) {
				    PrintWriter clientWriter = iterator.next();
				    clientWriter.println(userID + "님이 연결되었습니다.");
				}

				// 클라이언트 접속시 clientWriters
				clientWriters.add(writer);
				userName.add(userID); /////////////////////////


				// 플레이어가 7명 그리고 아침이 f고 저녁도 f일때 실행 = 최초 7명이라면 실행 , 게임이 진행되고 있는 상태에서는 아침이나 저녁의
				// 상태값이 하나라도 t이기 때문
				if (playerCount >= 7 && !(MORNING) && !(EVENING)) {
					// 역할 배정은 최초 1회만 실행하면 된다.
					// 역할();
//						ㄴ> 역할 배정이 끝나면 플레이어들 아이디와 역할을 저장해야한다.
					assignRolesRandomly();
					// 아침상태값 변경
					MORNING = true;
					// 저녁상태값 다시변경 초기화 작업이라고 생각해줌
					EVENING = false;
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
					if ((MORNING) && !(EVENING)) {
						dayTime(userID, message);

					}
					// 아침은 거짓, 저녁은 참
					if (!(MORNING) && (EVENING)) {
						night(userID, message);

					}
					if(!message.startsWith("/"))
					broadcastMessage(userID,message);
					
					if(!MORNING && !EVENING) {
						System.exit(0);
					}

				}

			} catch (IOException e) {
				System.out.println("[ "+userID +" ]님이 나갔습니다.");
				System.out.println(e.getMessage());
			} finally {
//				if (userID != null) {
//					clientWriters.remove(writer);
//					Iterator<PrintWriter> iterator = clientWriters.iterator();
//					while (iterator.hasNext()) {
//					    PrintWriter clientWriter = iterator.next();
//					    clientWriter.println(userID + "님이 나갔습니다.");
//					}
//
//				}
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
	            Socket clientSocket = serverSocket.accept();
	            if (playerCount < MAX_CLIENTS) {
	                new Handler(clientSocket).start();
	            } else {
	                System.out.println("최대 허용 클라이언트 수를 초과하여 새로운 클라이언트의 접속을 거부합니다.");
	                clientSocket.close();
	            }
			}
		} catch (IOException e) {
			System.err.println("90 포트에서 서버를 시작할 수 없습니다.");
		}
	}

	// 낮메서드 > 낮에 필요한 메서드를 하위 메서드들로 넣음
	private void dayTime(String userID, String message) throws IOException {
		MORNING = true;
		EVENING = false;
		System.out.println("현재 게임 상태는 낮입니다.");
		// UserSelection() -> null일수 있다. 조건문으로 검사해야함
		String target = UserSelection(userID, message);
		System.out.println("현재 게임 상태는 낮의 유저선택() 시간입니다.");
		System.out.println("유저선택()의 리턴 값입니다.\nㄴ Target : " + target);
		// selectedInformation(투표라면 투표한 상황, 능력사용이라면 능력을 사용한 후 상황)
		target = selectedInformation(target);
		System.out.println("현재 게임 상태는 낮의 유저선택한정보공개() 시간");
		System.out.println("유저선택한정보공개()의 리턴 값입니다.\nㄴ Target : " + target);
		// 추방
		System.out.println("현재 게임 상태는 낮의 추방() 입니다.");
		ClientOut(target);
		if(EVENING)
		broadcast("밤이 되었습니다 [/role 플레이어이름]을 사용하여 능력사용을 진행 할 수 있습니다.");
	}

	// 밤메서드 > 밤에 필요한 메서드를 하위 메서드들로 넣음
	private void night(String userID, String message) throws IOException {
		MORNING = false;
		EVENING = true;
		System.out.println("현재 게임 상태는 밤입니다.");
		

		// UserSelection() -> null일수 있다. 조건문으로 검사해야함
		String target = UserSelection(userID, message);
		System.out.println("현재 게임 상태는 밤의 유저선택() 시간입니다.");
		System.out.println("유저선택()의 리턴 값입니다.\nㄴ Target : " + target);
		// selectedInformation(투표라면 투표한 상황, 능력사용이라면 능력을 사용한 후 상황)
		target = selectedInformation(target);
		System.out.println("현재 게임 상태는 밤의 유저선택한정보공개() 시간");
		System.out.println("유저선택한정보공개()의 리턴 값입니다.\nㄴ Target : " + target);
		// 추방
		System.out.println("현재 게임 상태는 밤의 추방() 입니다.");
		ClientOut(target);
		
		if(MORNING) {
			broadcast("낮이 되었습니다 [/vote 플레이어이름]을 사용하여 능력사용을 진행 할 수 있습니다.");
			broadcast("");
			
		}
	}

	// 투표할때 사용할수있고, 밤에는 죽일 유저를 선택할수 있어 공통기능으로 사용될 유저를 선택하는 기능
	private String UserSelection(String myID, String message) throws IOException {
		// 내 아이디에게 어떤 유저를 선택했는지 보여주려면 나의 아이디와 원하는 유저의 아이디가 필요하다
		
		// 플레이어들의 클라이언트의정보가 담긴 map에서 내 소켓정보를 가져온다.
		PrintWriter writer = new PrintWriter(playerSockets.get(myID).getOutputStream(), true);

		// 아침이 참이고 저녁이 거짓 && message가 /vote로 시작할때
		if (MORNING && !(EVENING) && message.startsWith("/vote")) {
						
			// [/vote 유저명]으로 입력받았을때 " "공백을 기준으로 문자배열에 저장 = ["/vote","유저명"]
			String[] wantUserID = message.split(" ");

			// 닉네임이 정상적으로 저장되지 않았을때
			if (wantUserID.length < 2 || 2 > wantUserID.length) {
				writer.println("정상적으로 등록된 유저가 아닙니다.");
				return null;

			}
//			//현재 플레이중인 유저들의 아이디와 투표한 유저의 아이디가 존재 하지 않는다면?
//			if(playerMap.get(wantUserID) == null) {
//				System.out.println(myID+"님이 "+wantUserID+"라는 없는 아이디를 선택하였습니다.");
//				
//				return UserSelection(myID, message);
//			}
			// 투표한 사람검증 = Map(내아이디,타겟아이디)의 키값이 참인지 거짓인지 있다면 참으로 리턴받는다.
			if (playerVotes.containsKey(myID)) {
//				writer.println("투표한사람 검증 조건문 >>>>>>>>");
				writer.println("당신은 이미 투표를 마쳤습니다.");
				return null;

				// 배열의 길이가 2이고 투표MAP에 나의 아이디가 false라면 (투표를 하면 put으로 값을 넣었다)
			} else if (wantUserID.length == 2 && !(playerVotes.containsKey(myID))) {
				if (!playerMap.containsKey(wantUserID[1])) {
				    // 생존자 목록에 없는 경우
				    writer.println("내가 선택한 유저 닉네임은 현재 생존자 목록에 없습니다.");
				} else {
				    // 생존자 목록에 있는 경우
				    playerVotes.put(myID, wantUserID[1]);
				    updateVoteCounts(wantUserID[1]);
				    writer.println("내가 선택한 유저 닉네임은 [ " + wantUserID[1] + " ] 입니다.");
				    message = wantUserID[1];
				}
			}
			// 추방하기 위해서 유저이름을 리턴해준다. 비정상일 경우 null을 리턴한다. 마지막 메서드에서 null처리
			
			
			
			// 저녁일때 = 역할에 따라 플레이어를 선택 아이디를 리턴, 시민은 안리턴, 경찰은 직업리턴을 해준다.
		} else if (!(MORNING) && EVENING && message.startsWith("/role")) {
//		} else if ((아침) && !저녁) { // 테스트하기위해서 강제로 만듬
//			writer.println("아침 & 저녁 참,거짓 조건문>>>>>>>> 저녁 상태");

			if (message.startsWith("/role")) {

				// [/role 유저명]으로 입력받았을때 " "공백을 기준으로 문자배열에 저장 = ["/role","유저명"]
				String[] wantUserID = message.split(" ");
				
				if (!playerMap.containsKey(wantUserID[1])) {
				    // 생존자 목록에 없는 경우
				    writer.println("내가 선택한 유저 닉네임은 현재 생존자 목록에 없습니다.");
				    return null;
				}
				// 닉네임이 정상적으로 저장되지 않았을때
				if (wantUserID.length < 2 || 2 > wantUserID.length) {
					writer.println("정상적으로 등록된 유저가 아닙니다.");
					return null;

				}
				
				// 나에게 출력을 해준다.
				String myJob = playerMap.get(myID);
//				broadcast("myID ->" + myID);
//				broadcast("getKey(playerMap,myID) ->" + getKey(playerMap,myID));
//				broadcast("myJob ->" + myJob);
//				broadcast("playerVotes.containsKey(myJob) ->" + playerVotes.containsKey(myJob));
//				broadcast("playerMap ->" + playerMap);
//				broadcast("playerVotes ->" + playerVotes);
				// 능력사용한 사람검증 = Map(내아이디,타겟아이디)의 키값이 참인지 거짓인지 있다면 참으로 리턴받는다.
				if (playerVotes.containsKey(myJob)) {
					writer.println("밤 역할 검증 조건문 >>>>>>>>"); // @@@@@@@@@@@@@@@@@@@@@@
					writer.println("당신은 이미 역할을 마쳤습니다.");
					return null;


				} else if ((wantUserID.length == 2 && !(playerVotes.containsKey(myID))
						&& (playerMap.get(myID).contains(DOCTOR) || playerMap.get(myID).contains(MAFIA1)
								|| playerMap.get(myID).contains(MAFIA2) || playerMap.get(myID).contains(POLICE)))) {
					// 플레이어별 투표 정보 ( 투표를 하고 난 후에 초기화 작업이 이루어져야 한다. 게임체크할때 초기화를 해주면 좋을것같다)
//					writer.println("나의 직업이 마피아,경찰,의사인 경우 >>>>");
					writer.println("playerVotes >>>" + playerVotes );
//					writer.println("playerMap.get(myID).contains(DOCTOR)>>>>" + playerMap.get(myID).contains(DOCTOR));
//					writer.println("playerMap.get(myID).contains(MAFIA1)>>>>" + playerMap.get(myID).contains(MAFIA1));
//					writer.println("playerMap.get(myID).contains(MAFIA2)>>>>" + playerMap.get(myID).contains(MAFIA2));
					// 마피아,의사 일때
					if (playerMap.get(myID).contains(DOCTOR) || playerMap.get(myID).contains(MAFIA1)
							|| playerMap.get(myID).contains(MAFIA2)) {
						if (playerMap.get(myID).contains(MAFIA1) || playerMap.get(myID).contains(MAFIA2)) {
							// 플레이어별 투표 수
							updateVoteCounts(wantUserID[1]);
						}
						writer.println("[ " + wantUserID[1] + " ]를 선택했습니다.");
						// 마피아와 의사의 선택 Map<직업,타겟유저명>
						playerVotes.put(playerMap.get(myID), wantUserID[1]);
						return wantUserID[1];
						//// 경찰일때
					} else if (playerMap.get(myID).contains(POLICE)) {
						// 경찰이니까 카운트에 올릴 필요 없음
						playerVotes.put(playerMap.get(myID), wantUserID[1]);
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
		
		} else if(MORNING&&message.startsWith("/role")){
			writer.println("지금은 아침입니다");
			return null;
		} else if(EVENING && message.startsWith("/vote")){
			writer.println("지금은 저녁입니다 투표를 할 수 없습니다.");
			
			return null;
		} else if(message.startsWith("/")){
			writer.println("명령어를 정확히 입력해주세요");
			return null;
		}
		return message;

		// 만약 경찰이 밤에 유저를 선택했다면? 경찰의 능력을 사용할때이므로 밤일때의 조건에서 유저가 경찰일때 조건으로 유저의 아이디가 아닌 유저의
		// 역할을 리턴해준다.

	}

	private String selectedInformation(String target) {
		String killUser = null;
		if (MORNING) {
			if (playerVotes.size() != playerCount) {
				return null;
			}
			// 아침이라면 투표 최다 득표자를 출력
			int maxVotes = 0;
			Iterator<Map.Entry<String, Integer>> iterator = voteCounts.entrySet().iterator();
			while (iterator.hasNext()) {
			    Map.Entry<String, Integer> entry = iterator.next();
			    if (entry.getValue() > maxVotes) {
			        maxVotes = entry.getValue();
			        MostVotesPlayer = entry.getKey();
			    }
			}

			if (isTie(maxVotes)) {
				broadcast("동점이 발생하여 추방을 하지 않습니다.");
				return "reset";
			} else {
				broadcast(MostVotesPlayer + "님이 추방결정되었습니다. (" + maxVotes + "표)");
				killUser = MostVotesPlayer;
				return killUser;
			}

		}
		if (EVENING) {
			// 저녁이라면 의사가 마피아 선택과 같은지,마피아가 같은인원을 선택해 마피아의 대상을 출력, 마피아가 다른인원을뽑아 추방이안되는출력			
			boolean doctorsLive = false; //의사 존재하는지 체크
			boolean mafiaTwo = false;	//두명의 마피아가 살아있는지 체크
			boolean mafiaOne = false;	//마피아가 한명 살아있는지 체크
			String liveOneMafia = null ; // 살아남은 마피아
			String Mafiachose = null;	// 마피아들이 지목한 사람
			killUser = null;		//죽일대상 리턴용

			
			//일단 map에 마피아가 두명 있는지 확인
			if( playerMap.values().contains(MAFIA1) && playerMap.values().contains(MAFIA2)) {
				mafiaTwo = true; // 두명 있다면 두명에 대한 로직 실행
			}else if(playerMap.values().contains(MAFIA1) || playerMap.values().contains(MAFIA2)) {
				mafiaOne = true; // 마피아가 한명일때
				//마피아가 한명일때 역할의 이름을 리턴해준다.
				if(playerMap.values().contains(MAFIA1)) {
					liveOneMafia = MAFIA1; 
				}
				else if( playerMap.values().contains(MAFIA2)) {
					liveOneMafia = MAFIA2; 
				}
			}
			if(playerMap.values().contains(DOCTOR)) {
				doctorsLive = true; // 의사가 살아있다면 true
			}
			
			//경우의 수 다 씀
			//마피아둘이랑 의사랑 살아있을때
			if(mafiaTwo && doctorsLive) {
				//의사랑 마피아 둘이 투표를 했는지?
				if(playerVotes.get(DOCTOR) != null && playerVotes.get(MAFIA1) != null &&playerVotes.get(MAFIA2) != null) {
//					System.out.println("[의사]플레이어가 선택한 플레이어 :[ "+ playerVotes.get(DOCTOR)+" ]");
//					System.out.println("[마피아1]플레이어가 선택한 플레이어 :[ "+ playerVotes.get(MAFIA1)+" ]");
//					System.out.println("[마피아2]플레이어가 선택한 플레이어 :[ "+ playerVotes.get(MAFIA2)+" ]");
					System.out.println("[의사]플레이어가 선택한 플레이어 :[ "+ playerVotes.get(DOCTOR)+" ]"); //서버(관리자)로그용
					//두명의 마피아가 같은것을 선택했는지 체크
					if(playerVotes.get(MAFIA1).contains(playerVotes.get(MAFIA2))) {
						Mafiachose =  playerVotes.get(MAFIA1);
						System.out.println("[마피아]유저들이 같은 유저를 지목 :[ " + Mafiachose+" ]"); 
						//마피아와 의사가 같은 플레이어를 선택하였는지 비교
						if(playerVotes.get(DOCTOR).contains(Mafiachose) ) {
							//의사랑 같은 사람을 지목했을떄
							System.out.println("[의사]유저와 [마피아]유저들이 같은 유저를 지목. 추방될 유저 기록을 삭제합니다.\nㄴKillUser : [ "+killUser+ " ]");
							broadcast("[의사]가 밤에 기습으로 플레이어를 살렸습니다.");
							killUser = "reset";
						}else { //의사와 마피아2명(2명의 지목상대는 같다) 선택이 다를 경우 추방
							killUser = Mafiachose;
							System.out.println("[마피아]유저들이 선택한 유저와 [의사]유저가 선택한 유저가 다릅니다. 추방을 시작합니다.\nㄴkillUser : [ "+killUser+" ]");
							broadcast("[마피아]가 밤에 기습으로 플레이어를 죽였습니다.");
						}
						
					}else {//마피아가 다른 플레이어들을 지목한 경우
						killUser = "reset";
						System.out.println("[마피아]유저들이 서로 다른 유저를 지목했습니다. 추방할 수 없습니다.");
					}
				}

			}else if(mafiaOne && doctorsLive) {//마피아 하나랑 의사랑 살아있을때
				//의사와 마피아 하나가 투표 했는지?
				if(playerVotes.get(liveOneMafia) != null && playerVotes.get(DOCTOR) != null) {
					//같은 플레이어를 지목했는지?
					if(playerVotes.get(liveOneMafia).contains(playerVotes.get(DOCTOR))) {
						System.out.println("[의사]유저와 [마피아]유저가 같은 유저를 지목. 추방될 유저 기록을 삭제합니다.\nㄴKillUser : [ "+killUser+" ]");
						broadcast("[의사]가 밤에 기습으로 플레이어를 살렸습니다.");
						killUser = "reset";
					}else{
						System.out.println("[마피아]유저와 [의사]유저가 선택한 유저가 다릅니다. 추방을 시작합니다.\nㄴkillUser : [ "+killUser+" ]");
						killUser = playerVotes.get(liveOneMafia);
						broadcast("[마피아]가 밤에 기습으로 플레이어를 죽였습니다.");
						
					}
				}
			}else if(mafiaTwo) { //마피아 둘만 살아있을때
				//두명의 마피아가 살아있고 투표를 진행했는지 체크
				if(playerVotes.get(MAFIA1) != null && playerVotes.get(MAFIA2) != null) {
					//두명의 마피아가 같은것을 선택했는지 체크
					if(playerVotes.get(MAFIA1).contains(playerVotes.get(MAFIA2))) {
						System.out.println("[마피아]유저들이 같은 유저를 지목. 추방을 시작합니다.\nㄴKillUser : [" + playerVotes.get(MAFIA1)+" ]");
						broadcast("[마피아]가 밤에 기습으로 플레이어를 죽였습니다.");
						Mafiachose =  playerVotes.get(MAFIA1);
						killUser = Mafiachose;
					}else { // 다른 플레이어들을 선택했을떄
						killUser = "reset";
						System.out.println("[마피아]유저들이 다른 유저를 지목. 추방할 수 없습니다");
					}
					
				}
			}else if(mafiaOne) {//마피아 한명만 살아있을때
				//한명의 마피아가 투표를 했는지 체크
				if( playerVotes.get(liveOneMafia) != null ) {
					System.out.println("[마피아]유저가 유저를 지목. 추방을 시작합니다.\nㄴKillUser : [ " + playerVotes.get(liveOneMafia)+" ]");
					broadcast("[마피아]가 밤에 기습으로 플레이어를 죽였습니다.");
					killUser = playerVotes.get(liveOneMafia);
					
				}
			}	
			return killUser;
		}
		return killUser;

	}

	private void ClientOut(String dUser)  {
		System.out.println("추방될 유저의 닉네임 = [ " + dUser+" ]");
		
		//투표 정보를 7명이 했을 때 표시
		if(playerVotes.size() == playerCount && MORNING) {
			System.out.println("투표를 모두 마쳤습니다. 투표 정보를 공개합니다.");
			System.out.println(playerVotes);
			broadcast("투표를 모두 마쳤습니다.\nㅁㅁㅁㅁㅁㅁ투표정보 공개합니다.ㅁㅁㅁㅁㅁㅁ\n"+playerVotes);
		}else if(dUser == null) {
			System.out.println("추방할 유저의 정보가 없습니다.\nㄴKillUser : [" + dUser+" ]");
			return ;
		} 
		if(dUser == "reset") { // 같은 대상을 선택하여 초기화 진행
			playerVotes.clear(); // 투표정보 초기화
			//낮과 밤 바꾸기
			System.out.println("투표 정보를 초기화 합니다.");
			MORNING = !MORNING;
			EVENING = !EVENING;
			return ;
		}

		// 해당 플레이어의 클라이언트 소켓을 찾아서 종료
		try {
		Socket playerSocket = playerSockets.get(dUser);
		PrintWriter writer = new PrintWriter(playerSockets.get(dUser).getOutputStream(), true);
			if (playerSocket != null && !playerSocket.isClosed()) {
				//낮과 밤 바꾸기
				writer.println("@@@@@@@@@@@@@@@추방 되었습니다@@@@@@@@@@@@@@@@");
				playerSocket.close();
				broadcast("추방될 유저의 닉네임 = [ " + dUser+" ]");
				broadcast(dUser + "님의 클라이언트가 종료되었습니다.");
				Initialization(dUser); // 유저 HshMap의 정보 초기화작업				
				broadcast("남은 플레이어  : " + userName);
				MORNING = !MORNING;
				EVENING = !EVENING;
				if(gameEndCheck()) {
					//게임 종료를 체크하여 게임이 끝나는 상태인지 확인
					broadcast("게임이 종료되었습니다.");
					broadcast("각 플레이어 직업 :\n"+ copyPlayerMap);
					broadcast("모든 플레이어의 접속을 종료합니다.");
					saveToDatabase(mafiaWin,civilWin);
					playerMap.clear();
					playerVotes.clear();
					voteCounts.clear();
					clientWriters.clear();
					playerSockets.clear();//모든 소켓 제거
					writer.flush();
				}
			}
		} catch (IOException e) {
			System.err.println("클라이언트 종료 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
	
	private boolean gameEndCheck() {
		int mCount=0;
		int cCount=0;
	
		if(playerMap.containsValue(MAFIA1)) 
			mCount++;
		
		if(playerMap.containsValue(MAFIA2)) 
			mCount++;
		
		if(playerMap.containsValue(CITIZEN1)) 
			cCount++;
		
		if(playerMap.containsValue(CITIZEN2)) 
			cCount++;
		
		if(playerMap.containsValue(CITIZEN3)) 
			cCount++;
		
		if(cCount < mCount) { // 시민보다 마피아가 클경우
			MORNING = false;
			EVENING = false;
			broadcast("---------------------------마피아의 승리---------------------------");
			return true;
		}else if(mCount < 1) { // 마피아가 1보다 적을 경우
			MORNING = false;
			EVENING = false;
			broadcast("---------------------------시민의 승리---------------------------");
			return true;
		}
		
		
		return false;
	}

	// 초기화 메서드
	private void Initialization(String dUser) {
		System.out.println("초기화 작업을 시작합니다.");
		voteCounts.clear(); // 투표정보 초기화
		playerVotes.clear();
		playerCount--; // 현재 플레이어 인원 감소
		playerSockets.remove(dUser); // hashmap에 저장된 현재 플레이어들의 정보 삭제
		playerMap.remove(dUser); //현재 플레이어<유저명,역할>의 추방할 유저기록 삭제
		userName.removeIf(item -> item.equals(dUser)); //추방될 유저의 기록 삭제
		System.out.println("유저선택 정보 카운트 초기화\nㄴ>voteCounts : [ "+voteCounts+" ]\n"+
		"유저선택 정보 초기화\nㄴ>playerVotes : [ "+playerVotes+" ]\n"+
		"유저 수 감소\nㄴ>playerCount : [ "+playerCount+" ]\n" +
		"유저 클라이언트 정보 삭제\nㄴ>playerSockets : [ "+playerSockets+" ]\n"+
		"유저 역할 정보 삭제\nㄴ>playerMap : [ "+playerMap+" ]\n"+
		"유저 리스트 정보 삭제\nㄴ>userName : [ "+userName+" ]\n");
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
			broadcast("플레이어 수와 역할 수가 일치하지 않습니다.");
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
		copyPlayerMap.putAll(playerMap);// 역할 복사
		System.out.println("복사한 유저 정보 >\n"+ copyPlayerMap);
		broadcast("현재 플레이 중인 유저정보 : "+userName+" "); // 게임 시작시 한 번만 출력
		broadcast("낮입니다. /vote로 투표를 시작하세요~"); // 게임 시작시 출력용
		
	}

	// 클라이언트가 다른 모든 클라이언트에게 메시지를 출력하는 메서드
	private void broadcastMessage(String userID, String message) {
	    // 모든 클라이언트에게 메시지를 보냅니다.
	    Iterator<PrintWriter> iterator = clientWriters.iterator();
	    while (iterator.hasNext()) {
	        PrintWriter clientWriter = iterator.next();
	        clientWriter.println(userID + ": " + message);
	    }
	}


	// 모든 클라이언트에게 출력 (사회자(서버)가 클라이언트들에게 공통적으로 보여줄 메시지)
	private void broadcast(String message) {
	    // 모든 클라이언트에게 메시지를 보냅니다.
	    Iterator<PrintWriter> iterator = clientWriters.iterator();
	    while (iterator.hasNext()) {
	        PrintWriter clientWriter = iterator.next();
	        clientWriter.println(message);
	    }
	}


	// 플레이어별 투표 수 업데이트
	private void updateVoteCounts(String votee) {
		voteCounts.put(votee, voteCounts.getOrDefault(votee, 0) + 1);

	}

	// 동점 여부 확인
	private boolean isTie(int maxVotes) {
	    int count = 0;
	    Iterator<Integer> iterator = voteCounts.values().iterator();
	    while (iterator.hasNext()) {
	        int votes = iterator.next();
	        if (votes == maxVotes) {
	            count++;
	        }
	    }
	    System.out.println("count >>>"+ count);
	    return count > 1;
	}
	// 역할 분배 후 데이터베이스에 역할에 맞는 플레이어명 입력 메서드
		public void saveToDatabase(boolean mafiaWin, boolean civilWin) {
			try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
				// 게임 결과 테이블에 역할에 맞는 플레이어명을 입력하기 위한 SQL 문
				String sql = "INSERT INTO game_results (Mafia1, Mafia2, Citizen1, Citizen2, Citizen3, Doctor, Police, MafiaWin, CivilWin) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
				this.statement = connection.prepareStatement(sql);

				// 게임 결과 테이블에 값을 입력할 변수 초기화
				String mafia1 = null;
				String mafia2 = null;
				String citizen1 = null;
				String citizen2 = null;
				String citizen3 = null;
				String doctor = null;
				String police = null;

				// 각 플레이어 역할에 맞게 변수에 값을 할당
				for (Map.Entry<String, String> entry : copyPlayerMap.entrySet()) {
					String playerName = entry.getKey();
					String role = entry.getValue();
					switch (role) {
					case "마피아1":
						mafia1 = playerName;
						break;
					case "마피아2":
						mafia2 = playerName;
						break;
					case "시민1":
						citizen1 = playerName;
						break;
					case "시민2":
						citizen2 = playerName;
						break;
					case "시민3":
						citizen3 = playerName;
						break;
					case "의사":
						doctor = playerName;
						break;
					case "경찰":
						police = playerName;
						break;
					}
				}

				// SQL 문에 변수 값을 설정
				statement.setString(1, mafia1);
				statement.setString(2, mafia2);
				statement.setString(3, citizen1);
				statement.setString(4, citizen2);
				statement.setString(5, citizen3);
				statement.setString(6, doctor);
				statement.setString(7, police);
				statement.setBoolean(8, mafiaWin);
				statement.setBoolean(9, civilWin);

				// SQL 문 실행
				statement.executeUpdate();

				System.out.println("플레이어 역할을 데이터베이스에 저장했습니다.");

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		

	// 60초 Timer(Timer, 500); 추가 할지 말지 고민중

	
	// 추방된 사람들끼리 채팅방을 만들어주는 메서드 해보고 싶다.. 그냥 희망사항


}
