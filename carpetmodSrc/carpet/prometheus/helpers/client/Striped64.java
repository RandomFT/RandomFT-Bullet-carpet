package carpet.prometheus.helpers.client;

import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

abstract class Striped64 extends Number {
    static final ThreadLocal<int[]> threadHashCode = new ThreadLocal();
    static final Random rng = new Random();
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    transient volatile Cell[] cells;
    transient volatile long base;
    transient volatile int busy;
    private static final AtomicLongFieldUpdater<Striped64> CAS_BASE = AtomicLongFieldUpdater.newUpdater(Striped64.class, "base");
    private static final AtomicIntegerFieldUpdater<Striped64> CAS_BUSY = AtomicIntegerFieldUpdater.newUpdater(Striped64.class, "busy");

    Striped64() {
    }

    final boolean casBase(long cmp, long val) {
        return CAS_BASE.compareAndSet(this, cmp, val);
    }

    final boolean casBusy() {
        return CAS_BUSY.compareAndSet(this, 0, 1);
    }

    abstract long fn(long var1, long var3);

    final void retryUpdate(long x, int[] hc, boolean wasUncontended) {
        int h;
        if (hc == null) {
            threadHashCode.set(hc = new int[1]);
            int r = rng.nextInt();
            h = hc[0] = r == 0 ? 1 : r;
        }
        else {
            h = hc[0];
        }

        boolean collide = false;

        while (true) {
            Cell[] as;
            int n;
            long v;
            if ((as = this.cells) != null && (n = as.length) > 0) {
                Cell a;
                if ((a = as[n - 1 & h]) == null) {
                    if (this.busy == 0) {
                        Cell r = new Cell(x);
                        if (this.busy == 0 && this.casBusy()) {
                            boolean created = false;

                            try {
                                Cell[] rs;
                                int m;
                                int j;
                                if ((rs = this.cells) != null && (m = rs.length) > 0 && rs[j = m - 1 & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                this.busy = 0;
                            }

                            if (created) {
                                break;
                            }
                            continue;
                        }
                    }

                    collide = false;
                }
                else if (!wasUncontended) {
                    wasUncontended = true;
                }
                else {
                    if (a.cas(v = a.value, this.fn(v, x))) {
                        break;
                    }

                    if (n < NCPU && this.cells == as) {
                        if (!collide) {
                            collide = true;
                        }
                        else if (this.busy == 0 && this.casBusy()) {
                            try {
                                if (this.cells == as) {
                                   Cell[] rs = new Cell[n << 1];

                                    for (int i = 0; i < n; ++i) {
                                        rs[i] = as[i];
                                    }

                                    this.cells = rs;
                                }
                            } finally {
                                this.busy = 0;
                            }

                            collide = false;
                            continue;
                        }
                    }
                    else {
                        collide = false;
                    }
                }

                h ^= h << 13;
                h ^= h >>> 17;
                h ^= h << 5;
                hc[0] = h;
            }
            else if (this.busy == 0 && this.cells == as && this.casBusy()) {
                boolean init = false;

                try {
                    if (this.cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        this.cells = rs;
                        init = true;
                    }
                } finally {
                    this.busy = 0;
                }

                if (init) {
                    break;
                }
            }
            else if (this.casBase(v = this.base, this.fn(v, x))) {
                break;
            }
        }

    }

    final void internalReset(long initialValue) {
       Cell[] as = this.cells;
        this.base = initialValue;
        if (as != null) {
            int n = as.length;

            for (int i = 0; i < n; ++i) {
                Cell a = as[i];
                if (a != null) {
                    a.value = initialValue;
                }
            }
        }

    }

    static final class Cell {
        volatile long p0;
        volatile long p1;
        volatile long p2;
        volatile long p3;
        volatile long p4;
        volatile long p5;
        volatile long p6;
        volatile long value;
        volatile long q0;
        volatile long q1;
        volatile long q2;
        volatile long q3;
        volatile long q4;
        volatile long q5;
        volatile long q6;
        private static final AtomicLongFieldUpdater<Cell> CAS_VALUE = AtomicLongFieldUpdater.newUpdater(Cell.class, "value");

        Cell(long x) {
            this.value = x;
        }

        final boolean cas(long cmp, long val) {
            return CAS_VALUE.compareAndSet(this, cmp, val);
        }
    }
}
