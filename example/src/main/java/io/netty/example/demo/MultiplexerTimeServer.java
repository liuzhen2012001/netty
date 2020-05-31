package io.netty.example.demo;

import com.sun.xml.internal.ws.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import static com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolver.iterator;

public class MultiplexerTimeServer implements Runnable {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private volatile boolean stop;


    MultiplexerTimeServer(int port){
        try {
            //创建多路复用器selector ServerSocketChannel
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            //配置为非阻塞方式
            serverSocketChannel.configureBlocking(false);
            //绑定端口
            serverSocketChannel.socket().bind(new InetSocketAddress(port),1024);
            //把channel注册到Selecter上，监听SelectionKey.OP_ACCEPT
            serverSocketChannel.register(selector,SelectionKey.OP_ACCEPT);
            System.out.println("the time server is start in port :"+port);
        }catch (Exception e){
            System.out.printf(e+"");
        }
    }
    public void stop(){
        stop=true;
    }
    @Override
    public void run() {
        while (!stop){
            try {
                //循环体重遍历selector,超时时间为1s
                selector.select(1000);
                Set<SelectionKey>  selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it =  selectionKeys.iterator();
                SelectionKey key ;
                while (it.hasNext()){
                    key=it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    }catch (Exception e){
                        if(key!=null){
                            key.cancel();
                            key.channel().close();
                        }
                    }

                }
            }catch (Exception e){

                System.out.printf(e+"");
            }


        }
        if(selector!=null){
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if(!key.isValid()){
            return;
        }
        //新的连接
        if(key.isAcceptable()){
            ServerSocketChannel selectableChannel = (ServerSocketChannel) key.channel();
           SocketChannel sc =  selectableChannel.accept();
            sc.configureBlocking(false);
            sc.register(selector,SelectionKey.OP_READ);
        }
        else if(key.isReadable()){
           SocketChannel socketChannel = (SocketChannel) key.channel();
            ByteBuffer byteBuffer =ByteBuffer.allocate(1024);
             int readbytes = socketChannel.read(byteBuffer);
            if(readbytes >0){
                //读写转换
                byteBuffer.flip();
                byte[] bytes =new byte[byteBuffer.remaining()];

                byteBuffer.get(bytes);
                String body = new String(bytes,"utf-8");
                System.out.println("The time  server recevie order :"+body);

                String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)?new Date(System.currentTimeMillis()).toString():"BAD ORDER";
                doWrite(socketChannel,currentTime);
            }else  if(readbytes<0){
                key.cancel();
                socketChannel.close();
            }
        }
    }

    private void doWrite(SocketChannel socketChannel, String currentTime) throws IOException {
        if(currentTime!=null){
            byte[] bytes =currentTime.getBytes();
            ByteBuffer byteBuffer =ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
           int result =   socketChannel.write(byteBuffer);
            System.out.printf("发送："+currentTime+"-"+result);
        }
    }

    public static void main(String[] args) {
       Thread thread =new Thread(new MultiplexerTimeServer(12345));
       thread.start();
    }
}
