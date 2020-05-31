package io.netty.example.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeClient  implements Runnable{

    private int port;
    private String host;

    private Selector selector;
    private SocketChannel socketChannel;

    boolean stop;
    MultiplexerTimeClient(String host,int port){
        this.host=host;
        this.port=port;

        try {
            selector = Selector.open();
            socketChannel =SocketChannel.open();
            socketChannel.configureBlocking(false);
        }catch (Exception e){
            System.out.println(e+"");
        }
    }

    @Override
    public void run() {

        try {
            doConnect();
        }catch (Exception e){
            e.printStackTrace();
        }
        while (!stop){
            try {
                selector.select(1000);
               Set<SelectionKey> selectionKeySet =  selector.selectedKeys();
             Iterator<SelectionKey> iterable = selectionKeySet.iterator();
                SelectionKey key ;
             while (iterable.hasNext()){
                 key=iterable.next();
                 iterable.remove();
                 handleInpit(key);
             }
            }catch (Exception e){
                    e.printStackTrace();
            }
        }

    }

    private void handleInpit(SelectionKey key) throws IOException {
        if(!key.isValid()){
            return;
        }
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if(key.isConnectable()) {
            if (socketChannel.finishConnect()) {
                socketChannel.register(selector, SelectionKey.OP_READ);
                doWrite(socketChannel);
            }
        }
            else if(key.isReadable()){
                ByteBuffer byteBuffer =ByteBuffer.allocate(1024);
                int readBytes =  socketChannel.read(byteBuffer);
                if(readBytes>0) {
                    byteBuffer.flip();
                    byte[] bytes = new byte[byteBuffer.remaining()];

                    byteBuffer.get(bytes);
                    String body = new String(bytes, "utf-8");
                    System.out.println(body);

                    stop=true;
                }else if(readBytes<0) {
                    socketChannel.close();
                    key.cancel();
                }
            }
        }

    private void doConnect() throws IOException {
        if(socketChannel.connect(new InetSocketAddress(host,port))){
            socketChannel.register(selector,SelectionKey.OP_READ);
            doWrite(socketChannel);
        }else {
            socketChannel.register(selector,SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel socketChannel) throws IOException {
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer byteBuffer =ByteBuffer.allocate(req.length);
        byteBuffer.put(req);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        if (!byteBuffer.hasRemaining()){
            System.out.println("Send order 2 server succeed.");
    }
    }

    public static void main(String[] args) {
        MultiplexerTimeClient multiplexerTimeClient =new MultiplexerTimeClient("127.0.0.1",12345);
        Thread thread =new Thread(multiplexerTimeClient);
        thread.start();

    }
}

