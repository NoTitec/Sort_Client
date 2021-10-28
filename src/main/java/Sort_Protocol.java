import java.nio.ByteBuffer;
import java.util.Arrays;

public class Sort_Protocol {
    //프로토콜의 타입과 크기 나타냄 단위 바이트
    public static final int PT_UNDEFINED = -1;
    public static final int PT_EXIT = 0;//프로그램 종료
    public static final int PT_REQ_INPUT = 1;//숫자또는 문자열 입력 요청(서버)
    public static final int PT_RES_INPUT = 2;//숫자또는 문자열 입력 응답(클라이언트)
    public static final int PT_INPUT_RESULT = 3;//정렬 결과//(서버)
    public static final int COUNT=4;//입력자료개수필드
    public static final int LEN_PROTOCOL_TYPE = 1;//프로토콜 타입 길이
    public static final int LEN_MAX = 10000;//최대 데이터 길이

    protected int protocolType;
    private byte[] packet;    // 프로토콜과 데이터의 저장공간이 되는 바이트 배열

    public Sort_Protocol() {                    // 생성자
        this(PT_UNDEFINED);
    }

    public Sort_Protocol(int protocolType) {    // 생성자
        this.protocolType = protocolType;
        getPacket(protocolType);
    }

    public byte[] getPacket() {//기본 maxlen크기 패킷가져오는메소드
        return packet;
    }

    // 프로토콜 타입에 따라 바이트 배열 packet의 length가 다름
    public byte[] getPacket(int protocolType) {
        if (packet == null) {
            switch (protocolType) {
                case PT_REQ_INPUT://입력요청
                    packet = new byte[LEN_PROTOCOL_TYPE];
                    break;
                case PT_RES_INPUT://입력 응답
                    packet = new byte[LEN_PROTOCOL_TYPE + COUNT+LEN_MAX];//2
                    break;
                case PT_UNDEFINED://type 안정해짐
                    packet = new byte[LEN_PROTOCOL_TYPE+COUNT+LEN_MAX];
                    break;
                case PT_INPUT_RESULT://정렬 결과
                    packet = new byte[LEN_PROTOCOL_TYPE + COUNT+LEN_MAX];
                    break;
                case PT_EXIT://종료
                    packet = new byte[LEN_PROTOCOL_TYPE];
                    break;
            } // end switch
        } // end if
        packet[0] = (byte) protocolType;    // packet 바이트 배열의 첫 번째 바이트에 프로토콜 타입 설정
        return packet;
    }

    // Default 생성자로 생성한 후 Protocol 클래스의 packet 데이터를 바꾸기 위한 메서드
    public void setPacket(int pt, byte[] buf) {
        packet = null;
        packet = getPacket(pt);
        protocolType = pt;
        System.arraycopy(buf, 0, packet, 0, packet.length);
    }
    public byte[] intto4byte(int count){//int값을 4byte byte배열로 전환후 반환 1
        byte[] byteArray = ByteBuffer.allocate(4).putInt(count).array();

        return byteArray;
    }
    public void setInputdata(byte[] temp) {//클라이언트가 생성한 data정보가지는 temp배열을 패킷 type뒤에 저장하는 메소드

        System.arraycopy(temp, 0, packet, LEN_PROTOCOL_TYPE, temp.length);
        packet[LEN_PROTOCOL_TYPE + temp.length-1] = '\0';
    }
    public void setsorteddata(String[] sorted) {
        int c=sorted.length;//단어개수
        byte[] bytesorted=new byte[COUNT+LEN_MAX];//단어개수+sortedstring의 단어길이들+문자 저장할 byte배열
        //단어개수 4byte 배열 만들고 bytesorted배열에 4byte 저장
        System.arraycopy(intto4byte(c),0,bytesorted,0,intto4byte(c).length);
        int pos=5;//packet은 0위치 type정보 1~4위치 단어 개수정보 저장됨 따라서 word 길이+data저장할 위치는 5부터

        //sorted는 정렬된 문자열 배열이므로 bytesorted array의 5번째위치부터 길이+문자byte를 차례대로 넣는다
        for(int i=0;i<c;i++){
            //sorted 배열의 one word 길이정보 4byte 변환후 bytesorted에 저장
            System.arraycopy(intto4byte(sorted[i].length()),0,bytesorted,pos,intto4byte(sorted[i].length()).length);
            pos+=4;//4byte 길이정보 저장했으므로 pos +4
            byte[] onewordbyte=sorted[i].getBytes();//one word byte 전환
            //one word data를 길이정보 뒤에 저장
            System.arraycopy(onewordbyte,0,bytesorted,pos,onewordbyte.length);
            pos+=onewordbyte.length;//one word 길이만큼 pos +
        }
        System.arraycopy(bytesorted, 0, packet, LEN_PROTOCOL_TYPE, bytesorted.length);//bytesorted array를 packet type 정보뒤에 복사
        packet[LEN_PROTOCOL_TYPE + bytesorted.length-1] = '\0';//패킷 끝 나타냄
    }
    public  int byteArrayToInt(byte bytes[]) {//4byte 배열을 int 값으로 전환해주는 메소드
        return ((((int)bytes[0] & 0xff) << 24) |
                (((int)bytes[1] & 0xff) << 16) |
                (((int)bytes[2] & 0xff) << 8) |
                (((int)bytes[3] & 0xff)));
    }


    // 정렬한 결과 값을 프로토콜패킷으로부터 추출하여 문자열로 리턴(클라이언트)
    public String[] getsortResult() {

        int wordcount=byteArrayToInt(Arrays.copyOfRange(packet, 1, 5)) ;//읽을 단어수
        String[] results=new String[wordcount];//패킷에서 추출한 단어 저장할 string배열
        int pos=6;//패킷 타입 0위치, 문자 개수 정보위치 1~5 따라서 실제 word정보는 6번째 위치부터
        //단어길이읽고 읽은만큼 byte읽어서 그 string 을 results에 저장
        for(int i=0;i<wordcount;i++){
            //단어길이정보 추출
            int wordlength=byteArrayToInt(Arrays.copyOfRange(getPacket(), pos, pos+4));

            pos+=4;//4byte읽었으므로 pos 4증가
            byte[] word = Arrays.copyOfRange(getPacket(), pos, pos+wordlength);//길이만큼읽어 단어추출
            String onestr = new String(word);//추출 byte string으로 전환

            results[i]=onestr;//results에 word 저장
            pos+=wordlength;//word 길이만큼읽었으므로 pos +word 길이
        }

        return results;//추출한 String 문자열 반환
    }


}
