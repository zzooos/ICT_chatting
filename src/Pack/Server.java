package Pack;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

class CustomSocket{
   Socket socket;
   String name;
   CustomSocket(Socket socket){
      this.socket = socket;
   }
   void setName(String name) {
      this.name = name;
   }
}

// 클라이언트 소켓 스레드
class ClientThread extends Thread{
   //   HashMap<String, Integer> t;
   static HashMap<String, Integer> usedId = new HashMap<>();
   static HashMap<String, Socket> userSocket = new HashMap<>();   
   OutputStream os;
   CustomSocket customSocket;
   String recvStrData;
   String userListData;

   static ArrayList<CustomSocket> clientList = new ArrayList<>() {
      @Override
      public String toString() {
         String allName = "";
         for (int i = 0; i < clientList.size(); i++) {
            allName = allName + clientList.get(i).name + (i == clientList.size() -1 ? "": " ");
         }
         return allName;
      }
   };

   ClientThread(Socket socket){
      // 커스텀 소켓 생성
      this.customSocket = new CustomSocket(socket);

      // 생성한 커스텀 소켓을 클라이언트 소켓 리스트에 추가
      clientList.add(this.customSocket);
   }

   void dataSpread(String sdata){
      for (int i = 0; i < clientList.size(); i++) {
         try {
            os = clientList.get(i).socket.getOutputStream();
            byte[]data = sdata.getBytes();
            os.write(data);   
         } catch (Exception e) { e.printStackTrace(); }
      }
   }
   
   void dataSpread(String senderName ,String sdata){
      sdata = senderName + " : " + sdata;
      for (int i = 0; i < clientList.size(); i++) {
         try {
            os = clientList.get(i).socket.getOutputStream();
            byte[]data = sdata.getBytes();
            os.write(data);   
         } catch (Exception e) { e.printStackTrace(); }
      }
   }



   void connectionClose(CustomSocket customsocket) {
      clientList.remove(customsocket); // 1.퇴장 알림 전송 전에 삭제해야 됨
   }

   void connectionClose() {
      System.out.println("클라이언트가 접속 끊음");
      String leaveAnounce = "\t\t" + customSocket.name + "님이 나갔습니다.";

      clientList.remove(customSocket); // 1.퇴장 알림 전송 전에 삭제해야 됨
      usedId.put(customSocket.name, 0);
      System.out.println(usedId.get(customSocket.name));
      dataSpread(leaveAnounce); // 2.퇴장 알림

      // 인원목록 업데이트
      userListData = " " + clientList.toString();
      System.out.println(userListData);
      dataSpread(userListData);
   }

   // 소켓 목록의 모든 소켓을 닫는다.
   static void connectionAllClose() {
      for (int i = 0; i < clientList.size(); i++) {
         try {
            clientList.get(i).socket.close();         
         } catch (Exception e) { e.printStackTrace(); }
      }
   }

   @Override
   public void run() {
      try {
         InputStream is = customSocket.socket.getInputStream();
         byte[] recvData = new byte[1024];

         // 클라이언트 접속 시 보내는 닉네임을 받는다.
         int dataSize = is.read(recvData);
         String name = new String(recvData, 0, dataSize);
         if (ClientThread.usedId.containsKey(name)) {
            if (ClientThread.usedId.get(name) == 1) {
               os = customSocket.socket.getOutputStream();
               String duplicatedId = "아이디 중복";
               try {
                  byte[]data = duplicatedId.getBytes();
                  os.write(data);
               } catch (Exception e) {
                  e.printStackTrace();
               }
               connectionClose(customSocket);
               //               throw new Exception("아이디 중복 들어옴");
               return;
            }
         }
         ClientThread.usedId.put(name, 1);
         customSocket.setName(name);
         userSocket.put(name, customSocket.socket);

         // 입장 알림
         String sendData   = "\t\t" + name + "님이 들어왔습니다.";
         //System.out.println(sendData);
         dataSpread(sendData);

         this.sleep(500);   //쓰레드 강제 딜레이

         // 인원목록 업데이트
         // - 클라이언트 측에서 인원 목록임을 알려주기 위해 " "를 앞에 추가한다.
         userListData   = " " + clientList.toString(); 
         //System.out.println(userListData);
         dataSpread(userListData);

         // 신규 메시지 송신
         while(true) {
            int size = is.read(recvData); 
            if(size == -1) {

               break;
            }

            recvStrData = new String(recvData, 0, size);
            System.out.println(recvStrData);
            if (recvStrData.startsWith("/r ")) {
               recvStrData = recvStrData.replace("/r ", "");
               String[] rcvData_split = recvStrData.split(" ");
               if (rcvData_split.length <= 1 || rcvData_split[0] == "") {
                  continue;
               }
               String target = rcvData_split[0];
               if (!usedId.containsKey(target)) {
                  continue;
               }
               String message = customSocket.name + " 님으로부터 >> ";
               String senderMessage = "<<" + target + " 님에게 ";
               for (int i = 1; i < rcvData_split.length; i++) {
                  message += rcvData_split[i] + (i == rcvData_split.length - 1?"" :" ");
                  senderMessage += rcvData_split[i] + (i == rcvData_split.length - 1?"" :" ");
               }
               OutputStream targetos = userSocket.get(target).getOutputStream();
               os = customSocket.socket.getOutputStream();
               try {
                  byte[] data = message.getBytes();
                  byte[] senderData = senderMessage.getBytes();
                  os.write(senderData);
                  targetos.write(data);
               } catch (Exception e) {
                  e.printStackTrace();
               }
               continue;
            }
            //            if(t.get(recvStrData.split(" : ")[1])== 1)
            //            {
            //               //System.out.println(rec);
            //               os = customSocket.socket.getOutputStream();
            //               String filter   = "욕설을 입력하였습니다.";
            //               try {
            //                  byte[]data = filter.getBytes();
            //                    os.write(data);
            //            } catch (Exception e) {
            //               e.printStackTrace();
            //            }
            //               continue;
            //            }
            dataSpread(customSocket.name, recvStrData);   // 신규 메시지 송신
         }

         // 클라이언트의 연결이 끊긴 경우, 소켓을 닫는다.
         customSocket.socket.close();

      } catch (Exception e) {e.printStackTrace();}
      connectionClose();

   }
}

// 서버 소켓 생성 및 접속 관리 클래스
class ConnectThread extends Thread{
   // 서버 정보 필드
   final String serverIP = "0.0.0.0";
   final int serverPort = 5001;

   ServerSocket ss;

   @Override
   public void run() {
      try {
         ss = new ServerSocket();         
         System.out.println("서버 소켓 생성");

         ss.bind(new InetSocketAddress(serverIP, serverPort));
         System.out.println("바인딩 완료");

         // 접속 관리
         while(true) {
            Socket ds = ss.accept();
            ClientThread ct = new ClientThread(ds);
            //            System.out.println("접속 시도");
            ct.start();
         }
      } catch (Exception e) {e.printStackTrace();}
   }

   public void stopThread() {
      try {
         ss.close();
         ClientThread.connectionAllClose();
         this.sleep(10);
      } catch (Exception e) {
         e.printStackTrace();
      } finally {      
         this.interrupt();
         System.out.println("종료");
      }
   }
}

public class Server extends Application{
   ConnectThread connectThread;

   @Override
   public void start(Stage Stage) throws Exception {

      VBox root = new VBox();
      root.setPrefSize(400, 300);
      root.setSpacing(20);
      root.setAlignment(Pos.CENTER);

      //=---------------내부 컨트롤----------------
      Button serverOpenBtn = new Button("서버 오픈");
      Button serverCloseBtn = new Button("서버 종료");

      serverOpenBtn.setOnAction((ActionEvent arg0)-> {
         toggleBtn(serverOpenBtn);
         toggleBtn(serverCloseBtn);

         connectThread = new ConnectThread();
         connectThread.start();
      });

      serverCloseBtn.setOnAction((ActionEvent arg0)-> {
         toggleBtn(serverOpenBtn);
         toggleBtn(serverCloseBtn);

         connectThread.stopThread();
      });
      serverCloseBtn.setDisable(true);

      root.getChildren().addAll(serverOpenBtn, serverCloseBtn);
      //=---------------내부 컨트롤----------------

      Scene scene = new Scene(root);
      Stage.setScene(scene);
      Stage.setTitle("Server");
      Stage.show();
   }

   void toggleBtn(Control control) {
      control.setDisable(!control.isDisable());
   }

   public static void main(String[] args) {
      launch();
      System.out.println("서버 종료");
   }

}