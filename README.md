jgroups-master-example
======================

A very simple Maven project with JGroups that shows a dead-simple leader election mechanism through shared locks.

Because this is a sample project, it does take some liberties that are not advised for production. For example:

* The thread never willingly gives up the master lock
* There is no shutdown hook to cleanly release the lock
* Once the lock is gone, the process can never re-acquire the lock

These traits are not necessarily a bad thing. In the example I was working from, the only reason a process would give
up the lock is if it died unexpectedly, and that would mean re-starting the process anyway. Your requirements may vary.

In addition, we create several threads without explicitly stopping them. This is why the acquire thread and the status
printing thread are both marked as daemon. If the only threads left in the VM are daemon threads, the JVM will exit.

Running the Example
-------------------

First build the application by running

    mvn clean package

This creates the .jar file as well as copies the dependencies to target/lib

To run an instance, just type

    java -jar jgroups-master-example-1.0-SNAPSHOT

The first instance should become the master. Then, run additional processes in the same manner. Watch the console output
to see them harmonize on a consistent view of the cluster. Then, start killing the processes and watch them fail over to
a new master. Bring new instances back online, and watch them queue up for the master lock. It is normal for it to take a
couple seconds before switching to the next master. If you want to customize the udp.xml file for a lower timeout, feel
free, but you risk a lot of re-election if your network periodically gets slower than the timeout.

Caveat Emptor
-------------

I threw this code together to test if JGroups could work as an embedded, simple alternative to ZooKeeper. I have not tested
the performance or long-term reliability of this solution, but I see no reason why it shouldn't be fine.