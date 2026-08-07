package com.pigeostudios.sbwchunkload.scheduler;

import com.pigeostudios.sbwchunkload.state.ProjectileState;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Бакет TimingWheel'а: состояния снарядов, которым пора обновиться
 * в этот тик. Один бакет обрабатывается за тик — полного перебора
 * всех трекаемых снарядов каждый тик не происходит.
 */
public final class UpdateBucket {

    private final Deque<ProjectileState> states = new ArrayDeque<>();

    public void add(ProjectileState state) {
        states.addLast(state);
    }

    public ProjectileState poll() {
        return states.pollFirst();
    }

    public int size() {
        return states.size();
    }
}
