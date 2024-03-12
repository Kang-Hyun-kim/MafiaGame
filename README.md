# 설계
- 최대 7개의 Cilent가 서버에 접속 (8번째 이후에는 소켓 강제 종료)
- 클라이언트와 서버의 통신으로 게임을 실행
- 데이터베이스는 게임의 정보를 저장 ( 역할과 유저아이디, 이긴 팀과 진팀, 게임라운드 )
---------
# 사용 기술
- Java
- Mysql

-----

# 역할 분담
## 김강현
- Client & Server 통신과 메세지 관리, 게임 로직 설계
## 김융
- 데이터베이스 연결, 게임 정보 저장, 게임 로직 설계

-----

# 주요 자료 구조
```java
Set<PrintWriter> clientWriters = new HashSet<>(); // 클라이언트 정보 주소
Map<String, Socket> playerSockets = new HashSet<>(); // 플레이어 이름, 소켓정보
Map<String, Socket> playerVotes = new HashSet<>(); // 플레이어 투표 정보
Map<String, Socket> voteCounts = new HashSet<>(); // 플레이어 투표 수
Map<String, Socket> playerMap = new HashSet<>(); // 플레이어 이름, 역할 정보
Map<String, Socket> copyPlayerMap = new HashSet<>(); // 플레이어 역할 정보 데이터베이스 등록용
```

-----

# 데이터베이스

![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/6ce03477-0e8a-4ddb-94fb-bbfa0014e387)

- 역할과 승리팀의 정보(boolean)를 저장 + 게임의ID(현재 라운드)



-----

# Mafia Game 순서도
<img width="317" alt="순서도" src="https://github.com/maruduke/bitcamp-project1/assets/157556923/62693391-60b4-4743-8f51-573004ec7f80">


# 클래스 다이어그램
<br>

![Mafia클래스다이어그램2 drawio](https://github.com/maruduke/bitcamp-project1/assets/157556923/0f4ebe1c-8544-462a-9152-22ab3f600d12)

<br>

-------

# 게임실행
<br>

![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/045c211a-2313-49dd-a41e-816e1d2322cb)

- 1번플레이어~7번플레이어까지 접속이 완료되면 게임이 실행되며 역할이 자동으로 부여된다. 각 역할은 자신에게만 보인다.
<br>

![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/e825c7b7-db48-4b90-b73e-9dac753e7a1a)
- 서버를 관리자 역할로 만들어 게임이 게임의 상황을 알 수 있게 정보를 출력하게 하였다.

<br>

![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/56f60d21-c14f-4734-9977-88caac41e9d3)
![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/12f2c36f-aaba-41f5-b256-e21fbbddab0f)
![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/c1240ab2-fc8f-4326-a209-e94384176e5a)


유저명과 명령문을 잘 써야 명령어를 이용해 게임을 진행할 수 있다.

<br>


![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/510ca4dd-c1a4-465c-b389-122a613b06a3)
- 낮의 투표에서 동점이 발생하면 추방하지 않는다.

<br>

- 밤에는 특수역할(경찰, 의사, 마피아)이 능력을 사용할 수 있다. 경찰은 능력을 사용하는 건 상관없지만 의사와 마피아는 능력을 반드시 사용해야 밤이 끝나도록 만들었다
- 추방된 플레이어는 소켓을 종료시켜 유저 정보들을 초기화해 준다.
- 낮과 밤이 끝날 때 게임종료 조건을 검사하여 참, 거짓값을 리턴 받는다.

![image](https://github.com/maruduke/bitcamp-project1/assets/157556923/640e9f47-e481-4fb9-b99c-f6a98f3d6909)
- 게임이 종료되면 모든 플레이어를 서버와 연결을 끊는다. 그리고 복사해 놓은 유저의 역할과 승리한 팀의 값을 가지고 데이터베이스에 insert 하여 정보를 저장한다.
  

# 메서드로 보는 게임 진행 순서

```java
//Controller.java > class hadler > run
// while((message = reader.readLine())) != null){
  if(낮){ dayTime((userID, message); }
  
  if(밤){ night(userID, message); }
}
```


```java
낮(userID, message){
  String target = UserSelection(userID, message);
  target = selectedInformation(target);
  ClientOut(target);
}
```
- 밤도 같은 방식이다 하나의 메서드로 낮과 밤을 처리한다.
- 투표나 특수역할이 뽑은 인원을 정보를 초기화함
- 뽑힌 플레이어가 추방할 수 있는지 체크하고 값을 리턴
- 리턴된 값이 플레이어명이라면 추방을 한다. 그리고 추방이 완료되면 게임종료체크메서드를 실행. 게임을 진행할지 종료시킬지 판단한다.


