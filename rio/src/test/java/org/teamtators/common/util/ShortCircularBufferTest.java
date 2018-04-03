package org.teamtators.common.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.testng.Assert.*;

/**
 * @author Alex Mikhalev
 */
public class ShortCircularBufferTest {
    @Test
    public void testIterator() throws Exception {
        ShortCircularBuffer buf = new ShortCircularBuffer(10);
        for (int i = 0; i < 10; i++) {
            buf.push((short) (i * 2));
        }
        Iterator<Short> it = buf.iterator();
        int j = 0;
        while (it.hasNext()) {
            short k = it.next();
            Assert.assertEquals(k, j * 2, "iterator should return the same values as pushed");
            j++;
        }
        for (int i = 0; i < 10; i++) {
            buf.push((short) (i * 2 + 1));
        }
        j = 0;
        it = buf.iterator();
        while (it.hasNext()) {
            short k = it.next();
            Assert.assertEquals(k, j * 2 + 1, "iterator should return the same values as pushed");
            j++;
        }

        it = buf.iterator();
        j = 10;
        while (it.hasNext()) {
            it.next();
            it.remove();
            j--;
            Assert.assertEquals(buf.size(), j);
        }

        for (int i = 0; i < 10; i++) {
            buf.push((short) (i * 2 + 1));
        }
        it = buf.iterator();
        j = 10;
        while (it.hasNext()) {
            it.next();
        }
        while (!buf.isEmpty()) {
            it.remove();
            j--;
            Assert.assertEquals(buf.size(), j);
        }

        ShortCircularBuffer buf2 = new ShortCircularBuffer(10);
        assertThrows(NoSuchElementException.class, () -> {
            buf2.iterator().next();
        });
        assertThrows(IllegalStateException.class, () -> {
            buf2.iterator().remove();
        });
        assertThrows(ConcurrentModificationException.class, () -> {
            buf2.push((short) 0);
            Iterator<Short> it2 = buf2.iterator();
            it2.next();
            buf2.pop();
            it2.remove();
        });
    }

    @Test
    public void testSizeAndIsEmpty() throws Exception {
        ShortCircularBuffer buf = new ShortCircularBuffer(10);
        assertEquals(buf.getCapacity(), 10);
        assertEquals(buf.start, 0);
        assertEquals(buf.size(), 0);
        assertTrue(buf.isEmpty());
        boolean wasRoom = buf.push((short) 0);
        assertTrue(wasRoom);
        assertEquals(buf.start, 0);
        assertEquals(buf.size(), 1);
        assertFalse(buf.isEmpty());
        for (int i = 1; i <= 9; i++) {
            wasRoom = buf.push((short) i);
            assertTrue(wasRoom);
            assertEquals(buf.start, 0);
            assertEquals(buf.size(), i + 1);
            assertFalse(buf.isEmpty());
        }
        for (int i = 1; i <= 9; i++) {
            wasRoom = buf.push((short) i);
            assertFalse(wasRoom);
            assertEquals(buf.start, i);
            assertEquals(buf.size(), 10);
            assertFalse(buf.isEmpty());
        }
    }

    @Test
    public void testPop() throws Exception {
        ShortCircularBuffer buf = new ShortCircularBuffer(10);
        assertThrows(NoSuchElementException.class, buf::pop);
        assertNull(buf.peek());
        buf.push((short) 99);
        assertEquals(buf.pop(), 99);
        for (int i = 0; i < 10; i++) {
            buf.offer((short) (i + 100));
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(buf.peek(), new Short((short) (i + 100)));
            assertEquals(buf.pop(), i + 100);
        }
    }

    @Test
    public void testPoll() throws Exception {
        ShortCircularBuffer buf = new ShortCircularBuffer(10);
        assertNull(buf.poll());
        buf.push((short) 0);
        assertEquals(buf.poll(), new Short((short) 0));
    }

    @Test
    public void testSetCapacity() throws Exception {
        ShortCircularBuffer buf = new ShortCircularBuffer(10);
        for (int i = 0; i < 10; i++) {
            buf.push((short) (i + 100));
        }
        assertEquals(buf.size, 10);
        buf.setCapacity(20);
        assertEquals(buf.getCapacity(), 20);
        assertEquals(buf.size, 10);
        for (int i = 0; i < 10; i++) {
            assertEquals(buf.pop(), i + 100);
        }
        assertTrue(buf.isEmpty());
        buf.setCapacity(10);
        assertEquals(buf.getCapacity(), 10);
        for (int i = 0; i < 17; i++) {
            buf.push((short) (i + 100));
        }
        buf.setCapacity(5);
        assertEquals(buf.getCapacity(), 5);
        assertEquals(buf.size, 5);
        for (int i = 12; i < 17; i++) {
            assertEquals(buf.pop(), i + 100);
        }
        buf.setCapacity(0);
        assertEquals(buf.size, 0);
        assertNull(buf.poll());
    }
}