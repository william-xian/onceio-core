package top.onceio.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class ElementBuffer<E> {
    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private int size = 10;

    private Lock readLock = rwl.readLock();
    private Lock writeLock = rwl.writeLock();
    private List<E> buffer;
    private Consumer<List<E>> consumer;

    public ElementBuffer(int size, Consumer<List<E>> consumer) {
        this.size = size;
        this.buffer = new ArrayList<>(size);
        this.consumer = consumer;
    }

    public void append(E element) {
        writeLock.lock();
        try {
            if (buffer.size() >= this.size) {
                flush();
            }
            buffer.add(element);
        } catch (Exception e) {
        } finally {
            writeLock.unlock();
        }
    }

    public void append(List<E> elements) {
        writeLock.lock();
        try {
            int subFirst = 0;
            List<E> arrayList = new ArrayList<>(elements);
            while ((buffer.size() + arrayList.size()) >= this.size) {
                subFirst = size - buffer.size();
                List<E> sub = arrayList.subList(0, subFirst);
                buffer.addAll(sub);
                arrayList = arrayList.subList(subFirst, arrayList.size());
                flush();
            }
            buffer.addAll(arrayList);
        } catch (Exception e) {
            e.printStackTrace();
            OLog.error(e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    public void flush() {
        readLock.lock();
        try {
            consumer.accept(buffer);
            buffer.clear();
        } catch (Exception e) {
            OLog.error(e.getMessage());
        } finally {
            readLock.unlock();
        }
    }
}
