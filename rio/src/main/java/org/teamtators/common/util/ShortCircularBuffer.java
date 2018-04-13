package org.teamtators.common.util;

import com.google.common.base.Preconditions;

import java.util.AbstractQueue;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Alex Mikhalev
 */
public class ShortCircularBuffer extends AbstractQueue<Short> {
    short[] buffer;
    int start;
    int size;

    public ShortCircularBuffer(int capacity) {
        Preconditions.checkArgument(capacity >= 0, "capacity must be a positive integer");
        buffer = new short[capacity];
        start = 0;
        size = 0;
    }

    public void setCapacity(int newCapacity) {
        if (size > newCapacity) {
            start = (start + (size - newCapacity)) % buffer.length;
            size = newCapacity;
        }
        short[] newBuffer = new short[newCapacity];
        int n = Math.min(buffer.length - start, size);
        System.arraycopy(buffer, start, newBuffer, 0, n);
        int n2 = Math.max(0, start + size - buffer.length);
        System.arraycopy(buffer, 0, newBuffer, n, n2);
        buffer = newBuffer;
        start = 0;
    }

    public int getCapacity() {
        return buffer.length;
    }

    @Override
    public Iterator<Short> iterator() {
        return new SCBIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public boolean push(short value) {
        int idx = (start + size) % buffer.length;
        buffer[idx] = value;
        if (size >= buffer.length) {
            size = buffer.length;
            start++;
            if (start >= buffer.length) {
                start = 0;
            }
            return false;
        }
        size++;
        return true;
    }

    public short pop() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        short value = buffer[start];
        if (++start >= buffer.length) {
            start = 0;
        }
        size--;
        return value;
    }

    @Override
    public boolean offer(Short value) {
        return push(value);
    }

    @Override
    public Short poll() {
        if (isEmpty()) {
            return null;
        }
        return pop();
    }

    @Override
    public Short peek() {
        if (isEmpty()) {
            return null;
        }
        return buffer[start];
    }

    private class SCBIterator implements Iterator<Short> {
        int previous = -1;
        int current = 0;

        @Override
        public boolean hasNext() {
            return current < size;
        }

        @Override
        public Short next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            short value = buffer[(start + current) % buffer.length];
            previous = current++;
            return value;
        }

        @Override
        public void remove() {
            if (previous == -1) {
                throw new IllegalStateException();
            }
            if (isEmpty()) {
                throw new ConcurrentModificationException();
            }
            if (previous == 0) {
                start++;
                if (start >= buffer.length) {
                    start = 0;
                }
                size--;
                current--;
            } else if (previous == size - 1) {
                size--;
                current--;
                previous--;
            } else {
                throw new UnsupportedOperationException("can not remove from middle of circular buffer");
            }
        }
    }
}
