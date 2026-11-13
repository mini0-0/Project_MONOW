package com.monow.batch.stock.minuteprice.job;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MinutePriceExecutionLock implements JobExecutionListener {
    private static final long LOCK_KEY = 0x4D4F4E4F5701L;
    private final DataSource dataSource;
    private final Map<Long, Connection> connections = new ConcurrentHashMap<>();

    public MinutePriceExecutionLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void beforeJob(JobExecution execution) {
        try {
            Connection connection = dataSource.getConnection();
            try {
                try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                    statement.setLong(1, LOCK_KEY);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || !result.getBoolean(1)) {
                            throw new IllegalStateException("다른 분봉 수집 Job이 실행 중입니다.");
                        }
                    }
                }
                connections.put(execution.getId(), connection);
            } catch (Exception exception) {
                connection.close();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("분봉 실행 잠금을 확보하지 못했습니다.", exception);
        }
    }

    @Override
    public void afterJob(JobExecution execution) {
        Connection connection = connections.remove(execution.getId());
        if (connection == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, LOCK_KEY);
            statement.execute();
        } catch (SQLException exception) {
            try {
                connection.abort(Runnable::run);
            } catch (SQLException abortException) {
                exception.addSuppressed(abortException);
            }
            throw new IllegalStateException("분봉 실행 잠금 해제 실패", exception);
        } finally {
            try {
                connection.close();
            } catch (SQLException exception) {
                throw new IllegalStateException("분봉 실행 잠금 연결 종료 실패", exception);
            }
        }
    }
}
