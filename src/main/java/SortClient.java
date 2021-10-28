import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SortClient {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException{


        Socket socket = new Socket("localhost", 3000);//로컬호스트ip, 포트번호 3000에 연결하는 클라이언트 소켓
        OutputStream os = socket.getOutputStream();//송신 바이트 스트림
        InputStream is = socket.getInputStream();//수신 바이트 스트림

        Sort_Protocol protocol = new Sort_Protocol();//정의한 프로토콜 (구문,의미,순서,데이터 포함)
        byte[] buf = protocol.getPacket();//프로토콜내부의 packet바이트배열 기본생성자로 패킷초기화

        while(true){
            is.read(buf);//접속하자마자 서버로부터 문자열 정보 입력 요청프로토콜 객체 받음
            int packetType = buf[0];//수신받은 프로토콜 객체의 첫번째바이트는 해당 패킷의 타입을 나타냄
            protocol.setPacket(packetType,buf);//수신한 프로토콜 객체의타입에따라 buf 크기변경
            if(packetType == Sort_Protocol.PT_EXIT){//읽어온 packetType이 exit와 같으면 종료
                System.out.println("클라이언트 종료");
                break;
            }

            switch(packetType){//패킷 타입에따른 업무
                case Sort_Protocol.PT_REQ_INPUT://서버가 문자열입력요청 패킷 보냄
                    System.out.println("서버가 문자열 입력 요청");
                    System.out.println("문자또는 숫자 몇개 입력할지 설정");
                    byte[] temp=new byte[Sort_Protocol.COUNT+Sort_Protocol.LEN_MAX];//입력 문자개수와 각문자의 길이정보,문자데이터저장할 임시 byte배열

                    BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));//키보드입력 Reader
                    Scanner sc=new Scanner(System.in);//키보드입력 Scanner
                    int c=sc.nextInt();//입력받을 정수 입력
                    //temp 처음 4byte 공간에 입력 문자나숫자의 개수나타내는 c byte값 넣음
                    System.arraycopy(protocol.intto4byte(c),0,temp,0,protocol.intto4byte(c).length);
                    System.out.println("문자 또는 숫자"+c+"개 입력");

                    // temp 5번째 공간부터 4byte 공간에 oneword 길이정보넣고 그뒤에 data를 세드로넣는걸 c만큼 반복함
                    int pos=protocol.intto4byte(c).length+1;//temp 5번째 위치 나타냄

                    for (int i=1;i<=c;i++){//temp에 oneword 길이,data넣기 위에 입력받은 c번만큼 반복
                        System.out.println("input wordornum");
                        String in=userIn.readLine();//one word String입력

                        int onewordlen=in.length();//one word 길이

                        //one word 길이정보를 4바이트 배열로 바꾼후 temp의 5번째위치부터 4바이트공간에 저장
                        System.arraycopy(protocol.intto4byte(onewordlen),0,temp,pos,protocol.intto4byte(onewordlen).length);

                        pos+=protocol.intto4byte(onewordlen).length;//pos+=4 해서 pos가 이제 data저장할 위치 나타내게 함

                        //입력받은 oneword를 byte로 바꾼 후 temp배열의 길이정보뒤에 저장
                       System.arraycopy(in.trim().getBytes(),0,temp,pos,in.trim().getBytes().length);

                        pos+=in.trim().getBytes().length;//pos+=data length 해서 pos가 이제 새로운 word 길이정보 저장 위치 나타내게 함
                    }



                    // 입력문자열 정보 패킷 생성 및 패킷 전송
                    protocol = new Sort_Protocol(Sort_Protocol.PT_RES_INPUT);//protocoltype+count+lenmax 크기가지는 패킷 생성
                    System.out.println("입력정보전달패킷생성");

                    protocol.setInputdata(temp);//문자개수,각문자의 길이정보,data 가지는 temp byte배열받아 패킷에 복사
                    System.out.println("문자열 정보 전송");
                    os.write(protocol.getPacket());//완성된 프로토콜타입 객체를 서버에게 전달
                    break;

                case Sort_Protocol.PT_INPUT_RESULT:
                    System.out.println("서버가 정렬 결과 전송.");
                    String[] result = protocol.getsortResult();
                    //정렬된 문자열 출력
                    for (String str:result) {
                        System.out.print(str+" ");
                    }
                    System.out.println();
                    protocol = new Sort_Protocol(Sort_Protocol.PT_EXIT);//종료 패킷 생성
                    System.out.println("종료 패킷 전송");
                    os.write(protocol.getPacket());//종료 패킷 전송
                    break;
            }
        }
        os.close();//outstream 닫기
        is.close();//inputstream 닫기
        socket.close();//소켓 닫기
    }
}
