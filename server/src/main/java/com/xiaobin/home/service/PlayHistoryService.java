package com.xiaobin.home.service;

import com.xiaobin.home.entity.PlayHistory;
import com.xiaobin.home.repository.PlayHistoryDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PlayHistoryService {

    private static final Map<Long, Object> historyMap = new ConcurrentHashMap<>();

    @Autowired
    private PlayHistoryDao playHistoryDao;

    public void replaceHistory(PlayHistory playHistory) {
        Object object = historyMap.computeIfAbsent(playHistory.getFoldId(), id -> new Object());
        synchronized (object) {
            PlayHistory history = this.playHistoryDao.findOneByFoldIdAndUserIdAndStatus(playHistory.getFoldId(), playHistory.getUserId(), (short) 0);
            if (history != null) {
                playHistory.setId(history.getId());
            }
            this.playHistoryDao.save(playHistory);
        }
    }
}
