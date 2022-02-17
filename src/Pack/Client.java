package Pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class Client extends Application{
   // 접속 정보 필드
   final String serverIP = "192.168.55.109";
   final int serverPort = 5001;

   // UI 관련 필드
   final String talkTitle = "3조 채팅방";
   String clientName, contents;
   TextField nameInput, messageInput;
   Stage loginStage, mainStage;
   Socket cs;
   TextArea textArea;
   Label warningLabel;
   VBox userList;
   int MaxNameLength = 5;

   // 서버에 연결
   void connectServer() {

      clientName = nameInput.getText();

      // 닉네임 검사 (공백, 글자수, 띄워쓰기 : 닉네임 Split)
      if (clientName.equals("") || clientName.length() > MaxNameLength || clientName.indexOf(" ") != -1) {
         warningLabel.setTextFill(Color.RED);
         return;
      }

      // 서버로 소켓 접속
      cs = new Socket();
      try {
         cs.connect(new InetSocketAddress(serverIP, serverPort));
         System.out.println("접속완료");

         // 접속 완료 되었다면, 닉네임을 서버에 보낸다.
         sendName();
      } catch (Exception e) { 
         e.printStackTrace();
         // 오류가 나면 접속이 안되게 막는다.
         return; 
      } 

      // 로그인 창을 닫고, 메인창을 연다.
      loginStage.close();
      mainPage();
   }

   //사용자 닉네임을 서버에 전달
   void sendName() {
      try {
         OutputStream os = cs.getOutputStream();
         byte[] outputData = clientName.getBytes();
         os.write(outputData);
      } catch (IOException e) { e.printStackTrace(); }
   }

   // 서버로부터 인원 목록 혹은 대화 내용을 전달 받는다.
   void receiveMessage() {
      String recvString;

      try {
         InputStream is = cs.getInputStream();
         byte[]recvData = new byte[1024];

         while(true) {
            // 서버로부터 데이터를 전달 받는다.
            int size = is.read(recvData);
            // 서버가 종료된 경우???
            if (size == -1) {
               System.out.println("disconnect");
               break;
            }
            recvString = new String(recvData, 0, size);
            System.out.println("here");
            // 인원 목록 초기화, 대화 내용 추가
            try {
               if (recvString.equals("아이디 중복")) {
                  Platform.runLater(new Runnable() {

                     @Override
                     public void run() {
                        // TODO Auto-generated method stub
                        System.out.println(111111111);
                        try {
                           cs.close();
                        } catch (IOException e) {
                           // TODO Auto-generated catch block
                           e.printStackTrace();
                        }
                        System.out.println(222222);
                        mainStage.close();
                        System.out.println(33333);
                        loginStage.show();
                        System.out.println(44444);

                     }
                  });
                  throw new Exception("중복아이디였음");
               }

               // 만약 데이터 첫 글자가 " " 인 경우, 인원 목록을 초기화한다.
               // " 사람1 사람2 사람3" == 닉네임 리스트
               // "사람1사람2사람3" == 대화 내용
               if(recvString.substring(0, 1).equals(" "))
                  updateList(recvString);
               // 만약 데이터 첫 글자가 " "가 아닌 경우, 대화 내용을 추가한다.
               else {
                  updateChat(recvString);
               }
            } catch (StringIndexOutOfBoundsException e) {e.printStackTrace();}
         }
      } catch (IOException e) { e.printStackTrace(); 
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // 참여 인원 목록 갱신
   void updateList(String recvString) {

      Platform.runLater( () -> {
         System.out.println("닉네임 리스트 : " + recvString);
         // " "을 기준으로 문자열(전체 닉네임)을 잘라 배열에 넣는다.
         String[] recvUserList = recvString.split(" ");

         // 자른 문자열(잘린 닉네임)을 라벨로 생성한 후, 유저 목록에 배치한다.
         VBox temp = new VBox();
         temp.getChildren().add(new Label("\t참여중 인원"));
         for (int i = 1; i < recvUserList.length; i++)
            temp.getChildren().add(new Label(recvUserList[i]));

         userList.getChildren().setAll(temp);
      });
   }

   // 채팅 갱신
   void updateChat(String recvString) {
      Platform.runLater(new Runnable() {
         @Override
         public void run() {
            textArea.appendText(recvString + "\n");
         }
      });
      //System.out.println(recvString);
   }


   // 메인 페이지 구성
   // - 서버로부터 메시지 받는 스레드
   void mainPage() {
      mainStage = new Stage();
      mainStage.setOnCloseRequest((WindowEvent arg0)-> {
         try { 
            mainStage.close();
            cs.close();
         } catch(Exception e){
            e.printStackTrace();
         }
      });

      // 창 크기 고정
      mainStage.setResizable(false);

      //전체 영역
      VBox mainRoot = new VBox();
      mainRoot.setPrefSize(600, 730);

      //=---------------내부 컨트롤----------------
      //Header
      HBox header = new HBox();
      header.setBackground(new Background(new BackgroundFill(Color.rgb(175, 194, 224),
            CornerRadii.EMPTY, Insets.EMPTY)));

      // 방 이름

      Label headerLabel = new Label(talkTitle);
      headerLabel.setFont(new Font("Verdana", 25));
      headerLabel.setMinWidth(290);
      header.setMargin(headerLabel, new Insets(10, 0, 10, 10));

      // 닉네임
      Label nameLabel = new Label("[ " + clientName+ " ]");
      nameLabel.setFont(new Font("Verdana", 15));
      nameLabel.setMinWidth(280);
      nameLabel.setAlignment(Pos.BOTTOM_RIGHT);
      header.setMargin(nameLabel, new Insets(20, 20, 10, 0));

      header.getChildren().addAll(headerLabel, nameLabel);

      //Body
      HBox body = new HBox();
      //채팅 영역
      textArea = new TextArea();   
      textArea.setPrefSize(500, 650);
      textArea.setEditable(false); // 입력 못하게 막기
      textArea.setWrapText(true); // 줄바꿈
      textArea.setStyle("-fx-text-fill: white");
      textArea.setStyle("-fx-control-inner-background: #"+Paint.valueOf("F9F2EC").toString().substring(2));

      //인원 목록
      userList = new VBox();
      userList.setPrefSize(100, 650);
      userList.setDisable(true);
      userList.setBackground(new Background(new BackgroundFill(Color.rgb(212, 233, 238),
            CornerRadii.EMPTY, Insets.EMPTY)));
      //      userList.setBackground(new Background(new BackgroundFill(Color.WHITE,
      //            CornerRadii.EMPTY, Insets.EMPTY)));
      userList.setBorder(new Border(new BorderStroke(Color.GRAY,
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

      body.getChildren().addAll(textArea,userList);

      //Bottom
      HBox bottom = new HBox();

      //종료 Button
      Button exitBtn = new Button("나가기");
      exitBtn.setBackground(new Background(new BackgroundFill(Color.rgb(175, 194, 224),
            CornerRadii.EMPTY, Insets.EMPTY)));
      exitBtn.setPrefSize(100, 40);
      exitBtn.setOnAction((ActionEvent arg0)->{
         try { 
            mainStage.close();
            cs.close();
            loginStage.show();
         } 
         catch (Exception e) { e.printStackTrace(); }
      });

      //입력창
      int inputMaxLength = 300;
      messageInput = new TextField();
      messageInput.setMinWidth(400);
      messageInput.setMinHeight(40);
      // 입력 글자수 제한
      messageInput.setOnKeyReleased((t) -> {
         if (messageInput.getText().length() > inputMaxLength) {
            int CursorPos = messageInput.getCaretPosition();
            messageInput.setText(messageInput.getText(0, inputMaxLength));
            messageInput.positionCaret(CursorPos);
         } 
      });
      messageInput.setOnAction((ActionEvent arg0)-> { sendMessage(); });

      Platform.runLater(()-> { messageInput.requestFocus(); }); // 포커스

      //전송 Button
      Button sendBtn = new Button("보내기");
      sendBtn.setPrefSize(100, 40);
      sendBtn.setBackground(new Background(new BackgroundFill(Color.rgb(175, 194, 224),
            CornerRadii.EMPTY, Insets.EMPTY)));
      sendBtn.setOnAction((ActionEvent arg0)-> { sendMessage(); });

      bottom.getChildren().addAll(exitBtn, messageInput, sendBtn);


      mainRoot.getChildren().addAll(header, body, bottom);

      //=---------------내부 컨트롤----------------

      Scene mainScene = new Scene(mainRoot);
      mainStage.setScene(mainScene);
      mainStage.setTitle(talkTitle);
      mainStage.show();

      // 서버로부터 인원 목록 혹은 대화 내용을 전달 받는다.
      new Thread() {
         @Override
         public void run() {
            receiveMessage();            
         }
      }.start();
   }

   // 사용자 메시지를 서버로 전달
   void sendMessage() {
      try {
         OutputStream os = cs.getOutputStream();
         contents = messageInput.getText();
         byte[] data = contents.trim().getBytes();
         os.write(data);
         System.out.println("데이터 보냄");

         // 텍스트필드 초기화
         messageInput.setText("");
      } catch (IOException e) {
         e.printStackTrace();
      }
   };


   //로그인 페이지
   @Override
   public void start(Stage stage) throws Exception {
      //나가기 버튼 클릭시 다시 이전 페이지 호출을 위한 전역변수 설정
      this.loginStage = stage;

      // 창 크기 고정
      stage.setResizable(false);
      stage.setOnCloseRequest((WindowEvent arg0)->{
         try { 
            stage.close();
         } catch(Exception e){
            e.printStackTrace();
         }
      });

      // 로그인 창
      VBox loginRoot = new VBox();
      loginRoot.setAlignment(Pos.CENTER);
      loginRoot.setPrefSize(400, 300);
      loginRoot.setSpacing(5);

      //=---------------내부 컨트롤----------------
      // 제목
      Label titleLabel = new Label(talkTitle);
      titleLabel.setFont(new Font("Verdana", 18));
      loginRoot.setMargin(titleLabel, new Insets(0,0,30,0)); //margin 위 오 아 왼

      // 안내문구
      Label inputLabel = new Label("닉네임을 입력 해주세요.");

      // 경고문구
      warningLabel = new Label(MaxNameLength + "글자 이하로 입력하세요");
      warningLabel.setFont(new Font("Verdana", 10));

      // 닉네임 입력 칸
      nameInput = new TextField();
      nameInput.setMaxWidth(100);   //사이즈 가로 제한
      nameInput.setAlignment(Pos.CENTER);   //글자 가운데 정렬
      nameInput.setOnAction((ActionEvent arg0) -> { // 엔터
         connectServer();
      });

      // 접속 버튼
      Button btnConnect = new Button("접속");
      btnConnect.setOnAction((ActionEvent arg0)-> {
         connectServer();
      });

      loginRoot.getChildren().addAll(titleLabel, inputLabel, warningLabel, nameInput, btnConnect);
      //=---------------내부 컨트롤----------------

      Scene loginScene = new Scene(loginRoot); 
      stage.setScene(loginScene);
      stage.setTitle("로그인");
      stage.show();
   }

   public static void main(String[] args) {
      launch();
   }

}