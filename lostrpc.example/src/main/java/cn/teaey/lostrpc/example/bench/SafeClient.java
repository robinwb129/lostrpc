package cn.teaey.lostrpc.example.bench;

import cn.teaey.lostrpc.Connection;
import cn.teaey.lostrpc.Ctx;
import cn.teaey.lostrpc.Dispatcher;
import cn.teaey.lostrpc.client.NettyClient;
import cn.teaey.lostrpc.common.LostProto;
import cn.teaey.lostrpc.common.ProtobufInitializer;
import cn.teaey.lostrpc.common.Safety;
import cn.teaey.lostrpc.example.protobuf.TestProto;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaofei.wxf on 2014/3/19.
 */
public class SafeClient {
    private CountDownLatch cc;

    private long loop;

    private long everyLoop;
    private long totalNanos = 0;

    @Before
    public void setup() {
        loop = 10;
        everyLoop = 10 * 10000;
    }

    @After
    public void cleanup() {
        System.out.println();
        System.out.println("总共耗时: " + TimeUnit.NANOSECONDS.toMillis(totalNanos) + "ms");
        System.out.println("平均耗时: " + totalNanos / (loop * everyLoop) + "ns");
    }

    @Test
    public void loopNosafety() {
        Ctx ctx = new Ctx();
        NettyClient client = NettyClient.newInstance()
            .initializer(ProtobufInitializer.
                newInstance(Safety.NOT_SAFETY_CLIENT, ctx, LostProto.Packet.getDefaultInstance()))
            .dispatcher(new Dispatcher() {
                @Override
                public void dispatch(Connection o, Object p) {
                    //System.out.println(p);
                    cc.countDown();
                    System.out.print("\t\r" + ((LostProto.Packet) p).getPId());
                }
            }).connect(new InetSocketAddress(8888)).run();

        for (int j = 0; j < loop; j++) {
            final long s1 = System.nanoTime();
            for (int i = 0; i < everyLoop; i++) {
                cc = new CountDownLatch(1);
                long id = j * everyLoop + i + 1;
                client.ask(LostProto.Packet.newBuilder().setPId(id).setMethodName("login")
                    .setServiceName("com.taobao.teaey.lostrpc.LoginService")
                    .setTimestamp(System.currentTimeMillis()).setData(
                        TestProto.Login_C2S.newBuilder().setTimestamp(System.currentTimeMillis())
                            .build().toByteString()));
                try {
                    cc.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            final long s2 = System.nanoTime();
            totalNanos += (s2 - s1);
        }
        client.showdownNow();
    }

    @Test
    public void loopSafety() {
        Ctx ctx = new Ctx();
        NettyClient client = NettyClient.newInstance()
            .initializer(ProtobufInitializer.
                newInstance(Safety.newClientSafety(), ctx, LostProto.Packet.getDefaultInstance()))
            .dispatcher(new Dispatcher() {
                @Override
                public void dispatch(Connection o, Object p) {
                    System.out.printf("\t\r" + ((LostProto.Packet) p).getPId());
                    cc.countDown();
                }
            }).connect(new InetSocketAddress(8888)).run();

        client.handshake();
        for (int j = 0; j < loop; j++) {
            final long s1 = System.nanoTime();
            for (int i = 0; i < everyLoop; i++) {
                cc = new CountDownLatch(1);
                long id = j * everyLoop + i + 1;
                client.ask(LostProto.Packet.newBuilder().setPId(id).setMethodName("login")
                    .setServiceName("com.taobao.teaey.lostrpc.LoginService")
                    .setTimestamp(System.currentTimeMillis()).setData(
                        TestProto.Login_C2S.newBuilder().setTimestamp(System.currentTimeMillis())
                            .build().toByteString()));
                try {
                    cc.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            final long s2 = System.nanoTime();
            totalNanos += (s2 - s1);
        }
        client.showdownNow();
    }
}
