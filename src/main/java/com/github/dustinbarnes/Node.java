package com.github.dustinbarnes;

import org.jgroups.JChannel;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.locking.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

@SuppressWarnings("FieldCanBeLocal")
public class Node extends ReceiverAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    private AtomicBoolean isMaster = new AtomicBoolean(false);

    private Thread statusPrinter;
    private Thread acquiringThread;
    private LockService lockService;
    private JChannel channel;
    private Lock masterLock;

    public static void main(String[] args) throws Exception
    {
        Node node = new Node();
        node.start();
    }

    public Node() throws Exception
    {
        this.channel = new JChannel(Node.class.getClassLoader().getResourceAsStream("udp.xml"));
        this.channel.setReceiver(this);
        this.channel.connect("master-cluster");
        this.lockService = new LockService(this.channel);
    }

    public void start()
    {
        this.startAcquiringThread();
        this.startStatusPrintingThread();
    }

    @Override
    public void viewAccepted(View view)
    {
        LOG.info("Cluster event has happened!");
    }

    private void startAcquiringThread()
    {
        acquiringThread = new Thread()
        {
            @Override
            public void run()
            {
                Thread.currentThread().setName("acquire-lock");
                getLock();
            }
        };

        acquiringThread.setDaemon(true);
        acquiringThread.start();
    }

    private void getLock()
    {
        masterLock = lockService.getLock("master");
        masterLock.lock();

        isMaster.set(true);
        LOG.info("I have become the master!");
    }

    private void startStatusPrintingThread()
    {
        statusPrinter = new Thread() {
            @Override
            public void run()
            {
                Thread.currentThread().setName("status-printer");

                //noinspection InfiniteLoopStatement
                while ( true ) {
                    try
                    {
                        LOG.info("is master [" + isMaster + "]");
                        LOG.info("cluster view [" + channel.getViewAsString() + "]");
                        sleep(2000L);
                    }
                    catch ( InterruptedException e )
                    {
                        // If the sleep gets interrupted, that's fine.
                    }
                }
            }
        };

        statusPrinter.setDaemon(true);
        statusPrinter.start();
    }
}
